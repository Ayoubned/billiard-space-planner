package fr.billiardplanner.floorplan.application;

import fr.billiardplanner.floorplan.domain.FloorPlanStatus;

import java.time.Instant;
import java.util.UUID;

public record FloorPlanResult(
        UUID id,
        UUID projectId,
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        String sha256,
        FloorPlanStatus status,
        Integer pageCount,
        Instant createdAt,
        Instant updatedAt
) {
}