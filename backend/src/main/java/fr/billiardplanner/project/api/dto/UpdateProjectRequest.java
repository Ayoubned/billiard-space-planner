package fr.billiardplanner.project.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(

        @NotBlank(message = "Project name is required")
        @Size(max = 120, message = "Project name cannot exceed 120 characters")
        String name,

        @Size(max = 120)
        String clientName,

        @Size(max = 250)
        String siteAddress,

        @Size(max = 2000)
        String description
) {
}