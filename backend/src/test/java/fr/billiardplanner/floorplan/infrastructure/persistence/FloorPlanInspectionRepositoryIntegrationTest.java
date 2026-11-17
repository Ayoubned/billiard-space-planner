package fr.billiardplanner.floorplan.infrastructure.persistence;

import fr.billiardplanner.floorplan.domain.FloorPlan;
import fr.billiardplanner.floorplan.domain.FloorPlanImageMetadata;
import fr.billiardplanner.floorplan.domain.FloorPlanInspectionRepository;
import fr.billiardplanner.floorplan.domain.FloorPlanPage;
import fr.billiardplanner.floorplan.domain.FloorPlanRepository;
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
class FloorPlanInspectionRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgis/postgis:17-3.7")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("billiard_inspection_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private FloorPlanInspectionRepository inspectionRepository;

    @Autowired
    private FloorPlanRepository floorPlanRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private FloorPlan createFloorPlan() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        Project project = Project.create(
                "Inspection test project " + UUID.randomUUID(),
                "Test client",
                "Lyon, France",
                "Inspection repository integration test",
                now
        );

        Project savedProject = projectRepository.save(project);

        FloorPlan floorPlan = FloorPlan.create(
                savedProject.getId(),
                "floor-plan-" + UUID.randomUUID() + ".pdf",
                "floor-plans/" + UUID.randomUUID() + ".pdf",
                "application/pdf",
                1024L,
                "a".repeat(64),
                now
        );

        return floorPlanRepository.save(floorPlan);
    }

    private FloorPlanPage createPage(
            UUID floorPlanId,
            int pageNumber,
            double width,
            double height
    ) {
        return FloorPlanPage.create(
                floorPlanId,
                pageNumber,
                width,
                height,
                0
        );
    }

    @Test
    void shouldSaveAndRetrievePdfPagesInPageNumberOrder() {
        FloorPlan floorPlan = createFloorPlan();

        FloorPlanPage thirdPage = createPage(
                floorPlan.getId(), 3, 595.0, 842.0
        );

        FloorPlanPage firstPage = createPage(
                floorPlan.getId(), 1, 595.0, 842.0
        );

        FloorPlanPage secondPage = createPage(
                floorPlan.getId(), 2, 842.0, 595.0
        );

        inspectionRepository.savePdfPages(
                floorPlan.getId(),
                List.of(thirdPage, firstPage, secondPage)
        );

        List<FloorPlanPage> retrieved =
                inspectionRepository.findPdfPages(floorPlan.getId());

        assertEquals(3, retrieved.size());
        assertEquals(1, retrieved.get(0).pageNumber());
        assertEquals(2, retrieved.get(1).pageNumber());
        assertEquals(3, retrieved.get(2).pageNumber());

        assertEquals(
                842.0,
                retrieved.get(1).widthPoints()
        );
    }

    @Test
    void shouldReplaceExistingPdfPages() {
        FloorPlan floorPlan = createFloorPlan();

        inspectionRepository.savePdfPages(
                floorPlan.getId(),
                List.of(
                        createPage(floorPlan.getId(), 1, 595.0, 842.0),
                        createPage(floorPlan.getId(), 2, 595.0, 842.0)
                )
        );

        inspectionRepository.savePdfPages(
                floorPlan.getId(),
                List.of(
                        createPage(floorPlan.getId(), 1, 842.0, 595.0)
                )
        );

        List<FloorPlanPage> retrieved =
                inspectionRepository.findPdfPages(floorPlan.getId());

        assertEquals(1, retrieved.size());
        assertEquals(1, retrieved.get(0).pageNumber());
        assertEquals(842.0, retrieved.get(0).widthPoints());
    }

    @Test
    void shouldSaveAndRetrieveImageMetadata() {
        FloorPlan floorPlan = createFloorPlan();

        FloorPlanImageMetadata metadata = new FloorPlanImageMetadata(
                floorPlan.getId(),
                "PNG",
                1920,
                1080,
                "RGB",
                300,
                300,
                6.4,
                3.6
        );

        inspectionRepository.saveImageMetadata(metadata);

        FloorPlanImageMetadata retrieved =
                inspectionRepository.findImageMetadata(floorPlan.getId())
                        .orElseThrow();

        assertEquals(floorPlan.getId(), retrieved.floorPlanId());
        assertEquals("PNG", retrieved.format());
        assertEquals(1920, retrieved.widthPixels());
        assertEquals(1080, retrieved.heightPixels());
        assertEquals("RGB", retrieved.colorModel());
        assertEquals(300, retrieved.dpiX());
        assertEquals(300, retrieved.dpiY());
        assertEquals(6.4, retrieved.physicalWidthInches(), 0.0001);
        assertEquals(3.6, retrieved.physicalHeightInches(), 0.0001);
    }

    @Test
    void shouldAllowMissingImageDpiMetadata() {
        FloorPlan floorPlan = createFloorPlan();

        FloorPlanImageMetadata metadata = new FloorPlanImageMetadata(
                floorPlan.getId(),
                "JPEG",
                1920,
                1080,
                "RGB",
                null,
                null,
                null,
                null
        );

        inspectionRepository.saveImageMetadata(metadata);

        FloorPlanImageMetadata retrieved =
                inspectionRepository.findImageMetadata(floorPlan.getId())
                        .orElseThrow();

        assertNull(retrieved.dpiX());
        assertNull(retrieved.dpiY());
        assertNull(retrieved.physicalWidthInches());
        assertNull(retrieved.physicalHeightInches());
    }

    @Test
    void shouldReplacePdfPagesWhenSavingImageMetadata() {
        FloorPlan floorPlan = createFloorPlan();

        inspectionRepository.savePdfPages(
                floorPlan.getId(),
                List.of(createPage(floorPlan.getId(), 1, 595.0, 842.0))
        );

        FloorPlanImageMetadata metadata = new FloorPlanImageMetadata(
                floorPlan.getId(),
                "PNG",
                1920,
                1080,
                "RGB",
                null,
                null,
                null,
                null
        );

        inspectionRepository.saveImageMetadata(metadata);

        assertTrue(
                inspectionRepository.findPdfPages(floorPlan.getId()).isEmpty()
        );

        assertTrue(
                inspectionRepository.findImageMetadata(floorPlan.getId()).isPresent()
        );
    }

    @Test
    void shouldReplaceImageMetadataWhenSavingPdfPages() {
        FloorPlan floorPlan = createFloorPlan();

        inspectionRepository.saveImageMetadata(
                new FloorPlanImageMetadata(
                        floorPlan.getId(),
                        "PNG",
                        1920,
                        1080,
                        "RGB",
                        null,
                        null,
                        null,
                        null
                )
        );

        inspectionRepository.savePdfPages(
                floorPlan.getId(),
                List.of(createPage(floorPlan.getId(), 1, 595.0, 842.0))
        );

        assertTrue(
                inspectionRepository.findImageMetadata(floorPlan.getId()).isEmpty()
        );

        assertEquals(
                1,
                inspectionRepository.findPdfPages(floorPlan.getId()).size()
        );
    }

    @Test
    void shouldDeleteAllInspectionMetadataForFloorPlan() {
        FloorPlan floorPlan = createFloorPlan();

        inspectionRepository.savePdfPages(
                floorPlan.getId(),
                List.of(createPage(floorPlan.getId(), 1, 595.0, 842.0))
        );

        inspectionRepository.deleteByFloorPlanId(floorPlan.getId());

        assertTrue(
                inspectionRepository.findPdfPages(floorPlan.getId()).isEmpty()
        );

        assertTrue(
                inspectionRepository.findImageMetadata(floorPlan.getId()).isEmpty()
        );
    }

    @Test
    void shouldCascadeDeleteInspectionMetadataWhenFloorPlanIsDeleted() {
        FloorPlan floorPlan = createFloorPlan();

        inspectionRepository.savePdfPages(
                floorPlan.getId(),
                List.of(createPage(floorPlan.getId(), 1, 595.0, 842.0))
        );

        floorPlanRepository.deleteById(floorPlan.getId());

        assertTrue(
                inspectionRepository.findPdfPages(floorPlan.getId()).isEmpty()
        );

        assertTrue(
                inspectionRepository.findImageMetadata(floorPlan.getId()).isEmpty()
        );
    }
}
