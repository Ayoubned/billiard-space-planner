package fr.billiardplanner.project.application;

import fr.billiardplanner.project.application.dto.CreateProjectCommand;
import fr.billiardplanner.project.application.dto.UpdateProjectCommand;
import fr.billiardplanner.project.application.dto.ProjectResult;
import fr.billiardplanner.project.domain.Project;
import fr.billiardplanner.project.domain.ProjectRepository;
import fr.billiardplanner.project.application.dto.ListProjectsQuery;
import fr.billiardplanner.project.application.dto.ProjectPageResult;
import fr.billiardplanner.project.domain.ProjectPage;
import fr.billiardplanner.project.domain.ProjectSearchCriteria;
import fr.billiardplanner.project.domain.ProjectSortField;
import fr.billiardplanner.project.domain.SortDirection;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
public class ProjectApplicationService {

    private final ProjectRepository projectRepository;
    private final Clock clock;

    public ProjectApplicationService(
            ProjectRepository projectRepository,
            Clock clock
    ) {
        this.projectRepository = projectRepository;
        this.clock = clock;
    }


@Transactional(readOnly = true)
public ProjectPageResult list(ListProjectsQuery query) {
    if (query == null) {
        throw new IllegalArgumentException(
                "Project listing query is required"
        );
    }

    ProjectSearchCriteria criteria = new ProjectSearchCriteria(
            query.searchTerm(),
            query.status(),
            Boolean.TRUE.equals(query.includeArchived()),
            query.page() == null ? 0 : query.page(),
            query.size() == null ? 20 : query.size(),
            query.sortBy() == null
                    ? ProjectSortField.UPDATED_AT
                    : query.sortBy(),
            query.direction() == null
                    ? SortDirection.DESC
                    : query.direction()
    );

    ProjectPage result = projectRepository.search(criteria);

    return new ProjectPageResult(
            result.projects().stream()
                    .map(this::toResult)
                    .toList(),
            result.totalElements(),
            result.totalPages(),
            result.page(),
            result.size()
    );
    }


    public ProjectResult create(CreateProjectCommand command) {
        Project project = Project.create(
                command.name(),
                command.clientName(),
                command.siteAddress(),
                command.description(),
                clock.instant()
        );

        return toResult(projectRepository.save(project));
    }

    public ProjectResult getById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        return toResult(project);
    }

    public ProjectResult update(UUID id, UpdateProjectCommand command) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        project.updateDetails(
                command.name(),
                command.clientName(),
                command.siteAddress(),
                command.description(),
                clock.instant()
        );

        return toResult(projectRepository.save(project));
    }

    public ProjectResult archive(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        project.archive(clock.instant());

        return toResult(projectRepository.save(project));
    }

    private ProjectResult toResult(Project project) {
        return new ProjectResult(
                project.getId(),
                project.getName(),
                project.getClientName(),
                project.getSiteAddress(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getArchivedAt()
        );
    }
}