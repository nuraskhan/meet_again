package kz.nuraskhan.meet_again.magain.profile;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import kz.nuraskhan.meet_again.model.entity.Avatar;
import kz.nuraskhan.meet_again.model.entity.Profile;
import kz.nuraskhan.meet_again.model.repository.AvatarRepository;
import kz.nuraskhan.meet_again.model.repository.ProfileRepository;
import kz.nuraskhan.meet_again.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile pictures. The file lives in the database (the disk of the server does not survive a
 * restart on Heroku), already made safe by {@link ImageSanitizer}.
 */
@Service
public class AvatarService {

  /**
   * The largest upload accepted, bytes. The same number is in application.yaml
   * (spring.servlet.multipart.max-file-size), which stops a larger request before it gets here.
   */
  public static final long MAX_UPLOAD_BYTES = 2L * 1024 * 1024;

  /** Where a picture is served from; the token of the picture follows. */
  public static final String PUBLIC_PATH = "/public/avatar/";

  /** 18 random bytes = 144 bits: the address of a picture cannot be guessed. */
  private static final int TOKEN_BYTES = 18;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final AvatarRepository avatarRepository;
  private final ProfileRepository profileRepository;

  public AvatarService(AvatarRepository avatarRepository, ProfileRepository profileRepository) {
    this.avatarRepository = avatarRepository;
    this.profileRepository = profileRepository;
  }

  /**
   * Replaces the caller's picture with the uploaded one.
   *
   * @param uploaded the bytes of the file; its name and declared type are deliberately not
   *                 parameters - they are the uploader's claims and are not used for anything
   */
  @Transactional
  public void upload(Long me, byte[] uploaded) {
    if (uploaded.length == 0) {
      throw ApiException.badRequest("empty_file", "The file is empty");
    }
    if (uploaded.length > MAX_UPLOAD_BYTES) {
      throw tooLarge();
    }

    ImageSanitizer.Result safe = ImageSanitizer.sanitize(uploaded);

    Profile profile = profileRepository.findById(me)
        .orElseThrow(() -> ApiException.notFound("profile_not_found", "Profile not found"));

    Avatar avatar = avatarRepository.findById(me).orElseGet(() -> {
      Avatar created = new Avatar();
      created.userAccountId = me;
      return created;
    });

    // A new token on every upload: the old address stops working at once.
    avatar.token = newToken();
    avatar.contentType = safe.contentType();
    avatar.content = safe.content();
    avatar.updatedAt = Instant.now();
    avatarRepository.save(avatar);

    profile.avatarUrl = PUBLIC_PATH + avatar.token;
    profile.updatedAt = avatar.updatedAt;
  }

  /** Removes the caller's picture: the file itself is deleted. Does nothing if there is none. */
  @Transactional
  public void remove(Long me) {
    avatarRepository.findById(me).ifPresent(avatarRepository::delete);

    profileRepository.findById(me).ifPresent(profile -> {
      profile.avatarUrl = null;
      profile.updatedAt = Instant.now();
    });
  }

  public static ApiException tooLarge() {
    return new ApiException(HttpStatus.valueOf(413), "file_too_large",
        "The file is too large. The limit is " + (MAX_UPLOAD_BYTES / (1024 * 1024)) + " MB");
  }

  private static String newToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
