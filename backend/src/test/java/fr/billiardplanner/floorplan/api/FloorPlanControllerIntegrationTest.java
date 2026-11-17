
package fr.billiardplanner.floorplan.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.billiardplanner.floorplan.domain.FloorPlan;
import fr.billiardplanner.floorplan.domain.FloorPlanRepository;
import fr.billiardplanner.floorplan.domain.FloorPlanStatus;
import fr.billiardplanner.project.domain.Project;
import fr.billiardplanner.project.domain.ProjectRepository;

import org.junit.jupiter.api.Test;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FloorPlanControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    DockerImageName
                            .parse("postgis/postgis:17-3.7")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("billiard_floorplan_api_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add(
                "billiard-planner.storage.floor-plans-directory",
                () -> "target/test-floor-plans"
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FloorPlanRepository floorPlanRepository;

    @Value("${billiard-planner.storage.floor-plans-directory}")
    private String storageDirectory;

    private Project createProject() {
        Project project = Project.create(
                "Floor plan API test " + UUID.randomUUID(),
                "Integration client",
                "Lyon, France",
                "Floor plan controller integration test",
                Instant.now()
        );

        return projectRepository.save(project);
    }

    private byte[] createValidPdf() throws Exception {
    try (PDDocument document = new PDDocument();
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {

        document.addPage(new PDPage(PDRectangle.A4));
        document.save(output);

        return output.toByteArray();
    }
}

private byte[] createValidPng() throws Exception {
    BufferedImage image = new BufferedImage(
            100,
            100,
            BufferedImage.TYPE_INT_RGB
    );

    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}

private byte[] createValidJpeg() throws Exception {
    BufferedImage image = new BufferedImage(
            100,
            100,
            BufferedImage.TYPE_INT_RGB
    );

    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
        ImageIO.write(image, "jpeg", output);
        return output.toByteArray();
    }
}

    private UUID uploadPdf(
            Project project,
            String filename,
            byte[] content
    ) throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "application/pdf",
                content
        );

        String response = mockMvc.perform(
                        multipart(
                                "/api/v1/projects/{projectId}/floor-plans",
                                project.getId()
                        ).file(file)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(
                objectMapper.readTree(response)
                        .get("id")
                        .asText()
        );
    }

    private UUID uploadFile(
        Project project,
        String filename,
        String contentType,
        byte[] content
) throws Exception {

    MockMultipartFile file = new MockMultipartFile(
            "file",
            filename,
            contentType,
            content
    );

    String response = mockMvc.perform(
                    multipart(
                            "/api/v1/projects/{projectId}/floor-plans",
                            project.getId()
                    ).file(file)
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return UUID.fromString(
            objectMapper.readTree(response).get("id").asText()
    );
}

private void assertPreview(
        Project project,
        UUID floorPlanId,
        String filename,
        MediaType expectedContentType,
        byte[] expectedContent
) throws Exception {

    MvcResult result = mockMvc.perform(
                    get(
                            "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}/preview",
                            project.getId(),
                            floorPlanId
                    )
            )
            .andExpect(request().asyncStarted())
            .andReturn();

    mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isOk())
            .andExpect(content().contentType(expectedContentType))
            .andExpect(header().string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.containsString("inline"),
                            org.hamcrest.Matchers.containsString(filename)
                    )
            ))
            .andExpect(header().string(
                    "X-Content-Type-Options",
                    "nosniff"
            ))
            .andExpect(header().longValue(
                    "Content-Length",
                    expectedContent.length
            ))
            .andExpect(content().bytes(expectedContent));
}

    private UUID createProcessingFloorPlan(Project project) {
    Instant now = Instant.now();

    FloorPlan floorPlan = FloorPlan.create(
            project.getId(),
            "processing-plan.pdf",
            UUID.randomUUID().toString(),
            "application/pdf",
            100L,
            "a".repeat(64),
            now
    );

    floorPlan.markProcessing(now);

    return floorPlanRepository.save(floorPlan).getId();
}

    private Path getStoredFile(FloorPlan floorPlan) {
        return Path.of(
                storageDirectory,
                floorPlan.getStorageKey()
        );
    }

    // --------------------------------------------------
    // Upload tests
    // --------------------------------------------------

    @Test
    void shouldUploadPdfAndSaveItOnDisk() throws Exception {
        Project project = createProject();

        byte[] content = createValidPdf();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "floor-plan.pdf",
                "application/pdf",
                content
        );

        String response = mockMvc.perform(
                        multipart(
                                "/api/v1/projects/{projectId}/floor-plans",
                                project.getId()
                        ).file(file)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.projectId")
                        .value(project.getId().toString()))
                .andExpect(jsonPath("$.originalFilename")
                        .value("floor-plan.pdf"))
                .andExpect(jsonPath("$.contentType")
                        .value("application/pdf"))
                .andExpect(jsonPath("$.fileSizeBytes")
                        .value(content.length))
                .andExpect(jsonPath("$.sha256").exists())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.pageCount").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID floorPlanId = UUID.fromString(
                objectMapper.readTree(response)
                        .get("id")
                        .asText()
        );

        FloorPlan saved = floorPlanRepository.findById(floorPlanId)
                .orElseThrow();

        assertEquals(project.getId(), saved.getProjectId());
        assertEquals(FloorPlanStatus.READY, saved.getStatus());
        Path storedFile = getStoredFile(saved);

        assertTrue(
                Files.exists(storedFile),
                "Uploaded file should exist on disk"
        );

        assertArrayEquals(
                content,
                Files.readAllBytes(storedFile)
        );
    }

    @Test
    void shouldUploadPng() throws Exception {
        Project project = createProject();

        byte[] content = createValidPng();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "floor-plan.png",
                "image/png",
                content
        );

        mockMvc.perform(
                        multipart(
                                "/api/v1/projects/{projectId}/floor-plans",
                                project.getId()
                        ).file(file)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename")
                        .value("floor-plan.png"))
                .andExpect(jsonPath("$.contentType")
                        .value("image/png"))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void shouldUploadJpeg() throws Exception {
        Project project = createProject();

        byte[] content = createValidJpeg();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "floor-plan.jpg",
                "image/jpeg",
                content
        );

        mockMvc.perform(
                        multipart(
                                "/api/v1/projects/{projectId}/floor-plans",
                                project.getId()
                        ).file(file)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename")
                        .value("floor-plan.jpg"))
                .andExpect(jsonPath("$.contentType")
                        .value("image/jpeg"))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void shouldRejectInvalidFileSignature() throws Exception {
        Project project = createProject();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "floor-plan.pdf",
                "application/pdf",
                "This is not a PDF".getBytes()
        );

        mockMvc.perform(
                        multipart(
                                "/api/v1/projects/{projectId}/floor-plans",
                                project.getId()
                        ).file(file)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenProjectDoesNotExist() throws Exception {
        UUID unknownProjectId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "floor-plan.pdf",
                "application/pdf",
                createValidPdf()
        );

        mockMvc.perform(
                        multipart(
                                "/api/v1/projects/{projectId}/floor-plans",
                                unknownProjectId
                        ).file(file)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldRejectRequestWithoutFile() throws Exception {
        Project project = createProject();

        mockMvc.perform(
                        multipart(
                                "/api/v1/projects/{projectId}/floor-plans",
                                project.getId()
                        )
                )
                .andExpect(status().isBadRequest());
    }

    // --------------------------------------------------
    // List tests
    // --------------------------------------------------

    @Test
    void shouldListFloorPlansForProject() throws Exception {
        Project project = createProject();

        uploadPdf(project, "first.pdf", createValidPdf());
        uploadPdf(project, "second.pdf", createValidPdf());

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans",
                                project.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnEmptyListWhenProjectHasNoFloorPlans()
            throws Exception {

        Project project = createProject();

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans",
                                project.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --------------------------------------------------
    // Metadata tests
    // --------------------------------------------------

    @Test
    void shouldGetFloorPlanMetadata() throws Exception {
        Project project = createProject();

        UUID floorPlanId = uploadPdf(
                project,
                "metadata-test.pdf",
                createValidPdf()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}",
                                project.getId(),
                                floorPlanId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(floorPlanId.toString()))
                .andExpect(jsonPath("$.projectId")
                        .value(project.getId().toString()))
                .andExpect(jsonPath("$.originalFilename")
                        .value("metadata-test.pdf"))
                .andExpect(jsonPath("$.contentType")
                        .value("application/pdf"))
                .andExpect(jsonPath("$.status").value("READY"));    }

    @Test
    void shouldReturn404WhenFloorPlanDoesNotExist()
            throws Exception {

        Project project = createProject();
        UUID unknownFloorPlanId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}",
                                project.getId(),
                                unknownFloorPlanId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldNotAllowAccessToFloorPlanFromAnotherProject()
            throws Exception {

        Project ownerProject = createProject();
        Project otherProject = createProject();

        UUID floorPlanId = uploadPdf(
                ownerProject,
                "private-plan.pdf",
                createValidPdf()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}",
                                otherProject.getId(),
                                floorPlanId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }


    // --------------------------------------------------
    // Inspection metadata tests
    // --------------------------------------------------

    @Test
    void shouldReturnPdfInspectionMetadata() throws Exception {
        Project project = createProject();

        UUID floorPlanId = uploadPdf(
                project,
                "inspection.pdf",
                createValidPdf()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}/inspection",
                                project.getId(),
                                floorPlanId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floorPlanId")
                        .value(floorPlanId.toString()))
                .andExpect(jsonPath("$.type").value("PDF"))
                .andExpect(jsonPath("$.pageCount").value(1))
                .andExpect(jsonPath("$.pages.length()").value(1))
                .andExpect(jsonPath("$.pages[0].pageNumber").value(1))
                .andExpect(jsonPath("$.pages[0].widthPoints")
                        .value(org.hamcrest.Matchers.greaterThan(0.0)))
                .andExpect(jsonPath("$.pages[0].heightPoints")
                        .value(org.hamcrest.Matchers.greaterThan(0.0)))
                .andExpect(jsonPath("$.pages[0].rotationDegrees").value(0))
                .andExpect(jsonPath("$.image").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void shouldReturnPngInspectionMetadata() throws Exception {
        Project project = createProject();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "inspection.png",
                "image/png",
                createValidPng()
        );

        String response = mockMvc.perform(
                        multipart(
                                "/api/v1/projects/{projectId}/floor-plans",
                                project.getId()
                        ).file(file)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID floorPlanId = UUID.fromString(
                objectMapper.readTree(response).get("id").asText()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}/inspection",
                                project.getId(),
                                floorPlanId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floorPlanId")
                        .value(floorPlanId.toString()))
                .andExpect(jsonPath("$.type").value("IMAGE"))
                .andExpect(jsonPath("$.image.format").value("PNG"))
                .andExpect(jsonPath("$.image.widthPixels").value(100))
                .andExpect(jsonPath("$.image.heightPixels").value(100))
                .andExpect(jsonPath("$.image.colorModel").exists())
                .andExpect(jsonPath("$.pages.length()").value(0));
    }

    @Test
    void shouldReturnJpegInspectionMetadata() throws Exception {
        Project project = createProject();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "inspection.jpg",
                "image/jpeg",
                createValidJpeg()
        );

        String response = mockMvc.perform(
                        multipart(
                                "/api/v1/projects/{projectId}/floor-plans",
                                project.getId()
                        ).file(file)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID floorPlanId = UUID.fromString(
                objectMapper.readTree(response).get("id").asText()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}/inspection",
                                project.getId(),
                                floorPlanId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floorPlanId")
                        .value(floorPlanId.toString()))
                .andExpect(jsonPath("$.type").value("IMAGE"))
                .andExpect(jsonPath("$.image.format").value("JPEG"))
                .andExpect(jsonPath("$.image.widthPixels").value(100))
                .andExpect(jsonPath("$.image.heightPixels").value(100))
                .andExpect(jsonPath("$.image.colorModel").exists())
                .andExpect(jsonPath("$.pages.length()").value(0));
    }

    @Test
    void shouldReturn404WhenInspectionFloorPlanDoesNotExist()
            throws Exception {

        Project project = createProject();

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}/inspection",
                                project.getId(),
                                UUID.randomUUID()
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldNotAllowInspectionOfFloorPlanFromAnotherProject()
            throws Exception {

        Project ownerProject = createProject();
        Project otherProject = createProject();

        UUID floorPlanId = uploadPdf(
                ownerProject,
                "private-inspection.pdf",
                createValidPdf()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}/inspection",
                                otherProject.getId(),
                                floorPlanId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn409WhenFloorPlanIsStillProcessing()
            throws Exception {

        Project project = createProject();

        UUID floorPlanId = createProcessingFloorPlan(project);

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}/inspection",
                                project.getId(),
                                floorPlanId
                        )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }


    // --------------------------------------------------
    // Preview tests
    // --------------------------------------------------

    @Test
    void shouldPreviewPdfInline() throws Exception {
        Project project = createProject();
        byte[] content = createValidPdf();

        UUID floorPlanId = uploadPdf(
                project,
                "preview.pdf",
                content
        );

        assertPreview(
                project,
                floorPlanId,
                "preview.pdf",
                MediaType.APPLICATION_PDF,
                content
        );
    }

    @Test
    void shouldPreviewPngInline() throws Exception {
        Project project = createProject();
        byte[] content = createValidPng();

        UUID floorPlanId = uploadFile(
                project,
                "preview.png",
                "image/png",
                content
        );

        assertPreview(
                project,
                floorPlanId,
                "preview.png",
                MediaType.IMAGE_PNG,
                content
        );
    }

    @Test
    void shouldPreviewJpegInline() throws Exception {
        Project project = createProject();
        byte[] content = createValidJpeg();

        UUID floorPlanId = uploadFile(
                project,
                "preview.jpg",
                "image/jpeg",
                content
        );

        assertPreview(
                project,
                floorPlanId,
                "preview.jpg",
                MediaType.IMAGE_JPEG,
                content
        );
    }

    @Test
    void shouldReturn404WhenPreviewingUnknownFloorPlan()
            throws Exception {

        Project project = createProject();

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}/preview",
                                project.getId(),
                                UUID.randomUUID()
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldNotAllowPreviewFromAnotherProject()
            throws Exception {

        Project ownerProject = createProject();
        Project otherProject = createProject();

        UUID floorPlanId = uploadPdf(
                ownerProject,
                "private-preview.pdf",
                createValidPdf()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}/preview",
                                otherProject.getId(),
                                floorPlanId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }



    // --------------------------------------------------
    // Download tests
    // --------------------------------------------------

    @Test
    void shouldDownloadOriginalFile() throws Exception {
        Project project = createProject();

        byte[] content = "%PDF-1.7 test download content".getBytes();

        UUID floorPlanId = uploadPdf(
                project,
                "download-test.pdf",
                content
        );

        MvcResult result = mockMvc.perform(
                        get(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}/file",
                                project.getId(),
                                floorPlanId
                        )
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString(
                                "download-test.pdf"
                        )
                ))
                .andExpect(header().longValue(
                        "Content-Length",
                        content.length
                ))
                .andExpect(content().bytes(content));
    }

    // --------------------------------------------------
    // Delete tests
    // --------------------------------------------------

    @Test
    void shouldDeleteFloorPlanAndStoredFile() throws Exception {
        Project project = createProject();

        UUID floorPlanId = uploadPdf(
                project,
                "delete-test.pdf",
                createValidPdf()
        );

        FloorPlan floorPlan = floorPlanRepository.findById(floorPlanId)
                .orElseThrow();

        Path storedFile = getStoredFile(floorPlan);

        assertTrue(
                Files.exists(storedFile),
                "File should exist before deletion"
        );

        mockMvc.perform(
                        delete(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}",
                                project.getId(),
                                floorPlanId
                        )
                )
                .andExpect(status().isNoContent());

        assertTrue(
                floorPlanRepository.findById(floorPlanId).isEmpty(),
                "Database record should be deleted"
        );

        assertFalse(
                Files.exists(storedFile),
                "Stored file should be deleted"
        );
    }

    @Test
    void shouldReturn404WhenDeletingUnknownFloorPlan()
            throws Exception {

        Project project = createProject();

        mockMvc.perform(
                        delete(
                                "/api/v1/projects/{projectId}/floor-plans/{floorPlanId}",
                                project.getId(),
                                UUID.randomUUID()
                        )
                )
                .andExpect(status().isNotFound());
    }
}
