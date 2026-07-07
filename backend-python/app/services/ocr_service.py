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
            self._ocr = PaddleOCR(lang=settings.ocr_lang)
        return self._ocr

    def recognize(self, image: np.ndarray) -> list[dict]:
        """Run OCR on an image. Returns list of text entries with coordinates."""
        # PaddleOCR 3.x uses predict() instead of ocr()
        result = self.ocr.predict(image)

        if not result:
            return []

        entries = []
        # New API returns a list of dicts with 'rec_texts', 'rec_scores', 'dt_polys', etc.
        for item in result:
            texts = item.get("rec_texts", []) if isinstance(item, dict) else []
            scores = item.get("rec_scores", []) if isinstance(item, dict) else []
            polys = item.get("dt_polys", []) if isinstance(item, dict) else []

            if not texts:
                # Try old format: list of [bbox, (text, confidence)]
                if isinstance(item, list) and isinstance(item[0], list) if item else False:
                    for line in item:
                        if len(line) >= 2:
                            bbox = line[0]
                            text_info = line[1]
                            if isinstance(text_info, (list, tuple)) and len(text_info) >= 2:
                                text = text_info[0]
                                confidence = text_info[1]
                            else:
                                text = str(text_info)
                                confidence = 1.0
                            xs = [p[0] for p in bbox]
                            ys = [p[1] for p in bbox]
                            entries.append({
                                "id": len(entries),
                                "text": text,
                                "confidence": round(float(confidence), 4),
                                "bbox": {
                                    "x": int(min(xs)),
                                    "y": int(min(ys)),
                                    "w": int(max(xs) - min(xs)),
                                    "h": int(max(ys) - min(ys)),
                                },
                            })
                continue

            for i, text in enumerate(texts):
                confidence = scores[i] if i < len(scores) else 1.0
                if i < len(polys):
                    poly = polys[i]
                    xs = [p[0] for p in poly]
                    ys = [p[1] for p in poly]
                else:
                    xs, ys = [0], [0]

                entries.append({
                    "id": len(entries),
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
