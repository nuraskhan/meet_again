package kz.nuraskhan.meet_again.magain.chat;

import static kz.nuraskhan.meet_again.jooq.Tables.CHAT;
import static kz.nuraskhan.meet_again.jooq.Tables.CHAT_MEMBER;
import static kz.nuraskhan.meet_again.jooq.Tables.CHAT_MESSAGE;
import static kz.nuraskhan.meet_again.jooq.Tables.PROFILE;

import java.util.List;
import java.util.Optional;
import kz.nuraskhan.meet_again.jooq.tables.ChatMember;
import kz.nuraskhan.meet_again.magain.chat.dto.ChatDto;
import kz.nuraskhan.meet_again.magain.chat.dto.ChatMemberDto;
import kz.nuraskhan.meet_again.magain.chat.dto.ChatMessageDto;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Reading chats. None of the methods checks that the caller is a member: the controller does. */
@Component
public class ChatQuery {

  private final DSLContext dsl;

  public ChatQuery(DSLContext dsl) {
    this.dsl = dsl;
  }

  /** Chats the caller is in now, newest first. */
  public List<ChatDto> myChats(Long me) {
    ChatMember mine = CHAT_MEMBER.as("mine");
    ChatMember anyone = CHAT_MEMBER.as("anyone");

    Field<Integer> memberCount = DSL
        .selectCount()
        .from(anyone)
        .where(anyone.CHAT_ID.eq(CHAT.ID))
        .and(anyone.LEFT_AT.isNull())
        .asField("member_count");

    return dsl
        .select(CHAT.ID, CHAT.TITLE, CHAT.CREATED_AT, memberCount)
        .from(CHAT)
        .join(mine).on(mine.CHAT_ID.eq(CHAT.ID))
        .where(mine.USER_ACCOUNT_ID.eq(me))
        .and(mine.LEFT_AT.isNull())
        .orderBy(CHAT.ID.desc())
        .fetch(Records.mapping(ChatDto::new));
  }

  /** People in the chat now. */
  public List<ChatMemberDto> members(Long chatId) {
    return dsl
        .select(PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, PROFILE.AVATAR_URL)
        .from(CHAT_MEMBER)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(CHAT_MEMBER.USER_ACCOUNT_ID))
        .where(CHAT_MEMBER.CHAT_ID.eq(chatId))
        .and(CHAT_MEMBER.LEFT_AT.isNull())
        .orderBy(PROFILE.DISPLAY_NAME, PROFILE.USER_ACCOUNT_ID)
        .fetch(Records.mapping(ChatMemberDto::new));
  }

  /**
   * A page of messages, newest first.
   *
   * @param beforeId only messages older than this one; null = from the newest
   */
  public List<ChatMessageDto> messages(Long chatId, Long beforeId, int limit) {
    Condition older = beforeId == null ? DSL.noCondition() : CHAT_MESSAGE.ID.lt(beforeId);

    return dsl
        .select(CHAT_MESSAGE.ID, CHAT_MESSAGE.AUTHOR__USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME,
            CHAT_MESSAGE.TEXT, CHAT_MESSAGE.CREATED_AT)
        .from(CHAT_MESSAGE)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(CHAT_MESSAGE.AUTHOR__USER_ACCOUNT_ID))
        .where(CHAT_MESSAGE.CHAT_ID.eq(chatId))
        .and(older)
        .orderBy(CHAT_MESSAGE.ID.desc())
        .limit(limit)
        .fetch(Records.mapping(ChatMessageDto::new));
  }

  public Optional<ChatMessageDto> message(Long messageId) {
    return dsl
        .select(CHAT_MESSAGE.ID, CHAT_MESSAGE.AUTHOR__USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME,
            CHAT_MESSAGE.TEXT, CHAT_MESSAGE.CREATED_AT)
        .from(CHAT_MESSAGE)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(CHAT_MESSAGE.AUTHOR__USER_ACCOUNT_ID))
        .where(CHAT_MESSAGE.ID.eq(messageId))
        .fetchOptional(Records.mapping(ChatMessageDto::new));
  }
}
