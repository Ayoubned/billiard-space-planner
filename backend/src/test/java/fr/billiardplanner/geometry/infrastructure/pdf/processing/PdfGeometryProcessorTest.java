package fr.billiardplanner.geometry.infrastructure.pdf.processing;

import fr.billiardplanner.geometry.domain.Point2D;
import fr.billiardplanner.geometry.infrastructure.pdf.PdfVectorExtractionResult;
import fr.billiardplanner.geometry.infrastructure.pdf.PdfVectorPage;
import fr.billiardplanner.geometry.infrastructure.pdf.PdfVectorPath;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfGeometryProcessorTest {

    @Test
    void calculatesRectangleMetrics() {
        PdfGeometryProcessingResult result = process(
            path(
                List.of(
                    point(0, 0),
                    point(10, 0),
                    point(10, 10),
                    point(0, 10)
                ),
                true
            )
        );

        ProcessedPdfPath processed = result.pages().get(0)
            .paths().get(0);

        assertTrue(processed.isPolygon());
        assertEquals(100, processed.areaSquarePoints(), 0.001);
        assertEquals(40, processed.lengthPoints(), 0.001);

        assertEquals(0, processed.bounds().minX(), 0.001);
        assertEquals(0, processed.bounds().minY(), 0.001);
        assertEquals(10, processed.bounds().maxX(), 0.001);
        assertEquals(10, processed.bounds().maxY(), 0.001);
        assertEquals(10, processed.bounds().width(), 0.001);
        assertEquals(10, processed.bounds().height(), 0.001);
    }

    @Test
    void removesInsignificantSegments() {
        PdfGeometryProcessingResult result = process(
            path(
                List.of(
                    point(0, 0),
                    point(0.1, 0),
                    point(10, 0)
                ),
                false
            )
        );

        ProcessedPdfPath processed = result.pages().get(0)
            .paths().get(0);

        assertEquals(2, processed.points().size());
        assertEquals(point(0, 0), processed.points().get(0));
        assertEquals(point(10, 0), processed.points().get(1));
    }

    @Test
    void removesPathsWithOnlyInsignificantSegments() {
        PdfGeometryProcessingResult result = process(
            path(
                List.of(
                    point(0, 0),
                    point(0.1, 0)
                ),
                false
            )
        );

        assertEquals(0, result.pathCount());
    }

    @Test
    void mergesConnectedOpenPaths() {
        PdfGeometryProcessingResult result = process(
            path(
                List.of(point(0, 0), point(10, 0)),
                false
            ),
            path(
                List.of(point(10.2, 0), point(10.2, 10)),
                false
            )
        );

        assertEquals(1, result.pathCount());

        ProcessedPdfPath merged = result.pages().get(0)
            .paths().get(0);

        assertEquals(3, merged.points().size());
        assertEquals(point(10.1, 0), merged.points().get(1));
        assertFalse(merged.closed());
        assertEquals(20.1, merged.lengthPoints(), 0.001);
    }

    @Test
    void mergesPathsWhenTheirDirectionsAreReversed() {
        PdfGeometryProcessingResult result = process(
            path(
                List.of(point(0, 0), point(10, 0)),
                false
            ),
            path(
                List.of(point(10, 10), point(10.2, 0)),
                false
            )
        );

        assertEquals(1, result.pathCount());
        assertEquals(
            3,
            result.pages().get(0).paths().get(0).points().size()
        );
    }

    @Test
    void doesNotMergePathsOutsideConnectionTolerance() {
        PdfGeometryProcessingResult result = process(
            path(
                List.of(point(0, 0), point(10, 0)),
                false
            ),
            path(
                List.of(point(11, 0), point(11, 10)),
                false
            )
        );

        assertEquals(2, result.pathCount());
    }

    @Test
    void doesNotMergeClosedPaths() {
        PdfGeometryProcessingResult result = process(
            path(
                List.of(
                    point(0, 0),
                    point(10, 0),
                    point(10, 10),
                    point(0, 10)
                ),
                true
            ),
            path(
                List.of(
                    point(10, 0),
                    point(20, 0),
                    point(20, 10),
                    point(10, 10)
                ),
                true
            )
        );

        assertEquals(2, result.pathCount());
        assertEquals(2, result.polygonCount());
    }

    @Test
    void preservesRawExtractionAndPageMetadata() {
        PdfVectorPage page = new PdfVectorPage(
            1,
            600,
            400,
            10,
            20,
            90,
            List.of(path(
                List.of(point(0, 0), point(100, 0)),
                false
            ))
        );

        PdfVectorExtractionResult raw =
            new PdfVectorExtractionResult(List.of(page));

        PdfGeometryProcessingResult result =
            new PdfGeometryProcessor().process(raw);

        assertSame(raw, result.rawExtraction());

        ProcessedPdfPage processed = result.pages().get(0);

        assertEquals(600, processed.widthPoints());
        assertEquals(400, processed.heightPoints());
        assertEquals(10, processed.lowerLeftXPoints());
        assertEquals(20, processed.lowerLeftYPoints());
        assertEquals(90, processed.rotationDegrees());

        // Processing must not modify the original extraction.
        assertEquals(2, raw.pages().get(0).paths().get(0).points().size());
    }

    @Test
    void keepsClosedPathsButDoesNotClassifyTinyAreasAsPolygons() {
        PdfGeometryProcessingOptions options =
            new PdfGeometryProcessingOptions(0.25, 0.5, 2.0);

        PdfVectorExtractionResult raw = extraction(
            path(
                List.of(
                    point(0, 0),
                    point(1, 0),
                    point(1, 1),
                    point(0, 1)
                ),
                true
            )
        );

        PdfGeometryProcessingResult result =
            new PdfGeometryProcessor(options).process(raw);

        ProcessedPdfPath processed = result.pages().get(0)
            .paths().get(0);

        assertTrue(processed.closed());
        assertFalse(processed.isPolygon());
        assertNull(processed.areaSquarePoints());
    }

    @Test
    void rejectsNegativeProcessingThresholds() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new PdfGeometryProcessingOptions(-1, 0.5, 0.25)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new PdfGeometryProcessingOptions(0.25, -1, 0.25)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new PdfGeometryProcessingOptions(0.25, 0.5, -1)
        );
    }

    private PdfGeometryProcessingResult process(PdfVectorPath... paths) {
        return new PdfGeometryProcessor().process(
            extraction(List.of(paths))
        );
    }

    private PdfVectorExtractionResult extraction(
        PdfVectorPath... paths
    ) {
        return extraction(List.of(paths));
    }

    private PdfVectorExtractionResult extraction(
        List<PdfVectorPath> paths
    ) {
        PdfVectorPage page = new PdfVectorPage(
            1,
            600,
            400,
            0,
            0,
            0,
            paths
        );

        return new PdfVectorExtractionResult(List.of(page));
    }

    private PdfVectorPath path(
        List<Point2D> points,
        boolean closed
    ) {
        return new PdfVectorPath(points, closed, true, false);
    }

    private Point2D point(double x, double y) {
        return new Point2D(x, y);
    }
}