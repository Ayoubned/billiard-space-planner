package fr.billiardplanner.geometry.infrastructure.pdf;

public class PdfVectorExtractionException extends RuntimeException {

    public PdfVectorExtractionException(String message) {
        super(message);
    }

    public PdfVectorExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}