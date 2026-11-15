package fr.billiardplanner.project.domain;

import java.util.List;

public record ProjectPage(
        List<Project> projects,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public ProjectPage {
        projects = List.copyOf(projects);
    }
}