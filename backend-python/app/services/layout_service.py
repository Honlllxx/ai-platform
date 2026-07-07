"""Layout analysis service — identifies text blocks, tables, images in documents.

Uses the shared PPStructureV3 singleton from ``model_loader`` so layout and
table analysis share one model instance.
"""

import numpy as np

from app.services.model_loader import model_loader


class LayoutService:
    """Document layout analysis using PaddleOCR's built-in layout model."""

    def analyze(self, image: np.ndarray) -> list[dict]:
        """Analyze document layout. Returns regions with type, coordinates, content."""
        result = model_loader.predict(image)

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
                "confidence": 0.9,
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
