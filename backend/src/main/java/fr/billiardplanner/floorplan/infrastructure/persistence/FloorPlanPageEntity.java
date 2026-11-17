package fr.billiardplanner.floorplan.infrastructure.persistence;

import fr.billiardplanner.floorplan.domain.FloorPlanPage;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "floor_plan_pages",
        schema = "planner",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_floor_plan_pages_number",
                columnNames = {"floor_plan_id", "page_number"}
        )
)
public class FloorPlanPageEntity {

    @Id
    private UUID id;

    @Column(name = "floor_plan_id", nullable = false)
    private UUID floorPlanId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "width_points", nullable = false, precision = 10, scale = 3)
    private BigDecimal widthPoints;

    @Column(name = "height_points", nullable = false, precision = 10, scale = 3)
    private BigDecimal heightPoints;

    @Column(name = "rotation_degrees", nullable = false)
    private int rotationDegrees;

    protected FloorPlanPageEntity() {
    }

    public FloorPlanPageEntity(FloorPlanPage page) {
        this.id = page.id();
        this.floorPlanId = page.floorPlanId();
        this.pageNumber = page.pageNumber();
        this.widthPoints = BigDecimal.valueOf(page.widthPoints());
        this.heightPoints = BigDecimal.valueOf(page.heightPoints());
        this.rotationDegrees = page.rotationDegrees();
    }

    public FloorPlanPage toDomain() {
        return new FloorPlanPage(
                id,
                floorPlanId,
                pageNumber,
                widthPoints.doubleValue(),
                heightPoints.doubleValue(),
                rotationDegrees
        );
    }
}