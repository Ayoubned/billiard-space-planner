package fr.billiardplanner.floorplan.infrastructure.inspection;

import fr.billiardplanner.floorplan.application.inspection.ImageInspectionException;
import fr.billiardplanner.floorplan.application.inspection.ImageInspectionResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

class ImageFloorPlanInspectorTest {

    private ImageFloorPlanInspector inspector;

    @BeforeEach
    void setUp() {
        inspector = new ImageFloorPlanInspector();
    }

    @Test
    void shouldInspectPngDimensions() throws Exception {

        byte[] png = createImage("png", 1200, 800);

        ImageInspectionResult result = inspector.inspect(
                new ByteArrayInputStream(png),
                "image/png"
        );

        assertEquals("png", result.format());
        assertEquals(1200, result.widthPixels());
        assertEquals(800, result.heightPixels());
        assertNotNull(result.colorModel());
    }

    @Test
    void shouldInspectJpegDimensions() throws Exception {

        byte[] jpeg = createImage("jpeg", 1920, 1080);

        ImageInspectionResult result = inspector.inspect(
                new ByteArrayInputStream(jpeg),
                "image/jpeg"
        );

        assertEquals("jpeg", result.format());
        assertEquals(1920, result.widthPixels());
        assertEquals(1080, result.heightPixels());
    }

    @Test
    void shouldReadPngDpiMetadata() throws Exception {

        byte[] png = createImageWithDpi(
                "png",
                1200,
                800,
                300,
                150
        );

        ImageInspectionResult result = inspector.inspect(
                new ByteArrayInputStream(png),
                "image/png"
        );

        assertEquals(300, result.dpiX());
        assertEquals(150, result.dpiY());

        assertEquals(
                4.0,
                result.physicalWidthInches(),
                0.01
        );

        assertEquals(
                800.0 / 150.0,
                result.physicalHeightInches(),
                0.01
        );
    }

    @Test
    void shouldReadJpegDpiMetadata() throws Exception {

        byte[] jpeg = createImageWithDpi(
                "jpeg",
                640,
                480,
                300,
                150
        );

        ImageInspectionResult result = inspector.inspect(
                new ByteArrayInputStream(jpeg),
                "image/jpeg"
        );

        assertEquals(300, result.dpiX());
        assertEquals(150, result.dpiY());

        assertEquals(
                640.0 / 300.0,
                result.physicalWidthInches(),
                0.01
        );

        assertEquals(
                480.0 / 150.0,
                result.physicalHeightInches(),
                0.01
        );
    }

    @Test
    void shouldReturnNullDpiWhenMetadataIsMissing()
            throws Exception {

        byte[] png = createImage("png", 1200, 800);

        ImageInspectionResult result = inspector.inspect(
                new ByteArrayInputStream(png),
                "image/png"
        );

        assertNull(result.dpiX());
        assertNull(result.dpiY());
        assertNull(result.physicalWidthInches());
        assertNull(result.physicalHeightInches());
    }

