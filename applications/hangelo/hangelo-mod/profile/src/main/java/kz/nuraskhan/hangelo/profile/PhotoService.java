package kz.nuraskhan.hangelo.profile;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import kz.nuraskhan.hangelo.model.entity.Profile;
import kz.nuraskhan.hangelo.model.entity.ProfilePhoto;
import kz.nuraskhan.hangelo.model.repository.ProfilePhotoRepository;
import kz.nuraskhan.hangelo.model.repository.ProfileRepository;
import kz.nuraskhan.hangelo.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The photos of a profile: several, in the person's order, the first being the main one.
 *
 * <p>How many a person may have is decided by their privilege level (see Privilege.maxPhotos).
 *
 * <p>The files live in the database (the disk of the server does not survive a restart on Heroku),
 * already made safe by {@link ImageSanitizer}.
 */
@Service
public class PhotoService {

  /**
   * The largest upload accepted, bytes. The same number is in application.yaml
   * (spring.servlet.multipart.max-file-size), which stops a larger request before it gets here.
   * Six megabytes covers a photo straight from a phone camera.
   */
  public static final long MAX_UPLOAD_BYTES = 6L * 1024 * 1024;

  /** Where a photo is served from; the token of the photo follows. */
  public static final String PUBLIC_PATH = "/public/photo/";

  /** 18 random bytes = 144 bits: the address of a photo cannot be guessed. */
  private static final int TOKEN_BYTES = 18;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final ProfilePhotoRepository profilePhotoRepository;
  private final ProfileRepository profileRepository;

  public PhotoService(ProfilePhotoRepository profilePhotoRepository, ProfileRepository profileRepository) {
    this.profilePhotoRepository = profilePhotoRepository;
    this.profileRepository = profileRepository;
  }

  /**
   * Adds a photo after the ones the caller has. The first photo becomes the main one.
   *
   * @param uploaded the bytes of the file; its name and declared type are deliberately not
   *                 parameters - they are the uploader's claims and are not used for anything
   */
  @Transactional
  public void add(Long me, byte[] uploaded) {
    if (uploaded.length == 0) {
      throw ApiException.badRequest("empty_file", "The file is empty");
    }
    if (uploaded.length > MAX_UPLOAD_BYTES) {
      throw tooLarge();
    }

    Profile profile = profile(me);
    List<ProfilePhoto> photos = profilePhotoRepository.findByUserAccountIdOrderByPosition(me);

    // Checked before the picture is decoded: no work for an upload that cannot be kept.
    int allowed = profile.privilege.maxPhotos;
    if (photos.size() >= allowed) {
      throw ApiException.conflict("photo_limit",
          "Your plan allows " + allowed + " photos. Remove one to add another");
    }

    ImageSanitizer.Result safe = ImageSanitizer.sanitize(uploaded);

    ProfilePhoto photo = new ProfilePhoto();
    photo.userAccountId = me;
    photo.position = photos.size() + 1;
    photo.token = newToken();
    photo.contentType = safe.contentType();
    photo.content = safe.content();
    photo.createdAt = Instant.now();
    profilePhotoRepository.save(photo);

    photos.add(photo);
    showMain(profile, photos);
  }

  /** Removes one of the caller's photos: the file itself is deleted. The rest close the gap. */
  @Transactional
  public void remove(Long me, Long photoId) {
    Profile profile = profile(me);
    List<ProfilePhoto> photos = new ArrayList<>(profilePhotoRepository.findByUserAccountIdOrderByPosition(me));

    ProfilePhoto removed = mine(photos, photoId);
    photos.remove(removed);
    profilePhotoRepository.delete(removed);

    renumber(photos);
    showMain(profile, photos);
  }

  /** Puts one of the caller's photos first; the others keep their order behind it. */
  @Transactional
  public void makeMain(Long me, Long photoId) {
    Profile profile = profile(me);
    List<ProfilePhoto> photos = new ArrayList<>(profilePhotoRepository.findByUserAccountIdOrderByPosition(me));

    ProfilePhoto chosen = mine(photos, photoId);
    photos.remove(chosen);
    photos.add(0, chosen);

    renumber(photos);
    showMain(profile, photos);
  }

  public static ApiException tooLarge() {
    return new ApiException(HttpStatus.valueOf(413), "file_too_large",
        "The file is too large. The limit is " + (MAX_UPLOAD_BYTES / (1024 * 1024)) + " MB");
  }

  /** Someone else's photo "does not exist" for the caller. */
  private static ProfilePhoto mine(List<ProfilePhoto> myPhotos, Long photoId) {
    return myPhotos.stream()
        .filter(photo -> photo.id.equals(photoId))
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("photo_not_found", "Photo not found"));
  }

  /** Positions 1, 2, 3... in the order of the list. */
  private static void renumber(List<ProfilePhoto> photos) {
    for (int i = 0; i < photos.size(); i++) {
      photos.get(i).position = i + 1;
    }
  }

  /** Keeps profile.avatarUrl equal to the address of the first photo. */
  private static void showMain(Profile profile, List<ProfilePhoto> photos) {
    profile.avatarUrl = photos.isEmpty() ? null : PUBLIC_PATH + photos.get(0).token;
    profile.updatedAt = Instant.now();
  }

  private Profile profile(Long me) {
    return profileRepository.findById(me)
        .orElseThrow(() -> ApiException.notFound("profile_not_found", "Profile not found"));
  }

  private static String newToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
