package fr.billiardplanner.floorplan.api;

import fr.billiardplanner.floorplan.application.FloorPlanResult;
import fr.billiardplanner.floorplan.domain.FloorPlanStatus;

import java.time.Instant;
import java.util.UUID;

public record FloorPlanResponse(
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

    public static FloorPlanResponse from(FloorPlanResult result) {
        return new FloorPlanResponse(
                result.id(),
                result.projectId(),
                result.originalFilename(),
                result.contentType(),
                result.fileSizeBytes(),
                result.sha256(),
                result.status(),
                result.pageCount(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}