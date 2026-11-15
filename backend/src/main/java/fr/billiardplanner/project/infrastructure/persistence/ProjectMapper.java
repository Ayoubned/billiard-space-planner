package fr.billiardplanner.project.infrastructure.persistence;

import fr.billiardplanner.project.domain.Project;

import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectEntity toEntity(Project project) {
        return new ProjectEntity(project);
    }

    public Project toDomain(ProjectEntity entity) {
        return Project.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getClientName(),
                entity.getSiteAddress(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getArchivedAt()
        );
    }
}