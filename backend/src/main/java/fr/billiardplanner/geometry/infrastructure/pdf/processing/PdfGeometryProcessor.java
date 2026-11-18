package fr.billiardplanner.geometry.infrastructure.pdf.processing;

import fr.billiardplanner.geometry.domain.Point2D;
import fr.billiardplanner.geometry.infrastructure.pdf.PdfVectorExtractionResult;
import fr.billiardplanner.geometry.infrastructure.pdf.PdfVectorPage;
import fr.billiardplanner.geometry.infrastructure.pdf.PdfVectorPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PdfGeometryProcessor {

    private static final double EPSILON = 1e-9;

    private final PdfGeometryProcessingOptions options;

    public PdfGeometryProcessor() {
        this(PdfGeometryProcessingOptions.defaults());
    }

    public PdfGeometryProcessor(PdfGeometryProcessingOptions options) {
        this.options = Objects.requireNonNull(
            options,
            "Processing options are required"
        );
    }

    public PdfGeometryProcessingResult process(
        PdfVectorExtractionResult extraction
    ) {
        Objects.requireNonNull(extraction, "Extraction is required");

        List<ProcessedPdfPage> pages = extraction.pages()
            .stream()
            .map(this::processPage)
            .toList();

        return new PdfGeometryProcessingResult(extraction, pages);
    }

    private ProcessedPdfPage processPage(PdfVectorPage page) {
        List<WorkingPath> paths = new ArrayList<>();

        for (PdfVectorPath rawPath : page.paths()) {
            WorkingPath cleaned = cleanPath(rawPath);

            if (cleaned != null) {
                paths.add(cleaned);
            }
        }

        mergeConnectedPaths(paths);

        List<ProcessedPdfPath> processedPaths = paths.stream()
            .map(this::toProcessedPath)
            .toList();

        return new ProcessedPdfPage(
            page.pageNumber(),
            page.widthPoints(),
            page.heightPoints(),
            page.lowerLeftXPoints(),
            page.lowerLeftYPoints(),
            page.rotationDegrees(),
            processedPaths
        );
    }

    private WorkingPath cleanPath(PdfVectorPath path) {
        List<Point2D> sourcePoints = new ArrayList<>(path.points());

        if (path.closed() && sourcePoints.size() > 1
                && distance(
                    sourcePoints.get(0),
                    sourcePoints.get(sourcePoints.size() - 1)
                ) <= EPSILON) {
            sourcePoints.remove(sourcePoints.size() - 1);
        }

        List<Point2D> cleaned = new ArrayList<>();
        double minimumLength = options.minimumSegmentLengthPoints();

        for (Point2D point : sourcePoints) {
            if (cleaned.isEmpty()) {
                cleaned.add(point);
                continue;
            }

            Point2D previous = cleaned.get(cleaned.size() - 1);

            if (distance(previous, point) >= minimumLength
                    && distance(previous, point) > EPSILON) {
                cleaned.add(point);
            }
        }

        if (path.closed()) {
            while (cleaned.size() > 3
                    && distance(
                        cleaned.get(cleaned.size() - 1),
                        cleaned.get(0)
                    ) < minimumLength) {
                cleaned.remove(cleaned.size() - 1);
            }

            if (cleaned.size() < 3
                    || distance(
                        cleaned.get(cleaned.size() - 1),
                        cleaned.get(0)
                    ) < minimumLength) {
                return null;
            }
        } else if (cleaned.size() < 2) {
            return null;
        }

        return new WorkingPath(
            cleaned,
            path.closed(),
            path.stroked(),
            path.filled()
        );
    }

    private void mergeConnectedPaths(List<WorkingPath> paths) {
        boolean merged;

        do {
            merged = false;

            outer:
            for (int i = 0; i < paths.size(); i++) {
                WorkingPath first = paths.get(i);

                if (first.closed()) {
                    continue;
                }

                for (int j = i + 1; j < paths.size(); j++) {
                    WorkingPath second = paths.get(j);

                    if (second.closed()
                            || first.stroked() != second.stroked()
                            || first.filled() != second.filled()) {
                        continue;
                    }

                    List<Point2D> joined = tryMerge(first, second);

                    if (joined != null) {
                        paths.set(i, new WorkingPath(
                            joined,
                            false,
                            first.stroked(),
                            first.filled()
                        ));

                        paths.remove(j);
                        merged = true;
                        break outer;
                    }
                }
            }
        } while (merged);
    }

    private List<Point2D> tryMerge(
        WorkingPath first,
        WorkingPath second
    ) {
        List<List<Point2D>> firstOrientations = List.of(
            first.points(),
            reversed(first.points())
        );

        List<List<Point2D>> secondOrientations = List.of(
            second.points(),
            reversed(second.points())
        );

        for (List<Point2D> firstPoints : firstOrientations) {
            for (List<Point2D> secondPoints : secondOrientations) {
                Point2D firstEnd = firstPoints.get(
                    firstPoints.size() - 1
                );
                Point2D secondStart = secondPoints.get(0);

                if (distance(firstEnd, secondStart)
                        > options.connectionTolerancePoints()) {
                    continue;
                }

                Point2D join = midpoint(firstEnd, secondStart);

                List<Point2D> result = new ArrayList<>(firstPoints);

                result.set(result.size() - 1, join);

                for (int i = 1; i < secondPoints.size(); i++) {
                    Point2D point = secondPoints.get(i);

                    if (distance(
                        result.get(result.size() - 1),
                        point
                    ) > EPSILON) {
                        result.add(point);
                    }
                }

                return result;
            }
        }

        return null;
    }

    private ProcessedPdfPath toProcessedPath(WorkingPath path) {
        List<Point2D> points = path.points();

        PdfGeometryBounds bounds = calculateBounds(points);

        double length = calculateLength(points, path.closed());

        Double area = null;

        if (path.closed() && points.size() >= 3) {
            double calculatedArea = calculateArea(points);

            if (calculatedArea
                    > options.minimumPolygonAreaPointsSquared()) {
                area = calculatedArea;
            }
        }

        return new ProcessedPdfPath(
            points,
            path.closed(),
            path.stroked(),
            path.filled(),
            bounds,
            length,
            area
        );
    }

    private PdfGeometryBounds calculateBounds(List<Point2D> points) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Point2D point : points) {
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
        }

        return new PdfGeometryBounds(minX, minY, maxX, maxY);
    }

    private double calculateLength(
        List<Point2D> points,
        boolean closed
    ) {
        double length = 0;

        for (int i = 1; i < points.size(); i++) {
            length += distance(points.get(i - 1), points.get(i));
        }

        if (closed) {
            length += distance(
                points.get(points.size() - 1),
                points.get(0)
            );
        }

        return length;
    }

    private double calculateArea(List<Point2D> points) {
        double sum = 0;

        for (int i = 0; i < points.size(); i++) {
            Point2D current = points.get(i);
            Point2D next = points.get((i + 1) % points.size());

            sum += current.x() * next.y()
                - next.x() * current.y();
        }

        return Math.abs(sum) / 2.0;
    }

    private double distance(Point2D first, Point2D second) {
        return first.distanceTo(second);
    }

    private Point2D midpoint(Point2D first, Point2D second) {
        return new Point2D(
            (first.x() + second.x()) / 2.0,
            (first.y() + second.y()) / 2.0
        );
    }

    private List<Point2D> reversed(List<Point2D> points) {
        List<Point2D> result = new ArrayList<>(points);
        Collections.reverse(result);
        return result;
    }

    private record WorkingPath(
        List<Point2D> points,
        boolean closed,
        boolean stroked,
        boolean filled
    ) {
        private WorkingPath {
            points = List.copyOf(points);
        }
    }
}