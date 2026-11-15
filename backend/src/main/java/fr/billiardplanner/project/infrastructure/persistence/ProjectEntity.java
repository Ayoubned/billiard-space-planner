package fr.billiardplanner.project.infrastructure.persistence;

import fr.billiardplanner.project.domain.ProjectStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import fr.billiardplanner.project.domain.Project;

@Entity
@Table(name = "projects", schema = "planner")
public class ProjectEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "client_name", length = 120)
    private String clientName;

    @Column(name = "site_address", length = 250)
    private String siteAddress;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProjectStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected ProjectEntity() {
        // Required by JPA
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getClientName() {
        return clientName;
    }

    public String getSiteAddress() {
        return siteAddress;
    }

    public String getDescription() {
        return description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public Long getVersion() {
        return version;
    }

    public ProjectEntity(Project project) {
    this.id = project.getId();
    this.name = project.getName();
    this.clientName = project.getClientName();
    this.siteAddress = project.getSiteAddress();
    this.description = project.getDescription();
    this.status = project.getStatus();
    this.createdAt = project.getCreatedAt();
    this.updatedAt = project.getUpdatedAt();
    this.archivedAt = project.getArchivedAt();
    }

    public void updateFrom(Project project) {
    this.name = project.getName();
    this.clientName = project.getClientName();
    this.siteAddress = project.getSiteAddress();
    this.description = project.getDescription();
    this.status = project.getStatus();
    this.updatedAt = project.getUpdatedAt();
    this.archivedAt = project.getArchivedAt();
    }
}