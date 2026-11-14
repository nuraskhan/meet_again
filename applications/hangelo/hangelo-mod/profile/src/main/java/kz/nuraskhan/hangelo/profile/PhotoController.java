package kz.nuraskhan.hangelo.profile;

import java.io.IOException;
import java.security.Principal;
import kz.nuraskhan.hangelo.profile.dto.MyProfileDto;
import kz.nuraskhan.hangelo.web.ApiException;
import kz.nuraskhan.hangelo.web.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Adding, removing and ordering the caller's own photos. Showing a photo: PublicPhotoController. */
@RestController
@RequestMapping("/profile/me/photo")
public class PhotoController {

  private final PhotoService photoService;
  private final ProfileQuery profileQuery;

  public PhotoController(PhotoService photoService, ProfileQuery profileQuery) {
    this.photoService = photoService;
    this.profileQuery = profileQuery;
  }

  /** A form upload with one part named {@code file}. The photo is added after the existing ones. */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public MyProfileDto add(Principal principal, @RequestPart("file") MultipartFile file) throws IOException {
    Long me = CurrentUser.id(principal);

    // Checked before the bytes are pulled into memory.
    if (file.getSize() > PhotoService.MAX_UPLOAD_BYTES) {
      throw PhotoService.tooLarge();
    }

    // Only the bytes go further. The file name and the content type the browser sent are not read.
    photoService.add(me, file.getBytes());

    return my(me);
  }

  @PostMapping("/{photoId}/remove")
  public MyProfileDto remove(Principal principal, @PathVariable("photoId") Long photoId) {
    Long me = CurrentUser.id(principal);
    photoService.remove(me, photoId);
    return my(me);
  }

  @PostMapping("/{photoId}/make-main")
  public MyProfileDto makeMain(Principal principal, @PathVariable("photoId") Long photoId) {
    Long me = CurrentUser.id(principal);
    photoService.makeMain(me, photoId);
    return my(me);
  }

  private MyProfileDto my(Long me) {
    return profileQuery.my(me)
        .orElseThrow(() -> ApiException.notFound("profile_not_found", "Profile not found"));
  }
}
