"""Document format conversion — PDF / Word / Image → List[np.ndarray] (BGR).

All downstream AI pipeline stages expect numpy BGR image arrays. This module
provides a unified converter that routes to the correct strategy based on file type.
"""

import io
import logging
import os
import shutil
import subprocess
import tempfile
from typing import List, Tuple

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

logger = logging.getLogger(__name__)


class DocumentConverter:
    """Convert uploaded document bytes into a list of page images (BGR numpy arrays).

    Supported types (mapped from Java fileType field):
      - "image" (jpg, jpeg, png, bmp, tiff, webp)  → pass-through via PIL
      - "pdf"                                       → render each page via PyMuPDF
      - "word" (doc, docx)                          → LibreOffice → PDF, or python-docx fallback
    """

    DEFAULT_DPI = 200
    # Approximate lines per page for python-docx text-only fallback
    LINES_PER_PAGE = 40

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def convert(
        self,
        file_bytes: bytes,
        file_type: str,
        filename: str = "",
    ) -> Tuple[List[np.ndarray], dict]:
        """Convert file bytes to a list of BGR page images.

        Returns:
            (pages, metadata) where pages is List[np.ndarray] (BGR, one per page)
            and metadata carries page_count, original_type, converter_used, warnings.
        """
        ft = (file_type or "").lower()
        warnings: List[str] = []

        if ft in ("image", "jpg", "jpeg", "png", "bmp", "tiff", "tif", "webp"):
            pages, converter = self._convert_image(file_bytes)
        elif ft == "pdf":
            pages, converter = self._convert_pdf(file_bytes)
            if len(pages) == 0:
                # Some image-based PDFs may fail with PyMuPDF — try PIL fallback
                logger.info("PyMuPDF returned 0 pages; attempting PIL fallback")
                pages, converter = self._convert_image(file_bytes)
                if len(pages) > 0:
                    warnings.append("PDF rendered as single image (PyMuPDF could not parse pages)")
        elif ft in ("word", "doc", "docx"):
            pages, converter, w = self._convert_word(file_bytes, filename)
            warnings.extend(w)
        else:
            # Unknown type — try as image anyway
            logger.warning("Unknown file_type=%s; falling back to PIL image reader", ft)
            pages, converter = self._convert_image(file_bytes)
            if len(pages) == 0:
                raise ValueError(f"Unsupported file type '{ft}' and PIL could not read it as an image")
            warnings.append(f"Unknown file type '{ft}' treated as image")

        metadata = {
            "page_count": len(pages),
            "original_type": ft,
            "converter_used": converter,
            "warnings": warnings,
        }
        logger.info(
            "DocumentConverter: %d page(s)  type=%s  converter=%s  warnings=%d",
            len(pages), ft, converter, len(warnings),
        )
        return pages, metadata

    # ------------------------------------------------------------------
    # Image (pass-through)
    # ------------------------------------------------------------------

    @staticmethod
    def _convert_image(file_bytes: bytes) -> Tuple[List[np.ndarray], str]:
        """Read image bytes via PIL → single-page list."""
        pil_image = Image.open(io.BytesIO(file_bytes)).convert("RGB")
        bgr = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
        return [bgr], "pil"

    # ------------------------------------------------------------------
    # PDF → pages via PyMuPDF (fitz)
    # ------------------------------------------------------------------

    @staticmethod
    def _convert_pdf(file_bytes: bytes) -> Tuple[List[np.ndarray], str]:
        """Render each PDF page to a BGR numpy array at DEFAULT_DPI."""
        import fitz  # PyMuPDF — lazy import so the module loads even without it

        doc = fitz.open(stream=file_bytes, filetype="pdf")
        pages: List[np.ndarray] = []

        zoom = DocumentConverter.DEFAULT_DPI / 72.0  # PDF points → pixels
        matrix = fitz.Matrix(zoom, zoom)

        for i in range(doc.page_count):
            page = doc.load_page(i)
            pix = page.get_pixmap(matrix=matrix)
            # pix.samples is raw RGBA bytes
            img = Image.frombytes("RGBA", (pix.width, pix.height), pix.samples)
            rgb = img.convert("RGB")
            bgr = cv2.cvtColor(np.array(rgb), cv2.COLOR_RGB2BGR)
            pages.append(bgr)

        doc.close()
        return pages, "pymupdf"

    # ------------------------------------------------------------------
    # Word → pages (LibreOffice preferred, python-docx fallback)
    # ------------------------------------------------------------------

    def _convert_word(
        self, file_bytes: bytes, filename: str
    ) -> Tuple[List[np.ndarray], str, List[str]]:
        """Convert Word document to page images.

        Tier 1: LibreOffice headless → PDF → PyMuPDF render.
        Tier 2: python-docx → Pillow text rendering (lower fidelity).
        """
        warnings: List[str] = []

        # --- Tier 1: LibreOffice ---
        soffice = shutil.which("soffice") or shutil.which("soffice.exe")
        if soffice:
            try:
                pages = self._word_via_libreoffice(file_bytes, soffice)
                return pages, "libreoffice+pymupdf", warnings
            except Exception as exc:
                logger.warning("LibreOffice conversion failed: %s", exc)
                warnings.append(f"LibreOffice failed ({exc}); falling back to python-docx")

        # --- Tier 2: python-docx text rendering ---
        try:
            pages = self._word_via_python_docx(file_bytes)
            warnings.append("Word rendered as text-only images (python-docx fallback) — layout fidelity reduced")
            return pages, "python-docx+pillow", warnings
        except Exception as exc:
            logger.error("python-docx fallback also failed: %s", exc)
            raise ValueError(f"Cannot convert Word document: {exc}") from exc

    def _word_via_libreoffice(self, file_bytes: bytes, soffice_path: str) -> List[np.ndarray]:
        """Convert Word → PDF using LibreOffice headless, then render pages."""
        with tempfile.TemporaryDirectory() as tmpdir:
            docx_path = os.path.join(tmpdir, "input.docx")
            with open(docx_path, "wb") as f:
                f.write(file_bytes)

            result = subprocess.run(
                [soffice_path, "--headless", "--convert-to", "pdf", "--outdir", tmpdir, docx_path],
                capture_output=True, text=True, timeout=60,
            )
            if result.returncode != 0:
                raise RuntimeError(f"LibreOffice exit code {result.returncode}: {result.stderr}")

            pdf_path = os.path.join(tmpdir, "input.pdf")
            if not os.path.exists(pdf_path):
                raise FileNotFoundError(f"LibreOffice did not produce {pdf_path}")

            with open(pdf_path, "rb") as f:
                pdf_bytes = f.read()

            pages, _ = self._convert_pdf(pdf_bytes)
            return pages

    @staticmethod
    def _word_via_python_docx(file_bytes: bytes) -> List[np.ndarray]:
        """Render Word paragraphs as pill images (text-only, no layout)."""
        import docx

        doc = docx.Document(io.BytesIO(file_bytes))
        paragraphs = [p.text for p in doc.paragraphs if p.text.strip()]

        if not paragraphs:
            # Empty doc or all-empty paragraphs — produce a single blank page
            blank = np.ones((1200, 900, 3), dtype=np.uint8) * 255
            return [blank]

        # Split paragraphs into pages
        per_page = DocumentConverter.LINES_PER_PAGE
        page_chunks = [paragraphs[i:i + per_page] for i in range(0, len(paragraphs), per_page)]

        pages: List[np.ndarray] = []
        for chunk in page_chunks:
            pages.append(DocumentConverter._render_text_page(chunk))
        return pages

    @staticmethod
    def _render_text_page(lines: List[str], width: int = 1200,
                          line_height: int = 28, margin: int = 40) -> np.ndarray:
        """Render a list of text lines onto a white Pillow image, return as BGR numpy."""
        height = margin * 2 + len(lines) * line_height
        img = Image.new("RGB", (width, max(height, 200)), color=(255, 255, 255))
        draw = ImageDraw.Draw(img)

        # Try to use a CJK-capable font; fall back to default
        try:
            font = ImageFont.truetype("simhei.ttf", 16)  # Windows 黑体
        except (OSError, IOError):
            try:
                font = ImageFont.truetype("msyh.ttf", 16)  # 微软雅黑
            except (OSError, IOError):
                font = ImageFont.load_default()

        y = margin
        for line in lines:
            # Truncate long lines
            display = line[:120] if len(line) > 120 else line
            draw.text((margin, y), display, fill=(0, 0, 0), font=font)
            y += line_height

        return cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)


# Singleton
document_converter = DocumentConverter()
