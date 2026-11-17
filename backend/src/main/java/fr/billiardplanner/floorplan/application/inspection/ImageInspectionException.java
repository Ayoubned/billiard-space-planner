package fr.billiardplanner.floorplan.application.inspection;

public class ImageInspectionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ImageInspectionException(String message) {
        super(message);
    }

    public ImageInspectionException(String message, Throwable cause) {
        super(message, cause);
    }
}