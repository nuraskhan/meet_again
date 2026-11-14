package kz.nuraskhan.meet_again.magain.profile;

import java.time.Duration;
import kz.nuraskhan.meet_again.model.entity.ProfilePhoto;
import kz.nuraskhan.meet_again.model.repository.ProfilePhotoRepository;
import kz.nuraskhan.meet_again.web.ApiException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shows a profile photo. Open without a token (everything under {@code /public} is): an
 * {@code <img>} tag cannot send one. The protection is the address itself - it contains a random
 * token nobody can guess. A removed photo's address stops working.
 */
@RestController
@RequestMapping("/public/photo")
public class PublicPhotoController {

  private final ProfilePhotoRepository profilePhotoRepository;

  public PublicPhotoController(ProfilePhotoRepository profilePhotoRepository) {
    this.profilePhotoRepository = profilePhotoRepository;
  }

  @GetMapping("/{token}")
  public ResponseEntity<byte[]> photo(@PathVariable("token") String token) {
    ProfilePhoto photo = profilePhotoRepository.findByToken(token)
        .orElseThrow(() -> ApiException.notFound("photo_not_found", "Photo not found"));

    return ResponseEntity.ok()
        // The type comes from our own re-encoding: image/jpeg or image/png, never from the uploader.
        .contentType(MediaType.parseMediaType(photo.contentType))
        // An address never shows another picture, so a browser may keep it for good.
        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
        // Belt and braces, should a browser ever open the address as a page: no scripts, no
        // loading of anything, and the content is not to be guessed as another type.
        .header("Content-Security-Policy", "default-src 'none'; sandbox")
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Disposition", "inline")
        .body(photo.content);
  }
}
