package fr.billiardplanner.floorplan.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FloorPlanImageMetadataJpaRepository
        extends JpaRepository<FloorPlanImageMetadataEntity, UUID> {
}