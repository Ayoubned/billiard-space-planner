package fr.billiardplanner.floorplan.application;

import java.io.InputStream;
import java.util.UUID;

public record UploadFloorPlanCommand(
        UUID projectId,
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        InputStream inputStream
) {
}