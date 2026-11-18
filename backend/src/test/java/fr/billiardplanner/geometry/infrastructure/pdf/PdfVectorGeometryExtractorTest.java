package fr.billiardplanner.geometry.infrastructure.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfVectorGeometryExtractorTest {

    private final PdfVectorGeometryExtractor extractor =
        new PdfVectorGeometryExtractor();

    @Test
    void extractsPageDimensions() throws IOException {
        byte[] pdf = createPdf(document -> {
            document.addPage(new PDPage(
                new PDRectangle(600, 400)
            ));
        });

        PdfVectorExtractionResult result = extractor.extract(
            new ByteArrayInputStream(pdf)
        );

        assertEquals(1, result.pageCount());

        PdfVectorPage page = result.pages().get(0);

        assertEquals(1, page.pageNumber());
        assertEquals(600, page.widthPoints(), 0.001);
        assertEquals(400, page.heightPoints(), 0.001);
    }

    @Test
    void extractsOpenLineSegments() throws IOException {
        byte[] pdf = createPdf(document -> {
            PDPage page = new PDPage(
                new PDRectangle(600, 400)
            );
            document.addPage(page);

            try (PDPageContentStream content =
                     new PDPageContentStream(document, page)) {
                content.moveTo(100, 100);
                content.lineTo(300, 100);
                content.lineTo(300, 200);
                content.stroke();
            }
        });

        PdfVectorExtractionResult result = extractor.extract(
            new ByteArrayInputStream(pdf)
        );

        assertEquals(1, result.pathCount());

        PdfVectorPath path = result.pages().get(0).paths().get(0);

        assertFalse(path.closed());
        assertTrue(path.stroked());
        assertFalse(path.filled());
        assertEquals(3, path.points().size());

        assertEquals(100, path.points().get(0).x(), 0.001);
        assertEquals(100, path.points().get(0).y(), 0.001);
        assertEquals(300, path.points().get(2).x(), 0.001);
        assertEquals(200, path.points().get(2).y(), 0.001);
    }

    @Test
    void extractsClosedRectangle() throws IOException {
        byte[] pdf = createPdf(document -> {
            PDPage page = new PDPage(
                new PDRectangle(600, 400)
            );
            document.addPage(page);

            try (PDPageContentStream content =
                     new PDPageContentStream(document, page)) {
                content.addRect(50, 50, 200, 100);
                content.stroke();
            }
        });

        PdfVectorExtractionResult result = extractor.extract(
            new ByteArrayInputStream(pdf)
        );

        assertEquals(1, result.pathCount());

        PdfVectorPath path = result.pages().get(0).paths().get(0);

        assertTrue(path.closed());
        assertTrue(path.stroked());
        assertFalse(path.filled());
        assertEquals(4, path.points().size());
    }

    @Test
    void extractsFilledPaths() throws IOException {
        byte[] pdf = createPdf(document -> {
            PDPage page = new PDPage(
                new PDRectangle(600, 400)
            );
            document.addPage(page);

            try (PDPageContentStream content =
                     new PDPageContentStream(document, page)) {
                content.addRect(50, 50, 200, 100);
                content.fill();
            }
        });

        PdfVectorExtractionResult result = extractor.extract(
            new ByteArrayInputStream(pdf)
        );

        PdfVectorPath path = result.pages().get(0).paths().get(0);

        assertTrue(path.closed());
        assertFalse(path.stroked());
        assertTrue(path.filled());
    }

    @Test
    void approximatesBezierCurves() throws IOException {
        byte[] pdf = createPdf(document -> {
            PDPage page = new PDPage(
                new PDRectangle(600, 400)
            );
            document.addPage(page);

            try (PDPageContentStream content =
                     new PDPageContentStream(document, page)) {
                content.moveTo(50, 50);
                content.curveTo(
                    100, 200,
                    200, 200,
                    250, 50
                );
                content.stroke();
            }
        });

        PdfVectorExtractionResult result = extractor.extract(
            new ByteArrayInputStream(pdf)
        );

        PdfVectorPath path = result.pages().get(0).paths().get(0);

        assertEquals(17, path.points().size());
        assertEquals(50, path.points().get(0).x(), 0.001);
        assertEquals(250, path.points().get(16).x(), 0.001);
    }

    @Test
    void extractsMultiplePagesAndPreservesRotation() throws IOException {
        byte[] pdf = createPdf(document -> {
            PDPage first = new PDPage(
                new PDRectangle(600, 400)
            );
            document.addPage(first);

            PDPage second = new PDPage(
                new PDRectangle(400, 600)
            );
            second.setRotation(90);
            document.addPage(second);
        });

        PdfVectorExtractionResult result = extractor.extract(
            new ByteArrayInputStream(pdf)
        );

        assertEquals(2, result.pageCount());
        assertEquals(0, result.pages().get(0).rotationDegrees());
        assertEquals(90, result.pages().get(1).rotationDegrees());
        assertEquals(400, result.pages().get(1).widthPoints(), 0.001);
        assertEquals(600, result.pages().get(1).heightPoints(), 0.001);
    }

    @Test
    void rejectsEmptyPdf() {
        assertThrows(
            PdfVectorExtractionException.class,
            () -> extractor.extract(
                new ByteArrayInputStream(new byte[0])
            )
        );
    }

    @Test
    void rejectsInvalidPdf() {
        assertThrows(
            PdfVectorExtractionException.class,
            () -> extractor.extract(
                new ByteArrayInputStream(
                    "not a PDF".getBytes()
                )
            )
        );
    }

    private byte[] createPdf(PdfDocumentBuilder builder)
            throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            builder.build(document);
            document.save(output);

            return output.toByteArray();
        }
    }

    @FunctionalInterface
    private interface PdfDocumentBuilder {
        void build(PDDocument document) throws IOException;
    }
}