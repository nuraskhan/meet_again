package kz.nuraskhan.meet_again.magain.profile;

import java.time.Instant;
import kz.nuraskhan.meet_again.magain.profile.dto.UpdateProfileRequest;
import kz.nuraskhan.meet_again.model.entity.Profile;
import kz.nuraskhan.meet_again.model.repository.ProfileRepository;
import kz.nuraskhan.meet_again.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

  private final ProfileRepository profileRepository;

  public ProfileService(ProfileRepository profileRepository) {
    this.profileRepository = profileRepository;
  }

  @Transactional
  public void update(Long me, UpdateProfileRequest request) {
    Profile profile = profileRepository.findById(me)
        .orElseThrow(() -> ApiException.notFound("profile_not_found", "Profile not found"));

    profile.displayName = request.displayName().trim();
    profile.bio = request.bio();
    profile.city = request.city();
    profile.birthDate = request.birthDate();
    profile.updatedAt = Instant.now();
  }
}
