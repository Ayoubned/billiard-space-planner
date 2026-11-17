package fr.billiardplanner.floorplan.application.inspection;

import java.util.List;

public record PdfInspectionResult(
        int pageCount,
        List<PdfPageInfo> pages
) {
    public PdfInspectionResult {
        pages = List.copyOf(pages);

        if (pageCount != pages.size()) {
            throw new IllegalArgumentException(
                    "Page count must match the number of inspected pages"
            );
        }
    }
}