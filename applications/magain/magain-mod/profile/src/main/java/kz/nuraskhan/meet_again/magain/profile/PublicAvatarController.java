package kz.nuraskhan.meet_again.magain.profile;

import java.time.Duration;
import kz.nuraskhan.meet_again.model.entity.Avatar;
import kz.nuraskhan.meet_again.model.repository.AvatarRepository;
import kz.nuraskhan.meet_again.web.ApiException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shows a profile picture. Open without a token (everything under {@code /public} is): an
 * {@code <img>} tag cannot send one. The protection is the address itself - it contains a random
 * token nobody can guess, and it changes with every upload.
 */
@RestController
@RequestMapping("/public/avatar")
public class PublicAvatarController {

  private final AvatarRepository avatarRepository;

  public PublicAvatarController(AvatarRepository avatarRepository) {
    this.avatarRepository = avatarRepository;
  }

  @GetMapping("/{token}")
  public ResponseEntity<byte[]> avatar(@PathVariable("token") String token) {
    Avatar avatar = avatarRepository.findByToken(token)
        .orElseThrow(() -> ApiException.notFound("avatar_not_found", "Picture not found"));

    return ResponseEntity.ok()
        // The type comes from our own re-encoding: image/jpeg or image/png, never from the uploader.
        .contentType(MediaType.parseMediaType(avatar.contentType))
        // An address never shows another picture, so a browser may keep it for good.
        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
        // Belt and braces, should a browser ever open the address as a page: no scripts, no
        // loading of anything, and the content is not to be guessed as another type.
        .header("Content-Security-Policy", "default-src 'none'; sandbox")
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Disposition", "inline")
        .body(avatar.content);
  }
}
