package fr.billiardplanner.geometry.domain;

import java.util.Objects;

public record LineSegment(Point2D start, Point2D end) {

    public LineSegment {
        Objects.requireNonNull(start, "Start point is required");
        Objects.requireNonNull(end, "End point is required");

        if (start.equals(end)) {
            throw new IllegalArgumentException(
                "Line segment cannot have zero length"
            );
        }
    }

    public double length() {
        return start.distanceTo(end);
    }

    public Point2D midpoint() {
        return new Point2D(
            (start.x() + end.x()) / 2.0,
            (start.y() + end.y()) / 2.0
        );
    }
}