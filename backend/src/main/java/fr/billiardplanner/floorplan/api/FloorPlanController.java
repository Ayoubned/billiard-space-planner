package fr.billiardplanner.floorplan.api;

import fr.billiardplanner.floorplan.application.FloorPlanApplicationService;
import fr.billiardplanner.floorplan.application.FloorPlanDownload;
import fr.billiardplanner.floorplan.application.FloorPlanResult;
import fr.billiardplanner.floorplan.application.UploadFloorPlanCommand;
import fr.billiardplanner.floorplan.application.FloorPlanInspectionResult;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/floor-plans")
public class FloorPlanController {

    private final FloorPlanApplicationService floorPlanApplicationService;

    public FloorPlanController(
            FloorPlanApplicationService floorPlanApplicationService
    ) {
        this.floorPlanApplicationService = floorPlanApplicationService;
    }

    // --------------------------------------------------
    // Upload
    // --------------------------------------------------

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FloorPlanResponse> upload(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        FloorPlanResult result;

        try (InputStream inputStream = file.getInputStream()) {

            UploadFloorPlanCommand command = new UploadFloorPlanCommand(
                    projectId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    inputStream
            );

            result = floorPlanApplicationService.upload(command);
        }

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{floorPlanId}")
                .buildAndExpand(result.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(FloorPlanResponse.from(result));
    }

    // --------------------------------------------------
    // List floor plans
    // --------------------------------------------------

    @GetMapping
    public ResponseEntity<List<FloorPlanResponse>> list(
            @PathVariable UUID projectId
    ) {
        List<FloorPlanResponse> responses =
                floorPlanApplicationService.listByProjectId(projectId)
                        .stream()
                        .map(FloorPlanResponse::from)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    // --------------------------------------------------
    // Get metadata
    // --------------------------------------------------

    @GetMapping("/{floorPlanId}")
    public ResponseEntity<FloorPlanResponse> getById(
            @PathVariable UUID projectId,
            @PathVariable UUID floorPlanId
    ) {
        FloorPlanResult result =
                floorPlanApplicationService.getById(projectId, floorPlanId);

        return ResponseEntity.ok(FloorPlanResponse.from(result));
    }


    // --------------------------------------------------
    // Get inspection metadata
    // --------------------------------------------------

    @GetMapping("/{floorPlanId}/inspection")
    public ResponseEntity<FloorPlanInspectionResponse> getInspection(
            @PathVariable UUID projectId,
            @PathVariable UUID floorPlanId
    ) {
        FloorPlanInspectionResult result =
                floorPlanApplicationService.getInspection(
                        projectId,
                        floorPlanId
                );

        return ResponseEntity.ok(
                FloorPlanInspectionResponse.from(result)
        );
    }


    // --------------------------------------------------
    // Download original file
    // --------------------------------------------------

    @GetMapping("/{floorPlanId}/file")
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable UUID projectId,
            @PathVariable UUID floorPlanId
    ) throws IOException {

        FloorPlanDownload download =
                floorPlanApplicationService.openFile(projectId, floorPlanId);

        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(
                                download.originalFilename(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream inputStream = download.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.fileSizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(responseBody);
    }


    // --------------------------------------------------
    // Preview original file in browser
    // --------------------------------------------------

    @GetMapping("/{floorPlanId}/preview")
    public ResponseEntity<StreamingResponseBody> preview(
            @PathVariable UUID projectId,
            @PathVariable UUID floorPlanId
    ) throws IOException {

        FloorPlanDownload preview =
                floorPlanApplicationService.openFile(projectId, floorPlanId);

        ContentDisposition contentDisposition =
                ContentDisposition.inline()
                        .filename(
                                preview.originalFilename(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream inputStream = preview.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(preview.contentType()))
                .contentLength(preview.fileSizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(responseBody);
    }


    // --------------------------------------------------
    // Delete
    // --------------------------------------------------

    @DeleteMapping("/{floorPlanId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID floorPlanId
    ) throws IOException {

        floorPlanApplicationService.delete(projectId, floorPlanId);

        return ResponseEntity.noContent().build();
    }
}