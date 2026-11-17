package fr.billiardplanner.floorplan.application;

import fr.billiardplanner.floorplan.application.inspection.ImageInspectionResult;
import fr.billiardplanner.floorplan.application.inspection.PdfInspectionResult;
import fr.billiardplanner.floorplan.application.storage.FloorPlanStorage;
import fr.billiardplanner.floorplan.application.storage.StoredFile;
import fr.billiardplanner.floorplan.application.validation.FloorPlanFileValidator;
import fr.billiardplanner.floorplan.domain.FloorPlan;
import fr.billiardplanner.floorplan.domain.FloorPlanImageMetadata;
import fr.billiardplanner.floorplan.domain.FloorPlanInspectionRepository;
import fr.billiardplanner.floorplan.domain.FloorPlanPage;
import fr.billiardplanner.floorplan.domain.FloorPlanRepository;
import fr.billiardplanner.floorplan.infrastructure.inspection.ImageFloorPlanInspector;
import fr.billiardplanner.floorplan.infrastructure.inspection.PdfFloorPlanInspector;
import fr.billiardplanner.project.application.ProjectNotFoundException;
import fr.billiardplanner.project.domain.ProjectRepository;
import fr.billiardplanner.floorplan.domain.FloorPlanStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FloorPlanApplicationService {

    private static final int SIGNATURE_LENGTH = 8;

    private final ProjectRepository projectRepository;
    private final FloorPlanRepository floorPlanRepository;
    private final FloorPlanInspectionRepository inspectionRepository;
    private final FloorPlanStorage floorPlanStorage;
    private final FloorPlanFileValidator fileValidator;
    private final PdfFloorPlanInspector pdfInspector;
    private final ImageFloorPlanInspector imageInspector;
    private final Clock clock;

    public FloorPlanApplicationService(
            ProjectRepository projectRepository,
            FloorPlanRepository floorPlanRepository,
            FloorPlanInspectionRepository inspectionRepository,
            FloorPlanStorage floorPlanStorage,
            FloorPlanFileValidator fileValidator,
            PdfFloorPlanInspector pdfInspector,
            ImageFloorPlanInspector imageInspector,
            Clock clock
    ) {
        this.projectRepository = projectRepository;
        this.floorPlanRepository = floorPlanRepository;
        this.inspectionRepository = inspectionRepository;
        this.floorPlanStorage = floorPlanStorage;
        this.fileValidator = fileValidator;
        this.pdfInspector = pdfInspector;
        this.imageInspector = imageInspector;
        this.clock = clock;
    }

    // --------------------------------------------------
    // Upload and inspect
    // --------------------------------------------------

    public FloorPlanResult upload(UploadFloorPlanCommand command)
            throws IOException {

        validateCommand(command);

        projectRepository.findById(command.projectId())
                .orElseThrow(() ->
                        new ProjectNotFoundException(command.projectId())
                );

        PushbackInputStream input = new PushbackInputStream(
                command.inputStream(),
                SIGNATURE_LENGTH
        );

        byte[] header = input.readNBytes(SIGNATURE_LENGTH);
        input.unread(header);

        String validatedContentType = fileValidator.validate(
                command.originalFilename(),
                command.contentType(),
                command.fileSizeBytes(),
                header
        );

        StoredFile storedFile = floorPlanStorage.store(
                input,
                FloorPlanFileValidator.MAX_FILE_SIZE_BYTES
        );

        FloorPlan floorPlan;

        try {
            if (storedFile.fileSizeBytes() != command.fileSizeBytes()) {
                throw new IllegalArgumentException(
                        "Uploaded file size does not match the declared size"
                );
            }

            Instant now = clock.instant();

            floorPlan = FloorPlan.create(
                    command.projectId(),
                    command.originalFilename(),
                    storedFile.storageKey(),
                    validatedContentType,
                    storedFile.fileSizeBytes(),
                    storedFile.sha256(),
                    now
            );

            floorPlan = floorPlanRepository.save(floorPlan);

        } catch (RuntimeException exception) {
            deleteStoredFileAfterFailure(
                    storedFile.storageKey(),
                    exception
            );
            throw exception;
        }

        floorPlan.markProcessing(clock.instant());
        floorPlan = floorPlanRepository.save(floorPlan);

        Integer pageCount;

        try {
            pageCount = inspectAndPersist(floorPlan);
        } catch (IOException | RuntimeException inspectionException) {
            floorPlan.markFailed(clock.instant());

            FloorPlan failedFloorPlan =
                    floorPlanRepository.save(floorPlan);

            return toResult(failedFloorPlan);
        }

        floorPlan.markReady(pageCount, clock.instant());

        FloorPlan readyFloorPlan = floorPlanRepository.save(floorPlan);

        return toResult(readyFloorPlan);
    }

    private Integer inspectAndPersist(FloorPlan floorPlan)
            throws IOException {

        try (InputStream input = floorPlanStorage.open(
                floorPlan.getStorageKey()
        )) {

            if ("application/pdf".equals(floorPlan.getContentType())) {
                PdfInspectionResult result = pdfInspector.inspect(input);

                List<FloorPlanPage> pages = result.pages()
                        .stream()
                        .map(page -> new FloorPlanPage(
                                UUID.randomUUID(),
                                floorPlan.getId(),
                                page.pageNumber(),
                                page.widthPoints(),
                                page.heightPoints(),
                                page.rotationDegrees()
                        ))
                        .toList();

                inspectionRepository.savePdfPages(
                        floorPlan.getId(),
                        pages
                );

                return result.pageCount();
            }

            ImageInspectionResult result = imageInspector.inspect(
                    input,
                    floorPlan.getContentType()
            );

            FloorPlanImageMetadata metadata =
                    new FloorPlanImageMetadata(
                            floorPlan.getId(),
                            result.format(),
                            result.widthPixels(),
                            result.heightPixels(),
                            result.colorModel(),
                            result.dpiX(),
                            result.dpiY(),
                            result.physicalWidthInches(),
                            result.physicalHeightInches()
                    );

            inspectionRepository.saveImageMetadata(metadata);

            return null;
        }
    }

    // --------------------------------------------------
    // List floor plans
    // --------------------------------------------------

    public List<FloorPlanResult> listByProjectId(UUID projectId) {

        requireProject(projectId);

        return floorPlanRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    // --------------------------------------------------
    // Get floor plan metadata
    // --------------------------------------------------

    public FloorPlanResult getById(
            UUID projectId,
            UUID floorPlanId
    ) {
        FloorPlan floorPlan = findOwnedFloorPlan(projectId, floorPlanId);

        return toResult(floorPlan);
    }


    // --------------------------------------------------
    // Get floor plan inspection metadata
    // --------------------------------------------------

    public FloorPlanInspectionResult getInspection(
            UUID projectId,
            UUID floorPlanId
    ) {
        FloorPlan floorPlan = findOwnedFloorPlan(projectId, floorPlanId);

        if (floorPlan.getStatus() != fr.billiardplanner.floorplan.domain.FloorPlanStatus.READY) {
            throw new IllegalStateException(
                    "Floor plan inspection is not available. Current status: "
                            + floorPlan.getStatus()
            );
        }

        if ("application/pdf".equals(floorPlan.getContentType())) {
            List<FloorPlanPage> pages =
                    inspectionRepository.findPdfPages(floorPlanId);

            if (pages.isEmpty()) {
                throw new IllegalStateException(
                        "PDF inspection metadata is missing"
                );
            }

            return new FloorPlanInspectionResult(
                    floorPlanId,
                    "PDF",
                    floorPlan.getPageCount(),
                    pages,
                    null
            );
        }

        if ("image/png".equals(floorPlan.getContentType())
                || "image/jpeg".equals(floorPlan.getContentType())) {

            FloorPlanImageMetadata imageMetadata =
                    inspectionRepository.findImageMetadata(floorPlanId)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Image inspection metadata is missing"
                                    )
                            );

            return new FloorPlanInspectionResult(
                    floorPlanId,
                    "IMAGE",
                    null,
                    List.of(),
                    imageMetadata
            );
        }

        throw new IllegalStateException(
                "Unsupported floor plan content type: "
                        + floorPlan.getContentType()
        );
    }


    // --------------------------------------------------
    // Open original file
    // --------------------------------------------------

    public FloorPlanDownload openFile(
            UUID projectId,
            UUID floorPlanId
    ) throws IOException {

        FloorPlan floorPlan = findOwnedFloorPlan(projectId, floorPlanId);

        InputStream inputStream = floorPlanStorage.open(
                floorPlan.getStorageKey()
        );

        return new FloorPlanDownload(
                floorPlan.getId(),
                floorPlan.getProjectId(),
                floorPlan.getOriginalFilename(),
                floorPlan.getContentType(),
                floorPlan.getFileSizeBytes(),
                floorPlan.getSha256(),
                inputStream
        );
    }

    // --------------------------------------------------
    // Delete floor plan
    // --------------------------------------------------

    public void delete(
            UUID projectId,
            UUID floorPlanId
    ) throws IOException {

        FloorPlan floorPlan = findOwnedFloorPlan(projectId, floorPlanId);

        // Delete the database record first.
        // If file cleanup fails, an orphaned file may remain,
        // but the database will not reference a missing file.
        floorPlanRepository.deleteById(floorPlan.getId());

        floorPlanStorage.delete(floorPlan.getStorageKey());
    }

    // --------------------------------------------------
    // Internal helpers
    // --------------------------------------------------

    private void requireProject(UUID projectId) {

        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }

        projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ProjectNotFoundException(projectId)
                );
    }

    private FloorPlan findOwnedFloorPlan(
            UUID projectId,
            UUID floorPlanId
    ) {
        requireProject(projectId);

        if (floorPlanId == null) {
            throw new IllegalArgumentException("Floor plan ID is required");
        }

        FloorPlan floorPlan = floorPlanRepository.findById(floorPlanId)
                .orElseThrow(() ->
                        new FloorPlanNotFoundException(projectId, floorPlanId)
                );

        if (!floorPlan.getProjectId().equals(projectId)) {
            throw new FloorPlanNotFoundException(projectId, floorPlanId);
        }

        return floorPlan;
    }

    private void validateCommand(UploadFloorPlanCommand command) {

        if (command == null) {
            throw new IllegalArgumentException("Upload command is required");
        }

        if (command.projectId() == null) {
            throw new IllegalArgumentException("Project ID is required");
        }

        if (command.inputStream() == null) {
            throw new IllegalArgumentException("File content is required");
        }
    }

    private void deleteStoredFileAfterFailure(
            String storageKey,
            RuntimeException originalException
    ) {
        try {
            floorPlanStorage.delete(storageKey);
        } catch (IOException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private FloorPlanResult toResult(FloorPlan floorPlan) {

        return new FloorPlanResult(
                floorPlan.getId(),
                floorPlan.getProjectId(),
                floorPlan.getOriginalFilename(),
                floorPlan.getContentType(),
                floorPlan.getFileSizeBytes(),
                floorPlan.getSha256(),
                floorPlan.getStatus(),
                floorPlan.getPageCount(),
                floorPlan.getCreatedAt(),
                floorPlan.getUpdatedAt()
        );
    }
}
