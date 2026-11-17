package fr.billiardplanner.floorplan.application.validation;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class FloorPlanFileValidator {

    public static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;

    private static final byte[] PDF_SIGNATURE =
            "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] PNG_SIGNATURE =
            HexFormat.of().parseHex("89504e470d0a1a0a");

    private static final byte[] JPEG_SIGNATURE =
            HexFormat.of().parseHex("ffd8ff");

    /**
     * Validates a floor-plan file.
     *
     * @return the normalized MIME type to persist
     */
    public String validate(
            String originalFilename,
            String declaredContentType,
            long fileSizeBytes,
            byte[] fileHeader
    ) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }

        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File exceeds the maximum size of 25 MB"
            );
        }

        String extension = extractExtension(originalFilename);
        String expectedContentType = resolveContentType(extension);

        String normalizedContentType = normalizeContentType(declaredContentType);

        if (!expectedContentType.equals(normalizedContentType)) {
            throw new IllegalArgumentException(
                    "File extension and declared content type do not match"
            );
        }

        if (fileHeader == null) {
            throw new IllegalArgumentException("File content is missing");
        }

        if (!hasExpectedSignature(expectedContentType, fileHeader)) {
            throw new IllegalArgumentException(
                    "File content does not match its declared format"
            );
        }

        return expectedContentType;
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');

        if (lastDot < 0 || lastDot == filename.length() - 1) {
            throw new IllegalArgumentException(
                    "File must have a supported extension"
            );
        }

        return filename.substring(lastDot + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String resolveContentType(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> throw new IllegalArgumentException(
                    "Unsupported file format. Accepted formats: PDF, PNG, JPG, JPEG"
            );
        };
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException(
                    "File content type is required"
            );
        }

        String normalized = contentType.split(";", 2)[0].trim()
                .toLowerCase(Locale.ROOT);

        return normalized;
    }

    private boolean hasExpectedSignature(
            String contentType,
            byte[] header
    ) {
        return switch (contentType) {
            case "application/pdf" -> startsWith(header, PDF_SIGNATURE);
            case "image/png" -> startsWith(header, PNG_SIGNATURE);
            case "image/jpeg" -> startsWith(header, JPEG_SIGNATURE);
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }

        return true;
    }
}