package fr.billiardplanner.floorplan.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FloorPlanPageJpaRepository
        extends JpaRepository<FloorPlanPageEntity, UUID> {

    List<FloorPlanPageEntity> findAllByFloorPlanIdOrderByPageNumberAsc(
            UUID floorPlanId
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            DELETE FROM FloorPlanPageEntity page
            WHERE page.floorPlanId = :floorPlanId
            """)
    int deleteAllByFloorPlanId(@Param("floorPlanId") UUID floorPlanId);
}
