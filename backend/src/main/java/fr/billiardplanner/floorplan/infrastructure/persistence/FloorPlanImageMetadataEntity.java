package fr.billiardplanner.floorplan.infrastructure.persistence;

import fr.billiardplanner.floorplan.domain.FloorPlanImageMetadata;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "floor_plan_image_metadata", schema = "planner")
public class FloorPlanImageMetadataEntity {

    @Id
    @Column(name = "floor_plan_id")
    private UUID floorPlanId;

    @Column(nullable = false, length = 10)
    private String format;

    @Column(name = "width_pixels", nullable = false)
    private int widthPixels;

    @Column(name = "height_pixels", nullable = false)
    private int heightPixels;

    @Column(name = "color_model", nullable = false, length = 255)
    private String colorModel;

    @Column(name = "dpi_x")
    private Integer dpiX;

    @Column(name = "dpi_y")
    private Integer dpiY;

    @Column(name = "physical_width_inches", precision = 12, scale = 4)
    private BigDecimal physicalWidthInches;

    @Column(name = "physical_height_inches", precision = 12, scale = 4)
    private BigDecimal physicalHeightInches;

    protected FloorPlanImageMetadataEntity() {
    }

    public FloorPlanImageMetadataEntity(FloorPlanImageMetadata metadata) {
        this.floorPlanId = metadata.floorPlanId();
        this.format = metadata.format();
        this.widthPixels = metadata.widthPixels();
        this.heightPixels = metadata.heightPixels();
        this.colorModel = metadata.colorModel();
        this.dpiX = metadata.dpiX();
        this.dpiY = metadata.dpiY();
        this.physicalWidthInches = toBigDecimal(metadata.physicalWidthInches());
        this.physicalHeightInches = toBigDecimal(metadata.physicalHeightInches());
    }

    public FloorPlanImageMetadata toDomain() {
        return new FloorPlanImageMetadata(
                floorPlanId,
                format,
                widthPixels,
                heightPixels,
                colorModel,
                dpiX,
                dpiY,
                toDouble(physicalWidthInches),
                toDouble(physicalHeightInches)
        );
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}