package fr.billiardplanner.floorplan.application;

import java.io.InputStream;
import java.util.UUID;

public record FloorPlanDownload(
        UUID id,
        UUID projectId,
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        String sha256,
        InputStream inputStream
) {
}