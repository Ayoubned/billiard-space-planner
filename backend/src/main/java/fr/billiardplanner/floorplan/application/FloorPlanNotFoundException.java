package fr.billiardplanner.floorplan.application;

import java.util.UUID;

public class FloorPlanNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FloorPlanNotFoundException(UUID projectId, UUID floorPlanId) {
        super(
                "Floor plan " + floorPlanId
                        + " was not found in project " + projectId
        );
    }
}