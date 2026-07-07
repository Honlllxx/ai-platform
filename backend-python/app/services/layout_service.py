"""Layout analysis service — identifies text blocks, tables, images in documents."""

import numpy as np


class LayoutService:
    """Document layout analysis using PaddleOCR's built-in layout model."""

    def __init__(self):
        self._model = None

    @property
    def model(self):
        """Lazy-load PaddleOCR layout model."""
        if self._model is None:
            from paddleocr import PPStructureV3
            from app.config import get_settings
            settings = get_settings()
            self._model = PPStructureV3(
                lang=settings.ocr_lang,
            )
        return self._model

    def analyze(self, image: np.ndarray) -> list[dict]:
        """Analyze document layout. Returns regions with type, coordinates, content."""
        # PPStructureV3 uses predict() instead of direct call
        result = self.model.predict(image)

        regions = []
        for idx, block in enumerate(result):
            bbox = block.get("bbox", [0, 0, 0, 0])
            region_type = block.get("type", "text")

            region = {
                "id": f"region-{idx}",
                "type": region_type,
                "label": region_type,
                "x": int(bbox[0]),
                "y": int(bbox[1]),
                "width": int(bbox[2] - bbox[0]),
                "height": int(bbox[3] - bbox[1]),
                "confidence": 0.9,  # PaddleOCR doesn't expose per-block confidence easily
                "content": "",
            }

            # Extract text content if present
            if "res" in block and block["res"]:
                region["content"] = "\n".join(
                    item.get("text", "") for item in block["res"]
                )

            regions.append(region)

        return regions


# Singleton
layout_service = LayoutService()
