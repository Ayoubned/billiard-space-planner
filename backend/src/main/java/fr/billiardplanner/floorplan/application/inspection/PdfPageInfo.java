package fr.billiardplanner.floorplan.application.inspection;

public record PdfPageInfo(
        int pageNumber,
        double widthPoints,
        double heightPoints,
        int rotationDegrees
) {
}