package kz.nuraskhan.meet_again.magain.profile;

import static kz.nuraskhan.meet_again.jooq.Tables.PROFILE;
import static kz.nuraskhan.meet_again.jooq.Tables.USER_ACCOUNT;

import java.util.List;
import java.util.Optional;
import kz.nuraskhan.meet_again.magain.profile.dto.MyProfileDto;
import kz.nuraskhan.meet_again.magain.profile.dto.ProfileDto;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Reading profiles. */
@Component
public class ProfileQuery {

  private static final int SEARCH_LIMIT = 20;

  private final DSLContext dsl;

  public ProfileQuery(DSLContext dsl) {
    this.dsl = dsl;
  }

  public Optional<MyProfileDto> my(Long me) {
    return dsl
        .select(PROFILE.USER_ACCOUNT_ID, USER_ACCOUNT.EMAIL,
            DSL.field(USER_ACCOUNT.EMAIL_VERIFIED_AT.isNotNull()), PROFILE.DISPLAY_NAME, PROFILE.BIO,
            PROFILE.CITY, PROFILE.BIRTH_DATE, PROFILE.AVATAR_URL)
        .from(PROFILE)
        .join(USER_ACCOUNT).on(USER_ACCOUNT.ID.eq(PROFILE.USER_ACCOUNT_ID))
        .where(PROFILE.USER_ACCOUNT_ID.eq(me))
        .fetchOptional(Records.mapping(MyProfileDto::new));
  }

  /** Empty for a deactivated account too. */
  public Optional<ProfileDto> byId(Long userId) {
    return dsl
        .select(PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, PROFILE.BIO, PROFILE.CITY,
            PROFILE.BIRTH_DATE, PROFILE.AVATAR_URL)
        .from(PROFILE)
        .join(USER_ACCOUNT).on(USER_ACCOUNT.ID.eq(PROFILE.USER_ACCOUNT_ID))
        .where(PROFILE.USER_ACCOUNT_ID.eq(userId))
        .and(USER_ACCOUNT.DEACTIVATED_AT.isNull())
        .fetchOptional(Records.mapping(ProfileDto::new));
  }

  /**
   * People to send a friend request to: by a part of the name, or by the exact e-mail.
   * Never returns the caller.
   */
  public List<ProfileDto> search(Long me, String text) {
    String needle = text.trim();
    if (needle.isEmpty()) {
      return List.of();
    }

    return dsl
        .select(PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, PROFILE.BIO, PROFILE.CITY,
            PROFILE.BIRTH_DATE, PROFILE.AVATAR_URL)
        .from(PROFILE)
        .join(USER_ACCOUNT).on(USER_ACCOUNT.ID.eq(PROFILE.USER_ACCOUNT_ID))
        .where(USER_ACCOUNT.DEACTIVATED_AT.isNull())
        .and(PROFILE.USER_ACCOUNT_ID.ne(me))
        .and(PROFILE.DISPLAY_NAME.containsIgnoreCase(needle)
            .or(USER_ACCOUNT.EMAIL.eq(needle.toLowerCase())))
        .orderBy(PROFILE.DISPLAY_NAME, PROFILE.USER_ACCOUNT_ID)
        .limit(SEARCH_LIMIT)
        .fetch(Records.mapping(ProfileDto::new));
  }
}
