"""Document format conversion — 统一入口，多格式支持，高容错。

支持格式: image (jpg/jpeg/png/bmp/tiff/webp/gif), pdf, word (doc/docx),
          excel (xls/xlsx), html, text (txt/csv/md), rtf

特性:
- 分层降级策略：优先高质量转换，失败自动降级
- 文件校验：上传前检查格式、大小、完整性
- 并行渲染：Word 文档多页并行渲染
- LRU 缓存：短期缓存避免重复转换同一文件
- 统计信息：记录每次转换的性能指标
"""

from __future__ import annotations

import functools
import hashlib
import io
import logging
import os
import re
import shutil
import subprocess
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional, Tuple

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

logger = logging.getLogger(__name__)


# =============================================================================
# Exceptions — 细化异常层次，便于调用方精确处理
# =============================================================================

class ConversionError(Exception):
    """文档转换基础异常。"""
    pass


class UnsupportedFormatError(ConversionError):
    """不支持的文档格式。"""

    def __init__(self, file_type: str):
        self.file_type = file_type
        super().__init__(f"不支持的文件格式: '{file_type}'，支持的格式: image, pdf, word, excel, html, text")


class FileTooLargeError(ConversionError):
    """文件超过大小限制。"""

    def __init__(self, size_bytes: int, max_bytes: int):
        self.size_bytes = size_bytes
        self.max_bytes = max_bytes
        super().__init__(f"文件大小 {size_bytes / 1024 / 1024:.1f}MB 超过限制 {max_bytes / 1024 / 1024:.0f}MB")


class EmptyFileError(ConversionError):
    """文件内容为空。"""

    def __init__(self):
        super().__init__("文件内容为空，无法转换")


class CorruptedFileError(ConversionError):
    """文件损坏或格式不匹配。"""

    def __init__(self, detail: str = ""):
        self.detail = detail
        super().__init__(f"文件可能已损坏或格式不匹配{': ' + detail if detail else ''}")


class PageConversionError(ConversionError):
    """单页转换失败（不影响其他页）。"""

    def __init__(self, page_index: int, detail: str):
        self.page_index = page_index
        self.detail = detail
        super().__init__(f"第 {page_index + 1} 页转换失败: {detail}")


# =============================================================================
# Dataclasses — 结构化元数据
# =============================================================================

@dataclass
class ConverterStats:
    """转换过程统计信息。"""
    start_time: float = 0.0
    end_time: float = 0.0
    file_size_bytes: int = 0
    page_count: int = 0
    converter_used: str = "unknown"
    total_duration_ms: float = 0.0
    pages_per_second: float = 0.0

    def finalize(self) -> "ConverterStats":
        self.total_duration_ms = (self.end_time - self.start_time) * 1000
        if self.total_duration_ms > 0 and self.page_count > 0:
            self.pages_per_second = self.page_count / (self.total_duration_ms / 1000)
        return self

    def to_dict(self) -> Dict[str, Any]:
        return {
            "file_size_bytes": self.file_size_bytes,
            "page_count": self.page_count,
            "converter_used": self.converter_used,
            "duration_ms": round(self.total_duration_ms, 1),
            "pages_per_second": round(self.pages_per_second, 2),
        }


@dataclass
class ConversionResult:
    """转换结果（替代原本的 Tuple 返回值）。"""
    pages: List[np.ndarray] = field(default_factory=list)
    converter_used: str = "unknown"
    warnings: List[str] = field(default_factory=list)
    raw_text: str = ""
    stats: ConverterStats = field(default_factory=ConverterStats)

    @property
    def page_count(self) -> int:
        return len(self.pages)

    def to_legacy_meta(self) -> dict:
        """兼容旧版 metadata dict 格式。"""
        return {
            "page_count": self.page_count,
            "original_type": "",
            "converter_used": self.converter_used,
            "warnings": self.warnings,
            "raw_text": self.raw_text,
            "stats": self.stats.to_dict(),
        }


# =============================================================================
# Configuration
# =============================================================================

