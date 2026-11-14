package kz.nuraskhan.hangelo;

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
import java.util.List;
import javax.imageio.ImageIO;
import kz.nuraskhan.hangelo.model.entity.Profile;
import kz.nuraskhan.hangelo.model.enums.Privilege;
import kz.nuraskhan.hangelo.model.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * The photos of a profile: several, ordered, limited by the privilege level - and uploaded files,
 * of which nothing is trusted.
 *
 * <p>Guard tests: each "refuses" test in the second half is a way to attack an upload form. See the
 * list in ImageSanitizer (profile module).
 */
class PhotoApiTest extends ApiTestBase {

  private static final String PHOTO = "/profile/me/photo";

  @Autowired ProfileRepository profileRepository;

  // ------------------------------------------------------------------ several photos

  @Test
  void add__storesThePhoto_makesTheFirstOneMain_andServesItWithoutToken() throws IOException {
    TestUser user = register();

    //
    //
    Response response = upload(PHOTO, user, "me.png", "image/png", image(200, 100, "png", false));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    JsonNode photos = response.body().get("photos");
    assertThat(photos.size()).isEqualTo(1);
    String address = photos.get(0).get("url").asString();
    assertThat(address).startsWith("/public/photo/");
    assertThat(response.body().get("avatarUrl").asString()).isEqualTo(address);

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
  void add__keepsSeveral_inTheOrderTheyWereAdded_andOthersSeeThemAll() {
    TestUser user = register();
    TestUser other = register();

    //
    //
    String first = addPhoto(user);
    String second = addPhoto(user);
    String third = addPhoto(user);
    //
    //

    JsonNode seen = get("/profile/" + user.id(), other).body();
    assertThat(urls(seen.get("photos"))).containsExactly(first, second, third);
    assertThat(seen.get("avatarUrl").asString()).isEqualTo(first);
    for (String address : List.of(first, second, third)) {
      assertThat(getRaw(address).status()).isEqualTo(200);
    }
  }

  @Test
  void add__isLimitedByThePrivilegeLevel() {
    TestUser user = register(); // STANDARD: three photos
    addPhoto(user);
    addPhoto(user);
    addPhoto(user);

    //
    //
    Response fourthOnStandard = upload(PHOTO, user, "4.png", "image/png", image(10, 10, "png", false));
    setPrivilege(user, Privilege.PRO); // six photos
    Response fourthOnPro = upload(PHOTO, user, "4.png", "image/png", image(10, 10, "png", false));
    addPhoto(user);
    addPhoto(user);
    Response seventhOnPro = upload(PHOTO, user, "7.png", "image/png", image(10, 10, "png", false));
    //
    //

    assertThat(fourthOnStandard.status()).isEqualTo(409);
    assertThat(fourthOnStandard.body().get("code").asString()).isEqualTo("photo_limit");
    assertThat(fourthOnStandard.body().get("message").asString()).contains("3 photos");

    assertThat(fourthOnPro.status()).isEqualTo(200);
    assertThat(fourthOnPro.body().get("privilege").asString()).isEqualTo("PRO");
    assertThat(fourthOnPro.body().get("maxPhotos").asInt()).isEqualTo(6);

    assertThat(seventhOnPro.status()).isEqualTo(409);
    assertThat(seventhOnPro.body().get("message").asString()).contains("6 photos");
    assertThat(get("/profile/me", user).body().get("photos").size()).isEqualTo(6);
  }

  @Test
  void add__neverGoesBeyondTenPhotos_evenOnTheHighestLevel() {
    TestUser user = register();
    setPrivilege(user, Privilege.MAX);
    for (int i = 0; i < 10; i++) {
      addPhoto(user);
    }

    //
    //
    Response eleventh = upload(PHOTO, user, "11.png", "image/png", image(10, 10, "png", false));
    //
    //

    assertThat(eleventh.status()).isEqualTo(409);
    assertThat(eleventh.body().get("code").asString()).isEqualTo("photo_limit");
    assertThat(eleventh.body().get("message").asString()).contains("10 photos");

    JsonNode me = get("/profile/me", user).body();
    assertThat(me.get("maxPhotos").asInt()).isEqualTo(10);
    assertThat(me.get("photos").size()).isEqualTo(10);
  }

  @Test
  void noLevel__allowsMoreThanTenPhotos() {
    //
    //
    int most = Arrays.stream(Privilege.values()).mapToInt(level -> level.maxPhotos).max().orElseThrow();
    //
    //

    assertThat(most).isEqualTo(10);
    assertThat(Privilege.PHOTO_CEILING).isEqualTo(10);
  }

  @Test
  void makeMain__putsThePhotoFirst_andItBecomesThePictureShownEverywhere() {
    TestUser user = register();
    TestUser friend = register();
    makeFriends(user, friend);
    String first = addPhoto(user);
    String second = addPhoto(user);
    String third = addPhoto(user);
    long thirdId = photoId(user, third);

    //
    //
    Response response = post(PHOTO + "/" + thirdId + "/make-main", user);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    // The others keep their order behind it.
    assertThat(urls(response.body().get("photos"))).containsExactly(third, first, second);
    assertThat(response.body().get("avatarUrl").asString()).isEqualTo(third);
    // The friend's list shows the new main photo.
    assertThat(get("/friend/list", friend).body().get(0).get("avatarUrl").asString()).isEqualTo(third);
  }

  @Test
  void remove__deletesThePhoto_andTheNextOneBecomesMain() {
    TestUser user = register();
    String first = addPhoto(user);
    String second = addPhoto(user);
    String third = addPhoto(user);

    //
    //
    Response response = post(PHOTO + "/" + photoId(user, first) + "/remove", user);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(urls(response.body().get("photos"))).containsExactly(second, third);
    assertThat(response.body().get("avatarUrl").asString()).isEqualTo(second);
    assertThat(getRaw(first).status()).isEqualTo(404);
    // The freed place can be taken again.
    assertThat(upload(PHOTO, user, "new.png", "image/png", image(10, 10, "png", false)).status()).isEqualTo(200);
  }

  @Test
  void remove__ofTheLastPhoto_leavesNoPicture() {
    TestUser user = register();
    String only = addPhoto(user);

    //
    //
    Response response = post(PHOTO + "/" + photoId(user, only) + "/remove", user);
    //
    //

    assertThat(response.body().get("photos").size()).isZero();
    assertThat(response.body().get("avatarUrl").isNull()).isTrue();
  }

  @Test
  void removeAndMakeMain__workOnlyOnOnesOwnPhotos() {
    TestUser owner = register();
    TestUser other = register();
    String address = addPhoto(owner);
    long id = photoId(owner, address);

    //
    //
    Response remove = post(PHOTO + "/" + id + "/remove", other);
    Response makeMain = post(PHOTO + "/" + id + "/make-main", other);
    //
    //

    assertThat(remove.status()).isEqualTo(404);
    assertThat(makeMain.status()).isEqualTo(404);
    assertThat(getRaw(address).status()).isEqualTo(200);
  }

  // ------------------------------------------------------------------ what happens to a picture

  @Test
  void add__scalesALargePictureDown_keepingItsProportions() throws IOException {
    TestUser user = register();

    //
    //
    Response response = upload(PHOTO, user, "big.jpg", "image/jpeg", image(4000, 2000, "jpeg", false));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    BufferedImage served = read(response.body().get("avatarUrl").asString());
    assertThat(served.getWidth()).isEqualTo(1080);
    assertThat(served.getHeight()).isEqualTo(540);
  }

  @Test
  void add__turnsAPhotoThatAPhoneStoredSideways() throws IOException {
    TestUser user = register();
    // 200 wide, 100 high: red on the left, blue on the right - plus the note phones write:
    // "orientation 6", meaning "show it turned a quarter clockwise".
    byte[] sideways = withExifOrientation(leftRedRightBlue(200, 100), 6);

    //
    //
    Response response = upload(PHOTO, user, "portrait.jpg", "image/jpeg", sideways);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    BufferedImage served = read(response.body().get("avatarUrl").asString());
    // Now it stands upright: 100 wide, 200 high, the red half on top, the blue half below.
    assertThat(served.getWidth()).isEqualTo(100);
    assertThat(served.getHeight()).isEqualTo(200);
    assertThat(isReddish(served.getRGB(50, 30))).isTrue();
    assertThat(isBluish(served.getRGB(50, 170))).isTrue();
  }

  @Test
  void add__keepsTransparencyAsPng() {
    TestUser user = register();

    //
    //
    Response response = upload(PHOTO, user, "logo.png", "image/png", image(64, 64, "png", true));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    RawResponse picture = getRaw(response.body().get("avatarUrl").asString());
    assertThat(picture.headers().firstValue("Content-Type")).contains("image/png");
  }

  @Test
  void add__neverKeepsTheUploadedBytes_soNothingHiddenInTheFileSurvives() {
    TestUser user = register();
    // A real picture with something else glued after the end of the image data: a file that is at
    // once a valid PNG and a carrier for a script.
    byte[] payload = "<script>alert('stolen')</script>".getBytes(StandardCharsets.UTF_8);
    byte[] polyglot = concat(image(50, 50, "png", false), payload);

    //
    //
    Response response = upload(PHOTO, user, "nice.png", "image/png", polyglot);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    byte[] served = getRaw(response.body().get("avatarUrl").asString()).body();
    assertThat(new String(served, StandardCharsets.ISO_8859_1)).doesNotContain("<script>");
    assertThat(served).isNotEqualTo(polyglot);
  }

  // ------------------------------------------------------------------ what is refused

  @Test
  void add__needsToken() {
    //
    //
    Response response = upload(PHOTO, null, "a.png", "image/png", image(10, 10, "png", false));
    //
    //

    assertThat(response.status()).isEqualTo(401);
  }

  @Test
  void add__refusesFilesThatOnlyClaimToBePictures() {
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
    Response asHtml = upload(PHOTO, user, "photo.png", "image/png", html);
    Response asSvg = upload(PHOTO, user, "photo.svg", "image/svg+xml", svg);
    Response asProgram = upload(PHOTO, user, "photo.jpg", "image/jpeg", windowsProgram);
    Response asArchive = upload(PHOTO, user, "photo.png", "image/png", zipArchive);
    Response asGif = upload(PHOTO, user, "photo.gif", "image/gif", gif);
    Response withPathInName = upload(PHOTO, user, "../../etc/passwd", "image/png", html);
    //
    //

    for (Response refused : new Response[] {asHtml, asSvg, asProgram, asArchive, asGif, withPathInName}) {
      assertThat(refused.status()).isEqualTo(415);
      assertThat(refused.body().get("code").asString()).isEqualTo("unsupported_file");
    }
    assertThat(get("/profile/me", user).body().get("photos").size()).isZero();
  }

  @Test
  void add__refusesFileThatStartsLikeAPictureButIsNot() {
    TestUser user = register();
    byte[] pngSignatureThenGarbage =
        concat(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, new byte[300]);
    byte[] jpegSignatureThenGarbage = concat(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, new byte[300]);
    byte[] cutInHalf = Arrays.copyOf(image(300, 300, "png", false), 120);
    // A JPEG whose EXIF block points outside itself: built to make a careless parser read astray.
    byte[] exifPointingNowhere = withBrokenExif(leftRedRightBlue(20, 20));

    //
    //
    Response fakePng = upload(PHOTO, user, "a.png", "image/png", pngSignatureThenGarbage);
    Response fakeJpeg = upload(PHOTO, user, "a.jpg", "image/jpeg", jpegSignatureThenGarbage);
    Response truncated = upload(PHOTO, user, "a.png", "image/png", cutInHalf);
    Response brokenExif = upload(PHOTO, user, "a.jpg", "image/jpeg", exifPointingNowhere);
    //
    //

    for (Response refused : new Response[] {fakePng, fakeJpeg, truncated}) {
      assertThat(refused.status()).isEqualTo(400);
      assertThat(refused.body().get("code").asString()).isEqualTo("bad_image");
    }
    // The picture itself is fine; only the note about turning it is nonsense - and is ignored.
    assertThat(brokenExif.status()).isEqualTo(200);
  }

  @Test
  void add__refusesTinyFileThatDeclaresAnEnormousPicture_withoutDecodingIt() {
    TestUser user = register();
    // 33 bytes that say "I am 30000 x 30000 pixels": decoding would ask for gigabytes of memory.
    byte[] bomb = pngHeaderDeclaring(30_000, 30_000);

    //
    //
    Response response = upload(PHOTO, user, "small.png", "image/png", bomb);
    //
    //

    assertThat(response.status()).isEqualTo(400);
    assertThat(response.body().get("code").asString()).isEqualTo("image_too_large");
  }

  @Test
  void add__refusesEmptyFileAndFileOverTheSizeLimit() {
    TestUser user = register();
    // A valid PNG signature, then padding up to 6.5 MB: over the 6 MB limit.
    byte[] tooBig = concat(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'},
        new byte[6_500_000]);

    //
    //
    Response empty = upload(PHOTO, user, "a.png", "image/png", new byte[0]);
    Response oversized = upload(PHOTO, user, "a.png", "image/png", tooBig);
    //
    //

    assertThat(empty.status()).isEqualTo(400);
    assertThat(oversized.status()).isEqualTo(413);
    assertThat(oversized.body().get("code").asString()).isEqualTo("file_too_large");
  }

  @Test
  void publicPhoto__answersNotFoundForUnknownAddress() {
    //
    //
    RawResponse response = getRaw("/public/photo/no-such-token");
    //
    //

    assertThat(response.status()).isEqualTo(404);
  }

  // ------------------------------------------------------------------ helpers

  /** Adds a small photo and returns its address. */
  private String addPhoto(TestUser user) {
    Response response = upload(PHOTO, user, "p.png", "image/png", image(12, 12, "png", false));
    assertThat(response.status()).isEqualTo(200);
    JsonNode photos = response.body().get("photos");
    return photos.get(photos.size() - 1).get("url").asString();
  }

  private long photoId(TestUser owner, String address) {
    return get("/profile/me", owner).body().get("photos").valueStream()
        .filter(photo -> photo.get("url").asString().equals(address))
        .findFirst().orElseThrow()
        .get("id").asLong();
  }

  /** What only we can do: nothing in the API changes a privilege level. */
  private void setPrivilege(TestUser user, Privilege privilege) {
    Profile profile = profileRepository.findById(user.id()).orElseThrow();
    profile.privilege = privilege;
    profileRepository.save(profile);
  }

  private BufferedImage read(String address) throws IOException {
    return ImageIO.read(new ByteArrayInputStream(getRaw(address).body()));
  }

  private static List<String> urls(JsonNode photos) {
    return photos.valueStream().map(photo -> photo.get("url").asString()).toList();
  }

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
    return encode(image, format);
  }

