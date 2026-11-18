package fr.billiardplanner.geometry.domain;

import java.util.Objects;
import java.util.UUID;

public record Wall(
    UUID id,
    LineSegment centerline,
    double thickness,
    GeometrySource source,
    double confidence
) {
    public Wall {
        Objects.requireNonNull(id, "Wall ID is required");
        Objects.requireNonNull(centerline, "Centerline is required");
        Objects.requireNonNull(source, "Geometry source is required");

        if (!Double.isFinite(thickness) || thickness <= 0) {
            throw new IllegalArgumentException(
                "Wall thickness must be positive"
            );
        }

        if (!Double.isFinite(confidence)
                || confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException(
                "Confidence must be between 0 and 1"
            );
        }
    }
}