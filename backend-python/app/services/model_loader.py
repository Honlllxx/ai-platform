"""Shared PaddleOCR model loader — provides a single PPStructureV3 singleton.

Both layout_service and table_service import from here so only ONE model
instance is loaded into memory, halving GPU/RAM usage.
"""

import threading
import numpy as np


class _ModelLoader:
    """Thread-safe lazy singleton for PPStructureV3."""

    def __init__(self):
        self._model = None
        self._lock = threading.Lock()

    @property
    def ppstructure(self):
        """Return the shared PPStructureV3 instance, loading on first access."""
        if self._model is None:
            with self._lock:
                if self._model is None:
                    from paddleocr import PPStructureV3
                    from app.config import get_settings
                    settings = get_settings()
                    self._model = PPStructureV3(lang=settings.ocr_lang)
        return self._model

    def predict(self, image: np.ndarray, **kwargs) -> list:
        """Run PPStructureV3.predict() on the shared model.

        Args:
            image: BGR numpy array.
            **kwargs: Forwarded to PPStructureV3.predict() (e.g.
                      ``use_table_recognition=True`` for table mode).

        Returns:
            List of detected regions / tables.
        """
        return self.ppstructure.predict(image, **kwargs)


# Module-level singleton
model_loader = _ModelLoader()
