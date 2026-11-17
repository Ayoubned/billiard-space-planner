package fr.billiardplanner.floorplan.domain;

import java.util.Locale;
import java.util.UUID;

public record FloorPlanImageMetadata(
        UUID floorPlanId,
        String format,
        int widthPixels,
        int heightPixels,
        String colorModel,
        Integer dpiX,
        Integer dpiY,
        Double physicalWidthInches,
        Double physicalHeightInches
) {

    public FloorPlanImageMetadata {
        if (floorPlanId == null) {
            throw new IllegalArgumentException("Floor plan ID is required");
        }

        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("Image format is required");
        }

        format = format.toUpperCase(Locale.ROOT);

        if (!format.equals("PNG") && !format.equals("JPEG")) {
            throw new IllegalArgumentException("Unsupported image format");
        }

        if (widthPixels <= 0 || heightPixels <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }

        if (colorModel == null || colorModel.isBlank()) {
            throw new IllegalArgumentException("Color model is required");
        }

        if (dpiX != null && dpiX <= 0) {
            throw new IllegalArgumentException("Horizontal DPI must be positive");
        }

        if (dpiY != null && dpiY <= 0) {
            throw new IllegalArgumentException("Vertical DPI must be positive");
        }

        if (physicalWidthInches != null
                && (!Double.isFinite(physicalWidthInches) || physicalWidthInches <= 0)) {
            throw new IllegalArgumentException("Physical width must be positive and finite");
        }

        if (physicalHeightInches != null
                && (!Double.isFinite(physicalHeightInches) || physicalHeightInches <= 0)) {
            throw new IllegalArgumentException("Physical height must be positive and finite");
        }
    }
}