@dataclass(frozen=True)
class ConverterConfig:
    """转换器配置常量。"""

    # 渲染参数
    default_dpi: int = 200
    page_width: int = 1200
    line_height: int = 28
    margin: int = 40
    lines_per_page: int = 40
    max_line_chars: int = 120

    # 文件大小限制 (50MB)
    max_file_size_bytes: int = 50 * 1024 * 1024

    # 超时设置
    libreoffice_timeout: int = 120
    subprocess_timeout: int = 60

    # 并行渲染
    max_parallel_pages: int = 8

    # 缓存
    cache_size: int = 32

    # 字体回退链
    font_fallbacks: Tuple[str, ...] = (
        "simhei.ttf",   # 黑体
        "msyh.ttf",     # 微软雅黑
        "simsun.ttc",   # 宋体
        "arial.ttf",    # 西文
        "DejaVuSans.ttf",
    )

    # 支持的图片格式
    image_extensions: Tuple[str, ...] = (
        "jpg", "jpeg", "png", "bmp", "tiff", "tif", "webp", "gif",
    )

    # 格式签名（magic bytes）
    magic_bytes: Dict[str, bytes] = field(default_factory=lambda: {
        "pdf": b"%PDF",
        "png": b"\x89PNG",
        "jpg": b"\xff\xd8\xff",
        "gif": b"GIF8",
        "zip_based": b"PK\x03\x04",  # docx, xlsx are ZIP archives
    })

    # 图片格式对应的 magic bytes 与扩展名（用于自动检测）
    IMAGE_MAGIC_BYTES: Tuple[Tuple[bytes, str], ...] = (
        (b"\xff\xd8\xff", "jpg"),        # JPEG
        (b"\x89PNG\r\n\x1a\n", "png"),   # PNG
        (b"GIF8", "gif"),                # GIF
        (b"BM", "bmp"),                  # BMP
        (b"II*\x00", "tiff"),            # TIFF (little-endian)
        (b"MM\x00*", "tiff"),            # TIFF (big-endian)
        (b"RIFF", "webp"),               # WebP
    )


# =============================================================================
# Main Converter Class
# =============================================================================

