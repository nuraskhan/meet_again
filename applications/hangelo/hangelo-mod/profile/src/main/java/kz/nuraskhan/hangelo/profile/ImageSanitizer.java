package kz.nuraskhan.hangelo.profile;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import kz.nuraskhan.hangelo.web.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Turns bytes somebody uploaded into a picture that is safe to keep and to show.
 *
 * <p>Nothing about the upload is trusted - not its name, not its declared type. What is kept is
 * never the uploaded file: it is decoded and encoded again. Each step closes a hole:
 *
 * <ol>
 *   <li><b>Only JPEG and PNG, recognised by the first bytes of the content.</b> A file is what its
 *       bytes are, whatever it is called. So an HTML page, a script, an SVG (which can carry
 *       scripts), an archive or a program renamed to {@code photo.png} is refused.
 *   <li><b>The size in pixels is read from the header and checked before decoding.</b> A tiny file
 *       can declare an enormous picture; decoding it would eat the memory of the server.
 *   <li><b>A large picture is decoded already reduced</b> (every n-th pixel), so a photo from a
 *       phone camera never sits in memory at full size.
 *   <li><b>The picture is decoded and written anew.</b> This drops everything that is not pixels:
 *       the EXIF block (it often holds the GPS position of where the photo was taken), comments,
 *       and anything hidden after the image data in a file that is at once a picture and something
 *       else.
 * </ol>
 *
 * <p>One thing from EXIF is used before it is dropped: the orientation. A phone stores a portrait
 * photo sideways plus a note "turn it"; without the note the photo would lie on its side. The
 * pixels are turned for real, and the note is gone with the rest.
 */
final class ImageSanitizer {

  /** The largest picture accepted for decoding. 24 megapixels is a modern phone camera. */
  static final int MAX_INPUT_SIDE = 8000;
  static final long MAX_INPUT_PIXELS = 24_000_000L;

  /** What is kept: fits into a square of this side. Enough for a full-screen photo on a phone. */
  static final int OUTPUT_SIDE = 1080;

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
    int orientation = format.equals("jpeg") ? exifOrientation(uploaded) : 1;
    BufferedImage upright = scaleDownAndTurn(source, orientation);

    // A picture with transparency stays PNG; everything else becomes JPEG, which is smaller.
    boolean png = upright.getColorModel().hasAlpha();

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      boolean written = ImageIO.write(upright, png ? "png" : "jpeg", out);
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

      // Take every n-th pixel while decoding, keeping at least twice the size we want - the fine
      // scaling below then has enough to work with.
      int step = Math.max(1, Math.max(width, height) / (OUTPUT_SIDE * 2));
      ImageReadParam param = reader.getDefaultReadParam();
      param.setSourceSubsampling(step, step, 0, 0);

      BufferedImage image = reader.read(0, param);
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

  /**
   * Scales the picture down to {@link #OUTPUT_SIDE} (never up) and turns it as its EXIF orientation
   * asks, in one drawing.
   *
   * @param orientation the EXIF value, 1 to 8; 1 = as it is
   */
  private static BufferedImage scaleDownAndTurn(BufferedImage source, int orientation) {
    int width = source.getWidth();
    int height = source.getHeight();

    double scale = Math.min(1.0, (double) OUTPUT_SIDE / Math.max(width, height));
    int w = Math.max(1, (int) Math.round(width * scale));
    int h = Math.max(1, (int) Math.round(height * scale));

    // Orientations 5 to 8 turn the picture by a quarter: width and height change places.
    boolean quarterTurn = orientation >= 5;
    int targetWidth = quarterTurn ? h : w;
    int targetHeight = quarterTurn ? w : h;

    // Where a point (x, y) of the scaled picture goes. Arguments: m00, m10, m01, m11, m02, m12,
    // meaning x' = m00*x + m01*y + m02 and y' = m10*x + m11*y + m12.
    AffineTransform turn = switch (orientation) {
      case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);   // mirrored left-right
      case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);  // upside down
      case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);   // mirrored top-bottom
      case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);    // mirrored along the diagonal
      case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);   // a quarter turn clockwise
      case 7 -> new AffineTransform(0, -1, -1, 0, h, w);  // mirrored along the other diagonal
      case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);   // a quarter turn counter-clockwise
      default -> new AffineTransform();
    };
    turn.scale((double) w / width, (double) h / height);

    int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
    BufferedImage target = new BufferedImage(targetWidth, targetHeight, type);

    Graphics2D graphics = target.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.drawImage(source, turn, null);
    } finally {
      graphics.dispose();
    }
    return target;
  }

  /**
   * The orientation a JPEG asks for in its EXIF block: 1 to 8, or 1 when there is none or the block
   * is not as expected. Reads only what it needs, checks every offset against the length of the
   * data, and never fails: a strange block simply means "as it is".
   */
  static int exifOrientation(byte[] jpeg) {
    try {
      int at = 2; // after the start-of-image marker FF D8
      while (at + 4 <= jpeg.length) {
        if ((jpeg[at] & 0xFF) != 0xFF) {
          return 1;
        }
        int marker = jpeg[at + 1] & 0xFF;
        if (marker == 0xDA || marker == 0xD9) {
          return 1; // the image data begins: no EXIF before it
        }
        int length = u16(jpeg, at + 2, false);
        if (length < 2) {
          return 1;
        }

        boolean exif = marker == 0xE1 && at + 10 <= jpeg.length
            && jpeg[at + 4] == 'E' && jpeg[at + 5] == 'x' && jpeg[at + 6] == 'i' && jpeg[at + 7] == 'f'
            && jpeg[at + 8] == 0 && jpeg[at + 9] == 0;
        if (exif) {
          return orientationFromTiff(jpeg, at + 10, Math.min(jpeg.length, at + 2 + length));
        }

        at += 2 + length;
      }
      return 1;
    } catch (RuntimeException e) {
      return 1;
    }
  }

  private static int orientationFromTiff(byte[] data, int tiff, int end) {
    if (tiff + 8 > end) {
      return 1;
    }
    boolean littleEndian;
    if (data[tiff] == 'I' && data[tiff + 1] == 'I') {
      littleEndian = true;
    } else if (data[tiff] == 'M' && data[tiff + 1] == 'M') {
      littleEndian = false;
    } else {
      return 1;
    }

    long directoryOffset = u32(data, tiff + 4, littleEndian);
    if (directoryOffset < 8 || tiff + directoryOffset + 2 > end) {
      return 1;
    }
    int directory = (int) (tiff + directoryOffset);
    int entries = u16(data, directory, littleEndian);

    for (int i = 0; i < entries; i++) {
      int entry = directory + 2 + 12 * i;
      if (entry + 12 > end) {
        return 1;
      }
      if (u16(data, entry, littleEndian) == 0x0112) { // the orientation tag
        int value = u16(data, entry + 8, littleEndian);
        return value >= 1 && value <= 8 ? value : 1;
      }
    }
    return 1;
  }

  private static int u16(byte[] data, int at, boolean littleEndian) {
    int first = data[at] & 0xFF;
    int second = data[at + 1] & 0xFF;
    return littleEndian ? (second << 8) | first : (first << 8) | second;
  }

  private static long u32(byte[] data, int at, boolean littleEndian) {
    long a = data[at] & 0xFF;
    long b = data[at + 1] & 0xFF;
    long c = data[at + 2] & 0xFF;
    long d = data[at + 3] & 0xFF;
    return littleEndian ? (d << 24) | (c << 16) | (b << 8) | a : (a << 24) | (b << 16) | (c << 8) | d;
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
