
package fr.billiardplanner.project.domain;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {

    Project save(Project project);

    Optional<Project> findById(UUID id);

    ProjectPage search(ProjectSearchCriteria criteria);
}
