package fr.billiardplanner.geometry.infrastructure.pdf.processing;

import java.util.List;
import java.util.Objects;

public record ProcessedPdfPage(
    int pageNumber,
    double widthPoints,
    double heightPoints,
    double lowerLeftXPoints,
    double lowerLeftYPoints,
    int rotationDegrees,
    List<ProcessedPdfPath> paths
) {
    public ProcessedPdfPage {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Invalid page number");
        }

        if (!Double.isFinite(widthPoints) || widthPoints <= 0
                || !Double.isFinite(heightPoints) || heightPoints <= 0) {
            throw new IllegalArgumentException("Invalid page dimensions");
        }

        Objects.requireNonNull(paths, "Paths are required");
        paths = List.copyOf(paths);
    }

    public long polygonCount() {
        return paths.stream()
            .filter(ProcessedPdfPath::isPolygon)
            .count();
    }
}