package fr.billiardplanner.floorplan.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FloorPlanInspectionTest {

    @Test
    void shouldCreatePdfPage() {
        UUID floorPlanId = UUID.randomUUID();

        FloorPlanPage page = FloorPlanPage.create(
                floorPlanId, 1, 595.0, 842.0, 0
        );

        assertThat(page.id()).isNotNull();
        assertThat(page.floorPlanId()).isEqualTo(floorPlanId);
        assertThat(page.pageNumber()).isEqualTo(1);
    }

    @Test
    void shouldRejectInvalidPdfPageNumber() {
        assertThatThrownBy(() -> FloorPlanPage.create(
                UUID.randomUUID(), 0, 595.0, 842.0, 0
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidPdfPageDimensions() {
        assertThatThrownBy(() -> FloorPlanPage.create(
                UUID.randomUUID(), 1, 0, 842.0, 0
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidPdfPageRotation() {
        assertThatThrownBy(() -> FloorPlanPage.create(
                UUID.randomUUID(), 1, 595.0, 842.0, 45
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreatePngMetadata() {
        UUID floorPlanId = UUID.randomUUID();

        FloorPlanImageMetadata metadata = new FloorPlanImageMetadata(
                floorPlanId,
                "png",
                1920,
                1080,
                "RGB",
                300,
                300,
                6.4,
                3.6
        );

        assertThat(metadata.format()).isEqualTo("PNG");
        assertThat(metadata.widthPixels()).isEqualTo(1920);
    }

    @Test
    void shouldAllowMissingDpiAndPhysicalDimensions() {
        FloorPlanImageMetadata metadata = new FloorPlanImageMetadata(
                UUID.randomUUID(),
                "JPEG",
                1920,
                1080,
                "RGB",
                null,
                null,
                null,
                null
        );

        assertThat(metadata.dpiX()).isNull();
        assertThat(metadata.physicalWidthInches()).isNull();
    }

    @Test
    void shouldRejectUnsupportedImageFormat() {
        assertThatThrownBy(() -> new FloorPlanImageMetadata(
                UUID.randomUUID(),
                "GIF",
                1920,
                1080,
                "RGB",
                null,
                null,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidImageDimensions() {
        assertThatThrownBy(() -> new FloorPlanImageMetadata(
                UUID.randomUUID(),
                "PNG",
                0,
                1080,
                "RGB",
                null,
                null,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidDpi() {
        assertThatThrownBy(() -> new FloorPlanImageMetadata(
                UUID.randomUUID(),
                "PNG",
                1920,
                1080,
                "RGB",
                0,
                null,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidPhysicalDimensions() {
        assertThatThrownBy(() -> new FloorPlanImageMetadata(
                UUID.randomUUID(),
                "PNG",
                1920,
                1080,
                "RGB",
                null,
                null,
                -1.0,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}