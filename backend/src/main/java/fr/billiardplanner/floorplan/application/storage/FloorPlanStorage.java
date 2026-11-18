package fr.billiardplanner.floorplan.application.storage;

import java.io.IOException;
import java.io.InputStream;

public interface FloorPlanStorage {

    StoredFile store(InputStream inputStream, long maxBytes) throws IOException;

    InputStream open(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}