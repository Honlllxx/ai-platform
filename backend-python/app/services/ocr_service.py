"""PaddleOCR wrapper service."""

import numpy as np
from PIL import Image


class OcrService:
    """PaddleOCR encapsulation for text recognition."""

    def __init__(self):
        self._ocr = None

    @property
    def ocr(self):
        """Lazy-load PaddleOCR to avoid import overhead on startup."""
        if self._ocr is None:
            from paddleocr import PaddleOCR
            from app.config import get_settings
            settings = get_settings()
            self._ocr = PaddleOCR(
                lang=settings.ocr_lang,
                use_gpu=settings.ocr_use_gpu,
                show_log=False,
            )
        return self._ocr

    def recognize(self, image: np.ndarray) -> list[dict]:
        """Run OCR on an image. Returns list of text entries with coordinates."""
        # PaddleOCR accepts numpy array or image path
        result = self.ocr.ocr(image, cls=True)

        if not result or not result[0]:
            return []

        entries = []
        for idx, line in enumerate(result[0]):
            bbox = line[0]  # [[x1,y1],[x2,y2],[x3,y3],[x4,y4]]
            text = line[1][0]
            confidence = line[1][1]

            xs = [p[0] for p in bbox]
            ys = [p[1] for p in bbox]

            entries.append({
                "id": idx,
                "text": text,
                "confidence": round(float(confidence), 4),
                "bbox": {
                    "x": int(min(xs)),
                    "y": int(min(ys)),
                    "w": int(max(xs) - min(xs)),
                    "h": int(max(ys) - min(ys)),
                },
            })

        return entries

    def recognize_batch(self, images: list[np.ndarray]) -> list[list[dict]]:
        """Run OCR on multiple images."""
        return [self.recognize(img) for img in images]


# Singleton
ocr_service = OcrService()
