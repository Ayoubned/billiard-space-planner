package fr.billiardplanner.floorplan.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FloorPlanTest {

    private UUID projectId;
    private Instant now;
    private String validSha256;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        now = Instant.parse("2026-11-16T10:00:00Z");
        validSha256 = "a".repeat(64);
    }

    private FloorPlan createFloorPlan() {
        return FloorPlan.create(
                projectId,
                "floor-plan.pdf",
                "floor-plans/test-file.pdf",
                "application/pdf",
                1024,
                validSha256,
                now
        );
    }

    @Test
    void shouldCreateFloorPlanWithUploadedStatus() {
        FloorPlan floorPlan = createFloorPlan();

        assertNotNull(floorPlan.getId());
        assertEquals(projectId, floorPlan.getProjectId());
        assertEquals("floor-plan.pdf", floorPlan.getOriginalFilename());
        assertEquals("floor-plans/test-file.pdf", floorPlan.getStorageKey());
        assertEquals("application/pdf", floorPlan.getContentType());
        assertEquals(1024, floorPlan.getFileSizeBytes());
        assertEquals(validSha256, floorPlan.getSha256());
        assertEquals(FloorPlanStatus.UPLOADED, floorPlan.getStatus());
        assertNull(floorPlan.getPageCount());
        assertEquals(now, floorPlan.getCreatedAt());
        assertEquals(now, floorPlan.getUpdatedAt());
    }

    @Test
    void shouldAcceptSupportedContentTypes() {
        assertDoesNotThrow(() -> FloorPlan.create(
                projectId, "plan.pdf", "key-1",
                "application/pdf", 1024, validSha256, now
        ));

        assertDoesNotThrow(() -> FloorPlan.create(
                projectId, "plan.png", "key-2",
                "image/png", 1024, validSha256, now
        ));

        assertDoesNotThrow(() -> FloorPlan.create(
                projectId, "plan.jpg", "key-3",
                "image/jpeg", 1024, validSha256, now
        ));
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        assertThrows(IllegalArgumentException.class, () ->
                FloorPlan.create(
                        projectId, "plan.txt", "key",
                        "text/plain", 1024, validSha256, now
                )
        );
    }

    @Test
    void shouldRejectEmptyFilename() {
        assertThrows(IllegalArgumentException.class, () ->
                FloorPlan.create(
                        projectId, "  ", "key",
                        "application/pdf", 1024, validSha256, now
                )
        );
    }

    @Test
    void shouldRejectMissingProjectId() {
        assertThrows(NullPointerException.class, () ->
                FloorPlan.create(
                        null, "plan.pdf", "key",
                        "application/pdf", 1024, validSha256, now
                )
        );
    }

    @Test
    void shouldRejectInvalidSha256() {
        assertThrows(IllegalArgumentException.class, () ->
                FloorPlan.create(
                        projectId, "plan.pdf", "key",
                        "application/pdf", 1024, "invalid-hash", now
                )
        );
    }

    @Test
    void shouldNormalizeSha256ToLowercase() {
        String uppercaseHash = "A".repeat(64);

        FloorPlan floorPlan = FloorPlan.create(
                projectId, "plan.pdf", "key",
                "application/pdf", 1024, uppercaseHash, now
        );

        assertEquals("a".repeat(64), floorPlan.getSha256());
    }

    @Test
    void shouldRejectZeroFileSize() {
        assertThrows(IllegalArgumentException.class, () ->
                FloorPlan.create(
                        projectId, "plan.pdf", "key",
                        "application/pdf", 0, validSha256, now
                )
        );
    }

    @Test
    void shouldRejectNegativeFileSize() {
        assertThrows(IllegalArgumentException.class, () ->
                FloorPlan.create(
                        projectId, "plan.pdf", "key",
                        "application/pdf", -100, validSha256, now
                )
        );
    }

    @Test
    void shouldTransitionFromUploadedToProcessing() {
        FloorPlan floorPlan = createFloorPlan();
        Instant processingTime = now.plusSeconds(60);

        floorPlan.markProcessing(processingTime);

        assertEquals(FloorPlanStatus.PROCESSING, floorPlan.getStatus());
        assertEquals(processingTime, floorPlan.getUpdatedAt());
    }

    @Test
    void shouldTransitionFromProcessingToReady() {
        FloorPlan floorPlan = createFloorPlan();
        Instant readyTime = now.plusSeconds(120);

        floorPlan.markProcessing(now.plusSeconds(60));
        floorPlan.markReady(3, readyTime);

        assertEquals(FloorPlanStatus.READY, floorPlan.getStatus());
        assertEquals(3, floorPlan.getPageCount());
        assertEquals(readyTime, floorPlan.getUpdatedAt());
    }

    @Test
    void shouldTransitionFromProcessingToFailed() {
        FloorPlan floorPlan = createFloorPlan();
        Instant failedTime = now.plusSeconds(120);

        floorPlan.markProcessing(now.plusSeconds(60));
        floorPlan.markFailed(failedTime);

        assertEquals(FloorPlanStatus.FAILED, floorPlan.getStatus());
        assertEquals(failedTime, floorPlan.getUpdatedAt());
    }

    @Test
    void shouldRejectInvalidStatusTransitions() {
        FloorPlan floorPlan = createFloorPlan();

        assertThrows(IllegalStateException.class, () ->
                floorPlan.markReady(1, now)
        );

        assertThrows(IllegalStateException.class, () ->
                floorPlan.markFailed(now)
        );

        floorPlan.markProcessing(now.plusSeconds(60));

        assertThrows(IllegalStateException.class, () ->
                floorPlan.markProcessing(now.plusSeconds(120))
        );
    }

    @Test
    void shouldRejectInvalidPageCount() {
        FloorPlan floorPlan = createFloorPlan();
        floorPlan.markProcessing(now.plusSeconds(60));

        assertThrows(IllegalArgumentException.class, () ->
                floorPlan.markReady(0, now.plusSeconds(120))
        );

        assertThrows(IllegalArgumentException.class, () ->
                floorPlan.markReady(-1, now.plusSeconds(120))
        );

        // Invalid input must not change the current state.
        assertEquals(FloorPlanStatus.PROCESSING, floorPlan.getStatus());
        assertNull(floorPlan.getPageCount());
    }


@Test
void shouldRejectNullTimestampWithoutChangingState() {
    FloorPlan floorPlan = createFloorPlan();

    assertThrows(NullPointerException.class, () ->
            floorPlan.markProcessing(null)
    );

    assertEquals(FloorPlanStatus.UPLOADED, floorPlan.getStatus());
    assertEquals(now, floorPlan.getUpdatedAt());

    floorPlan.markProcessing(now.plusSeconds(60));

    assertThrows(NullPointerException.class, () ->
            floorPlan.markReady(3, null)
    );

    assertEquals(FloorPlanStatus.PROCESSING, floorPlan.getStatus());
    assertNull(floorPlan.getPageCount());
    assertEquals(now.plusSeconds(60), floorPlan.getUpdatedAt());

    assertThrows(NullPointerException.class, () ->
            floorPlan.markFailed(null)
    );

    assertEquals(FloorPlanStatus.PROCESSING, floorPlan.getStatus());
    assertEquals(now.plusSeconds(60), floorPlan.getUpdatedAt());
}

}