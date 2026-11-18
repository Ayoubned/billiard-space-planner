package fr.billiardplanner.geometry.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Room(
    UUID id,
    String name,
    Polygon2D boundary,
    List<Wall> walls,
    CoordinateSystem coordinateSystem
) {
    public Room {
        Objects.requireNonNull(id, "Room ID is required");
        Objects.requireNonNull(boundary, "Boundary is required");
        Objects.requireNonNull(walls, "Walls are required");
        Objects.requireNonNull(
            coordinateSystem,
            "Coordinate system is required"
        );

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                "Room name is required"
            );
        }

        walls = List.copyOf(walls);
    }

    public double areaSquareMeters() {
        double scale = coordinateSystem.unitsPerMeter();

        return boundary.area() / (scale * scale);
    }

    public double perimeterMeters() {
        return coordinateSystem.toMeters(
            boundary.perimeter()
        );
    }
}