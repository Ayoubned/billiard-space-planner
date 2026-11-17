package fr.billiardplanner.floorplan.application.inspection;

public record ImageInspectionResult(
        String format,
        int widthPixels,
        int heightPixels,
        String colorModel,
        Integer dpiX,
        Integer dpiY,
        Double physicalWidthInches,
        Double physicalHeightInches
) {
}