package kz.nuraskhan.hangelo.calendar;

import static kz.nuraskhan.hangelo.jooq.Tables.AVAILABILITY;
import static kz.nuraskhan.hangelo.jooq.Tables.FRIENDSHIP;
import static kz.nuraskhan.hangelo.jooq.Tables.PROFILE;
import static kz.nuraskhan.hangelo.jooq.Tables.USER_ACCOUNT;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kz.nuraskhan.hangelo.calendar.dto.AvailabilityDto;
import kz.nuraskhan.hangelo.calendar.dto.FriendAvailabilityDto;
import kz.nuraskhan.hangelo.model.enums.FriendshipStatus;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Reading calendars: one's own marks, and what friends let their friends see of theirs. */
@Component
public class AvailabilityQuery {

  /** The most marks one answer carries. */
  private static final int LIMIT = 500;

  private final DSLContext dsl;

  public AvailabilityQuery(DSLContext dsl) {
    this.dsl = dsl;
  }

  /** The caller's own marks that touch the days from..to, earliest first. */
  public List<AvailabilityDto> mine(Long me, LocalDate from, LocalDate to) {
    return dsl
        .select(AVAILABILITY.ID, AVAILABILITY.KIND, AVAILABILITY.FROM_DATE, AVAILABILITY.TO_DATE,
            AVAILABILITY.CITY, AVAILABILITY.NOTE)
        .from(AVAILABILITY)
        .where(AVAILABILITY.USER_ACCOUNT_ID.eq(me))
        .and(AVAILABILITY.DELETED_AT.isNull())
        .and(touches(from, to))
        .orderBy(AVAILABILITY.FROM_DATE, AVAILABILITY.ID)
        .limit(LIMIT)
        .fetch(Records.mapping(AvailabilityDto::new));
  }

  public Optional<AvailabilityDto> one(Long availabilityId) {
    return dsl
        .select(AVAILABILITY.ID, AVAILABILITY.KIND, AVAILABILITY.FROM_DATE, AVAILABILITY.TO_DATE,
            AVAILABILITY.CITY, AVAILABILITY.NOTE)
        .from(AVAILABILITY)
        .where(AVAILABILITY.ID.eq(availabilityId))
        .fetchOptional(Records.mapping(AvailabilityDto::new));
  }

  /**
   * What the caller's friends marked for the days from..to: the kind, the days, the city - not
   * the note. Only of people the caller is friends with now; anyone else's calendar is closed.
   *
   * @param onlyUserId only this friend's marks; null = all friends. Someone who is not a friend
   *                   simply has nothing here.
   */
  public List<FriendAvailabilityDto> ofFriends(Long me, LocalDate from, LocalDate to, Long onlyUserId) {
    // A friendship is one row between two people, whoever asked whom.
    Condition isMyFriend = DSL.exists(DSL
        .selectOne()
        .from(FRIENDSHIP)
        .where(FRIENDSHIP.STATUS.eq(FriendshipStatus.ACCEPTED))
        .and(FRIENDSHIP.REQUESTER__USER_ACCOUNT_ID.eq(me)
            .and(FRIENDSHIP.ADDRESSEE__USER_ACCOUNT_ID.eq(AVAILABILITY.USER_ACCOUNT_ID))
            .or(FRIENDSHIP.ADDRESSEE__USER_ACCOUNT_ID.eq(me)
                .and(FRIENDSHIP.REQUESTER__USER_ACCOUNT_ID.eq(AVAILABILITY.USER_ACCOUNT_ID)))));

    Condition onlyThisOne = onlyUserId == null
        ? DSL.noCondition()
        : AVAILABILITY.USER_ACCOUNT_ID.eq(onlyUserId);

    return dsl
        .select(PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, PROFILE.AVATAR_URL,
            AVAILABILITY.KIND, AVAILABILITY.FROM_DATE, AVAILABILITY.TO_DATE, AVAILABILITY.CITY)
        .from(AVAILABILITY)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(AVAILABILITY.USER_ACCOUNT_ID))
        .join(USER_ACCOUNT).on(USER_ACCOUNT.ID.eq(AVAILABILITY.USER_ACCOUNT_ID))
        .where(AVAILABILITY.DELETED_AT.isNull())
        .and(USER_ACCOUNT.DEACTIVATED_AT.isNull())
        .and(isMyFriend)
        .and(onlyThisOne)
        .and(touches(from, to))
        .orderBy(AVAILABILITY.FROM_DATE, PROFILE.DISPLAY_NAME, AVAILABILITY.ID)
        .limit(LIMIT)
        .fetch(Records.mapping(FriendAvailabilityDto::new));
  }

  /** "The mark has at least one day inside from..to." */
  private static Condition touches(LocalDate from, LocalDate to) {
    return AVAILABILITY.FROM_DATE.le(to).and(AVAILABILITY.TO_DATE.ge(from));
  }
}
