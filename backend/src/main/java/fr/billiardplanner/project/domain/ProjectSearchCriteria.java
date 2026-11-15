package fr.billiardplanner.project.domain;

public record ProjectSearchCriteria(
        String searchTerm,
        ProjectStatus status,
        boolean includeArchived,
        int page,
        int size,
        ProjectSortField sortBy,
        SortDirection direction
) {
    public ProjectSearchCriteria {
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        if (searchTerm != null) {
            searchTerm = searchTerm.trim();

            if (searchTerm.isBlank()) {
                searchTerm = null;
            }
        }

        if (sortBy == null) {
            sortBy = ProjectSortField.UPDATED_AT;
        }

        if (direction == null) {
            direction = SortDirection.DESC;
        }
    }
}