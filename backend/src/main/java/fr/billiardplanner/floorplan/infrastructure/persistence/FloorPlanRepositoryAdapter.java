package fr.billiardplanner.floorplan.infrastructure.persistence;

import fr.billiardplanner.floorplan.domain.FloorPlan;
import fr.billiardplanner.floorplan.domain.FloorPlanRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class FloorPlanRepositoryAdapter implements FloorPlanRepository {

    private final FloorPlanJpaRepository jpaRepository;
    private final FloorPlanMapper mapper;

    public FloorPlanRepositoryAdapter(
            FloorPlanJpaRepository jpaRepository,
            FloorPlanMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public FloorPlan save(FloorPlan floorPlan) {
        FloorPlanEntity entity = jpaRepository.findById(floorPlan.getId())
                .map(existing -> {
                    existing.updateFrom(floorPlan);
                    return existing;
                })
                .orElseGet(() -> mapper.toEntity(floorPlan));

        FloorPlanEntity saved = jpaRepository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FloorPlan> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FloorPlan> findByProjectId(UUID projectId) {
        return jpaRepository
                .findAllByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
