package fr.billiardplanner.floorplan.infrastructure.inspection;

import fr.billiardplanner.floorplan.application.inspection.ImageInspectionException;
import fr.billiardplanner.floorplan.application.inspection.ImageInspectionResult;

import org.springframework.stereotype.Component;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

@Component
public class ImageFloorPlanInspector {

    private static final int MAX_IMAGE_SIZE_BYTES = 25 * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 20_000;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;

    /**
     * Inspects a PNG or JPEG image.
     *
     * The caller owns the supplied InputStream.
     * This method does not close it.
     */
    public ImageInspectionResult inspect(
            InputStream inputStream,
            String expectedContentType
    ) {

        if (inputStream == null) {
            throw new IllegalArgumentException(
                    "Image input stream is required"
            );
        }

        if (expectedContentType == null) {
            throw new IllegalArgumentException(
                    "Image content type is required"
            );
        }

        byte[] imageBytes;

        try {
            imageBytes = inputStream.readNBytes(
                    MAX_IMAGE_SIZE_BYTES + 1
            );
        } catch (IOException exception) {
            throw new ImageInspectionException(
                    "Could not read the image",
                    exception
            );
        }

        if (imageBytes.length == 0) {
            throw new ImageInspectionException("Image file is empty");
        }

        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            throw new ImageInspectionException(
                    "Image exceeds the maximum supported size of 25 MB"
            );
        }

