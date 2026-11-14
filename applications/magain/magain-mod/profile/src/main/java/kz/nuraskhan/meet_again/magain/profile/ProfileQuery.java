package kz.nuraskhan.meet_again.magain.profile;

import static kz.nuraskhan.meet_again.jooq.Tables.PROFILE;
import static kz.nuraskhan.meet_again.jooq.Tables.PROFILE_INTEREST;
import static kz.nuraskhan.meet_again.jooq.Tables.PROFILE_PHOTO;
import static kz.nuraskhan.meet_again.jooq.Tables.PROFILE_PROMPT;
import static kz.nuraskhan.meet_again.jooq.Tables.USER_ACCOUNT;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import kz.nuraskhan.meet_again.magain.profile.dto.MyProfileDto;
import kz.nuraskhan.meet_again.magain.profile.dto.PhotoDto;
import kz.nuraskhan.meet_again.magain.profile.dto.ProfileCardDto;
import kz.nuraskhan.meet_again.magain.profile.dto.ProfileDto;
import kz.nuraskhan.meet_again.magain.profile.dto.PromptDto;
import kz.nuraskhan.meet_again.model.enums.Privilege;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Records;
import org.springframework.stereotype.Component;

/** Reading profiles. */
@Component
public class ProfileQuery {

  private static final int SEARCH_LIMIT = 20;

  private final DSLContext dsl;

  public ProfileQuery(DSLContext dsl) {
    this.dsl = dsl;
  }

  /** The caller's own profile, with what only they may see. */
  public Optional<MyProfileDto> my(Long me) {
    Record row = dsl
        .select(PROFILE.asterisk(), USER_ACCOUNT.EMAIL, USER_ACCOUNT.EMAIL_VERIFIED_AT)
        .from(PROFILE)
        .join(USER_ACCOUNT).on(USER_ACCOUNT.ID.eq(PROFILE.USER_ACCOUNT_ID))
        .where(PROFILE.USER_ACCOUNT_ID.eq(me))
        .fetchOne();

    if (row == null) {
      return Optional.empty();
    }

    Privilege privilege = row.get(PROFILE.PRIVILEGE);

    return Optional.of(new MyProfileDto(
        me,
        row.get(USER_ACCOUNT.EMAIL),
        row.get(USER_ACCOUNT.EMAIL_VERIFIED_AT) != null,
        row.get(PROFILE.DISPLAY_NAME),
        row.get(PROFILE.BIRTH_DATE),
        age(row.get(PROFILE.BIRTH_DATE)),
        row.get(PROFILE.PRONOUNS),
        row.get(PROFILE.CITY),
        row.get(PROFILE.HOMETOWN),
        row.get(PROFILE.OCCUPATION),
        row.get(PROFILE.EDUCATION),
        row.get(PROFILE.LANGUAGES),
        row.get(PROFILE.BIO),
        row.get(PROFILE.AVATAR_URL),
        privilege,
        privilege.maxPhotos,
        photos(me),
        interests(me),
        prompts(me)));
  }

  /** A person as anyone logged in sees them. Empty for a deactivated account too. */
  public Optional<ProfileDto> byId(Long userId) {
    Record row = dsl
        .select(PROFILE.asterisk())
        .from(PROFILE)
        .join(USER_ACCOUNT).on(USER_ACCOUNT.ID.eq(PROFILE.USER_ACCOUNT_ID))
        .where(PROFILE.USER_ACCOUNT_ID.eq(userId))
        .and(USER_ACCOUNT.DEACTIVATED_AT.isNull())
        .fetchOne();

    if (row == null) {
      return Optional.empty();
    }

    return Optional.of(new ProfileDto(
        userId,
        row.get(PROFILE.DISPLAY_NAME),
        age(row.get(PROFILE.BIRTH_DATE)), // the date itself stays with its owner
        row.get(PROFILE.PRONOUNS),
        row.get(PROFILE.CITY),
        row.get(PROFILE.HOMETOWN),
        row.get(PROFILE.OCCUPATION),
        row.get(PROFILE.EDUCATION),
        row.get(PROFILE.LANGUAGES),
        row.get(PROFILE.BIO),
        row.get(PROFILE.AVATAR_URL),
        row.get(PROFILE.PRIVILEGE),
        photos(userId),
        interests(userId),
        prompts(userId)));
  }

  /**
   * People to send a friend request to: by a part of the name, or by the exact e-mail.
   * Never returns the caller.
   */
  public List<ProfileCardDto> search(Long me, String text) {
    String needle = text.trim();
    if (needle.isEmpty()) {
      return List.of();
    }

    return dsl
        .select(PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, PROFILE.CITY, PROFILE.AVATAR_URL)
        .from(PROFILE)
        .join(USER_ACCOUNT).on(USER_ACCOUNT.ID.eq(PROFILE.USER_ACCOUNT_ID))
        .where(USER_ACCOUNT.DEACTIVATED_AT.isNull())
        .and(PROFILE.USER_ACCOUNT_ID.ne(me))
        .and(PROFILE.DISPLAY_NAME.containsIgnoreCase(needle)
            .or(USER_ACCOUNT.EMAIL.eq(needle.toLowerCase(Locale.ROOT))))
        .orderBy(PROFILE.DISPLAY_NAME, PROFILE.USER_ACCOUNT_ID)
        .limit(SEARCH_LIMIT)
        .fetch(Records.mapping(ProfileCardDto::new));
  }

  /** The photos of a person, the main one first. The bytes are not read here - only the addresses. */
  private List<PhotoDto> photos(Long userId) {
    return dsl
        .select(PROFILE_PHOTO.ID, PROFILE_PHOTO.TOKEN)
        .from(PROFILE_PHOTO)
        .where(PROFILE_PHOTO.USER_ACCOUNT_ID.eq(userId))
        .orderBy(PROFILE_PHOTO.POSITION)
        .fetch(row -> new PhotoDto(row.value1(), PhotoService.PUBLIC_PATH + row.value2()));
  }

  private List<String> interests(Long userId) {
    return dsl
        .select(PROFILE_INTEREST.LABEL)
        .from(PROFILE_INTEREST)
        .where(PROFILE_INTEREST.USER_ACCOUNT_ID.eq(userId))
        .orderBy(PROFILE_INTEREST.POSITION)
        .fetch(PROFILE_INTEREST.LABEL);
  }

  private List<PromptDto> prompts(Long userId) {
    return dsl
        .select(PROFILE_PROMPT.QUESTION, PROFILE_PROMPT.ANSWER)
        .from(PROFILE_PROMPT)
        .where(PROFILE_PROMPT.USER_ACCOUNT_ID.eq(userId))
        .orderBy(PROFILE_PROMPT.POSITION)
        .fetch(Records.mapping(PromptDto::new));
  }

  /**
   * Whole years since the date of birth; null without one.
   *
   * <p>"Today" is taken in UTC, the same for every server and every reader. Around a birthday the
   * age can therefore change up to half a day early or late for the person, depending on where
   * they are; a date has no time zone to do better with.
   */
  static Integer age(LocalDate birthDate) {
    return birthDate == null ? null : Period.between(birthDate, LocalDate.now(ZoneOffset.UTC)).getYears();
  }
}
