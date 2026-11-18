package fr.billiardplanner.geometry.infrastructure.pdf;

import fr.billiardplanner.geometry.domain.Point2D;

import java.util.List;
import java.util.Objects;

public record PdfVectorPath(
    List<Point2D> points,
    boolean closed,
    boolean stroked,
    boolean filled
) {
    public PdfVectorPath {
        Objects.requireNonNull(points, "Points are required");
        points = List.copyOf(points);

        if (points.size() < 2) {
            throw new IllegalArgumentException(
                "A vector path requires at least two points"
            );
        }
    }
}