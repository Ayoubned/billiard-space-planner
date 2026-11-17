package fr.billiardplanner.floorplan.infrastructure.persistence;

import fr.billiardplanner.floorplan.domain.FloorPlan;
import org.springframework.stereotype.Component;

@Component
public class FloorPlanMapper {

    public FloorPlanEntity toEntity(FloorPlan floorPlan) {
        return new FloorPlanEntity(floorPlan);
    }

    public FloorPlan toDomain(FloorPlanEntity entity) {
        return FloorPlan.reconstitute(
                entity.getId(),
                entity.getProjectId(),
                entity.getOriginalFilename(),
                entity.getStorageKey(),
                entity.getContentType(),
                entity.getFileSizeBytes(),
                entity.getSha256(),
                entity.getStatus(),
                entity.getPageCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}