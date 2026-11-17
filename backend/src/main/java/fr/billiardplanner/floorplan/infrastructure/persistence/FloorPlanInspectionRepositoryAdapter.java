package fr.billiardplanner.floorplan.infrastructure.persistence;

import fr.billiardplanner.floorplan.domain.FloorPlanImageMetadata;
import fr.billiardplanner.floorplan.domain.FloorPlanInspectionRepository;
import fr.billiardplanner.floorplan.domain.FloorPlanPage;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class FloorPlanInspectionRepositoryAdapter
        implements FloorPlanInspectionRepository {

    private final FloorPlanPageJpaRepository pageRepository;
    private final FloorPlanImageMetadataJpaRepository imageMetadataRepository;

    public FloorPlanInspectionRepositoryAdapter(
            FloorPlanPageJpaRepository pageRepository,
            FloorPlanImageMetadataJpaRepository imageMetadataRepository
    ) {
        this.pageRepository = pageRepository;
        this.imageMetadataRepository = imageMetadataRepository;
    }

    @Override
    public void savePdfPages(UUID floorPlanId, List<FloorPlanPage> pages) {
        if (floorPlanId == null) {
            throw new IllegalArgumentException("Floor plan ID is required");
        }
        if (pages == null) {
            throw new IllegalArgumentException("PDF pages are required");
        }
        if (pages.stream().anyMatch(page -> !floorPlanId.equals(page.floorPlanId()))) {
            throw new IllegalArgumentException(
                    "All pages must belong to the specified floor plan"
            );
        }

        pageRepository.deleteAllByFloorPlanId(floorPlanId);
        imageMetadataRepository.deleteById(floorPlanId);

        pageRepository.saveAll(
                pages.stream()
                        .map(FloorPlanPageEntity::new)
                        .toList()
        );
    }

    @Override
    public void saveImageMetadata(FloorPlanImageMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Image metadata is required");
        }

        UUID floorPlanId = metadata.floorPlanId();

        pageRepository.deleteAllByFloorPlanId(floorPlanId);

        imageMetadataRepository.save(
                new FloorPlanImageMetadataEntity(metadata)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FloorPlanPage> findPdfPages(UUID floorPlanId) {
        return pageRepository
                .findAllByFloorPlanIdOrderByPageNumberAsc(floorPlanId)
                .stream()
                .map(FloorPlanPageEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FloorPlanImageMetadata> findImageMetadata(UUID floorPlanId) {
        return imageMetadataRepository.findById(floorPlanId)
                .map(FloorPlanImageMetadataEntity::toDomain);
    }

    @Override
    public void deleteByFloorPlanId(UUID floorPlanId) {
        if (floorPlanId == null) {
            return;
        }

        pageRepository.deleteAllByFloorPlanId(floorPlanId);
        imageMetadataRepository.deleteById(floorPlanId);
    }
}