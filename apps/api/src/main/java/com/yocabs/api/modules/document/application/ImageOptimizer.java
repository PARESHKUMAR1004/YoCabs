package com.yocabs.api.modules.document.application;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Turns whatever a phone camera produced into a photo fit to show a traveller: stood upright,
 * no larger than a screen needs, and a few hundred kilobytes instead of many megabytes.
 *
 * Big images are decoded already reduced (sub-sampled), so a 100-megapixel photo never has to
 * exist in memory in full.
 */
@Component
public class ImageOptimizer {

    private final int maxEdgePx;
    private final float jpegQuality;

    public ImageOptimizer(
            @Value("${yocabs.photos.max-edge-px:1600}") int maxEdgePx,
            @Value("${yocabs.photos.jpeg-quality:0.82}") float jpegQuality
    ) {
        this.maxEdgePx = maxEdgePx;
        this.jpegQuality = jpegQuality;
    }

    /** The optimised photo, always a JPEG. Throws IllegalArgumentException if it cannot be read. */
    public byte[] optimize(byte[] original) {
        try {
            BufferedImage decoded = decode(original);
            BufferedImage upright = applyOrientation(decoded, exifOrientation(original));
            return encode(scaleDown(upright));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("This image could not be read. Please use a JPEG or PNG photo.");
        }
    }

    private BufferedImage decode(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new IOException("Unsupported image format");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(input, true, true);

                int longest = Math.max(reader.getWidth(0), reader.getHeight(0));
                int step = Math.max(1, longest / (maxEdgePx * 2));

                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(step, step, 0, 0);

                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }

    /** The EXIF orientation (1 to 8) a camera recorded, or 1 when there is none. */
    private static int exifOrientation(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            if (exif != null && exif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return exif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception ignored) {
            // No readable metadata simply means the photo is already upright.
        }
        return 1;
    }

    /** Rotates or flips so the picture looks right without relying on the orientation tag. */
    private static BufferedImage applyOrientation(BufferedImage source, int orientation) {

        int w = source.getWidth();
        int h = source.getHeight();
        AffineTransform transform = new AffineTransform();
        boolean swapped = false;

        switch (orientation) {
            case 2 -> { transform.translate(w, 0); transform.scale(-1, 1); }
            case 3 -> { transform.translate(w, h); transform.rotate(Math.PI); }
            case 4 -> { transform.translate(0, h); transform.scale(1, -1); }
            case 6 -> { transform.translate(h, 0); transform.rotate(Math.PI / 2); swapped = true; }
            case 8 -> { transform.translate(0, w); transform.rotate(3 * Math.PI / 2); swapped = true; }
            default -> {
                return source;
            }
        }

        BufferedImage result = blank(swapped ? h : w, swapped ? w : h);
        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(source, transform, null);
        graphics.dispose();
        return result;
    }

    private BufferedImage scaleDown(BufferedImage source) {

        int longest = Math.max(source.getWidth(), source.getHeight());

        if (longest <= maxEdgePx) {
            return flatten(source);
        }

        double scale = (double) maxEdgePx / longest;
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage result = blank(width, height);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return result;
    }

    /** JPEG has no transparency: paint anything see-through onto white. */
    private static BufferedImage flatten(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage result = blank(source.getWidth(), source.getHeight());
        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return result;
    }

    private static BufferedImage blank(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private byte[] encode(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();

        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(jpegQuality);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(output);
                writer.write(null, new IIOImage(image, null, null), param);
            }

            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
