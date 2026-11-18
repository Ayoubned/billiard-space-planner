package fr.billiardplanner.floorplan.application.storage;

public record StoredFile(
        String storageKey,
        long fileSizeBytes,
        String sha256
) {
}