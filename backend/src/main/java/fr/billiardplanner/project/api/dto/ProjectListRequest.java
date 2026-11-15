package fr.billiardplanner.project.api.dto;

import fr.billiardplanner.project.domain.ProjectSortField;
import fr.billiardplanner.project.domain.ProjectStatus;
import fr.billiardplanner.project.domain.SortDirection;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ProjectListRequest(

        String search,

        ProjectStatus status,

        Boolean includeArchived,

        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size,

        ProjectSortField sortBy,

        SortDirection direction
) {
}