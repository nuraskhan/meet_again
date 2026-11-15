package kz.nuraskhan.hangelo.profile;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import kz.nuraskhan.hangelo.profile.dto.MyProfileDto;
import kz.nuraskhan.hangelo.profile.dto.ProfileCardDto;
import kz.nuraskhan.hangelo.profile.dto.ProfileDto;
import kz.nuraskhan.hangelo.profile.dto.UpdateProfileRequest;
import kz.nuraskhan.hangelo.web.ApiException;
import kz.nuraskhan.hangelo.web.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
public class ProfileController {

  private final ProfileService profileService;
  private final ProfileQuery profileQuery;

  public ProfileController(ProfileService profileService, ProfileQuery profileQuery) {
    this.profileService = profileService;
    this.profileQuery = profileQuery;
  }

  @GetMapping("/me")
  public MyProfileDto me(Principal principal) {
    return profileQuery.my(CurrentUser.id(principal)).orElseThrow(ProfileController::notFound);
  }

  @PostMapping("/me")
  public MyProfileDto updateMe(Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
    Long me = CurrentUser.id(principal);
    profileService.update(me, request);
    return profileQuery.my(me).orElseThrow(ProfileController::notFound);
  }

  /** "Start Plus": moves the caller to the Plus plan. A demonstration - no payment is taken. */
  @PostMapping("/plus/start")
  public MyProfileDto startPlus(Principal principal) {
    Long me = CurrentUser.id(principal);
    profileService.startPlus(me);
    return profileQuery.my(me).orElseThrow(ProfileController::notFound);
  }

  /** Back to the free plan. */
  @PostMapping("/plus/cancel")
  public MyProfileDto cancelPlus(Principal principal) {
    Long me = CurrentUser.id(principal);
    profileService.cancelPlus(me);
    return profileQuery.my(me).orElseThrow(ProfileController::notFound);
  }

  @GetMapping("/search")
  public List<ProfileCardDto> search(Principal principal, @RequestParam("text") String text) {
    return profileQuery.search(CurrentUser.id(principal), text);
  }

  @GetMapping("/{userId}")
  public ProfileDto byId(@PathVariable("userId") Long userId) {
    return profileQuery.byId(userId).orElseThrow(ProfileController::notFound);
  }

  private static ApiException notFound() {
    return ApiException.notFound("profile_not_found", "Profile not found");
  }
}