  /** A JPEG whose left half is red and right half is blue. */
  private static byte[] leftRedRightBlue(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.RED);
    graphics.fillRect(0, 0, width / 2, height);
    graphics.setColor(Color.BLUE);
    graphics.fillRect(width / 2, 0, width - width / 2, height);
    graphics.dispose();
    return encode(image, "jpeg");
  }

  private static byte[] encode(BufferedImage image, String format) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(image, format, out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The JPEG with an EXIF block saying this orientation, put right after the start of the file. */
  private static byte[] withExifOrientation(byte[] jpeg, int orientation) {
    byte[] tiff = ByteBuffer.allocate(26)
        .put(new byte[] {'M', 'M', 0, 42})  // big-endian TIFF
        .putInt(8)                           // the directory starts at byte 8
        .putShort((short) 1)                 // one entry
        .putShort((short) 0x0112)            // tag: orientation
        .putShort((short) 3)                 // type: 16-bit number
        .putInt(1)                           // one value
        .putShort((short) orientation).putShort((short) 0)
        .putInt(0)                           // no further directory
        .array();
    return withApp1(jpeg, concat(new byte[] {'E', 'x', 'i', 'f', 0, 0}, tiff));
  }

  /** The JPEG with an EXIF block whose directory "starts" far beyond the end of the block. */
  private static byte[] withBrokenExif(byte[] jpeg) {
    byte[] tiff = ByteBuffer.allocate(8)
        .put(new byte[] {'I', 'I', 42, 0})
        .putInt(0x7FFFFFF0)
        .array();
    return withApp1(jpeg, concat(new byte[] {'E', 'x', 'i', 'f', 0, 0}, tiff));
  }

  private static byte[] withApp1(byte[] jpeg, byte[] payload) {
    byte[] segment = ByteBuffer.allocate(4 + payload.length)
        .put((byte) 0xFF).put((byte) 0xE1)
        .putShort((short) (payload.length + 2))
        .put(payload)
        .array();
    // After the two bytes that start every JPEG.
    return concat(concat(Arrays.copyOf(jpeg, 2), segment), Arrays.copyOfRange(jpeg, 2, jpeg.length));
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

    java.util.zip.CRC32 crc = new java.util.zip.CRC32();
    crc.update(type);
    crc.update(data);

    return ByteBuffer.allocate(8 + 4 + 4 + 13 + 4)
        .put(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})
        .putInt(13).put(type).put(data).putInt((int) crc.getValue())
        .array();
  }

  private static boolean isReddish(int rgb) {
    Color color = new Color(rgb);
    return color.getRed() > 180 && color.getBlue() < 80;
  }

  private static boolean isBluish(int rgb) {
    Color color = new Color(rgb);
    return color.getBlue() > 180 && color.getRed() < 80;
  }

  private static byte[] concat(byte[] first, byte[] second) {
    byte[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }
}
