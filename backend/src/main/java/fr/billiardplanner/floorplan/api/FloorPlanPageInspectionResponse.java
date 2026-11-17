package fr.billiardplanner.floorplan.api;

import fr.billiardplanner.floorplan.domain.FloorPlanPage;

public record FloorPlanPageInspectionResponse(
        int pageNumber,
        double widthPoints,
        double heightPoints,
        int rotationDegrees
) {

    public static FloorPlanPageInspectionResponse from(FloorPlanPage page) {
        return new FloorPlanPageInspectionResponse(
                page.pageNumber(),
                page.widthPoints(),
                page.heightPoints(),
                page.rotationDegrees()
        );
    }
}