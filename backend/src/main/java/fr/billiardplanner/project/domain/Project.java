package fr.billiardplanner.project.domain;

import java.time.Instant;
import java.util.UUID;

public final class Project {

    private final UUID id;
    private String name;
    private String clientName;
    private String siteAddress;
    private String description;
    private ProjectStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant archivedAt;

    private Project(
            UUID id,
            String name,
            String clientName,
            String siteAddress,
            String description,
            ProjectStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant archivedAt
    ) {
        this.id = id;
        this.name = validateRequired(name, "name", 120);
        this.clientName = normalizeOptional(clientName, "clientName", 120);
        this.siteAddress = normalizeOptional(siteAddress, "siteAddress", 250);
        this.description = normalizeOptional(description, "description", 2000);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.archivedAt = archivedAt;
    }

    public static Project create(
            String name,
            String clientName,
            String siteAddress,
            String description,
            Instant now
    ) {
        if (now == null) {
            throw new IllegalArgumentException("Creation date is required");
        }

        return new Project(
                UUID.randomUUID(),
                name,
                clientName,
                siteAddress,
                description,
                ProjectStatus.DRAFT,
                now,
                now,
                null
        );
    }

    public void updateDetails(
            String name,
            String clientName,
            String siteAddress,
            String description,
            Instant now
    ) {
        if (status == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "An archived project cannot be updated"
            );
        }

        if (now == null || now.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                    "Update date must be valid and not precede the previous update"
            );
        }

        // Validate everything before changing the object.
        String validatedName = validateRequired(name, "name", 120);
        String validatedClientName = normalizeOptional(clientName, "clientName", 120);
        String validatedSiteAddress = normalizeOptional(siteAddress, "siteAddress", 250);
        String validatedDescription = normalizeOptional(description, "description", 2000);

        // Apply changes only after all validations succeed.
        this.name = validatedName;
        this.clientName = validatedClientName;
        this.siteAddress = validatedSiteAddress;
        this.description = validatedDescription;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        if (now == null || now.isBefore(updatedAt)) {
            throw new IllegalArgumentException("Archive date is invalid");
        }

        if (status == ProjectStatus.ARCHIVED) {
            return;
        }

        this.status = ProjectStatus.ARCHIVED;
        this.archivedAt = now;
        this.updatedAt = now;
    }

    private static String validateRequired(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must not exceed " + maxLength + " characters"
            );
        }

        return normalized;
    }

    private static String normalizeOptional(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must not exceed " + maxLength + " characters"
            );
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getClientName() {
        return clientName;
    }

    public String getSiteAddress() {
        return siteAddress;
    }

    public String getDescription() {
        return description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public static Project reconstitute(
        UUID id,
        String name,
        String clientName,
        String siteAddress,
        String description,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
    ) {
    return new Project(
            id,
            name,
            clientName,
            siteAddress,
            description,
            status,
            createdAt,
            updatedAt,
            archivedAt
        );
    }
}