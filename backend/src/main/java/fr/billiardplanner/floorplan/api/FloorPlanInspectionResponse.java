package fr.billiardplanner.floorplan.api;

import fr.billiardplanner.floorplan.application.FloorPlanInspectionResult;

import java.util.List;
import java.util.UUID;

public record FloorPlanInspectionResponse(
        UUID floorPlanId,
        String type,
        Integer pageCount,
        List<FloorPlanPageInspectionResponse> pages,
        FloorPlanImageInspectionResponse image
) {

    public FloorPlanInspectionResponse {
        pages = pages == null ? List.of() : List.copyOf(pages);
    }

    public static FloorPlanInspectionResponse from(
            FloorPlanInspectionResult result
    ) {
        List<FloorPlanPageInspectionResponse> pages =
                result.pages()
                        .stream()
                        .map(FloorPlanPageInspectionResponse::from)
                        .toList();

        FloorPlanImageInspectionResponse image =
                result.imageMetadata() == null
                        ? null
                        : FloorPlanImageInspectionResponse.from(
                                result.imageMetadata()
                        );

        return new FloorPlanInspectionResponse(
                result.floorPlanId(),
                result.type(),
                result.pageCount(),
                pages,
                image
        );
    }
}