package kz.nuraskhan.meet_again.magain.profile;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import kz.nuraskhan.meet_again.web.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Turns bytes somebody uploaded into a picture that is safe to keep and to show.
 *
 * <p>Nothing about the upload is trusted - not its name, not its declared type. What is kept is
 * never the uploaded file: it is decoded and encoded again, small. Each step closes a hole:
 *
 * <ol>
 *   <li><b>Only JPEG and PNG, recognised by the first bytes of the content.</b> A file is what its
 *       bytes are, whatever it is called. So an HTML page, a script, an SVG (which can carry
 *       scripts), an archive or a program renamed to {@code photo.png} is refused.
 *   <li><b>The size in pixels is read from the header and checked before decoding.</b> A tiny file
 *       can declare an enormous picture; decoding it would eat the memory of the server.
 *   <li><b>The picture is decoded and written anew.</b> This drops everything that is not pixels:
 *       the EXIF block (it often holds the GPS position of where the photo was taken), comments,
 *       and anything hidden after the image data in a file that is at once a picture and something
 *       else.
 *   <li><b>The result is scaled down</b> to what an avatar needs.
 * </ol>
 */
final class ImageSanitizer {

  /** The largest picture accepted for decoding. 24 megapixels is a modern phone camera. */
  static final int MAX_INPUT_SIDE = 8000;
  static final long MAX_INPUT_PIXELS = 24_000_000L;

  /** What is kept: fits into a square of this side. */
  static final int OUTPUT_SIDE = 512;

  /** @param contentType {@code image/jpeg} or {@code image/png} */
  record Result(String contentType, byte[] content) {
  }

  private ImageSanitizer() {}

  static Result sanitize(byte[] uploaded) {
    String format = detectFormat(uploaded);
    if (format == null) {
      throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_file",
          "Only JPEG and PNG pictures are accepted");
    }

    BufferedImage source = decode(uploaded, format);
    BufferedImage small = scaleDown(source);

    // A picture with transparency stays PNG; everything else becomes JPEG, which is smaller.
    boolean png = small.getColorModel().hasAlpha();

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      boolean written = ImageIO.write(small, png ? "png" : "jpeg", out);
      if (!written) {
        throw badImage();
      }
      return new Result(png ? "image/png" : "image/jpeg", out.toByteArray());
    } catch (IOException e) {
      throw badImage();
    }
  }

  /** @return "jpeg", "png", or null for anything else */
  static String detectFormat(byte[] bytes) {
    if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
      return "jpeg";
    }
    if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
      return "png";
    }
    return null;
  }

  private static BufferedImage decode(byte[] uploaded, String format) {
    // The reader is chosen by the format we recognised, not by whatever the library would guess:
    // no other decoder ever sees the bytes.
    Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
    if (!readers.hasNext()) {
      throw badImage();
    }
    ImageReader reader = readers.next();

    try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(uploaded))) {
      reader.setInput(in, true, true); // one image, metadata ignored

      int width = reader.getWidth(0);
      int height = reader.getHeight(0);

      if (width < 1 || height < 1) {
        throw badImage();
      }
      if (width > MAX_INPUT_SIDE || height > MAX_INPUT_SIDE || (long) width * height > MAX_INPUT_PIXELS) {
        throw ApiException.badRequest("image_too_large",
            "The picture is too large. Use one up to " + MAX_INPUT_SIDE + " pixels on a side");
      }

      BufferedImage image = reader.read(0);
      if (image == null) {
        throw badImage();
      }
      return image;
    } catch (ApiException e) {
      throw e;
    } catch (IOException | RuntimeException e) {
      // A damaged file, or one built to break the decoder: the answer is the same.
      throw badImage();
    } finally {
      reader.dispose();
    }
  }

  private static BufferedImage scaleDown(BufferedImage source) {
    int width = source.getWidth();
    int height = source.getHeight();

    // Never enlarged: a small picture stays as it is (but is still drawn anew).
    double scale = Math.min(1.0, (double) OUTPUT_SIDE / Math.max(width, height));
    int targetWidth = Math.max(1, (int) Math.round(width * scale));
    int targetHeight = Math.max(1, (int) Math.round(height * scale));

    int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
    BufferedImage target = new BufferedImage(targetWidth, targetHeight, type);

    Graphics2D graphics = target.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
    } finally {
      graphics.dispose();
    }
    return target;
  }

  private static boolean startsWith(byte[] bytes, int... signature) {
    if (bytes.length < signature.length) {
      return false;
    }
    for (int i = 0; i < signature.length; i++) {
      if ((bytes[i] & 0xFF) != signature[i]) {
        return false;
      }
    }
    return true;
  }

  private static ApiException badImage() {
    return ApiException.badRequest("bad_image", "This file cannot be read as a picture");
  }
}
