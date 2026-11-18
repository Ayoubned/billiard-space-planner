package fr.billiardplanner.geometry.domain;

public record Point2D(double x, double y) {

    public Point2D {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException(
                "Point coordinates must be finite"
            );
        }
    }

    public double distanceTo(Point2D other) {
        if (other == null) {
            throw new IllegalArgumentException("Other point is required");
        }

        return Math.hypot(
            other.x - x,
            other.y - y
        );
    }
}