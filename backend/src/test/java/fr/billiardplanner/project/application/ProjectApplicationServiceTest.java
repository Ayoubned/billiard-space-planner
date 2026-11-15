
package fr.billiardplanner.project.application;

import fr.billiardplanner.project.application.dto.CreateProjectCommand;
import fr.billiardplanner.project.application.dto.UpdateProjectCommand;
import fr.billiardplanner.project.application.dto.ListProjectsQuery;
import fr.billiardplanner.project.application.dto.ProjectResult;
import fr.billiardplanner.project.application.dto.ProjectPageResult;

import fr.billiardplanner.project.domain.Project;
import fr.billiardplanner.project.domain.ProjectPage;
import fr.billiardplanner.project.domain.ProjectRepository;
import fr.billiardplanner.project.domain.ProjectSearchCriteria;
import fr.billiardplanner.project.domain.ProjectSortField;
import fr.billiardplanner.project.domain.ProjectStatus;
import fr.billiardplanner.project.domain.SortDirection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectApplicationServiceTest {

    private ProjectRepository projectRepository;
    private ProjectApplicationService service;

    private final Instant fixedInstant =
            Instant.parse("2026-11-15T12:00:00Z");

    private final Clock fixedClock =
            Clock.fixed(fixedInstant, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);

        service = new ProjectApplicationService(
                projectRepository,
                fixedClock
        );
    }

    @Test
    void shouldCreateProject() {
        CreateProjectCommand command = new CreateProjectCommand(
                "Billiard Shop",
                "John Doe",
                "Lyon",
                "New shop layout"
        );

        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResult result = service.create(command);

        assertNotNull(result.id());
        assertEquals("Billiard Shop", result.name());
        assertEquals("John Doe", result.clientName());
        assertEquals("Lyon", result.siteAddress());
        assertEquals("New shop layout", result.description());
        assertEquals(ProjectStatus.DRAFT, result.status());
        assertEquals(fixedInstant, result.createdAt());

        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void shouldRetrieveExistingProject() {
        Project project = createTestProject();

        when(projectRepository.findById(project.getId()))
                .thenReturn(Optional.of(project));

        ProjectResult result = service.getById(project.getId());

        assertEquals(project.getId(), result.id());
        assertEquals("Test Project", result.name());

        verify(projectRepository).findById(project.getId());
    }

    @Test
    void shouldThrowWhenProjectDoesNotExist() {
        UUID unknownId = UUID.randomUUID();

        when(projectRepository.findById(unknownId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> service.getById(unknownId)
        );

        verify(projectRepository).findById(unknownId);
    }

    @Test
    void shouldUpdateExistingProject() {
        Project project = createTestProject();

        when(projectRepository.findById(project.getId()))
                .thenReturn(Optional.of(project));

        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProjectCommand command = new UpdateProjectCommand(
                "Updated Project",
                "Updated Client",
                "Paris",
                "Updated description"
        );

        ProjectResult result = service.update(project.getId(), command);

        assertEquals("Updated Project", result.name());
        assertEquals("Updated Client", result.clientName());
        assertEquals("Paris", result.siteAddress());
        assertEquals("Updated description", result.description());
        assertEquals(fixedInstant, result.updatedAt());

        verify(projectRepository).findById(project.getId());
        verify(projectRepository).save(project);
    }

    @Test
    void shouldArchiveExistingProject() {
        Project project = createTestProject();

        when(projectRepository.findById(project.getId()))
                .thenReturn(Optional.of(project));

        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResult result = service.archive(project.getId());

        assertEquals(ProjectStatus.ARCHIVED, result.status());
        assertEquals(fixedInstant, result.archivedAt());
        assertEquals(fixedInstant, result.updatedAt());

        verify(projectRepository).findById(project.getId());
        verify(projectRepository).save(project);
    }

    @Test
    void shouldListProjectsWithDefaultCriteria() {
        Project project = createTestProject();

        when(projectRepository.search(any(ProjectSearchCriteria.class)))
                .thenReturn(new ProjectPage(
                        List.of(project),
                        1,
                        1,
                        0,
                        20
                ));

        ProjectPageResult result = service.list(
                new ListProjectsQuery(
                        null, null, null, null, null, null, null
                )
        );

        assertEquals(1, result.projects().size());
        assertEquals(project.getId(), result.projects().getFirst().id());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(0, result.page());
        assertEquals(20, result.size());

        verify(projectRepository).search(
                new ProjectSearchCriteria(
                        null,
                        null,
                        false,
                        0,
                        20,
                        ProjectSortField.UPDATED_AT,
                        SortDirection.DESC
                )
        );
    }

    @Test
    void shouldListProjectsWithCustomFiltersAndSorting() {
        Project project = createTestProject();

        when(projectRepository.search(any(ProjectSearchCriteria.class)))
                .thenReturn(new ProjectPage(
                        List.of(project),
                        1,
                        1,
                        1,
                        5
                ));

        ProjectPageResult result = service.list(
                new ListProjectsQuery(
                        "Test",
                        ProjectStatus.DRAFT,
                        true,
                        1,
                        5,
                        ProjectSortField.NAME,
                        SortDirection.ASC
                )
        );

        assertEquals(1, result.projects().size());
        assertEquals(1, result.page());
        assertEquals(5, result.size());

        verify(projectRepository).search(
                new ProjectSearchCriteria(
                        "Test",
                        ProjectStatus.DRAFT,
                        true,
                        1,
                        5,
                        ProjectSortField.NAME,
                        SortDirection.ASC
                )
        );
    }

    @Test
    void shouldExcludeArchivedProjectsByDefault() {
        when(projectRepository.search(any(ProjectSearchCriteria.class)))
                .thenReturn(new ProjectPage(
                        List.of(),
                        0,
                        0,
                        0,
                        20
                ));

        service.list(new ListProjectsQuery(
                null, null, null, null, null, null, null
        ));

        verify(projectRepository).search(
                argThat(criteria -> !criteria.includeArchived())
        );
    }

    @Test
    void shouldIncludeArchivedProjectsWhenRequested() {
        when(projectRepository.search(any(ProjectSearchCriteria.class)))
                .thenReturn(new ProjectPage(
                        List.of(),
                        0,
                        0,
                        0,
                        20
                ));

        service.list(new ListProjectsQuery(
                null, null, true, null, null, null, null
        ));

        verify(projectRepository).search(
                argThat(ProjectSearchCriteria::includeArchived)
        );
    }

    @Test
    void shouldRejectNegativePageNumber() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.list(new ListProjectsQuery(
                        null, null, null, -1, 20, null, null
                ))
        );

        verifyNoInteractions(projectRepository);
    }

    @Test
    void shouldRejectPageSizeAboveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.list(new ListProjectsQuery(
                        null, null, null, 0, 101, null, null
                ))
        );

        verifyNoInteractions(projectRepository);
    }

    @Test
    void shouldRejectNullListingQuery() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.list(null)
        );

        verifyNoInteractions(projectRepository);
    }

    private Project createTestProject() {
        return Project.create(
                "Test Project",
                "Test Client",
                "Lyon",
                "Test description",
                fixedInstant.minusSeconds(3600)
        );
    }
}
