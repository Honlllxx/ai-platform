"""Table detection and structure reconstruction service."""

import numpy as np
import json


class TableService:
    """Table detection using PaddleOCR's table model."""

    def __init__(self):
        self._engine = None

    @property
    def engine(self):
        """Lazy-load PaddleOCR table engine."""
        if self._engine is None:
            from paddleocr import PPStructureV3
            from app.config import get_settings
            settings = get_settings()
            self._engine = PPStructureV3(
                lang=settings.ocr_lang,
            )
        return self._engine

    def extract_tables(self, image: np.ndarray) -> list[dict]:
        """Extract tables from image. Returns structured table data."""
        result = self.engine.predict(image, use_table_recognition=True)

        tables = []
        for block in result:
            if block.get("type") == "table":
                bbox = block.get("bbox", [0, 0, 0, 0])
                cells = block.get("res", {}).get("cells", [])
                html = block.get("res", {}).get("html", "")

                # Build headers and rows from cells
                headers, rows = self._parse_cells(cells)

                tables.append({
                    "bbox": {
                        "x": int(bbox[0]),
                        "y": int(bbox[1]),
                        "w": int(bbox[2] - bbox[0]),
                        "h": int(bbox[3] - bbox[1]),
                    },
                    "headers": headers,
                    "rows": rows,
                    "html": html,
                    "cell_count": len(cells),
                })

        return tables

    def _parse_cells(self, cells: list) -> tuple[list[str], list[list[str]]]:
        """Parse PaddleOCR table cells into headers and rows."""
        if not cells:
            return [], []

        # Determine row count
        max_row = max(c.get("row", 0) for c in cells)
        max_col = max(c.get("col", 0) for c in cells)

        # Build grid
        grid = [["" for _ in range(max_col + 1)] for _ in range(max_row + 1)]
        for cell in cells:
            r = cell.get("row", 0)
            c = cell.get("col", 0)
            text = cell.get("text", "")
            grid[r][c] = text

        headers = grid[0] if grid else []
        rows = grid[1:] if len(grid) > 1 else []

        return headers, rows


# Singleton
table_service = TableService()
