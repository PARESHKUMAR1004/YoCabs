package com.yocabs.api.modules.document.application;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageOptimizerTest {

    private final ImageOptimizer optimizer = new ImageOptimizer(1600, 0.82f);

    @Test
    void bigPhotosAreShrunkToScreenSizeAndBecomeJpegs() throws IOException {
        BufferedImage result = decode(optimizer.optimize(image(3200, 2400, "png")));

        assertEquals(1600, result.getWidth());
        assertEquals(1200, result.getHeight());
    }

    @Test
    void smallPhotosKeepTheirSize() throws IOException {
        BufferedImage result = decode(optimizer.optimize(image(400, 300, "png")));

        assertEquals(400, result.getWidth());
        assertEquals(300, result.getHeight());
    }

    @Test
    void theOutputIsMuchSmallerThanAPhoneOriginal() {
        // Noise does not compress, like the fine detail in a real photograph.
        byte[] original = noisyImage(2400, 1800);

        assertTrue(optimizer.optimize(original).length < original.length / 4);
    }

    @Test
    void aPhotoTheCameraRecordedSidewaysIsStoodUpright() throws IOException {
        // Landscape pixels, tagged "rotate 90 degrees clockwise" the way a phone held upright records it.
        byte[] sideways = withOrientation(image(400, 200, "jpg"), 6);

        BufferedImage result = decode(optimizer.optimize(sideways));

        assertEquals(200, result.getWidth());
        assertEquals(400, result.getHeight());
    }

    @Test
    void somethingThatIsNotAnImageIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(new byte[] {1, 2, 3, 4}));
    }

    private static byte[] noisyImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.util.Random random = new java.util.Random(7);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, random.nextInt(0x1000000));
            }
        }

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bytes);
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static byte[] image(int width, int height, String format) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        // Noise-like stripes so the file does not compress to nothing.
        for (int x = 0; x < width; x += 8) {
            graphics.setColor(new Color((x * 31) % 256, (x * 17) % 256, (x * 7) % 256));
            graphics.fillRect(x, 0, 8, height);
        }
        graphics.dispose();

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, bytes);
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** Inserts an EXIF block carrying the orientation right after the JPEG start marker. */
    private static byte[] withOrientation(byte[] jpeg, int orientation) {
        byte[] exif = {
                (byte) 0xFF, (byte) 0xE1, 0x00, 0x22,                 // APP1, length 34
                'E', 'x', 'i', 'f', 0, 0,                              // Exif header
                'M', 'M', 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08,          // big-endian TIFF, IFD at 8
                0x00, 0x01,                                            // one entry
                0x01, 0x12, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01,        // orientation, SHORT, count 1
                0x00, (byte) orientation, 0x00, 0x00,                  // value
                0x00, 0x00, 0x00, 0x00                                 // no next IFD
        };

        byte[] result = new byte[jpeg.length + exif.length];
        System.arraycopy(jpeg, 0, result, 0, 2);
        System.arraycopy(exif, 0, result, 2, exif.length);
        System.arraycopy(jpeg, 2, result, 2 + exif.length, jpeg.length - 2);
        return result;
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        assertEquals((byte) 0xFF, bytes[0]);
        assertEquals((byte) 0xD8, bytes[1]);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
}
