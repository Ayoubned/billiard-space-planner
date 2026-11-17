package fr.billiardplanner.floorplan.application.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class FloorPlanFileValidatorTest {

    private FloorPlanFileValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FloorPlanFileValidator();
    }

    @Test
    void shouldAcceptValidPdf() {
        byte[] header = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);

        String result = validator.validate(
                "floor-plan.pdf",
                "application/pdf",
                1024,
                header
        );

        assertEquals("application/pdf", result);
    }

    @Test
    void shouldAcceptValidPng() {
        byte[] header =
                HexFormat.of().parseHex("89504e470d0a1a0a");

        String result = validator.validate(
                "floor-plan.png",
                "image/png",
                1024,
                header
        );

        assertEquals("image/png", result);
    }

    @Test
    void shouldAcceptValidJpeg() {
        byte[] header =
                HexFormat.of().parseHex("ffd8ffe000");

        String result = validator.validate(
                "floor-plan.jpg",
                "image/jpeg",
                1024,
                header
        );

        assertEquals("image/jpeg", result);
    }

    @Test
    void shouldAcceptJpegWithJpegExtension() {
        byte[] header =
                HexFormat.of().parseHex("ffd8ffe000");

        String result = validator.validate(
                "floor-plan.JPEG",
                "image/jpeg",
                1024,
                header
        );

        assertEquals("image/jpeg", result);
    }

    @Test
    void shouldRejectUnsupportedExtension() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "floor-plan.dwg",
                        "application/octet-stream",
                        1024,
                        new byte[]{1, 2, 3}
                )
        );
    }

    @Test
    void shouldRejectMismatchedContentType() {
        byte[] header =
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "floor-plan.pdf",
                        "image/png",
                        1024,
                        header
                )
        );
    }

    @Test
    void shouldRejectInvalidFileSignature() {
        byte[] fakePdf =
                "This is not a PDF".getBytes(StandardCharsets.US_ASCII);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "floor-plan.pdf",
                        "application/pdf",
                        1024,
                        fakePdf
                )
        );
    }

    @Test
    void shouldRejectFilesLargerThan25Mb() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "floor-plan.pdf",
                        "application/pdf",
                        FloorPlanFileValidator.MAX_FILE_SIZE_BYTES + 1,
                        "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)
                )
        );
    }

    @Test
    void shouldRejectEmptyFiles() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "floor-plan.pdf",
                        "application/pdf",
                        0,
                        new byte[0]
                )
        );
    }

    @Test
    void shouldRejectMissingFilename() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        " ",
                        "application/pdf",
                        1024,
                        "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)
                )
        );
    }

    @Test
    void shouldRejectMissingContentType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "floor-plan.pdf",
                        null,
                        1024,
                        "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)
                )
        );
    }

    @Test
    void shouldRejectTruncatedSignature() {
        byte[] truncatedHeader =
                HexFormat.of().parseHex("89504e470d0a");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "floor-plan.png",
                        "image/png",
                        1024,
                        truncatedHeader
                )
        );
    }
}