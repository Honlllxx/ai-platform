"""Document image preprocessing — deskew, stamp removal."""

import cv2
import numpy as np


class PreprocessService:
    """OpenCV-based document image preprocessing."""

    def deskew(self, image: np.ndarray) -> np.ndarray:
        """Correct image skew by detecting dominant line angle via Hough transform."""
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        blur = cv2.GaussianBlur(gray, (5, 5), 0)
        edges = cv2.Canny(blur, 50, 150, apertureSize=3)

        lines = cv2.HoughLines(edges, 1, np.pi / 180, 200)
        if lines is None:
            return image

        angles = []
        for line in lines:
            rho, theta = line[0]
            angle = np.degrees(theta) - 90
            if -45 < angle < 45:
                angles.append(angle)

        if not angles:
            return image

        median_angle = np.median(angles)
        if abs(median_angle) < 0.5:
            return image

        h, w = image.shape[:2]
        center = (w // 2, h // 2)
        matrix = cv2.getRotationMatrix2D(center, median_angle, 1.0)
        rotated = cv2.warpAffine(
            image, matrix, (w, h),
            borderMode=cv2.BORDER_CONSTANT,
            borderValue=(255, 255, 255),
        )
        return rotated

    def remove_stamp(self, image: np.ndarray) -> np.ndarray:
        """Detect and remove red stamp regions using HSV color space."""
        hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)

        # Red color range (two ranges because red wraps around in HSV)
        lower_red1 = np.array([0, 50, 50])
        upper_red1 = np.array([10, 255, 255])
        lower_red2 = np.array([160, 50, 50])
        upper_red2 = np.array([180, 255, 255])

        mask1 = cv2.inRange(hsv, lower_red1, upper_red1)
        mask2 = cv2.inRange(hsv, lower_red2, upper_red2)
        stamp_mask = cv2.bitwise_or(mask1, mask2)

        # Apply morphological operations to fill stamp regions
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
        stamp_mask = cv2.morphologyEx(stamp_mask, cv2.MORPH_CLOSE, kernel, iterations=3)
        stamp_mask = cv2.dilate(stamp_mask, kernel, iterations=2)

        # Inpaint stamp regions
        result = cv2.inpaint(image, stamp_mask, inpaintRadius=5, flags=cv2.INPAINT_TELEA)
        return result

    def process(self, image: np.ndarray, remove_stamps: bool = True) -> tuple[np.ndarray, dict]:
        """Run full preprocessing pipeline. Returns (processed_image, metadata)."""
        meta = {}

        # Step 1: Deskew
        deskewed = self.deskew(image)
        meta["deskew"] = {
            "applied": not np.array_equal(deskewed, image),
            "shape": deskewed.shape[:2],
        }

        # Step 2: Remove stamps
        if remove_stamps:
            deskewed = self.remove_stamp(deskewed)
            meta["stamp_removal"] = {"applied": True}

        return deskewed, meta


# Singleton
preprocess_service = PreprocessService()
