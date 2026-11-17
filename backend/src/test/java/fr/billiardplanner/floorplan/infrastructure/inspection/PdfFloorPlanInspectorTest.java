package fr.billiardplanner.floorplan.infrastructure.inspection;

import fr.billiardplanner.floorplan.application.inspection.PdfInspectionException;
import fr.billiardplanner.floorplan.application.inspection.PdfInspectionResult;
import fr.billiardplanner.floorplan.application.inspection.PdfPageInfo;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class PdfFloorPlanInspectorTest {

    private PdfFloorPlanInspector inspector;

    @BeforeEach
    void setUp() {
        inspector = new PdfFloorPlanInspector();
    }

    @Test
    void shouldInspectSinglePagePdf() throws Exception {

        byte[] pdf = createPdf(
                new PDPage(new PDRectangle(612, 792))
        );

        PdfInspectionResult result = inspector.inspect(
                new ByteArrayInputStream(pdf)
        );

        assertEquals(1, result.pageCount());
        assertEquals(1, result.pages().size());

        PdfPageInfo page = result.pages().get(0);

        assertEquals(1, page.pageNumber());
        assertEquals(612.0, page.widthPoints(), 0.01);
        assertEquals(792.0, page.heightPoints(), 0.01);
        assertEquals(0, page.rotationDegrees());
    }

    @Test
    void shouldInspectAllPagesOfMultiPagePdf() throws Exception {

        PDPage firstPage = new PDPage(
                new PDRectangle(612, 792)
        );

        PDPage secondPage = new PDPage(
                new PDRectangle(800, 600)
        );

        PDPage thirdPage = new PDPage(
                new PDRectangle(400, 300)
        );

        thirdPage.setRotation(90);

        byte[] pdf = createPdf(
                firstPage,
                secondPage,
                thirdPage
        );

        PdfInspectionResult result = inspector.inspect(
                new ByteArrayInputStream(pdf)
        );

        assertEquals(3, result.pageCount());
        assertEquals(3, result.pages().size());

        assertEquals(612.0, result.pages().get(0).widthPoints(), 0.01);
        assertEquals(792.0, result.pages().get(0).heightPoints(), 0.01);
        assertEquals(0, result.pages().get(0).rotationDegrees());

        assertEquals(800.0, result.pages().get(1).widthPoints(), 0.01);
        assertEquals(600.0, result.pages().get(1).heightPoints(), 0.01);

        assertEquals(400.0, result.pages().get(2).widthPoints(), 0.01);
        assertEquals(300.0, result.pages().get(2).heightPoints(), 0.01);
        assertEquals(90, result.pages().get(2).rotationDegrees());
    }

    @Test
    void shouldRejectCorruptedPdf() {

        byte[] corruptedPdf = "%PDF-1.7".getBytes();

        assertThrows(
                PdfInspectionException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(corruptedPdf)
                )
        );
    }

    @Test
    void shouldRejectEncryptedPdf() throws Exception {

        byte[] encryptedPdf = createEncryptedPdf();

        PdfInspectionException exception = assertThrows(
                PdfInspectionException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(encryptedPdf)
                )
        );

        assertTrue(
                exception.getMessage().toLowerCase().contains("password")
        );
    }

    @Test
    void shouldRejectEmptyPdf() {

        assertThrows(
                PdfInspectionException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(new byte[0])
                )
        );
    }

    @Test
    void shouldRejectPdfLargerThan25Mb() {

        byte[] oversizedPdf = new byte[25 * 1024 * 1024 + 1];

        assertThrows(
                PdfInspectionException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(oversizedPdf)
                )
        );
    }

    @Test
    void shouldRejectNullInputStream() {

        assertThrows(
                IllegalArgumentException.class,
                () -> inspector.inspect(null)
        );
    }

    @Test
    void shouldNotCloseInputStream() throws Exception {

        byte[] pdf = createPdf(
                new PDPage(new PDRectangle(612, 792))
        );

        class TrackingInputStream extends ByteArrayInputStream {

            private boolean closed;

            TrackingInputStream(byte[] bytes) {
                super(bytes);
            }

            @Override
            public void close() throws IOException {
                closed = true;
                super.close();
            }
        }

        TrackingInputStream input = new TrackingInputStream(pdf);

        inspector.inspect(input);

        assertFalse(input.closed);
    }

    private byte[] createPdf(PDPage... pages) throws IOException {

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            for (PDPage page : pages) {
                document.addPage(page);
            }

            document.save(output);

            return output.toByteArray();
        }
    }

    private byte[] createEncryptedPdf() throws IOException {

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            document.addPage(
                    new PDPage(new PDRectangle(612, 792))
            );

            StandardProtectionPolicy policy =
                    new StandardProtectionPolicy(
                            "owner-password",
                            "user-password",
                            new AccessPermission()
                    );

            policy.setEncryptionKeyLength(128);

            document.protect(policy);
            document.save(output);

            return output.toByteArray();
        }
    }
}