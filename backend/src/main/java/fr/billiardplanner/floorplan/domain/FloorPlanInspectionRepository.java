package fr.billiardplanner.floorplan.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FloorPlanInspectionRepository {

    void savePdfPages(UUID floorPlanId, List<FloorPlanPage> pages);

    void saveImageMetadata(FloorPlanImageMetadata metadata);

    List<FloorPlanPage> findPdfPages(UUID floorPlanId);

    Optional<FloorPlanImageMetadata> findImageMetadata(UUID floorPlanId);

    void deleteByFloorPlanId(UUID floorPlanId);
}