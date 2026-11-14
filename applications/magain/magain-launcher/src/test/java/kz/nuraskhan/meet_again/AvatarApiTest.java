package kz.nuraskhan.meet_again;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * The profile picture is an uploaded file, and nothing about an upload is trusted.
 *
 * <p>Guard tests: each "refuses" test below is a way to attack an upload form. See the list in
 * ImageSanitizer (profile module).
 */
class AvatarApiTest extends ApiTestBase {

  private static final String UPLOAD = "/profile/me/avatar";

  // ------------------------------------------------------------------ what works

  @Test
  void upload__storesThePictureAndServesItWithoutToken() throws IOException {
    TestUser user = register();
    TestUser other = register();

    //
    //
    Response response = upload(UPLOAD, user, "me.png", "image/png", image(200, 100, "png", false));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    String address = response.body().get("avatarUrl").asString();
    assertThat(address).startsWith("/public/avatar/");

    // Everyone sees the same address in the profile.
    assertThat(get("/profile/" + user.id(), other).body().get("avatarUrl").asString()).isEqualTo(address);

    RawResponse picture = getRaw(address);
    assertThat(picture.status()).isEqualTo(200);
    assertThat(picture.headers().firstValue("Content-Type")).contains("image/jpeg");
    assertThat(picture.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
    assertThat(picture.headers().firstValue("Content-Security-Policy").orElse("")).contains("default-src 'none'");
    assertThat(picture.headers().firstValue("Cache-Control").orElse("")).contains("immutable");

    BufferedImage served = ImageIO.read(new ByteArrayInputStream(picture.body()));
    assertThat(served.getWidth()).isEqualTo(200);
    assertThat(served.getHeight()).isEqualTo(100);
  }

  @Test
  void upload__scalesALargePictureDown_keepingItsProportions() throws IOException {
    TestUser user = register();

    //
    //
    Response response = upload(UPLOAD, user, "big.jpg", "image/jpeg", image(2000, 1000, "jpeg", false));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    BufferedImage served =
        ImageIO.read(new ByteArrayInputStream(getRaw(response.body().get("avatarUrl").asString()).body()));
    assertThat(served.getWidth()).isEqualTo(512);
    assertThat(served.getHeight()).isEqualTo(256);
  }

  @Test
  void upload__keepsTransparencyAsPng() {
    TestUser user = register();

    //
    //
    Response response = upload(UPLOAD, user, "logo.png", "image/png", image(64, 64, "png", true));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    RawResponse picture = getRaw(response.body().get("avatarUrl").asString());
    assertThat(picture.headers().firstValue("Content-Type")).contains("image/png");
  }

  @Test
  void upload__neverKeepsTheUploadedBytes_soNothingHiddenInTheFileSurvives() {
    TestUser user = register();
    // A real picture with something else glued after the end of the image data: a file that is at
    // once a valid PNG and a carrier for a script.
    byte[] payload = "<script>alert('stolen')</script>".getBytes(StandardCharsets.UTF_8);
    byte[] polyglot = concat(image(50, 50, "png", false), payload);

    //
    //
    Response response = upload(UPLOAD, user, "nice.png", "image/png", polyglot);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    byte[] served = getRaw(response.body().get("avatarUrl").asString()).body();
    assertThat(new String(served, StandardCharsets.ISO_8859_1)).doesNotContain("<script>");
    assertThat(served).isNotEqualTo(polyglot);
  }

  @Test
  void upload__again_replacesThePicture_andTheOldAddressStopsWorking() {
    TestUser user = register();
    String first = upload(UPLOAD, user, "a.png", "image/png", image(10, 10, "png", false))
        .body().get("avatarUrl").asString();

    //
    //
    Response response = upload(UPLOAD, user, "b.png", "image/png", image(20, 20, "png", false));
    //
    //

    String second = response.body().get("avatarUrl").asString();
    assertThat(second).isNotEqualTo(first);
    assertThat(getRaw(second).status()).isEqualTo(200);
    assertThat(getRaw(first).status()).isEqualTo(404);
  }

  @Test
  void remove__clearsThePicture_andItsAddressStopsWorking() {
    TestUser user = register();
    String address = upload(UPLOAD, user, "a.png", "image/png", image(10, 10, "png", false))
        .body().get("avatarUrl").asString();

    //
    //
    Response response = post(UPLOAD + "/remove", user);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("avatarUrl").isNull()).isTrue();
    assertThat(getRaw(address).status()).isEqualTo(404);
  }

  // ------------------------------------------------------------------ what is refused

  @Test
  void upload__needsToken() {
    //
    //
    Response response = upload(UPLOAD, null, "a.png", "image/png", image(10, 10, "png", false));
    //
    //

    assertThat(response.status()).isEqualTo(401);
  }

