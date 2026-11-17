package fr.billiardplanner.floorplan.infrastructure.persistence;

import fr.billiardplanner.floorplan.domain.FloorPlan;
import fr.billiardplanner.floorplan.domain.FloorPlanRepository;
import fr.billiardplanner.floorplan.domain.FloorPlanStatus;
import fr.billiardplanner.project.domain.Project;
import fr.billiardplanner.project.domain.ProjectRepository;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class FloorPlanRepositoryIntegrationTest {

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
    private FloorPlanRepository floorPlanRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private Project createProject() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        Project project = Project.create(
                "Floor plan test project " + UUID.randomUUID(),
                "Test client",
                "Lyon, France",
                "Floor plan integration test",
                now
        );

        return projectRepository.save(project);
    }

    private FloorPlan createFloorPlan(UUID projectId, Instant createdAt) {
        return FloorPlan.create(
                projectId,
                "floor-plan-" + UUID.randomUUID() + ".pdf",
                "floor-plans/" + UUID.randomUUID() + ".pdf",
                "application/pdf",
                1024L,
                "a".repeat(64),
                createdAt
        );
    }

    @Test
    void shouldSaveAndRetrieveFloorPlan() {
        Project project = createProject();

        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        FloorPlan floorPlan = createFloorPlan(project.getId(), now);

        FloorPlan saved = floorPlanRepository.save(floorPlan);

        assertNotNull(saved.getId());
        assertEquals(project.getId(), saved.getProjectId());
        assertEquals(FloorPlanStatus.UPLOADED, saved.getStatus());

        FloorPlan retrieved = floorPlanRepository.findById(saved.getId())
                .orElseThrow();

        assertEquals(saved.getId(), retrieved.getId());
        assertEquals(project.getId(), retrieved.getProjectId());
        assertEquals(saved.getOriginalFilename(), retrieved.getOriginalFilename());
        assertEquals(saved.getStorageKey(), retrieved.getStorageKey());
        assertEquals("application/pdf", retrieved.getContentType());
        assertEquals(1024L, retrieved.getFileSizeBytes());
        assertEquals("a".repeat(64), retrieved.getSha256());
        assertEquals(FloorPlanStatus.UPLOADED, retrieved.getStatus());

        assertEquals(
                saved.getCreatedAt().truncatedTo(ChronoUnit.MICROS),
                retrieved.getCreatedAt().truncatedTo(ChronoUnit.MICROS)
        );
    }

    @Test
    void shouldFindFloorPlansByProjectId() {
        Project project = createProject();
        Project anotherProject = createProject();

        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        FloorPlan first = createFloorPlan(
                project.getId(),
                now
        );

        FloorPlan second = createFloorPlan(
                project.getId(),
                now.plusSeconds(60)
        );

        FloorPlan unrelated = createFloorPlan(
                anotherProject.getId(),
                now.plusSeconds(120)
        );

        floorPlanRepository.save(first);
        floorPlanRepository.save(second);
        floorPlanRepository.save(unrelated);

        List<FloorPlan> result =
                floorPlanRepository.findByProjectId(project.getId());

        assertEquals(2, result.size());

        assertEquals(
                second.getId(),
                result.get(0).getId()
        );

        assertEquals(
                first.getId(),
                result.get(1).getId()
        );

        assertTrue(
                result.stream().allMatch(
                        plan -> plan.getProjectId().equals(project.getId())
                )
        );
    }

    @Test
    void shouldUpdateExistingFloorPlanWithoutChangingItsIdentity() {
        Project project = createProject();

        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        FloorPlan original = createFloorPlan(project.getId(), createdAt);

        FloorPlan saved = floorPlanRepository.save(original);

        Instant processingTime = createdAt.plusSeconds(60);
        saved.markProcessing(processingTime);

        Instant readyTime = createdAt.plusSeconds(120);
        saved.markReady(3, readyTime);

        FloorPlan updated = floorPlanRepository.save(saved);

        assertEquals(saved.getId(), updated.getId());
        assertEquals(project.getId(), updated.getProjectId());
        assertEquals(FloorPlanStatus.READY, updated.getStatus());
        assertEquals(3, updated.getPageCount());

        assertEquals(
                createdAt,
                updated.getCreatedAt()
        );

        assertEquals(
                readyTime,
                updated.getUpdatedAt()
        );

        FloorPlan retrieved = floorPlanRepository.findById(saved.getId())
                .orElseThrow();

        assertEquals(FloorPlanStatus.READY, retrieved.getStatus());
        assertEquals(3, retrieved.getPageCount());
        assertEquals(
                readyTime,
                retrieved.getUpdatedAt()
        );
    }

    @Test
    void shouldDeleteFloorPlanWithoutDeletingItsProject() {
        Project project = createProject();

        FloorPlan floorPlan = createFloorPlan(
                project.getId(),
                Instant.now().truncatedTo(ChronoUnit.MICROS)
        );

        FloorPlan saved = floorPlanRepository.save(floorPlan);

        assertTrue(
                floorPlanRepository.findById(saved.getId()).isPresent()
        );

        floorPlanRepository.deleteById(saved.getId());

        assertTrue(
                floorPlanRepository.findById(saved.getId()).isEmpty()
        );

        assertTrue(
                projectRepository.findById(project.getId()).isPresent()
        );
    }
}
