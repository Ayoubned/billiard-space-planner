package fr.billiardplanner.project.application.dto;

import fr.billiardplanner.project.domain.ProjectSortField;
import fr.billiardplanner.project.domain.ProjectStatus;
import fr.billiardplanner.project.domain.SortDirection;

public record ListProjectsQuery(
        String searchTerm,
        ProjectStatus status,
        Boolean includeArchived,
        Integer page,
        Integer size,
        ProjectSortField sortBy,
        SortDirection direction
) {
}