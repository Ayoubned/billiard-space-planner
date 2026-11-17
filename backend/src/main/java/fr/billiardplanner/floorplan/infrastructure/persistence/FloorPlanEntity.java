package fr.billiardplanner.floorplan.infrastructure.persistence;

import fr.billiardplanner.floorplan.domain.FloorPlan;
import fr.billiardplanner.floorplan.domain.FloorPlanStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "floor_plans", schema = "planner")
public class FloorPlanEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, length = 500, unique = true)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FloorPlanStatus status;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected FloorPlanEntity() {
        // Required by JPA
    }

    public FloorPlanEntity(FloorPlan floorPlan) {
        this.id = floorPlan.getId();
        this.projectId = floorPlan.getProjectId();
        this.originalFilename = floorPlan.getOriginalFilename();
        this.storageKey = floorPlan.getStorageKey();
        this.contentType = floorPlan.getContentType();
        this.fileSizeBytes = floorPlan.getFileSizeBytes();
        this.sha256 = floorPlan.getSha256();
        this.status = floorPlan.getStatus();
        this.pageCount = floorPlan.getPageCount();
        this.createdAt = floorPlan.getCreatedAt();
        this.updatedAt = floorPlan.getUpdatedAt();
    }

    public void updateFrom(FloorPlan floorPlan) {
        this.status = floorPlan.getStatus();
        this.pageCount = floorPlan.getPageCount();
        this.updatedAt = floorPlan.getUpdatedAt();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public FloorPlanStatus getStatus() {
        return status;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}