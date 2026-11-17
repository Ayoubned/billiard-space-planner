package fr.billiardplanner.floorplan.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class FloorPlan {

    private final UUID id;
    private final UUID projectId;
    private final String originalFilename;
    private final String storageKey;
    private final String contentType;
    private final long fileSizeBytes;
    private final String sha256;

    private FloorPlanStatus status;
    private Integer pageCount;
    private final Instant createdAt;
    private Instant updatedAt;

    private FloorPlan(
            UUID id,
            UUID projectId,
            String originalFilename,
            String storageKey,
            String contentType,
            long fileSizeBytes,
            String sha256,
            FloorPlanStatus status,
            Integer pageCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.projectId = Objects.requireNonNull(projectId);
        this.originalFilename = requireText(originalFilename, "Filename");
        this.storageKey = requireText(storageKey, "Storage key");
        this.contentType = validateContentType(contentType);

        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be positive.");
        }

        this.fileSizeBytes = fileSizeBytes;
        this.sha256 = validateSha256(sha256);
        this.status = Objects.requireNonNull(status);
        this.pageCount = validatePageCount(pageCount);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static FloorPlan create(
            UUID projectId,
            String originalFilename,
            String storageKey,
            String contentType,
            long fileSizeBytes,
            String sha256,
            Instant now
    ) {
        return new FloorPlan(
                UUID.randomUUID(),
                projectId,
                originalFilename,
                storageKey,
                contentType,
                fileSizeBytes,
                sha256,
                FloorPlanStatus.UPLOADED,
                null,
                now,
                now
        );
    }


public void markProcessing(Instant now) {
    if (status != FloorPlanStatus.UPLOADED) {
        throw new IllegalStateException(
                "Only uploaded floor plans can be processed."
        );
    }

    Instant timestamp = Objects.requireNonNull(now, "Timestamp is required.");

    status = FloorPlanStatus.PROCESSING;
    updatedAt = timestamp;
}

public void markReady(Integer pageCount, Instant now) {
    if (status != FloorPlanStatus.PROCESSING) {
        throw new IllegalStateException(
                "Only processing floor plans can become ready."
        );
    }

    Integer validatedPageCount = validatePageCount(pageCount);
    Instant timestamp = Objects.requireNonNull(now, "Timestamp is required.");

    this.pageCount = validatedPageCount;
    status = FloorPlanStatus.READY;
    updatedAt = timestamp;
}

public void markFailed(Instant now) {
    if (status != FloorPlanStatus.PROCESSING) {
        throw new IllegalStateException(
                "Only processing floor plans can fail."
        );
    }

    Instant timestamp = Objects.requireNonNull(now, "Timestamp is required.");

    status = FloorPlanStatus.FAILED;
    updatedAt = timestamp;
}


    public static FloorPlan reconstitute(
            UUID id,
            UUID projectId,
            String originalFilename,
            String storageKey,
            String contentType,
            long fileSizeBytes,
            String sha256,
            FloorPlanStatus status,
            Integer pageCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new FloorPlan(
                id,
                projectId,
                originalFilename,
                storageKey,
                contentType,
                fileSizeBytes,
                sha256,
                status,
                pageCount,
                createdAt,
                updatedAt
        );
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }

        return value.trim();
    }

    private static String validateContentType(String contentType) {
        String value = requireText(contentType, "Content type");

        if (!value.equals("application/pdf")
                && !value.equals("image/png")
                && !value.equals("image/jpeg")) {
            throw new IllegalArgumentException(
                    "Unsupported floor plan content type."
            );
        }

        return value;
    }

    private static String validateSha256(String sha256) {
        String value = requireText(sha256, "SHA-256");

        if (!value.matches("(?i)[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Invalid SHA-256 hash.");
        }

        return value.toLowerCase();
    }

    private static Integer validatePageCount(Integer pageCount) {
        if (pageCount != null && pageCount <= 0) {
            throw new IllegalArgumentException(
                    "Page count must be positive."
            );
        }

        return pageCount;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getSha256() { return sha256; }
    public FloorPlanStatus getStatus() { return status; }
    public Integer getPageCount() { return pageCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}