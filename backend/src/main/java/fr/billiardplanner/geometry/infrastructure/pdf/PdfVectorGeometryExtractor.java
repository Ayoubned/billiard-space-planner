package fr.billiardplanner.geometry.infrastructure.pdf;

import fr.billiardplanner.geometry.domain.Point2D;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Math.pow;

public class PdfVectorGeometryExtractor {

    private static final int MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024;
    private static final int CURVE_SUBDIVISIONS = 16;

    public PdfVectorExtractionResult extract(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "Input stream is required");

        try {
            byte[] pdfBytes = inputStream.readNBytes(
                MAX_FILE_SIZE_BYTES + 1
            );

            if (pdfBytes.length == 0) {
                throw new PdfVectorExtractionException("PDF is empty");
            }

            if (pdfBytes.length > MAX_FILE_SIZE_BYTES) {
                throw new PdfVectorExtractionException(
                    "PDF exceeds the maximum allowed size"
                );
            }

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                if (document.isEncrypted()) {
                    throw new PdfVectorExtractionException(
                        "Encrypted PDFs are not supported"
                    );
                }

                if (document.getNumberOfPages() == 0) {
                    throw new PdfVectorExtractionException(
                        "PDF contains no pages"
                    );
                }

                List<PdfVectorPage> pages = new ArrayList<>();

                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    PDPage page = document.getPage(i);
                    PDRectangle cropBox = page.getCropBox();

                    PageGeometryEngine engine =
                        new PageGeometryEngine(page);

                    engine.processPage(page);

                    pages.add(new PdfVectorPage(
                           i + 1,
                            cropBox.getWidth(),
                            cropBox.getHeight(),
                            cropBox.getLowerLeftX(),
                            cropBox.getLowerLeftY(),
                            page.getRotation(),
                            engine.getPaths()
                    ));
                }

                return new PdfVectorExtractionResult(pages);
            }
        } catch (PdfVectorExtractionException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PdfVectorExtractionException(
                "Unable to extract PDF vector geometry",
                exception
            );
        }
    }

    private static class PageGeometryEngine
            extends PDFGraphicsStreamEngine {

        private final List<PdfVectorPath> paths = new ArrayList<>();

        private final List<SubpathBuilder> subpaths = new ArrayList<>();

        private SubpathBuilder currentSubpath;

        private java.awt.geom.Point2D currentPoint;

        protected PageGeometryEngine(PDPage page) {
            super(page);
        }

        List<PdfVectorPath> getPaths() {
            return List.copyOf(paths);
        }

        @Override
        public void appendRectangle(
            java.awt.geom.Point2D p0,
            java.awt.geom.Point2D p1,
            java.awt.geom.Point2D p2,
            java.awt.geom.Point2D p3
        ) {
            beginSubpath(p0.getX(), p0.getY());
            addPoint(p1.getX(), p1.getY());
            addPoint(p2.getX(), p2.getY());
            addPoint(p3.getX(), p3.getY());

            currentSubpath.closed = true;
            currentPoint = new java.awt.geom.Point2D.Double(p0.getX(), p0.getY());
        }

        @Override
        public void drawImage(PDImage image) {
            // Images are intentionally ignored.
            // This extractor only collects vector drawing geometry.
        }

        @Override
        public void moveTo(float x, float y) {
            beginSubpath(x, y);
        }

        @Override
        public void lineTo(float x, float y) {
            if (currentSubpath == null) {
                return;
            }

            addPoint(x, y);
        }

        @Override
        public void curveTo(
            float x1, float y1,
            float x2, float y2,
            float x3, float y3
        ) {
            if (currentSubpath == null || currentPoint == null) {
                return;
            }

            double x0 = currentPoint.getX();
            double y0 = currentPoint.getY();

            for (int i = 1; i <= CURVE_SUBDIVISIONS; i++) {
                double t = (double) i / CURVE_SUBDIVISIONS;
                double inverseT = 1.0 - t;

                double x =
                    pow(inverseT, 3) * x0
                    + 3 * pow(inverseT, 2) * t * x1
                    + 3 * inverseT * pow(t, 2) * x2
                    + pow(t, 3) * x3;

                double y =
                    pow(inverseT, 3) * y0
                    + 3 * pow(inverseT, 2) * t * y1
                    + 3 * inverseT * pow(t, 2) * y2
                    + pow(t, 3) * y3;

                addPoint(x, y);
            }
        }

        @Override
        public void closePath() {
            if (currentSubpath != null) {
                currentSubpath.closed = true;

                Point2D start = currentSubpath.points.get(0);
                currentPoint = new java.awt.geom.Point2D.Double(start.x(), start.y());
            }
        }

        @Override
        public void endPath() {
            finishPath(false, false);
        }

        @Override
        public void strokePath() {
            finishPath(true, false);
        }

        @Override
        public void fillPath(int windingRule) {
            finishPath(false, true);
        }

        @Override
        public void fillAndStrokePath(int windingRule) {
            finishPath(true, true);
        }

        @Override
        public void clip(int windingRule) {
            // Clipping is intentionally not applied in this extraction stage.
        }

        @Override
        public void shadingFill(COSName shadingName) {
            // Shading does not provide useful boundary geometry here.
        }

        @Override
        public java.awt.geom.Point2D getCurrentPoint() {
            return currentPoint;
        }

        private void beginSubpath(double x, double y) {
            currentSubpath = new SubpathBuilder();
            subpaths.add(currentSubpath);
            addPoint(x, y);
        }

        private void addPoint(double x, double y) {
            if (!java.lang.Double.isFinite(x) || !java.lang.Double.isFinite(y)) {
                return;
            }

            Point2D point = new Point2D(x, y);

            if (currentSubpath == null) {
                beginSubpath(x, y);
                return;
            }

            List<Point2D> points = currentSubpath.points;

            if (points.isEmpty()
                    || !points.get(points.size() - 1).equals(point)) {
                points.add(point);
            }

            currentPoint = new java.awt.geom.Point2D.Double(x, y);
        }

        private void finishPath(boolean stroked, boolean filled) {
            for (SubpathBuilder subpath : subpaths) {
                if (subpath.points.size() >= 2) {
                    paths.add(new PdfVectorPath(
                        subpath.points,
                        subpath.closed,
                        stroked,
                        filled
                    ));
                }
            }

            subpaths.clear();
            currentSubpath = null;
            currentPoint = null;
        }
    }

    private static class SubpathBuilder {
        private final List<Point2D> points = new ArrayList<>();
        private boolean closed;
    }
}