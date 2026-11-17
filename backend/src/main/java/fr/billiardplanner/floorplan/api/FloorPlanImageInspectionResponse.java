package fr.billiardplanner.floorplan.api;

import fr.billiardplanner.floorplan.domain.FloorPlanImageMetadata;

public record FloorPlanImageInspectionResponse(
        String format,
        int widthPixels,
        int heightPixels,
        String colorModel,
        Integer dpiX,
        Integer dpiY,
        Double physicalWidthInches,
        Double physicalHeightInches
) {

    public static FloorPlanImageInspectionResponse from(
            FloorPlanImageMetadata metadata
    ) {
        return new FloorPlanImageInspectionResponse(
                metadata.format(),
                metadata.widthPixels(),
                metadata.heightPixels(),
                metadata.colorModel(),
                metadata.dpiX(),
                metadata.dpiY(),
                metadata.physicalWidthInches(),
                metadata.physicalHeightInches()
        );
    }
}