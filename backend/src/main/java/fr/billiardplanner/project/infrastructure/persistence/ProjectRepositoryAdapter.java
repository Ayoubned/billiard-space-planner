
package fr.billiardplanner.project.infrastructure.persistence;

import fr.billiardplanner.project.domain.Project;
import fr.billiardplanner.project.domain.ProjectPage;
import fr.billiardplanner.project.domain.ProjectRepository;
import fr.billiardplanner.project.domain.ProjectSearchCriteria;
import fr.billiardplanner.project.domain.ProjectSortField;
import fr.billiardplanner.project.domain.SortDirection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectJpaRepository jpaRepository;
    private final ProjectMapper mapper;

    public ProjectRepositoryAdapter(
            ProjectJpaRepository jpaRepository,
            ProjectMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Project save(Project project) {
        ProjectEntity entity = jpaRepository.findById(project.getId())
                .map(existing -> {
                    existing.updateFrom(project);
                    return existing;
                })
                .orElseGet(() -> mapper.toEntity(project));

        ProjectEntity saved = jpaRepository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectPage search(ProjectSearchCriteria criteria) {
        Pageable pageable = createPageable(criteria);

        Page<ProjectEntity> result = jpaRepository.search(
                criteria.searchTerm() == null ? "" : criteria.searchTerm(),
                criteria.status(),
                criteria.includeArchived(),
                pageable
        );

        return new ProjectPage(
                result.getContent()
                        .stream()
                        .map(mapper::toDomain)
                        .toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    private Pageable createPageable(ProjectSearchCriteria criteria) {
        String property = switch (criteria.sortBy()) {
            case NAME -> "name";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
        };

        Sort.Direction direction =
                criteria.direction() == SortDirection.ASC
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, property)
                .and(Sort.by(Sort.Direction.ASC, "id"));

        return PageRequest.of(
                criteria.page(),
                criteria.size(),
                sort
        );
    }
}
