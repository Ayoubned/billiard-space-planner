package fr.billiardplanner.floorplan.infrastructure.storage;

import fr.billiardplanner.floorplan.application.storage.FloorPlanStorage;
import fr.billiardplanner.floorplan.application.storage.StoredFile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class LocalFloorPlanStorage implements FloorPlanStorage {

    private static final int BUFFER_SIZE = 8192;

    private final Path storageDirectory;

    public LocalFloorPlanStorage(
            @Value("${billiard-planner.storage.floor-plans-directory:./data/floor-plans}")
            String storageDirectory
    ) {
        this.storageDirectory = Path.of(storageDirectory)
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public StoredFile store(InputStream inputStream, long maxBytes)
            throws IOException {

        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream is required");
        }

        if (maxBytes <= 0) {
            throw new IllegalArgumentException("Maximum file size must be positive");
        }

        Files.createDirectories(storageDirectory);

        String storageKey = UUID.randomUUID().toString();

        Path temporaryFile = Files.createTempFile(
                storageDirectory,
                ".upload-",
                ".tmp"
        );

        Path targetFile = resolveStoragePath(storageKey);

        try {
            MessageDigest digest = createSha256Digest();
            long totalBytes = 0;

            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    totalBytes += bytesRead;

                    if (totalBytes > maxBytes) {
                        throw new IOException(
                                "File exceeds the maximum allowed size"
                        );
                    }

                    digest.update(buffer, 0, bytesRead);
                    output.write(buffer, 0, bytesRead);
                }
            }

            if (totalBytes == 0) {
                throw new IOException("Uploaded file is empty");
            }

            try {
                Files.move(
                        temporaryFile,
                        targetFile,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, targetFile);
            }

            return new StoredFile(
                    storageKey,
                    totalBytes,
                    HexFormat.of().formatHex(digest.digest())
            );

        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temporaryFile);
            throw exception;
        }
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(resolveStoragePath(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveStoragePath(storageKey));
    }

    private Path resolveStoragePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key is required");
        }

        UUID.fromString(storageKey);

        Path resolved = storageDirectory.resolve(storageKey).normalize();

        if (!resolved.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        return resolved;
    }

    private MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }
}