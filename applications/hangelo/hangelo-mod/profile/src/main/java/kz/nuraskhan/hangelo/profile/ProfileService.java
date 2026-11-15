package kz.nuraskhan.hangelo.profile;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import kz.nuraskhan.hangelo.profile.dto.PromptDto;
import kz.nuraskhan.hangelo.profile.dto.UpdateProfileRequest;
import kz.nuraskhan.hangelo.model.entity.Profile;
import kz.nuraskhan.hangelo.model.enums.Privilege;
import kz.nuraskhan.hangelo.model.entity.ProfileInterest;
import kz.nuraskhan.hangelo.model.entity.ProfilePrompt;
import kz.nuraskhan.hangelo.model.repository.ProfileInterestRepository;
import kz.nuraskhan.hangelo.model.repository.ProfilePromptRepository;
import kz.nuraskhan.hangelo.model.repository.ProfileRepository;
import kz.nuraskhan.hangelo.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

  private final ProfileRepository profileRepository;
  private final ProfileInterestRepository profileInterestRepository;
  private final ProfilePromptRepository profilePromptRepository;

  public ProfileService(ProfileRepository profileRepository,
                        ProfileInterestRepository profileInterestRepository,
                        ProfilePromptRepository profilePromptRepository) {
    this.profileRepository = profileRepository;
    this.profileInterestRepository = profileInterestRepository;
    this.profilePromptRepository = profilePromptRepository;
  }

  /**
   * Replaces what the caller says about themselves: the text fields, the interests, the prompts.
   * The photos and the privilege level are not touched - the request has no way to carry them.
   */
  @Transactional
  public void update(Long me, UpdateProfileRequest request) {
    if (!profileRepository.existsById(me)) {
      throw ApiException.notFound("profile_not_found", "Profile not found");
    }

    // The lists first: their removal clears the persistence context, so the profile is taken
    // after it.
    profileInterestRepository.deleteAllOf(me);
    int position = 1;
    for (String label : withoutRepeats(request.interests())) {
      ProfileInterest interest = new ProfileInterest();
      interest.userAccountId = me;
      interest.position = position++;
      interest.label = label;
      profileInterestRepository.save(interest);
    }

    profilePromptRepository.deleteAllOf(me);
    position = 1;
    for (PromptDto prompt : request.prompts() == null ? List.<PromptDto>of() : request.prompts()) {
      ProfilePrompt saved = new ProfilePrompt();
      saved.userAccountId = me;
      saved.position = position++;
      saved.question = prompt.question().trim();
      saved.answer = prompt.answer().trim();
      profilePromptRepository.save(saved);
    }

    Profile profile = profileRepository.findById(me).orElseThrow();
    profile.displayName = request.displayName().trim();
    profile.birthDate = request.birthDate();
    profile.pronouns = blankToNull(request.pronouns());
    profile.city = blankToNull(request.city());
    profile.hometown = blankToNull(request.hometown());
    profile.occupation = blankToNull(request.occupation());
    profile.education = blankToNull(request.education());
    profile.languages = blankToNull(request.languages());
    profile.bio = blankToNull(request.bio());
    profile.updatedAt = Instant.now();
  }

  /** Trimmed, in the given order, each once whatever the letter case. */
  private static List<String> withoutRepeats(List<String> labels) {
    if (labels == null) {
      return List.of();
    }
    Set<String> seen = new LinkedHashSet<>();
    List<String> result = new java.util.ArrayList<>();
    for (String label : labels) {
      String trimmed = label.trim();
      if (seen.add(trimmed.toLowerCase(java.util.Locale.ROOT))) {
        result.add(trimmed);
      }
    }
    return result;
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }

  /**
   * "Start Plus": the person moves to the Plus plan.
   *
   * <p>A DEMONSTRATION: no payment is taken and none is asked for - the level is switched, and
   * that is all. When payments come, this is where a paid subscription will be checked first.
   *
   * <p>Only from the free plan. Someone on a higher level stays there: taking Plus must never
   * lower what a person already has.
   */
  @Transactional
  public void startPlus(Long me) {
    Profile profile = profileRepository.findById(me)
        .orElseThrow(() -> ApiException.notFound("profile_not_found", "Profile not found"));
    if (profile.privilege == Privilege.STANDARD) {
      profile.privilege = Privilege.PLUS;
    }
  }

  /** Back to the free plan. Only from Plus: a level that was given is not dropped this way. */
  @Transactional
  public void cancelPlus(Long me) {
    Profile profile = profileRepository.findById(me)
        .orElseThrow(() -> ApiException.notFound("profile_not_found", "Profile not found"));
    if (profile.privilege == Privilege.PLUS) {
      profile.privilege = Privilege.STANDARD;
    }
  }
}
