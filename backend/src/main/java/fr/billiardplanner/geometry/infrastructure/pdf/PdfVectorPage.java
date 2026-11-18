package fr.billiardplanner.geometry.infrastructure.pdf;

import java.util.List;
import java.util.Objects;

public record PdfVectorPage(
    int pageNumber,
    double widthPoints,
    double heightPoints,
    double lowerLeftXPoints,
    double lowerLeftYPoints,
    int rotationDegrees,
    List<PdfVectorPath> paths
) {
    public PdfVectorPage {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be positive");
        }

        if (!Double.isFinite(widthPoints) || widthPoints <= 0
                || !Double.isFinite(heightPoints) || heightPoints <= 0) {
            throw new IllegalArgumentException("Invalid page dimensions");
        }

        if (!Double.isFinite(lowerLeftXPoints)
                || !Double.isFinite(lowerLeftYPoints)) {
            throw new IllegalArgumentException("Invalid page origin");
        }

        if (rotationDegrees % 90 != 0) {
            throw new IllegalArgumentException(
                "Page rotation must be a multiple of 90 degrees"
            );
        }

        Objects.requireNonNull(paths, "Paths are required");
        paths = List.copyOf(paths);
    }
}