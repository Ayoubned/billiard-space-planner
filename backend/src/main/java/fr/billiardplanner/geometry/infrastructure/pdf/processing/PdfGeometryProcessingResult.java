package fr.billiardplanner.geometry.infrastructure.pdf.processing;

import fr.billiardplanner.geometry.infrastructure.pdf.PdfVectorExtractionResult;

import java.util.List;
import java.util.Objects;

public record PdfGeometryProcessingResult(
    PdfVectorExtractionResult rawExtraction,
    List<ProcessedPdfPage> pages
) {
    public PdfGeometryProcessingResult {
        Objects.requireNonNull(
            rawExtraction,
            "Raw extraction is required"
        );
        Objects.requireNonNull(pages, "Processed pages are required");

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

    public long polygonCount() {
        return pages.stream()
            .mapToLong(ProcessedPdfPage::polygonCount)
            .sum();
    }
}