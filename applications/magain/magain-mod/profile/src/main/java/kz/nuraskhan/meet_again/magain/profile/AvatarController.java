package kz.nuraskhan.meet_again.magain.profile;

import java.io.IOException;
import java.security.Principal;
import kz.nuraskhan.meet_again.magain.profile.dto.MyProfileDto;
import kz.nuraskhan.meet_again.web.ApiException;
import kz.nuraskhan.meet_again.web.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Uploading and removing the caller's own picture. Showing a picture: PublicAvatarController. */
@RestController
@RequestMapping("/profile/me/avatar")
public class AvatarController {

  private final AvatarService avatarService;
  private final ProfileQuery profileQuery;

  public AvatarController(AvatarService avatarService, ProfileQuery profileQuery) {
    this.avatarService = avatarService;
    this.profileQuery = profileQuery;
  }

  /** A form upload with one part named {@code file}. */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public MyProfileDto upload(Principal principal, @RequestPart("file") MultipartFile file)
      throws IOException {
    Long me = CurrentUser.id(principal);

    // Checked before the bytes are pulled into memory.
    if (file.getSize() > AvatarService.MAX_UPLOAD_BYTES) {
      throw AvatarService.tooLarge();
    }

    // Only the bytes go further. The file name and the content type the browser sent are not read.
    avatarService.upload(me, file.getBytes());

    return profileQuery.my(me).orElseThrow(AvatarController::notFound);
  }

  @PostMapping("/remove")
  public MyProfileDto remove(Principal principal) {
    Long me = CurrentUser.id(principal);
    avatarService.remove(me);
    return profileQuery.my(me).orElseThrow(AvatarController::notFound);
  }

  private static ApiException notFound() {
    return ApiException.notFound("profile_not_found", "Profile not found");
  }
}
