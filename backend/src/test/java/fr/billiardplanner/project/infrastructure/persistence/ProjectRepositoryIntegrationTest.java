
package fr.billiardplanner.project.infrastructure.persistence;

import fr.billiardplanner.project.domain.Project;
import fr.billiardplanner.project.domain.ProjectPage;
import fr.billiardplanner.project.domain.ProjectRepository;
import fr.billiardplanner.project.domain.ProjectSearchCriteria;
import fr.billiardplanner.project.domain.ProjectSortField;
import fr.billiardplanner.project.domain.ProjectStatus;
import fr.billiardplanner.project.domain.SortDirection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class ProjectRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    DockerImageName
                            .parse("postgis/postgis:17-3.7")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("billiard_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProjectRepository projectRepository;

    private Project createProject(String name, String clientName) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        Project project = Project.create(
                name,
                clientName,
                "Lyon, France",
                "Integration test",
                now
        );

        return projectRepository.save(project);
    }

    private ProjectSearchCriteria criteria(
            String searchTerm,
            ProjectStatus status,
            boolean includeArchived,
            int page,
            int size,
            ProjectSortField sortBy,
            SortDirection direction
    ) {
        return new ProjectSearchCriteria(
                searchTerm,
                status,
                includeArchived,
                page,
                size,
                sortBy,
                direction
        );
    }

    @Test
    void shouldSaveAndRetrieveProject() {
        Project saved = createProject(
                "Integration test project",
                "Test client"
        );

        assertNotNull(saved.getId());
        assertEquals(ProjectStatus.DRAFT, saved.getStatus());

        Project retrieved = projectRepository.findById(saved.getId())
                .orElseThrow();

        assertEquals(saved.getId(), retrieved.getId());
        assertEquals("Integration test project", retrieved.getName());
        assertEquals("Test client", retrieved.getClientName());

        assertEquals(
                saved.getCreatedAt().truncatedTo(ChronoUnit.MICROS),
                retrieved.getCreatedAt().truncatedTo(ChronoUnit.MICROS)
        );
    }

    @Test
    void shouldUpdateExistingProjectWithoutChangingItsIdOrCreationDate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        Project original = Project.create(
                "Original project",
                "Original client",
                "Original address",
                null,
                now
        );

        Project saved = projectRepository.save(original);

        Instant updateTime = now.plusSeconds(60);

        saved.updateDetails(
                "Updated project",
                "Updated client",
                "Updated address",
                "Updated description",
                updateTime
        );

        Project updated = projectRepository.save(saved);

        assertEquals(saved.getId(), updated.getId());
        assertEquals("Updated project", updated.getName());
        assertEquals("Updated client", updated.getClientName());
        assertEquals(now, updated.getCreatedAt());
        assertEquals(updateTime, updated.getUpdatedAt());
    }

    @Test
    void shouldSearchProjectsByName() {
        createProject(
                "name-search-alpha",
                "Client Alpha"
        );

        createProject(
                "name-search-beta",
                "Client Beta"
        );

        createProject(
                "unrelated-project",
                "Another Client"
        );

        ProjectPage result = projectRepository.search(
                criteria(
                        "name-search-alpha",
                        null,
                        false,
                        0,
                        10,
                        ProjectSortField.NAME,
                        SortDirection.ASC
                )
        );

        assertEquals(1, result.totalElements());
        assertEquals(
                "name-search-alpha",
                result.projects().getFirst().getName()
        );
    }

    @Test
    void shouldSearchProjectsByClientName() {
        createProject(
                "client-search-project",
                "unique-client-search"
        );

        createProject(
                "another-client-project",
                "Different Client"
        );

        ProjectPage result = projectRepository.search(
                criteria(
                        "unique-client-search",
                        null,
                        false,
                        0,
                        10,
                        ProjectSortField.NAME,
                        SortDirection.ASC
                )
        );

        assertEquals(1, result.totalElements());
        assertEquals(
                "unique-client-search",
                result.projects().getFirst().getClientName()
        );
    }

    @Test
    void shouldFilterProjectsByStatus() {
        Project draft = createProject(
                "status-filter-draft",
                "Status Client"
        );

        Project archived = createProject(
                "status-filter-archived",
                "Status Client"
        );

        archived.archive(Instant.now().plusSeconds(60));
        projectRepository.save(archived);

        ProjectPage result = projectRepository.search(
                criteria(
                        "status-filter",
                        ProjectStatus.DRAFT,
                        false,
                        0,
                        10,
                        ProjectSortField.NAME,
                        SortDirection.ASC
                )
        );

        assertEquals(1, result.totalElements());
        assertEquals(draft.getId(), result.projects().getFirst().getId());
    }

    @Test
    void shouldExcludeArchivedProjectsByDefault() {
        createProject(
                "archive-filter-active",
                "Archive Client"
        );

        Project archived = createProject(
                "archive-filter-archived",
                "Archive Client"
        );

        archived.archive(Instant.now().plusSeconds(60));
        projectRepository.save(archived);

        ProjectPage result = projectRepository.search(
                criteria(
                        "archive-filter",
                        null,
                        false,
                        0,
                        10,
                        ProjectSortField.NAME,
                        SortDirection.ASC
                )
        );

        assertEquals(1, result.totalElements());
        assertTrue(
                result.projects().stream()
                        .noneMatch(p -> p.getStatus() == ProjectStatus.ARCHIVED)
        );
    }

    @Test
    void shouldIncludeArchivedProjectsWhenRequested() {
        createProject(
                "archive-include-active",
                "Archive Client"
        );

        Project archived = createProject(
                "archive-include-archived",
                "Archive Client"
        );

        archived.archive(Instant.now().plusSeconds(60));
        projectRepository.save(archived);

        ProjectPage result = projectRepository.search(
                criteria(
                        "archive-include",
                        null,
                        true,
                        0,
                        10,
                        ProjectSortField.NAME,
                        SortDirection.ASC
                )
        );

        assertEquals(2, result.totalElements());

        assertTrue(
                result.projects().stream()
                        .anyMatch(p -> p.getStatus() == ProjectStatus.ARCHIVED)
        );
    }

    @Test
    void shouldPaginateProjectsCorrectly() {
        for (int i = 1; i <= 5; i++) {
            createProject(
                    "pagination-project-" + i,
                    "Pagination Client"
            );
        }

        ProjectPage firstPage = projectRepository.search(
                criteria(
                        "pagination-project",
                        null,
                        false,
                        0,
                        2,
                        ProjectSortField.NAME,
                        SortDirection.ASC
                )
        );

        assertEquals(5, firstPage.totalElements());
        assertEquals(3, firstPage.totalPages());
        assertEquals(0, firstPage.page());
        assertEquals(2, firstPage.size());
        assertEquals(2, firstPage.projects().size());

        ProjectPage secondPage = projectRepository.search(
                criteria(
                        "pagination-project",
                        null,
                        false,
                        1,
                        2,
                        ProjectSortField.NAME,
                        SortDirection.ASC
                )
        );

        assertEquals(1, secondPage.page());
        assertEquals(2, secondPage.projects().size());

        List<String> firstPageNames = firstPage.projects()
                .stream()
                .map(Project::getName)
                .toList();

        List<String> secondPageNames = secondPage.projects()
                .stream()
                .map(Project::getName)
                .toList();

        assertTrue(
                firstPageNames.stream()
                        .noneMatch(secondPageNames::contains)
        );
    }

    @Test
    void shouldSortProjectsInAscendingAndDescendingOrder() {
        createProject("sorting-project-charlie", "Sorting Client");
        createProject("sorting-project-alpha", "Sorting Client");
        createProject("sorting-project-bravo", "Sorting Client");

        ProjectPage ascending = projectRepository.search(
                criteria(
                        "sorting-project",
                        null,
                        false,
                        0,
                        10,
                        ProjectSortField.NAME,
                        SortDirection.ASC
                )
        );

        ProjectPage descending = projectRepository.search(
                criteria(
                        "sorting-project",
                        null,
                        false,
                        0,
                        10,
                        ProjectSortField.NAME,
                        SortDirection.DESC
                )
        );

        List<String> ascendingNames = ascending.projects()
                .stream()
                .map(Project::getName)
                .toList();

        List<String> descendingNames = descending.projects()
                .stream()
                .map(Project::getName)
                .toList();

        assertEquals(
                List.of(
                        "sorting-project-alpha",
                        "sorting-project-bravo",
                        "sorting-project-charlie"
                ),
                ascendingNames
        );

        assertEquals(
                List.of(
                        "sorting-project-charlie",
                        "sorting-project-bravo",
                        "sorting-project-alpha"
                ),
                descendingNames
        );
    }
}
