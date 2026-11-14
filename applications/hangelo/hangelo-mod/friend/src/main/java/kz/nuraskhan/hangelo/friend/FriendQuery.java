package kz.nuraskhan.hangelo.friend;

import static kz.nuraskhan.hangelo.jooq.Tables.FRIENDSHIP;
import static kz.nuraskhan.hangelo.jooq.Tables.PROFILE;

import java.util.List;
import kz.nuraskhan.hangelo.friend.dto.FriendDto;
import kz.nuraskhan.hangelo.friend.dto.FriendRequestDto;
import kz.nuraskhan.hangelo.model.enums.FriendshipStatus;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Reading friends and friend requests. */
@Component
public class FriendQuery {

  private final DSLContext dsl;

  public FriendQuery(DSLContext dsl) {
    this.dsl = dsl;
  }

  public List<FriendDto> friends(Long me) {
    // A friendship is one row, and the caller can be on either side of it.
    Field<Long> friendId = DSL
        .when(FRIENDSHIP.REQUESTER__USER_ACCOUNT_ID.eq(me), FRIENDSHIP.ADDRESSEE__USER_ACCOUNT_ID)
        .otherwise(FRIENDSHIP.REQUESTER__USER_ACCOUNT_ID);

    return dsl
        .select(PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, PROFILE.CITY, PROFILE.AVATAR_URL,
            FRIENDSHIP.RESPONDED_AT)
        .from(FRIENDSHIP)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(friendId))
        .where(FRIENDSHIP.STATUS.eq(FriendshipStatus.ACCEPTED))
        .and(FRIENDSHIP.REQUESTER__USER_ACCOUNT_ID.eq(me)
            .or(FRIENDSHIP.ADDRESSEE__USER_ACCOUNT_ID.eq(me)))
        .orderBy(PROFILE.DISPLAY_NAME, PROFILE.USER_ACCOUNT_ID)
        .fetch(Records.mapping(FriendDto::new));
  }

  /** Requests sent to the caller and not answered yet, newest first. */
  public List<FriendRequestDto> incoming(Long me) {
    return dsl
        .select(FRIENDSHIP.ID, PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, PROFILE.AVATAR_URL,
            FRIENDSHIP.CREATED_AT)
        .from(FRIENDSHIP)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(FRIENDSHIP.REQUESTER__USER_ACCOUNT_ID))
        .where(FRIENDSHIP.STATUS.eq(FriendshipStatus.PENDING))
        .and(FRIENDSHIP.ADDRESSEE__USER_ACCOUNT_ID.eq(me))
        .orderBy(FRIENDSHIP.ID.desc())
        .fetch(Records.mapping(FriendRequestDto::new));
  }

  /** Requests the caller sent that are not answered yet, newest first. */
  public List<FriendRequestDto> outgoing(Long me) {
    return dsl
        .select(FRIENDSHIP.ID, PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, PROFILE.AVATAR_URL,
            FRIENDSHIP.CREATED_AT)
        .from(FRIENDSHIP)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(FRIENDSHIP.ADDRESSEE__USER_ACCOUNT_ID))
        .where(FRIENDSHIP.STATUS.eq(FriendshipStatus.PENDING))
        .and(FRIENDSHIP.REQUESTER__USER_ACCOUNT_ID.eq(me))
        .orderBy(FRIENDSHIP.ID.desc())
        .fetch(Records.mapping(FriendRequestDto::new));
  }
}
