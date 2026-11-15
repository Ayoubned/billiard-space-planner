package fr.billiardplanner.project.application.dto;

import fr.billiardplanner.project.domain.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResult(
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
}