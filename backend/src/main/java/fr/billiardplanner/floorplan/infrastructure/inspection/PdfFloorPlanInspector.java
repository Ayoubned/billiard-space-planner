package fr.billiardplanner.floorplan.infrastructure.inspection;

import fr.billiardplanner.floorplan.application.inspection.PdfInspectionException;
import fr.billiardplanner.floorplan.application.inspection.PdfInspectionResult;
import fr.billiardplanner.floorplan.application.inspection.PdfPageInfo;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfFloorPlanInspector {

    private static final int MAX_PDF_SIZE_BYTES = 25 * 1024 * 1024;

    /**
     * Inspects every page of a PDF.
     *
     * The caller owns the supplied InputStream.
     * This method does not close it.
     */
    public PdfInspectionResult inspect(InputStream inputStream) {

        if (inputStream == null) {
            throw new IllegalArgumentException(
                    "PDF input stream is required"
            );
        }

        byte[] pdfBytes;

        try {
            // Read at most 25 MB + 1 byte to detect oversized documents.
            pdfBytes = inputStream.readNBytes(MAX_PDF_SIZE_BYTES + 1);
        } catch (IOException exception) {
            throw new PdfInspectionException(
                    "Could not read the PDF file",
                    exception
            );
        }

        if (pdfBytes.length == 0) {
            throw new PdfInspectionException("PDF file is empty");
        }

        if (pdfBytes.length > MAX_PDF_SIZE_BYTES) {
            throw new PdfInspectionException(
                    "PDF file exceeds the maximum supported size of 25 MB"
            );
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            if (document.isEncrypted()) {
                throw new PdfInspectionException(
                        "Encrypted PDFs are not supported"
                );
            }

            int pageCount = document.getNumberOfPages();

            if (pageCount == 0) {
                throw new PdfInspectionException(
                        "PDF must contain at least one page"
                );
            }

            List<PdfPageInfo> pages = new ArrayList<>(pageCount);

            for (int index = 0; index < pageCount; index++) {

                PDPage page = document.getPage(index);

                pages.add(inspectPage(index + 1, page));
            }

            return new PdfInspectionResult(pageCount, pages);

        } catch (InvalidPasswordException exception) {
            throw new PdfInspectionException(
                    "Password-protected PDFs are not supported",
                    exception
            );

        } catch (IOException exception) {
            throw new PdfInspectionException(
                    "PDF is corrupted or cannot be read",
                    exception
            );
        }
    }

    private PdfPageInfo inspectPage(int pageNumber, PDPage page) {

        PDRectangle cropBox = page.getCropBox();

        double width = cropBox.getWidth();
        double height = cropBox.getHeight();

        if (!Double.isFinite(width)
                || !Double.isFinite(height)
                || width <= 0
                || height <= 0) {

            throw new PdfInspectionException(
                    "PDF page " + pageNumber
                            + " has invalid dimensions"
            );
        }

        int rotation = Math.floorMod(page.getRotation(), 360);

        if (rotation % 90 != 0) {
            throw new PdfInspectionException(
                    "PDF page " + pageNumber
                            + " has an unsupported rotation"
            );
        }

        return new PdfPageInfo(
                pageNumber,
                width,
                height,
                rotation
        );
    }
}