        try (
                ImageInputStream imageInput =
                        ImageIO.createImageInputStream(
                                new ByteArrayInputStream(imageBytes)
                        )
        ) {

            if (imageInput == null) {
                throw new ImageInspectionException(
                        "Image format could not be read"
                );
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageInput);

            if (!readers.hasNext()) {
                throw new ImageInspectionException(
                        "Unsupported or corrupted image"
                );
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInput, true, false);

                String format = normalizeFormat(
                        reader.getFormatName()
                );

                validateFormat(format, expectedContentType);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                validateDimensions(width, height);

                DpiMetadata dpi = readDpi(reader, format);

                // Decode only after checking dimensions and pixel count.
                var image = reader.read(0);

                if (image == null) {
                    throw new ImageInspectionException(
                            "Image content could not be decoded"
                    );
                }

                String colorModel = image.getColorModel().toString();

                Double physicalWidth = dpi.dpiX() == null
                        ? null
                        : width / (double) dpi.dpiX();

                Double physicalHeight = dpi.dpiY() == null
                        ? null
                        : height / (double) dpi.dpiY();

                return new ImageInspectionResult(
                        format,
                        width,
                        height,
                        colorModel,
                        dpi.dpiX(),
                        dpi.dpiY(),
                        physicalWidth,
                        physicalHeight
                );

            } finally {
                reader.dispose();
            }

        } catch (ImageInspectionException exception) {
            throw exception;

        } catch (IOException | RuntimeException exception) {
            throw new ImageInspectionException(
                    "Image is corrupted or cannot be inspected",
                    exception
            );
        }
    }

    private void validateDimensions(int width, int height) {

        if (width <= 0 || height <= 0) {
            throw new ImageInspectionException(
                    "Image has invalid dimensions"
            );
        }

        if (width > MAX_IMAGE_DIMENSION
                || height > MAX_IMAGE_DIMENSION) {
            throw new ImageInspectionException(
                    "Image dimensions exceed the maximum of 20,000 pixels"
            );
        }

        long pixelCount = (long) width * height;

        if (pixelCount > MAX_IMAGE_PIXELS) {
            throw new ImageInspectionException(
                    "Image exceeds the maximum of 20 million pixels"
            );
        }
    }

    private String normalizeFormat(String format) {
        return format.toLowerCase(Locale.ROOT);
    }

    private void validateFormat(
            String format,
            String expectedContentType
    ) {

        boolean valid = switch (expectedContentType) {
            case "image/png" -> format.equals("png");
            case "image/jpeg" ->
                    format.equals("jpeg") || format.equals("jpg");
            default -> false;
        };

        if (!valid) {
            throw new ImageInspectionException(
                    "Image content does not match its declared format"
            );
        }
    }

    private DpiMetadata readDpi(
            ImageReader reader,
            String format
    ) {

        try {
            IIOMetadata metadata = reader.getImageMetadata(0);

            if (metadata == null) {
                return new DpiMetadata(null, null);
            }

            String[] formatNames = metadata.getMetadataFormatNames();

            if (formatNames == null) {
                return new DpiMetadata(null, null);
            }

            for (String formatName : formatNames) {

                Node root = metadata.getAsTree(formatName);

                if (format.equals("png")) {
                    Node physicalNode = findNode(root, "pHYs");

                    if (physicalNode != null) {
                        DpiMetadata dpi = readPngDpi(physicalNode);

                        if (dpi.hasAnyValue()) {
                            return dpi;
                        }
                    }
                }

                if (format.equals("jpeg")) {
                    Node jfifNode = findNode(root, "app0JFIF");

                    if (jfifNode != null) {
                        DpiMetadata dpi = readJpegDpi(jfifNode);

                        if (dpi.hasAnyValue()) {
                            return dpi;
                        }
                    }
                }
            }

        } catch (Exception ignored) {
            // DPI is optional. Invalid or missing metadata must not
            // prevent inspection of an otherwise valid image.
        }

        return new DpiMetadata(null, null);
    }

    private DpiMetadata readPngDpi(Node physicalNode) {

        NamedNodeMap attributes = physicalNode.getAttributes();

        Node xAttribute = attributes.getNamedItem(
                "pixelsPerUnitXAxis"
        );

        Node yAttribute = attributes.getNamedItem(
                "pixelsPerUnitYAxis"
        );

        Node unitAttribute = attributes.getNamedItem(
                "unitSpecifier"
        );

        if (xAttribute == null
                || yAttribute == null
                || unitAttribute == null
                || !"meter".equals(unitAttribute.getNodeValue())) {
            return new DpiMetadata(null, null);
        }

        double pixelsPerMeterX = Double.parseDouble(
                xAttribute.getNodeValue()
        );

        double pixelsPerMeterY = Double.parseDouble(
                yAttribute.getNodeValue()
        );

        return new DpiMetadata(
                pixelsPerMeterToDpi(pixelsPerMeterX),
                pixelsPerMeterToDpi(pixelsPerMeterY)
        );
    }

    private DpiMetadata readJpegDpi(Node jfifNode) {

        NamedNodeMap attributes = jfifNode.getAttributes();

        Node unitAttribute = attributes.getNamedItem("resUnits");
        Node xAttribute = attributes.getNamedItem("Xdensity");
        Node yAttribute = attributes.getNamedItem("Ydensity");

        if (unitAttribute == null
                || xAttribute == null
                || yAttribute == null) {
            return new DpiMetadata(null, null);
        }

        int units = Integer.parseInt(unitAttribute.getNodeValue());

        double densityX = Double.parseDouble(
                xAttribute.getNodeValue()
        );

        double densityY = Double.parseDouble(
                yAttribute.getNodeValue()
        );

        return switch (units) {
            case 1 -> new DpiMetadata(
                    validDpi(densityX),
                    validDpi(densityY)
            );

            case 2 -> new DpiMetadata(
                    validDpi(densityX * 2.54),
                    validDpi(densityY * 2.54)
            );

            default -> new DpiMetadata(null, null);
        };
    }

    private Integer pixelsPerMeterToDpi(double pixelsPerMeter) {

        if (!Double.isFinite(pixelsPerMeter)
                || pixelsPerMeter <= 0) {
            return null;
        }

        return validDpi(pixelsPerMeter * 0.0254);
    }

    private Integer validDpi(double dpi) {

        if (!Double.isFinite(dpi)
                || dpi <= 0
                || dpi > Integer.MAX_VALUE) {
            return null;
        }

        return (int) Math.round(dpi);
    }

    private Node findNode(Node node, String nodeName) {

        if (node == null) {
            return null;
        }

        if (nodeName.equals(node.getNodeName())) {
            return node;
        }

        Node child = node.getFirstChild();

        while (child != null) {
            Node result = findNode(child, nodeName);

            if (result != null) {
                return result;
            }

            child = child.getNextSibling();
        }

        return null;
    }

    private record DpiMetadata(
            Integer dpiX,
            Integer dpiY
    ) {
        boolean hasAnyValue() {
            return dpiX != null || dpiY != null;
        }
    }
}
