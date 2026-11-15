package fr.billiardplanner.project.application.dto;

public record CreateProjectCommand(
        String name,
        String clientName,
        String siteAddress,
        String description
) {
}