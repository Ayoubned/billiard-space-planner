package fr.billiardplanner.geometry.infrastructure.pdf.processing;

import fr.billiardplanner.geometry.domain.Point2D;

import java.util.List;
import java.util.Objects;

public record ProcessedPdfPath(
    List<Point2D> points,
    boolean closed,
    boolean stroked,
    boolean filled,
    PdfGeometryBounds bounds,
    double lengthPoints,
    Double areaSquarePoints
) {
    public ProcessedPdfPath {
        Objects.requireNonNull(points, "Points are required");
        Objects.requireNonNull(bounds, "Bounds are required");

        points = List.copyOf(points);

        if (points.size() < 2) {
            throw new IllegalArgumentException(
                "Processed path requires at least two points"
            );
        }

        if (!Double.isFinite(lengthPoints) || lengthPoints <= 0) {
            throw new IllegalArgumentException(
                "Path length must be positive and finite"
            );
        }

        if (areaSquarePoints != null
                && (!Double.isFinite(areaSquarePoints)
                || areaSquarePoints <= 0)) {
            throw new IllegalArgumentException(
                "Polygon area must be positive and finite"
            );
        }

        if (areaSquarePoints != null && !closed) {
            throw new IllegalArgumentException(
                "An open path cannot have a polygon area"
            );
        }
    }

    public boolean isPolygon() {
        return areaSquarePoints != null;
    }
}