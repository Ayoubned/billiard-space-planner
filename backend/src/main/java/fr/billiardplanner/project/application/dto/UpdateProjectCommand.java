package fr.billiardplanner.project.application.dto;

public record UpdateProjectCommand(
        String name,
        String clientName,
        String siteAddress,
        String description
) {
}