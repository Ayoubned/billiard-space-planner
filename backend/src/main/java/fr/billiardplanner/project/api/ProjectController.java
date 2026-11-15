package fr.billiardplanner.project.api;

import fr.billiardplanner.project.api.dto.CreateProjectRequest;
import fr.billiardplanner.project.api.dto.UpdateProjectRequest;
import fr.billiardplanner.project.api.dto.ProjectListRequest;

import fr.billiardplanner.project.application.ProjectApplicationService;
import fr.billiardplanner.project.application.dto.CreateProjectCommand;
import fr.billiardplanner.project.application.dto.UpdateProjectCommand;
import fr.billiardplanner.project.application.dto.ListProjectsQuery;
import fr.billiardplanner.project.application.dto.ProjectResult;
import fr.billiardplanner.project.application.dto.ProjectPageResult;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectApplicationService projectService;

    public ProjectController(ProjectApplicationService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<ProjectPageResult> list(
            @Valid @ModelAttribute ProjectListRequest request
    ) {
        ListProjectsQuery query = new ListProjectsQuery(
                request.search(),
                request.status(),
                request.includeArchived(),
                request.page(),
                request.size(),
                request.sortBy(),
                request.direction()
        );

        return ResponseEntity.ok(projectService.list(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResult> getById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProjectResult> create(
            @Valid @RequestBody CreateProjectRequest request
    ) {
        CreateProjectCommand command = new CreateProjectCommand(
                request.name(),
                request.clientName(),
                request.siteAddress(),
                request.description()
        );

        ProjectResult result = projectService.create(command);

        return ResponseEntity
                .created(URI.create("/api/v1/projects/" + result.id()))
                .body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResult> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        UpdateProjectCommand command = new UpdateProjectCommand(
                request.name(),
                request.clientName(),
                request.siteAddress(),
                request.description()
        );

        return ResponseEntity.ok(
                projectService.update(id, command)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(
            @PathVariable UUID id
    ) {
        projectService.archive(id);

        return ResponseEntity.noContent().build();
    }
}