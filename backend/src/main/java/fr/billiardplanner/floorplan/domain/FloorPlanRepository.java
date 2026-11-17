package fr.billiardplanner.floorplan.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FloorPlanRepository {

    FloorPlan save(FloorPlan floorPlan);

    Optional<FloorPlan> findById(UUID id);

    List<FloorPlan> findByProjectId(UUID projectId);

    void deleteById(UUID id);
}