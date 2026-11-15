package fr.billiardplanner.project.application.dto;

import java.util.List;

public record ProjectPageResult(
        List<ProjectResult> projects,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public ProjectPageResult {
        projects = List.copyOf(projects);
    }
}