package fr.billiardplanner.geometry.infrastructure.pdf.processing;

public record PdfGeometryBounds(
    double minX,
    double minY,
    double maxX,
    double maxY
) {
    public PdfGeometryBounds {
        if (!Double.isFinite(minX) || !Double.isFinite(minY)
                || !Double.isFinite(maxX) || !Double.isFinite(maxY)) {
            throw new IllegalArgumentException("Bounds must be finite");
        }

        if (maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("Invalid geometry bounds");
        }
    }

    public double width() {
        return maxX - minX;
    }

    public double height() {
        return maxY - minY;
    }
}