    @Test
    void shouldRejectCorruptedImage() {

        byte[] corrupted = "not an image".getBytes();

        assertThrows(
                ImageInspectionException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(corrupted),
                        "image/png"
                )
        );
    }

    @Test
    void shouldRejectFormatMismatch() throws Exception {

        byte[] png = createImage("png", 100, 100);

        assertThrows(
                ImageInspectionException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(png),
                        "image/jpeg"
                )
        );
    }

    @Test
    void shouldRejectEmptyImage() {

        assertThrows(
                ImageInspectionException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(new byte[0]),
                        "image/png"
                )
        );
    }

    @Test
    void shouldRejectOversizedImageFile() {

        byte[] oversized = new byte[25 * 1024 * 1024 + 1];

        assertThrows(
                ImageInspectionException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(oversized),
                        "image/png"
                )
        );
    }

    @Test
    void shouldRejectImageExceedingPixelLimit()
            throws Exception {

        // 5,000 x 5,000 = 25 million pixels.
        byte[] pngHeader = createPngHeader(5000, 5000);

        assertThrows(
                ImageInspectionException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(pngHeader),
                        "image/png"
                )
        );
    }

    @Test
    void shouldRejectImageExceedingDimensionLimit()
            throws Exception {

        byte[] pngHeader = createPngHeader(20001, 1);

        assertThrows(
                ImageInspectionException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(pngHeader),
                        "image/png"
                )
        );
    }

    @Test
    void shouldRejectNullInputStream() {

        assertThrows(
                IllegalArgumentException.class,
                () -> inspector.inspect(null, "image/png")
        );
    }

    @Test
    void shouldRejectNullContentType() {

        assertThrows(
                IllegalArgumentException.class,
                () -> inspector.inspect(
                        new ByteArrayInputStream(new byte[0]),
                        null
                )
        );
    }

    private byte[] createImage(
            String format,
            int width,
            int height
    ) throws IOException {

        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );

        try (ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            ImageIO.write(image, format, output);

            return output.toByteArray();
        }
    }

    private byte[] createImageWithDpi(
            String format,
            int width,
            int height,
            int dpiX,
            int dpiY
    ) throws Exception {

        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName(format);

        assertTrue(writers.hasNext(), "Image writer must exist");

        ImageWriter writer = writers.next();

        try (ByteArrayOutputStream bytes =
                     new ByteArrayOutputStream();
             ImageOutputStream output =
                     ImageIO.createImageOutputStream(bytes)) {

            ImageWriteParam writeParam =
                    writer.getDefaultWriteParam();

            IIOMetadata metadata = writer.getDefaultImageMetadata(
                    javax.imageio.ImageTypeSpecifier
                            .createFromRenderedImage(image),
                    writeParam
            );

            String nativeFormat =
                    metadata.getNativeMetadataFormatName();

            IIOMetadataNode root = (IIOMetadataNode)
                    metadata.getAsTree(nativeFormat);

            if (format.equals("png")) {
                addPngDpi(root, dpiX, dpiY);
            } else {
                addJpegDpi(root, dpiX, dpiY);
            }

            metadata.setFromTree(nativeFormat, root);

            writer.setOutput(output);

            writer.write(
                    null,
                    new IIOImage(image, null, metadata),
                    writeParam
            );

            output.flush();

            return bytes.toByteArray();

        } finally {
            writer.dispose();
        }
    }

    private void addPngDpi(
            IIOMetadataNode root,
            int dpiX,
            int dpiY
    ) {

        IIOMetadataNode physical = new IIOMetadataNode("pHYs");

        physical.setAttribute(
                "pixelsPerUnitXAxis",
                String.valueOf((int) Math.round(dpiX / 0.0254))
        );

        physical.setAttribute(
                "pixelsPerUnitYAxis",
                String.valueOf((int) Math.round(dpiY / 0.0254))
        );

        physical.setAttribute("unitSpecifier", "meter");

        root.appendChild(physical);
    }

    private void addJpegDpi(
            IIOMetadataNode root,
            int dpiX,
            int dpiY
    ) {

        IIOMetadataNode jfif = findNode(root, "app0JFIF");

        if (jfif == null) {
            IIOMetadataNode variety = findNode(
                    root,
                    "JPEGvariety"
            );

            assertNotNull(variety, "JPEGvariety metadata node must exist");

            jfif = new IIOMetadataNode("app0JFIF");
            variety.appendChild(jfif);
        }

        jfif.setAttribute("majorVersion", "1");
        jfif.setAttribute("minorVersion", "2");
        jfif.setAttribute("resUnits", "1");
        jfif.setAttribute("Xdensity", String.valueOf(dpiX));
        jfif.setAttribute("Ydensity", String.valueOf(dpiY));
        jfif.setAttribute("thumbWidth", "0");
        jfif.setAttribute("thumbHeight", "0");
    }

    private IIOMetadataNode findNode(
            IIOMetadataNode node,
            String nodeName
    ) {

        if (nodeName.equals(node.getNodeName())) {
            return node;
        }

        for (int i = 0; i < node.getLength(); i++) {
            if (node.item(i) instanceof IIOMetadataNode child) {
                IIOMetadataNode result = findNode(child, nodeName);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Creates a PNG containing only its signature and IHDR chunk.
     * This lets the test exercise dimension validation without
     * allocating a huge BufferedImage.
     */
    private byte[] createPngHeader(
            int width,
            int height
    ) throws IOException {

        byte[] signature = new byte[] {
                (byte) 137, 80, 78, 71, 13, 10, 26, 10
        };

        byte[] chunkType = "IHDR".getBytes();

        byte[] imageHeader = ByteBuffer.allocate(13)
                .putInt(width)
                .putInt(height)
                .put((byte) 8)
                .put((byte) 2)
                .put((byte) 0)
                .put((byte) 0)
                .put((byte) 0)
                .array();

        CRC32 crc = new CRC32();
        crc.update(chunkType);
        crc.update(imageHeader);

        try (ByteArrayOutputStream bytes =
                     new ByteArrayOutputStream();
             DataOutputStream output =
                     new DataOutputStream(bytes)) {

            output.write(signature);
            output.writeInt(13);
            output.write(chunkType);
            output.write(imageHeader);
            output.writeInt((int) crc.getValue());

            return bytes.toByteArray();
        }
    }
}