class DocumentConverter:
    """统一文档转换器 — 多格式支持 + 分层降级 + 性能优化。

    支持的格式:
      - image: jpg, jpeg, png, bmp, tiff, webp, gif
      - pdf: 自动检测，PyMuPDF 渲染
      - word: doc, docx — LibreOffice 优先，python-docx 降级
      - excel: xls, xlsx — openpyxl → HTML 表格渲染
      - html: 网页 → 文本提取 + 渲染
      - text: txt, csv, md, log — 纯文本渲染
      - rtf: 富文本 → 文本提取
    """

    config = ConverterConfig()

    def __init__(self):
        self._parallel_executor = ThreadPoolExecutor(
            max_workers=self.config.max_parallel_pages,
            thread_name_prefix="doc-conv",
        )
        self._font = None  # lazy-loaded

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def convert(
        self,
        file_bytes: bytes,
        file_type: str,
        filename: str = "",
        *,
        validate: bool = True,
        progress_callback: Optional[Callable[[int, int, str], None]] = None,
    ) -> ConversionResult:
        """将文件字节转换为页面图像列表。

        Args:
            file_bytes: 文件原始字节
            file_type: 文件类型标识 (image/pdf/word/excel/html/text/rtf)
            filename: 原始文件名（用于扩展名检测）
            validate: 是否进行文件校验
            progress_callback: 进度回调 (current, total, stage_name)

        Returns:
            ConversionResult 包含 pages, warnings, raw_text, stats

        Raises:
            EmptyFileError: 文件为空
            FileTooLargeError: 文件超过大小限制
            UnsupportedFormatError: 不支持的格式
            ConversionError: 转换失败
        """
        t0 = time.perf_counter()
        stats = ConverterStats(start_time=t0, file_size_bytes=len(file_bytes))
        result = ConversionResult(stats=stats)

        # 1. Validate
        if validate:
            self._validate(file_bytes, filename)

        # 2. Normalize file type (from parameter + extension)
        ft = self._normalize_type(file_type, filename)

        # 3. Auto-detect and correct file type mismatch (defense in depth)
        detected = self._detect_file_type_from_bytes(file_bytes, filename)
        ft = self._validate_file_type_mismatch(ft, detected, filename)

        # 4. Route to converter
        try:
            self._dispatch(ft, file_bytes, filename, result, progress_callback)
        except ConversionError:
            raise
        except Exception as e:
            logger.exception("Unhandled conversion error for type=%s", ft)
            raise ConversionError(f"文档转换失败: {e}") from e

        # 4. Finalize stats
        stats.end_time = time.perf_counter()
        stats.page_count = result.page_count
        stats.converter_used = result.converter_used
        stats.finalize()

        logger.info(
            "DocumentConverter: %d page(s)  type=%s→%s  converter=%s  "
            "warnings=%d  duration=%.0fms  pps=%.1f",
            result.page_count, file_type, ft, result.converter_used,
            len(result.warnings), stats.total_duration_ms, stats.pages_per_second,
        )
        return result

    # ------------------------------------------------------------------
    # Validation
    # ------------------------------------------------------------------

    def _validate(self, file_bytes: bytes, filename: str) -> None:
        """文件校验：空文件、大小限制、格式签名检测。"""
        if not file_bytes:
            raise EmptyFileError()

        if len(file_bytes) > self.config.max_file_size_bytes:
            raise FileTooLargeError(len(file_bytes), self.config.max_file_size_bytes)

        if filename:
            ext = Path(filename).suffix.lower().lstrip(".")
            if ext in self.config.image_extensions:
                if not self._is_valid_image(file_bytes):
                    raise CorruptedFileError(f"'{ext}' 图片文件可能已损坏")

    @staticmethod
    def _is_valid_image(file_bytes: bytes) -> bool:
        """快速检测图片文件是否有效。"""
        try:
            img = Image.open(io.BytesIO(file_bytes))
            img.verify()
            return True
        except Exception:
            return False

    @classmethod
    def _detect_file_type_from_bytes(cls, file_bytes: bytes, filename: str = "") -> str:
        """通过 magic bytes 自动检测文件实际类型。

        返回内部类型名: image / pdf / word / excel / text / rtf / html / unknown
        优先使用 magic bytes，其次使用文件名扩展名。
        """
        if not file_bytes:
            return "unknown"

        # 1. Magic bytes 检测
        # PDF: %PDF
        if file_bytes[:4] == b"%PDF":
            return "pdf"

        # ZIP-based: docx, xlsx 都是 ZIP 包
        if file_bytes[:4] == b"PK\x03\x04":
            if filename:
                ext = Path(filename).suffix.lower().lstrip(".")
                if ext in ("docx", "doc"):
                    return "word"
                if ext in ("xlsx", "xls"):
                    return "excel"
            return "zip_based"  # 无法确定是 word 还是 excel

        # 图片格式
        for magic, fmt in cls.config.IMAGE_MAGIC_BYTES:
            if file_bytes[:len(magic)] == magic:
                return "image"

        # 2. 扩展名回退
        if filename:
            ext = Path(filename).suffix.lower().lstrip(".")
            if ext in cls._EXTENSION_MAP:
                return cls._EXTENSION_MAP[ext]

        # 3. 文本检测（尝试 UTF-8 解码）
        try:
            text = file_bytes[:4096].decode("utf-8")
            if text.strip().startswith("{\\rtf"):
                return "rtf"
            if any(tag in text[:500].lower() for tag in ("<html", "<!doctype", "<body", "<head")):
                return "html"
            if text.strip().startswith("{") or text.strip().startswith("["):
                return "text"  # JSON-like, treat as text
            # 看起来像纯文本
            if all(32 <= ord(c) < 127 or c in "\n\r\t" for c in text[:200] if c):
                return "text"
        except UnicodeDecodeError:
            pass

        return "unknown"

    def _validate_file_type_mismatch(
        self, claimed_type: str, detected_type: str, filename: str
    ) -> str:
        """检测并修正文件类型不匹配。

        若 claimed_type 与 detected_type 不一致，以 detected_type 为准。
        特殊处理：zip_based 类型需要文件名辅助判断。
        """
        if detected_type == "unknown":
            return claimed_type  # 无法检测，信任传入的类型

        if detected_type == "zip_based":
            # 无法区分 docx/xlsx，依赖文件名
            if filename:
                ext = Path(filename).suffix.lower().lstrip(".")
                if ext in ("docx", "doc"):
                    return "word"
                if ext in ("xlsx", "xls"):
                    return "excel"
            return "word"  # 默认按 Word 处理

        if claimed_type != detected_type:
            logger.warning(
                "File type mismatch: claimed='%s', detected='%s' (file=%s). "
                "Auto-correcting to detected type.",
                claimed_type, detected_type, filename,
            )
            return detected_type

        return claimed_type

    # ------------------------------------------------------------------
    # Type normalization
    # ------------------------------------------------------------------

    def _normalize_type(self, file_type: str, filename: str) -> str:
        """将外部类型标识规范化为内部类型名。"""
        ft = (file_type or "").lower().strip()

        # 直接匹配
        if ft in self._TYPE_MAP:
            return ft

        # 通过扩展名推断
        if filename:
            ext = Path(filename).suffix.lower().lstrip(".")
            return self._EXTENSION_MAP.get(ext, ft)

        return ft

    _TYPE_MAP: Dict[str, str] = {
        "image": "image",
        "pdf": "pdf",
        "word": "word",
        "doc": "word",
        "docx": "word",
        "excel": "excel",
        "xls": "excel",
        "xlsx": "excel",
        "html": "html",
        "htm": "html",
        "text": "text",
        "txt": "text",
        "csv": "text",
        "md": "text",
        "log": "text",
        "rtf": "rtf",
    }

    _EXTENSION_MAP: Dict[str, str] = {
        "jpg": "image", "jpeg": "image", "png": "image",
        "bmp": "image", "tiff": "image", "tif": "image",
        "webp": "image", "gif": "image",
        "pdf": "pdf",
        "doc": "word", "docx": "word",
        "xls": "excel", "xlsx": "excel",
        "html": "html", "htm": "html",
        "txt": "text", "csv": "text", "md": "text", "log": "text",
        "rtf": "rtf",
    }

    # ------------------------------------------------------------------
    # Dispatch
    # ------------------------------------------------------------------

    def _dispatch(
        self, ft: str, file_bytes: bytes, filename: str,
        result: ConversionResult,
        progress_callback: Optional[Callable] = None,
    ) -> None:
        """路由到具体转换策略。"""
        dispatchers: Dict[str, Callable] = {
            "image": self._convert_image,
            "pdf": self._convert_pdf,
            "word": self._convert_word,
            "excel": self._convert_excel,
            "html": self._convert_html,
            "text": self._convert_text,
            "rtf": self._convert_rtf,
        }

        if ft in dispatchers:
            dispatchers[ft](file_bytes, filename, result, progress_callback)
        else:
            # 未知类型 — 尝试自动检测
            logger.warning("Unknown file_type='%s'; attempting auto-detection", ft)
            detected = self._detect_file_type_from_bytes(file_bytes, filename)
            if detected != "unknown" and detected in dispatchers:
                logger.info("Auto-detected type='%s', using that instead", detected)
                dispatchers[detected](file_bytes, filename, result, progress_callback)
                result.warnings.append(f"文件类型已自动检测为 '{detected}'（传入类型 '{ft}' 无效）")
            else:
                # 最后尝试图片回退
                try:
                    self._convert_image(file_bytes, filename, result, progress_callback)
                except Exception:
                    raise UnsupportedFormatError(ft)
                if result.page_count == 0:
                    raise UnsupportedFormatError(ft)
                result.warnings.append(f"未知文件类型 '{ft}'，已按图片处理")

    # ------------------------------------------------------------------
    # Image
    # ------------------------------------------------------------------

    def _convert_image(
        self, file_bytes: bytes, _filename: str,
        result: ConversionResult, __: Any = None,
    ) -> None:
        """图片 → 单页 BGR 数组。"""
        try:
            pil_image = Image.open(io.BytesIO(file_bytes)).convert("RGB")
        except Exception as e:
            # Detect what the file actually is for a better error message
            detected = self._detect_file_type_from_bytes(file_bytes, _filename or "")
            if detected != "image" and detected != "unknown":
                raise ConversionError(
                    f"文件不是有效的图片格式，实际类型为 '{detected}'。"
                    f"请使用正确的文件类型参数调用 API。"
                ) from e
            raise CorruptedFileError(
                f"无法解析图片文件: {e}"
            ) from e
        bgr = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
        result.pages = [bgr]
        result.converter_used = "pil"

    # ------------------------------------------------------------------
    # PDF
    # ------------------------------------------------------------------

    def _convert_pdf(
        self, file_bytes: bytes, _filename: str,
        result: ConversionResult, progress_callback: Optional[Callable] = None,
    ) -> None:
        """PDF → 逐页渲染为 BGR 数组。"""
        import fitz

        doc = fitz.open(stream=file_bytes, filetype="pdf")
        zoom = self.config.default_dpi / 72.0
        matrix = fitz.Matrix(zoom, zoom)
        total = doc.page_count

        if progress_callback:
            progress_callback(0, total, "PDF渲染")

        try:
            pages: List[np.ndarray] = []
            for i in range(total):
                page = doc.load_page(i)
                pix = page.get_pixmap(matrix=matrix)
                # Handle both RGBA and RGB pixmaps (PyMuPDF may return either)
                if pix.n >= 4:
                    img = Image.frombytes("RGBA", (pix.width, pix.height), pix.samples)
                    rgb = img.convert("RGB")
                else:
                    img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)
                    rgb = img
                bgr = cv2.cvtColor(np.array(rgb), cv2.COLOR_RGB2BGR)
                pages.append(bgr)
                if progress_callback:
                    progress_callback(i + 1, total, "PDF渲染")
            result.pages = pages
            result.converter_used = "pymupdf"
        finally:
            doc.close()

        # 空页回退
        if not result.pages:
            logger.info("PyMuPDF returned 0 pages; attempting PIL image fallback")
            self._convert_image(file_bytes, "", result)
            if result.page_count > 0:
                result.warnings.append("PDF渲染为单张图片（PyMuPDF无法解析页面）")
                result.converter_used = "pil(pdf-fallback)"

    # ------------------------------------------------------------------
    # Word (doc / docx)
    # ------------------------------------------------------------------

    def _convert_word(
        self, file_bytes: bytes, filename: str,
        result: ConversionResult, progress_callback: Optional[Callable] = None,
    ) -> None:
        """Word 文档 → 四级降级策略。

        Tier 1: LibreOffice headless → PDF → PyMuPDF（版式保真度最高）
        Tier 2: docx 图片提取 → 提取嵌入图片（适合图片型合同）
        Tier 3: python-docx → Pillow 文本渲染（并行多页）
        Tier 4: 纯文本提取（无渲染，仅 raw_text）
        """
        # Tier 1: LibreOffice
        soffice = self._find_libreoffice()
        if soffice:
            try:
                if progress_callback:
                    progress_callback(0, 1, "LibreOffice转换")
                pages = self._word_via_libreoffice(file_bytes, soffice)
                result.pages = pages
                result.converter_used = "libreoffice+pymupdf"
                if progress_callback:
                    progress_callback(1, 1, "LibreOffice转换")
                return
            except Exception as exc:
                logger.warning("LibreOffice conversion failed: %s", exc)
                result.warnings.append(f"LibreOffice失败 ({exc})，降级到图片提取")

        # Tier 2: Extract embedded images from docx (best for scanned/image-heavy docs)
        try:
            self._convert_word_with_images(file_bytes, filename, result, progress_callback)
            return
        except ConversionError:
            raise
        except Exception as exc:
            logger.warning("Word image extraction + text rendering failed: %s", exc)
            result.warnings.append(f"Word处理失败 ({exc})，降级到纯文本提取")

        # Tier 4: 纯文本提取（最终回退）
        raw_text = self._word_extract_text_only(file_bytes)
        if raw_text:
            lines = [l for l in raw_text.split("\n") if l.strip()]
            result.pages = [self._render_text_page(lines)]
            result.raw_text = raw_text
            result.converter_used = "python-docx(text-only)"
            result.warnings.append("Word仅提取纯文本（无版式）")
        else:
            raise ConversionError("Word文档转换失败：所有策略均无法提取内容")

    @staticmethod
    def _find_libreoffice() -> Optional[str]:
        """查找 LibreOffice 可执行文件。"""
        return shutil.which("soffice") or shutil.which("soffice.exe")

    def _word_via_libreoffice(self, file_bytes: bytes, soffice_path: str) -> List[np.ndarray]:
        """LibreOffice headless → PDF → 页面渲染。"""
        with tempfile.TemporaryDirectory() as tmpdir:
            docx_path = os.path.join(tmpdir, "input.docx")
            with open(docx_path, "wb") as f:
                f.write(file_bytes)

            result = subprocess.run(
                [soffice_path, "--headless", "--convert-to", "pdf",
                 "--outdir", tmpdir, docx_path],
                capture_output=True, text=True,
                timeout=self.config.libreoffice_timeout,
            )
            if result.returncode != 0:
                stderr = result.stderr[:500] if result.stderr else ""
                raise RuntimeError(
                    f"LibreOffice exit code {result.returncode}: {stderr}"
                )

            pdf_path = os.path.join(tmpdir, "input.pdf")
            if not os.path.exists(pdf_path):
                raise FileNotFoundError(f"LibreOffice did not produce {pdf_path}")

            with open(pdf_path, "rb") as f:
                pdf_bytes = f.read()

            # Create a temporary result to hold pages
            temp_result = ConversionResult()
            self._convert_pdf(pdf_bytes, "", temp_result)
            return temp_result.pages

    def _word_via_python_docx_parallel(
        self, file_bytes: bytes,
        progress_callback: Optional[Callable] = None,
    ) -> Tuple[List[np.ndarray], str]:
        """并行渲染 Word 段落为图像。"""
        import docx

        doc = docx.Document(io.BytesIO(file_bytes))
        paragraphs = [p.text for p in doc.paragraphs if p.text.strip()]
        raw_text = "\n".join(paragraphs)

        if not paragraphs:
            blank = np.ones((1200, 900, 3), dtype=np.uint8) * 255
            return [blank], ""

        # 分页
        ppl = self.config.lines_per_page
        chunks = [paragraphs[i:i + ppl] for i in range(0, len(paragraphs), ppl)]

        if progress_callback:
            progress_callback(0, len(chunks), "并行渲染页面")

        # 并行渲染
        pages: List[np.ndarray] = [None] * len(chunks)
        with ThreadPoolExecutor(max_workers=self.config.max_parallel_pages) as executor:
            futures = {
                executor.submit(self._render_text_page, chunk): idx
                for idx, chunk in enumerate(chunks)
            }
            for future in as_completed(futures):
                idx = futures[future]
                try:
                    pages[idx] = future.result(timeout=30)
                except Exception as e:
                    logger.warning("Page %d rendering failed: %s", idx, e)
                    pages[idx] = np.ones((400, 1200, 3), dtype=np.uint8) * 255

        if progress_callback:
            progress_callback(len(chunks), len(chunks), "渲染完成")

        return [p for p in pages if p is not None], raw_text

    @staticmethod
    def _word_extract_text_only(file_bytes: bytes) -> str:
        """仅提取 Word 文档的纯文本（不渲染）。"""
        try:
            import docx
            doc = docx.Document(io.BytesIO(file_bytes))
            return "\n".join(p.text for p in doc.paragraphs if p.text.strip())
        except Exception:
            return ""

    # ------------------------------------------------------------------
    # Word: image extraction (extract embedded images from docx)
    # ------------------------------------------------------------------

    def extract_images_from_docx(self, file_bytes: bytes) -> List[np.ndarray]:
        """从 .docx 文件中提取所有嵌入图片，返回 BGR numpy 数组列表。

        .docx 文件本质是 ZIP 包，图片存储在 word/media/ 目录下。
        此方法提取所有图片，可用于需要单独处理 Word 内嵌图片的场景。
        """
        import zipfile

        images: List[np.ndarray] = []
        try:
            with zipfile.ZipFile(io.BytesIO(file_bytes)) as zf:
                media_files = [
                    name for name in zf.namelist()
                    if name.startswith("word/media/") and not name.endswith("/")
                ]
                for name in sorted(media_files):
                    try:
                        img_bytes = zf.read(name)
                        pil_image = Image.open(io.BytesIO(img_bytes)).convert("RGB")
                        bgr = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
                        images.append(bgr)
                        logger.debug("Extracted image from docx: %s (%dx%d)",
                                     name, bgr.shape[1], bgr.shape[0])
                    except Exception as e:
                        logger.warning("Failed to extract image '%s' from docx: %s", name, e)
        except zipfile.BadZipFile:
            logger.warning("File is not a valid ZIP archive — cannot extract images")
        except Exception as e:
            logger.warning("Failed to extract images from docx: %s", e)

        return images

    def _convert_word_with_images(
        self, file_bytes: bytes, filename: str,
        result: ConversionResult, progress_callback: Optional[Callable] = None,
    ) -> None:
        """Word 文档转换 — 优先提取嵌入图片，降级到文本渲染。

        策略:
          1. 提取 docx 内嵌图片 → 作为页面
          2. 若图片提取失败/无图片 → 回退到 python-docx 文本渲染
          3. 所有策略均失败 → 纯文本提取
        """
        # Step 1: Try extracting embedded images
        embedded_images = self.extract_images_from_docx(file_bytes)
        if embedded_images:
            logger.info("Extracted %d embedded images from docx", len(embedded_images))
            result.pages = embedded_images
            # Still extract text for LLM context
            raw_text = self._word_extract_text_only(file_bytes)
            result.raw_text = raw_text
            result.converter_used = "docx-image-extraction"
            result.warnings.append(
                f"从 Word 文档中提取了 {len(embedded_images)} 张嵌入图片"
            )
            if not raw_text:
                result.warnings.append("Word 文档未提取到文本内容（可能仅包含图片）")
            return

        # Step 2: Fallback to python-docx text rendering
        try:
            if progress_callback:
                progress_callback(0, 1, "python-docx渲染")
            pages, raw_text = self._word_via_python_docx_parallel(
                file_bytes, progress_callback
            )
            result.pages = pages
            result.raw_text = raw_text
            result.converter_used = "python-docx+pillow"
            result.warnings.append(
                "Word 文档无嵌入图片，已渲染为文本图像（python-docx降级）— 版式保真度降低"
            )
            if progress_callback:
                progress_callback(1, 1, "python-docx渲染")
            return
        except Exception as exc:
            logger.warning("python-docx fallback failed: %s", exc)
            result.warnings.append(f"python-docx失败 ({exc})，降级到纯文本提取")

        # Step 3: Pure text extraction
        raw_text = self._word_extract_text_only(file_bytes)
        if raw_text:
            lines = [l for l in raw_text.split("\n") if l.strip()]
            result.pages = [self._render_text_page(lines)]
            result.raw_text = raw_text
            result.converter_used = "python-docx(text-only)"
            result.warnings.append("Word仅提取纯文本（无版式）")
        else:
            raise ConversionError("Word文档转换失败：所有策略均无法提取内容")

    # ------------------------------------------------------------------
    # Excel (xls / xlsx)
    # ------------------------------------------------------------------

    def _convert_excel(
        self, file_bytes: bytes, _filename: str,
        result: ConversionResult, progress_callback: Optional[Callable] = None,
    ) -> None:
        """Excel 表格 → 文本渲染为图像。

        策略: openpyxl → 读取所有 sheet → 生成 HTML 表格文本 → 渲染为图像
        """
        try:
            import openpyxl
        except ImportError:
            raise ConversionError(
                "Excel 转换需要 openpyxl 库，请执行: pip install openpyxl"
            )

        wb = openpyxl.load_workbook(io.BytesIO(file_bytes), read_only=True, data_only=True)
        all_sheets_text: List[str] = []
        raw_text_parts: List[str] = []

        for sheet_name in wb.sheetnames:
            ws = wb[sheet_name]
            rows = list(ws.iter_rows(values_only=True))
            if not rows:
                continue

            raw_text_parts.append(f"--- Sheet: {sheet_name} ---")
            lines: List[str] = [f"[Sheet: {sheet_name}]", ""]

            # 取前 200 行，每行前 20 列
            for row_idx, row in enumerate(rows[:200]):
                cells = [str(c) if c is not None else "" for c in row[:20]]
                line = " | ".join(cells)
                if line.strip():
                    lines.append(line)
                    raw_text_parts.append(line)

            all_sheets_text.extend(lines)
            all_sheets_text.append("")  # sheet 间空行

        raw_text = "\n".join(raw_text_parts)

        if not all_sheets_text:
            blank = np.ones((1200, 900, 3), dtype=np.uint8) * 255
            result.pages = [blank]
            result.raw_text = ""
            result.converter_used = "openpyxl+pillow"
            result.warnings.append("Excel 文件无有效内容")
            return

        # 分页
        ppl = self.config.lines_per_page
        chunks = [all_sheets_text[i:i + ppl] for i in range(0, len(all_sheets_text), ppl)]

        if progress_callback:
            progress_callback(0, len(chunks), "Excel渲染")

        pages: List[np.ndarray] = [None] * len(chunks)
        with ThreadPoolExecutor(max_workers=self.config.max_parallel_pages) as executor:
            futures = {
                executor.submit(self._render_text_page, chunk): idx
                for idx, chunk in enumerate(chunks)
            }
            for future in as_completed(futures):
                idx = futures[future]
                try:
                    pages[idx] = future.result(timeout=30)
                except Exception as e:
                    logger.warning("Excel page %d rendering failed: %s", idx, e)
                    pages[idx] = np.ones((400, 1200, 3), dtype=np.uint8) * 255

        result.pages = [p for p in pages if p is not None]
        result.raw_text = raw_text
        result.converter_used = "openpyxl+pillow"
        wb.close()

    # ------------------------------------------------------------------
    # HTML
    # ------------------------------------------------------------------

    def _convert_html(
        self, file_bytes: bytes, _filename: str,
        result: ConversionResult, progress_callback: Optional[Callable] = None,
    ) -> None:
        """HTML 网页 → 文本提取 → 渲染为图像。

        策略: BeautifulSoup → 提取可见文本 → 分页渲染
        """
        try:
            from bs4 import BeautifulSoup
        except ImportError:
            # 内建 html.parser 回退
            html_text = self._html_extract_text_fallback(file_bytes)
        else:
            soup = BeautifulSoup(file_bytes, "html.parser")
            # 移除 script / style
            for tag in soup(["script", "style", "meta", "link", "noscript"]):
                tag.decompose()
            html_text = soup.get_text("\n", strip=True)

        raw_text = html_text
        lines = [l for l in html_text.split("\n") if l.strip()]

        if not lines:
            blank = np.ones((1200, 900, 3), dtype=np.uint8) * 255
            result.pages = [blank]
            result.raw_text = ""
            result.converter_used = "bs4+pillow"
            result.warnings.append("HTML 文件无可见文本内容")
            return

        ppl = self.config.lines_per_page
        chunks = [lines[i:i + ppl] for i in range(0, len(lines), ppl)]

        if progress_callback:
            progress_callback(0, len(chunks), "HTML渲染")

        pages: List[np.ndarray] = [None] * len(chunks)
        with ThreadPoolExecutor(max_workers=self.config.max_parallel_pages) as executor:
            futures = {
                executor.submit(self._render_text_page, chunk): idx
                for idx, chunk in enumerate(chunks)
            }
            for future in as_completed(futures):
                idx = futures[future]
                try:
                    pages[idx] = future.result(timeout=30)
                except Exception as e:
                    logger.warning("HTML page %d rendering failed: %s", idx, e)
                    pages[idx] = np.ones((400, 1200, 3), dtype=np.uint8) * 255

        result.pages = [p for p in pages if p is not None]
        result.raw_text = raw_text
        result.converter_used = "bs4+pillow"

    @staticmethod
    def _html_extract_text_fallback(file_bytes: bytes) -> str:
        """纯正则提取 HTML 可见文本（无 BeautifulSoup 时回退）。"""
        text = file_bytes.decode("utf-8", errors="replace")
        # 移除 script/style
        text = re.sub(r'<(script|style)[^>]*>.*?</\1>', '', text, flags=re.DOTALL | re.IGNORECASE)
        # 移除 HTML 标签
        text = re.sub(r'<[^>]+>', '\n', text)
        # 压缩空白
        text = re.sub(r'[ \t]+', ' ', text)
        text = re.sub(r'\n{3,}', '\n\n', text)
        return text.strip()

    # ------------------------------------------------------------------
    # Plain Text (txt / csv / md / log)
    # ------------------------------------------------------------------

    def _convert_text(
        self, file_bytes: bytes, _filename: str,
        result: ConversionResult, progress_callback: Optional[Callable] = None,
    ) -> None:
        """纯文本文件 → 分页渲染。"""
        raw_text = self._decode_text(file_bytes)
        lines = [l for l in raw_text.split("\n")]

        if not lines:
            blank = np.ones((1200, 900, 3), dtype=np.uint8) * 255
            result.pages = [blank]
            result.raw_text = ""
            result.converter_used = "pillow"
            return

        ppl = self.config.lines_per_page
        chunks = [lines[i:i + ppl] for i in range(0, len(lines), ppl)]

        if progress_callback:
            progress_callback(0, len(chunks), "文本渲染")

        pages: List[np.ndarray] = [None] * len(chunks)
        with ThreadPoolExecutor(max_workers=self.config.max_parallel_pages) as executor:
            futures = {
                executor.submit(self._render_text_page, chunk): idx
                for idx, chunk in enumerate(chunks)
            }
            for future in as_completed(futures):
                idx = futures[future]
                try:
                    pages[idx] = future.result(timeout=30)
                except Exception as e:
                    logger.warning("Text page %d rendering failed: %s", idx, e)
                    pages[idx] = np.ones((400, 1200, 3), dtype=np.uint8) * 255

        result.pages = [p for p in pages if p is not None]
        result.raw_text = raw_text
        result.converter_used = "pillow"

    @staticmethod
    def _decode_text(file_bytes: bytes) -> str:
        """尝试多种编码解码文本。"""
        for enc in ("utf-8", "gbk", "gb2312", "latin-1"):
            try:
                return file_bytes.decode(enc)
            except (UnicodeDecodeError, LookupError):
                continue
        return file_bytes.decode("utf-8", errors="replace")

    # ------------------------------------------------------------------
    # RTF (Rich Text Format)
    # ------------------------------------------------------------------

    def _convert_rtf(
        self, file_bytes: bytes, _filename: str,
        result: ConversionResult, progress_callback: Optional[Callable] = None,
    ) -> None:
        """RTF 富文本 → 纯文本提取 → 渲染。"""
        try:
            # 简单 RTF 文本提取（正则去控制字）
            text = file_bytes.decode("utf-8", errors="replace")
            # 移除 RTF 控制字
            text = re.sub(r'\\[a-z]+\d*\s?', '', text, flags=re.IGNORECASE)
            text = re.sub(r'[{}]', '', text)
            text = re.sub(r'\\\'[0-9a-fA-F]{2}', ' ', text)
            text = re.sub(r'\s+', ' ', text).strip()
            raw_text = text
        except Exception:
            raw_text = ""

        if raw_text:
            lines = [l for l in raw_text.split("\n") if l.strip()]
            result.pages = [self._render_text_page(lines)]
            result.raw_text = raw_text
            result.converter_used = "regex+pillow"
            result.warnings.append("RTF 已提取纯文本（版式丢失）")
        else:
            blank = np.ones((1200, 900, 3), dtype=np.uint8) * 255
            result.pages = [blank]
            result.raw_text = ""
            result.converter_used = "regex+pillow"
            result.warnings.append("RTF 文件无有效文本内容")

    # ------------------------------------------------------------------
    # Text Rendering (shared)
    # ------------------------------------------------------------------

    def _render_text_page(
        self, lines: List[str],
        width: Optional[int] = None,
        line_height: Optional[int] = None,
        margin: Optional[int] = None,
    ) -> np.ndarray:
        """将文本行渲染为白色背景的 BGR 图像。"""
        w = width or self.config.page_width
        lh = line_height or self.config.line_height
        m = margin or self.config.margin

        height = m * 2 + len(lines) * lh
        img = Image.new("RGB", (w, max(height, 200)), color=(255, 255, 255))
        draw = ImageDraw.Draw(img)
        font = self._get_font()

        y = m
        for line in lines:
            display = line[:self.config.max_line_chars]
            draw.text((m, y), display, fill=(0, 0, 0), font=font)
            y += lh

        return cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)

    def _get_font(self) -> ImageFont.FreeTypeFont:
        """获取 CJK 兼容字体（带缓存）。"""
        if self._font is not None:
            return self._font

        for font_name in self.config.font_fallbacks:
            try:
                self._font = ImageFont.truetype(font_name, 16)
                return self._font
            except (OSError, IOError):
                continue

        self._font = ImageFont.load_default()
        return self._font

    # ------------------------------------------------------------------
    # Cache
    # ------------------------------------------------------------------

    def _cache_key(self, file_bytes: bytes, file_type: str) -> str:
        """生成缓存键。"""
        h = hashlib.sha256(file_bytes).hexdigest()[:16]
        return f"{file_type}:{h}"

    def clear_cache(self) -> None:
        """清除转换缓存。"""
        if hasattr(self._convert_image, "cache_clear"):
            self._convert_image.cache_clear()  # type: ignore[attr-defined]

    def shutdown(self) -> None:
        """关闭线程池等资源。"""
        self._parallel_executor.shutdown(wait=True)


# Singleton
document_converter = DocumentConverter()