  @Test
  void upload__refusesFilesThatOnlyClaimToBePictures() {
    TestUser user = register();
    byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
    byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
        .getBytes(StandardCharsets.UTF_8);
    byte[] windowsProgram = concat(new byte[] {'M', 'Z', (byte) 0x90, 0}, new byte[200]);
    byte[] zipArchive = concat(new byte[] {'P', 'K', 3, 4}, new byte[200]);
    byte[] gif = concat("GIF89a".getBytes(StandardCharsets.US_ASCII), new byte[200]);

    //
    //
    // The name and the declared type say "picture" every time; the content says otherwise.
    Response asHtml = upload(UPLOAD, user, "photo.png", "image/png", html);
    Response asSvg = upload(UPLOAD, user, "photo.svg", "image/svg+xml", svg);
    Response asProgram = upload(UPLOAD, user, "photo.jpg", "image/jpeg", windowsProgram);
    Response asArchive = upload(UPLOAD, user, "photo.png", "image/png", zipArchive);
    Response asGif = upload(UPLOAD, user, "photo.gif", "image/gif", gif);
    Response withPathInName = upload(UPLOAD, user, "../../etc/passwd", "image/png", html);
    //
    //

    for (Response refused : new Response[] {asHtml, asSvg, asProgram, asArchive, asGif, withPathInName}) {
      assertThat(refused.status()).isEqualTo(415);
      assertThat(refused.body().get("code").asString()).isEqualTo("unsupported_file");
    }
    assertThat(get("/profile/me", user).body().get("avatarUrl").isNull()).isTrue();
  }

  @Test
  void upload__refusesFileThatStartsLikeAPictureButIsNot() {
    TestUser user = register();
    byte[] pngSignatureThenGarbage =
        concat(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, new byte[300]);
    byte[] jpegSignatureThenGarbage = concat(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, new byte[300]);
    byte[] cutInHalf = Arrays.copyOf(image(300, 300, "png", false), 120);

    //
    //
    Response fakePng = upload(UPLOAD, user, "a.png", "image/png", pngSignatureThenGarbage);
    Response fakeJpeg = upload(UPLOAD, user, "a.jpg", "image/jpeg", jpegSignatureThenGarbage);
    Response truncated = upload(UPLOAD, user, "a.png", "image/png", cutInHalf);
    //
    //

    for (Response refused : new Response[] {fakePng, fakeJpeg, truncated}) {
      assertThat(refused.status()).isEqualTo(400);
      assertThat(refused.body().get("code").asString()).isEqualTo("bad_image");
    }
  }

  @Test
  void upload__refusesTinyFileThatDeclaresAnEnormousPicture_withoutDecodingIt() {
    TestUser user = register();
    // 45 bytes that say "I am 30000 x 30000 pixels": decoding would ask for gigabytes of memory.
    byte[] bomb = pngHeaderDeclaring(30_000, 30_000);

    //
    //
    Response response = upload(UPLOAD, user, "small.png", "image/png", bomb);
    //
    //

    assertThat(response.status()).isEqualTo(400);
    assertThat(response.body().get("code").asString()).isEqualTo("image_too_large");
  }

  @Test
  void upload__refusesEmptyFileAndFileOverTheSizeLimit() {
    TestUser user = register();
    // A valid PNG signature, then padding up to 2.5 MB: over the 2 MB limit.
    byte[] tooBig = concat(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'},
        new byte[2_500_000]);

    //
    //
    Response empty = upload(UPLOAD, user, "a.png", "image/png", new byte[0]);
    Response oversized = upload(UPLOAD, user, "a.png", "image/png", tooBig);
    //
    //

    assertThat(empty.status()).isEqualTo(400);
    assertThat(oversized.status()).isEqualTo(413);
    assertThat(oversized.body().get("code").asString()).isEqualTo("file_too_large");
  }

  @Test
  void updateProfile__cannotSetThePictureToALink() {
    TestUser user = register();

    //
    //
    Response response = post("/profile/me", user,
        Map.of("displayName", "Linker", "avatarUrl", "https://tracker.example.test/pixel.png"));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("avatarUrl").isNull()).isTrue();
  }

  @Test
  void publicAvatar__answersNotFoundForUnknownAddress() {
    //
    //
    RawResponse response = getRaw("/public/avatar/no-such-token");
    //
    //

    assertThat(response.status()).isEqualTo(404);
  }

  // ------------------------------------------------------------------ helpers

  /** A real picture of the given size and format, with a shape in it so it is not one flat colour. */
  private static byte[] image(int width, int height, String format, boolean transparent) {
    BufferedImage image = new BufferedImage(width, height,
        transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    if (!transparent) {
      graphics.setColor(new Color(228, 87, 46));
      graphics.fillRect(0, 0, width, height);
    }
    graphics.setColor(new Color(23, 117, 107));
    graphics.fillOval(width / 4, height / 4, Math.max(1, width / 2), Math.max(1, height / 2));
    graphics.dispose();

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(image, format, out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The signature and the header chunk of a PNG that declares this size, and nothing after. */
  private static byte[] pngHeaderDeclaring(int width, int height) {
    byte[] type = "IHDR".getBytes(StandardCharsets.US_ASCII);
    byte[] data = ByteBuffer.allocate(13)
        .putInt(width).putInt(height)
        .put((byte) 8)  // bit depth
        .put((byte) 2)  // colour type: RGB
        .put((byte) 0).put((byte) 0).put((byte) 0)
        .array();

    CRC32 crc = new CRC32();
    crc.update(type);
    crc.update(data);

    return ByteBuffer.allocate(8 + 4 + 4 + 13 + 4)
        .put(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})
        .putInt(13).put(type).put(data).putInt((int) crc.getValue())
        .array();
  }

  private static byte[] concat(byte[] first, byte[] second) {
    byte[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }
}
