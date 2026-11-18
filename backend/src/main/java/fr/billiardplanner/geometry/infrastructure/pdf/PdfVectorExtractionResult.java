package fr.billiardplanner.geometry.infrastructure.pdf;

import java.util.List;
import java.util.Objects;

public record PdfVectorExtractionResult(
    List<PdfVectorPage> pages
) {
    public PdfVectorExtractionResult {
        Objects.requireNonNull(pages, "Pages are required");
        pages = List.copyOf(pages);
    }

    public int pageCount() {
        return pages.size();
    }

    public int pathCount() {
        return pages.stream()
            .mapToInt(page -> page.paths().size())
            .sum();
    }
}