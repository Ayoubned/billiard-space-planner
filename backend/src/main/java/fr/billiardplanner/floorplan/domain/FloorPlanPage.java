package fr.billiardplanner.floorplan.domain;

import java.util.UUID;

public record FloorPlanPage(
        UUID id,
        UUID floorPlanId,
        int pageNumber,
        double widthPoints,
        double heightPoints,
        int rotationDegrees
) {

    public FloorPlanPage {
        if (id == null) {
            throw new IllegalArgumentException("Page ID is required");
        }
        if (floorPlanId == null) {
            throw new IllegalArgumentException("Floor plan ID is required");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be positive");
        }
        if (!Double.isFinite(widthPoints) || widthPoints <= 0) {
            throw new IllegalArgumentException("Page width must be positive and finite");
        }
        if (!Double.isFinite(heightPoints) || heightPoints <= 0) {
            throw new IllegalArgumentException("Page height must be positive and finite");
        }
        if (!isValidRotation(rotationDegrees)) {
            throw new IllegalArgumentException("Invalid page rotation");
        }
    }

    public static FloorPlanPage create(
            UUID floorPlanId,
            int pageNumber,
            double widthPoints,
            double heightPoints,
            int rotationDegrees
    ) {
        return new FloorPlanPage(
                UUID.randomUUID(),
                floorPlanId,
                pageNumber,
                widthPoints,
                heightPoints,
                rotationDegrees
        );
    }

    private static boolean isValidRotation(int rotation) {
        return rotation == 0 || rotation == 90
                || rotation == 180 || rotation == 270;
    }
}