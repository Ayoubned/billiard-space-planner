package fr.billiardplanner.floorplan.infrastructure.storage;

import fr.billiardplanner.floorplan.application.storage.StoredFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class LocalFloorPlanStorageTest {

    @TempDir
    Path tempDir;

    private LocalFloorPlanStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalFloorPlanStorage(tempDir.toString());
    }

    @Test
    void shouldStoreAndReadFile() throws IOException {
        byte[] content = "Billiard floor plan test".getBytes();

        StoredFile stored = storage.store(
                new ByteArrayInputStream(content),
                1024
        );

        assertNotNull(stored.storageKey());
        assertTrue(stored.fileSizeBytes() > 0);

        try (var input = storage.open(stored.storageKey())) {
            assertArrayEquals(content, input.readAllBytes());
        }

        assertTrue(
                Files.exists(tempDir.resolve(stored.storageKey()))
        );
    }

    @Test
    void shouldCalculateCorrectSha256() throws Exception {
        byte[] content = "Billiard floor plan test".getBytes();

        StoredFile stored = storage.store(
                new ByteArrayInputStream(content),
                1024
        );

        String expectedHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
        );

        assertEquals(expectedHash, stored.sha256());
        assertEquals(64, stored.sha256().length());
    }

    @Test
    void shouldGenerateUniqueStorageKeys() throws IOException {
        byte[] content = "test".getBytes();

        StoredFile first = storage.store(
                new ByteArrayInputStream(content),
                1024
        );

        StoredFile second = storage.store(
                new ByteArrayInputStream(content),
                1024
        );

        assertNotEquals(first.storageKey(), second.storageKey());
    }

    @Test
    void shouldRejectFilesExceedingMaximumSize() throws IOException{
        byte[] content = new byte[1025];

        assertThrows(
                IOException.class,
                () -> storage.store(
                        new ByteArrayInputStream(content),
                        1024
                )
        );

        assertEquals(0, countFiles());
    }

    @Test
    void shouldRejectEmptyFiles() throws IOException{
        byte[] content = new byte[0];

        assertThrows(
                IOException.class,
                () -> storage.store(
                        new ByteArrayInputStream(content),
                        1024
                )
        );

        assertEquals(0, countFiles());
    }

    @Test
    void shouldDeleteStoredFile() throws IOException {
        byte[] content = "file to delete".getBytes();

        StoredFile stored = storage.store(
                new ByteArrayInputStream(content),
                1024
        );

        Path storedPath = tempDir.resolve(stored.storageKey());

        assertTrue(Files.exists(storedPath));

        storage.delete(stored.storageKey());

        assertFalse(Files.exists(storedPath));
    }

    @Test
    void shouldRejectInvalidStorageKeys() throws IOException {
        assertThrows(
                IllegalArgumentException.class,
                () -> storage.open("../outside.txt")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.delete("../../outside.txt")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.open("")
        );
    }

    @Test
    void shouldFailWhenOpeningNonexistentFile() throws IOException {
        String validButUnknownKey =
                "123e4567-e89b-12d3-a456-426614174000";

        assertThrows(
                NoSuchFileException.class,
                () -> storage.open(validButUnknownKey)
        );
    }

    private long countFiles() throws IOException {
        try (var files = Files.list(tempDir)) {
            return files.count();
        }
    }
}