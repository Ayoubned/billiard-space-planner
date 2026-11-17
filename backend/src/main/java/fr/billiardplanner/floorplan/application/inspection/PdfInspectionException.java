package fr.billiardplanner.floorplan.application.inspection;

public class PdfInspectionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PdfInspectionException(String message) {
        super(message);
    }

    public PdfInspectionException(String message, Throwable cause) {
        super(message, cause);
    }
}