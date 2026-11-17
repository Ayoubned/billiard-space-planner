package fr.billiardplanner.floorplan.application;

import fr.billiardplanner.floorplan.application.inspection.ImageInspectionResult;
import fr.billiardplanner.floorplan.application.inspection.PdfInspectionResult;
import fr.billiardplanner.floorplan.application.inspection.PdfPageInfo;
import fr.billiardplanner.floorplan.application.storage.FloorPlanStorage;
import fr.billiardplanner.floorplan.application.storage.StoredFile;
import fr.billiardplanner.floorplan.application.validation.FloorPlanFileValidator;
import fr.billiardplanner.floorplan.domain.FloorPlan;
import fr.billiardplanner.floorplan.domain.FloorPlanInspectionRepository;
import fr.billiardplanner.floorplan.domain.FloorPlanRepository;
import fr.billiardplanner.floorplan.domain.FloorPlanStatus;
import fr.billiardplanner.floorplan.infrastructure.inspection.ImageFloorPlanInspector;
import fr.billiardplanner.floorplan.infrastructure.inspection.PdfFloorPlanInspector;
import fr.billiardplanner.project.application.ProjectNotFoundException;
import fr.billiardplanner.project.domain.Project;
import fr.billiardplanner.project.domain.ProjectRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloorPlanApplicationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-11-16T15:00:00Z");

    private static final String SHA256 = "a".repeat(64);

    private static final long MAX_SIZE =
            FloorPlanFileValidator.MAX_FILE_SIZE_BYTES;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private FloorPlanRepository floorPlanRepository;

    @Mock
    private FloorPlanInspectionRepository inspectionRepository;

    @Mock
    private FloorPlanStorage floorPlanStorage;

    @Mock
    private PdfFloorPlanInspector pdfInspector;

    @Mock
    private ImageFloorPlanInspector imageInspector;

    private FloorPlanApplicationService service;

    private UUID projectId;
    private Project project;

    @BeforeEach
    void setUp() {
        service = new FloorPlanApplicationService(
                projectRepository,
                floorPlanRepository,
                inspectionRepository,
                floorPlanStorage,
                new FloorPlanFileValidator(),
                pdfInspector,
                imageInspector,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        projectId = UUID.randomUUID();

        project = Project.create(
                "Upload test project",
                "Test client",
                "Lyon, France",
                "Upload integration test",
                NOW
        );
    }

    @Test
    void shouldSuccessfullyUploadPdf() throws IOException {
        byte[] content = "%PDF-1.7".getBytes();

        assertSuccessfulUpload(
                "floor-plan.pdf",
                "application/pdf",
                content
        );

        verify(pdfInspector).inspect(any(InputStream.class));
        verify(inspectionRepository).savePdfPages(
                any(UUID.class),
                anyList()
        );
        verifyNoInteractions(imageInspector);
    }

    @Test
    void shouldSuccessfullyUploadPng() throws IOException {
        byte[] content = hex("89504e470d0a1a0a");

        assertSuccessfulUpload(
                "floor-plan.png",
                "image/png",
                content
        );

        verify(imageInspector).inspect(
                any(InputStream.class),
                eq("image/png")
        );
        verify(inspectionRepository).saveImageMetadata(any());
        verifyNoInteractions(pdfInspector);
    }

    @Test
    void shouldSuccessfullyUploadJpeg() throws IOException {
        byte[] content = hex("ffd8ffe000");

        assertSuccessfulUpload(
                "floor-plan.jpg",
                "image/jpeg",
                content
        );

        verify(imageInspector).inspect(
                any(InputStream.class),
                eq("image/jpeg")
        );
        verify(inspectionRepository).saveImageMetadata(any());
        verifyNoInteractions(pdfInspector);
    }

    @Test
    void shouldRejectUploadWhenProjectDoesNotExist() {
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.empty());

        UploadFloorPlanCommand command = command(
                "floor-plan.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes()
        );

        assertThrows(
                ProjectNotFoundException.class,
                () -> service.upload(command)
        );

        verifyNoInteractions(floorPlanStorage);
        verifyNoInteractions(floorPlanRepository);
        verifyNoInteractions(inspectionRepository);
        verifyNoInteractions(pdfInspector);
        verifyNoInteractions(imageInspector);
    }

    @Test
    void shouldRejectInvalidFileSignature() {
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));

        byte[] invalidContent = "Not a PDF".getBytes();

        UploadFloorPlanCommand command = command(
                "floor-plan.pdf",
                "application/pdf",
                invalidContent
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(command)
        );

        verifyNoInteractions(floorPlanStorage);
        verifyNoInteractions(floorPlanRepository);
        verifyNoInteractions(inspectionRepository);
    }

    @Test
    void shouldRejectFileSizeMismatchAndDeleteStoredFile()
            throws IOException {

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));

        byte[] content = "%PDF-1.7".getBytes();

        String storageKey = UUID.randomUUID().toString();

        when(floorPlanStorage.store(
                any(InputStream.class),
                eq(MAX_SIZE)
        )).thenReturn(
                new StoredFile(storageKey, content.length, SHA256)
        );

        UploadFloorPlanCommand command = new UploadFloorPlanCommand(
                projectId,
                "floor-plan.pdf",
                "application/pdf",
                content.length + 10,
                new ByteArrayInputStream(content)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(command)
        );

        verify(floorPlanStorage).delete(storageKey);
        verifyNoInteractions(floorPlanRepository);
        verifyNoInteractions(inspectionRepository);
        verifyNoInteractions(pdfInspector);
        verifyNoInteractions(imageInspector);
    }

    @Test
    void shouldDeleteStoredFileWhenDatabasePersistenceFails()
            throws IOException {

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));

        byte[] content = "%PDF-1.7".getBytes();

        String storageKey = UUID.randomUUID().toString();

        when(floorPlanStorage.store(
                any(InputStream.class),
                eq(MAX_SIZE)
        )).thenReturn(
                new StoredFile(storageKey, content.length, SHA256)
        );

        when(floorPlanRepository.save(any(FloorPlan.class)))
                .thenThrow(new IllegalStateException("Database failure"));

        UploadFloorPlanCommand command = command(
                "floor-plan.pdf",
                "application/pdf",
                content
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.upload(command)
        );

        verify(floorPlanStorage).delete(storageKey);
        verifyNoInteractions(pdfInspector);
        verifyNoInteractions(imageInspector);
    }

    @Test
    void shouldRejectFilesLargerThan25Mb() {
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));

        byte[] content = "%PDF-1.7".getBytes();

        UploadFloorPlanCommand command = new UploadFloorPlanCommand(
                projectId,
                "floor-plan.pdf",
                "application/pdf",
                MAX_SIZE + 1,
                new ByteArrayInputStream(content)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(command)
        );

        verifyNoInteractions(floorPlanStorage);
        verifyNoInteractions(floorPlanRepository);
    }

    @Test
    void shouldRejectNullCommand() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(null)
        );

        verifyNoInteractions(projectRepository);
        verifyNoInteractions(floorPlanStorage);
        verifyNoInteractions(floorPlanRepository);
    }

    @Test
    void shouldRejectCommandWithoutProjectId() {
        UploadFloorPlanCommand command = new UploadFloorPlanCommand(
                null,
                "floor-plan.pdf",
                "application/pdf",
                8,
                new ByteArrayInputStream("%PDF-1.7".getBytes())
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(command)
        );

        verifyNoInteractions(projectRepository);
        verifyNoInteractions(floorPlanStorage);
    }

    @Test
    void shouldRejectCommandWithoutInputStream() {
        UploadFloorPlanCommand command = new UploadFloorPlanCommand(
                projectId,
                "floor-plan.pdf",
                "application/pdf",
                8,
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(command)
        );

        verifyNoInteractions(floorPlanStorage);
    }

    private void assertSuccessfulUpload(
            String filename,
            String contentType,
            byte[] content
    ) throws IOException {

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));

        String storageKey = UUID.randomUUID().toString();

        when(floorPlanStorage.store(
                any(InputStream.class),
                eq(MAX_SIZE)
        )).thenReturn(
                new StoredFile(storageKey, content.length, SHA256)
        );

        when(floorPlanStorage.open(storageKey))
                .thenReturn(new ByteArrayInputStream(content));

        when(floorPlanRepository.save(any(FloorPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        if ("application/pdf".equals(contentType)) {
            when(pdfInspector.inspect(any(InputStream.class)))
                    .thenReturn(new PdfInspectionResult(
                            1,
                            List.of(
                                    new PdfPageInfo(
                                            1,
                                            595.0,
                                            842.0,
                                            0
                                    )
                            )
                    ));
        } else {
            String format = "image/png".equals(contentType)
                    ? "png"
                    : "jpeg";

            when(imageInspector.inspect(
                    any(InputStream.class),
                    eq(contentType)
            )).thenReturn(new ImageInspectionResult(
                    format,
                    1200,
                    800,
                    "RGB",
                    null,
                    null,
                    null,
                    null
            ));
        }

        UploadFloorPlanCommand command = command(
                filename,
                contentType,
                content
        );

        FloorPlanResult result = service.upload(command);

        assertNotNull(result.id());
        assertEquals(projectId, result.projectId());
        assertEquals(filename, result.originalFilename());
        assertEquals(contentType, result.contentType());
        assertEquals(content.length, result.fileSizeBytes());
        assertEquals(SHA256, result.sha256());
        assertEquals(FloorPlanStatus.READY, result.status());

        if ("application/pdf".equals(contentType)) {
            assertEquals(1, result.pageCount());
        } else {
            assertNull(result.pageCount());
        }

        assertEquals(NOW, result.createdAt());
        assertEquals(NOW, result.updatedAt());

        verify(projectRepository).findById(projectId);

        verify(floorPlanStorage).store(
                any(InputStream.class),
                eq(MAX_SIZE)
        );

        verify(floorPlanStorage).open(storageKey);

        verify(floorPlanRepository, times(3))
                .save(any(FloorPlan.class));

        verify(floorPlanStorage, never()).delete(anyString());
    }

    private UploadFloorPlanCommand command(
            String filename,
            String contentType,
            byte[] content
    ) {
        return new UploadFloorPlanCommand(
                projectId,
                filename,
                contentType,
                content.length,
                new ByteArrayInputStream(content)
        );
    }

    private byte[] hex(String value) {
        return java.util.HexFormat.of().parseHex(value);
    }
}
