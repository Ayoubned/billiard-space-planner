package fr.billiardplanner.geometry.infrastructure.pdf.processing;

public record PdfGeometryProcessingOptions(
    double minimumSegmentLengthPoints,
    double connectionTolerancePoints,
    double minimumPolygonAreaPointsSquared
) {
    public PdfGeometryProcessingOptions {
        if (!Double.isFinite(minimumSegmentLengthPoints)
                || minimumSegmentLengthPoints < 0) {
            throw new IllegalArgumentException(
                "Minimum segment length must be finite and non-negative"
            );
        }

        if (!Double.isFinite(connectionTolerancePoints)
                || connectionTolerancePoints < 0) {
            throw new IllegalArgumentException(
                "Connection tolerance must be finite and non-negative"
            );
        }

        if (!Double.isFinite(minimumPolygonAreaPointsSquared)
                || minimumPolygonAreaPointsSquared < 0) {
            throw new IllegalArgumentException(
                "Minimum polygon area must be finite and non-negative"
            );
        }
    }

    public static PdfGeometryProcessingOptions defaults() {
        return new PdfGeometryProcessingOptions(
            0.25,
            0.5,
            0.25
        );
    }
}