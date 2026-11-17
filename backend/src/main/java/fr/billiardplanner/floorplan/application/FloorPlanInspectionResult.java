package fr.billiardplanner.floorplan.application;

import fr.billiardplanner.floorplan.domain.FloorPlanImageMetadata;
import fr.billiardplanner.floorplan.domain.FloorPlanPage;

import java.util.List;
import java.util.UUID;

public record FloorPlanInspectionResult(
        UUID floorPlanId,
        String type,
        Integer pageCount,
        List<FloorPlanPage> pages,
        FloorPlanImageMetadata imageMetadata
) {

    public FloorPlanInspectionResult {
        pages = pages == null ? List.of() : List.copyOf(pages);
    }
}