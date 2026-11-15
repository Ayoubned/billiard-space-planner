package fr.billiardplanner.project.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ProjectTest {

    private final Instant now =
            Instant.parse("2026-11-15T10:00:00Z");

    @Test
    void shouldCreateProjectWithDraftStatus() {
        Project project = Project.create(
                " Lyon Billiard Club ",
                "John Smith",
                "Lyon, France",
                "New billiard room",
                now
        );

        assertNotNull(project.getId());
        assertEquals("Lyon Billiard Club", project.getName());
        assertEquals("John Smith", project.getClientName());
        assertEquals(ProjectStatus.DRAFT, project.getStatus());
        assertEquals(now, project.getCreatedAt());
        assertEquals(now, project.getUpdatedAt());
        assertNull(project.getArchivedAt());
    }

    @Test
    void shouldAllowOptionalFieldsToBeEmpty() {
        Project project = Project.create(
                "New project",
                null,
                " ",
                null,
                now
        );

        assertNull(project.getClientName());
        assertNull(project.getSiteAddress());
        assertNull(project.getDescription());
    }

    @Test
    void shouldRejectBlankProjectName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Project.create("", null, null, null, now)
        );
    }

    @Test
    void shouldRejectProjectNameLongerThan120Characters() {
        String longName = "A".repeat(121);

        assertThrows(
                IllegalArgumentException.class,
                () -> Project.create(longName, null, null, null, now)
        );
    }

    @Test
    void shouldUpdateProjectDetails() {
        Project project = Project.create(
                "Old name", null, null, null, now
        );

        Instant updateTime = now.plusSeconds(60);

        project.updateDetails(
                "New name",
                "Jane Smith",
                "Paris, France",
                "Updated description",
                updateTime
        );

        assertEquals("New name", project.getName());
        assertEquals("Jane Smith", project.getClientName());
        assertEquals("Paris, France", project.getSiteAddress());
        assertEquals("Updated description", project.getDescription());
        assertEquals(now, project.getCreatedAt());
        assertEquals(updateTime, project.getUpdatedAt());
    }

    @Test
    void shouldArchiveProject() {
        Project project = Project.create(
                "Test project", null, null, null, now
        );

        Instant archiveTime = now.plusSeconds(120);

        project.archive(archiveTime);

        assertEquals(ProjectStatus.ARCHIVED, project.getStatus());
        assertEquals(archiveTime, project.getArchivedAt());
        assertEquals(archiveTime, project.getUpdatedAt());
    }

    @Test
    void shouldNotAllowUpdatingArchivedProject() {
        Project project = Project.create(
                "Test project", null, null, null, now
        );

        project.archive(now.plusSeconds(60));

        assertThrows(
                IllegalStateException.class,
                () -> project.updateDetails(
                        "Changed", null, null, null,
                        now.plusSeconds(120)
                )
        );
    }

    @Test
void shouldNotPartiallyUpdateWhenValidationFails() {
    Instant now = Instant.parse("2026-01-01T10:00:00Z");

    Project project = Project.create(
            "Original project",
            "Original client",
            "Original address",
            "Original description",
            now
    );

    assertThrows(
            IllegalArgumentException.class,
            () -> project.updateDetails(
                    "New name",
                    "New client",
                    "New address",
                    "x".repeat(2001),
                    now.plusSeconds(60)
            )
    );

    assertEquals("Original project", project.getName());
    assertEquals("Original client", project.getClientName());
    assertEquals("Original address", project.getSiteAddress());
    assertEquals("Original description", project.getDescription());
        }
}