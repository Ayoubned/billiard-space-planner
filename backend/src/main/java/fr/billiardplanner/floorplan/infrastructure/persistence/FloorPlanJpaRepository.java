package fr.billiardplanner.floorplan.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FloorPlanJpaRepository
        extends JpaRepository<FloorPlanEntity, UUID> {

    List<FloorPlanEntity> findAllByProjectIdOrderByCreatedAtDesc(
            UUID projectId
    );
}