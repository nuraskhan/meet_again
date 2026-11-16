package kz.nuraskhan.hangelo.chat;

import static kz.nuraskhan.hangelo.jooq.Tables.CHAT;
import static kz.nuraskhan.hangelo.jooq.Tables.EVENT;
import static kz.nuraskhan.hangelo.jooq.Tables.CHAT_MEMBER;
import static kz.nuraskhan.hangelo.jooq.Tables.CHAT_MESSAGE;
import static kz.nuraskhan.hangelo.jooq.Tables.PROFILE;

import java.util.List;
import java.util.Optional;
import kz.nuraskhan.hangelo.jooq.tables.ChatMember;
import kz.nuraskhan.hangelo.jooq.tables.ChatMessage;
import kz.nuraskhan.hangelo.chat.dto.ChatDto;
import kz.nuraskhan.hangelo.chat.dto.ChatMemberDto;
import kz.nuraskhan.hangelo.chat.dto.ChatMessageDto;
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

  /**
   * Chats the caller is in now, the one written in most recently first; a chat nobody has written
   * in stands by the moment it was created. Each carries its newest message and how many messages
   * are new for the caller.
   */
  public List<ChatDto> myChats(Long me) {
    ChatMember mine = CHAT_MEMBER.as("mine");
    ChatMember anyone = CHAT_MEMBER.as("anyone");
    ChatMessage last = CHAT_MESSAGE.as("last");
    ChatMessage newer = CHAT_MESSAGE.as("newer");
    ChatMessage unread = CHAT_MESSAGE.as("unread");

    Field<Integer> memberCount = DSL
        .selectCount()
        .from(anyone)
        .where(anyone.CHAT_ID.eq(CHAT.ID))
        .and(anyone.LEFT_AT.isNull())
        .asField("member_count");

    // New for the caller: written by someone else after the last message the caller has seen.
    Field<Integer> unreadCount = DSL
        .selectCount()
        .from(unread)
        .where(unread.CHAT_ID.eq(CHAT.ID))
        .and(unread.AUTHOR__USER_ACCOUNT_ID.ne(me))
        .and(unread.ID.gt(DSL.coalesce(mine.LAST_READ__CHAT_MESSAGE_ID, 0L)))
        .asField("unread_count");

    return dsl
        .select(CHAT.ID, CHAT.TITLE, CHAT.CREATED_AT, memberCount, EVENT.ID, EVENT.EVENT_TYPE,
            last.ID, last.AUTHOR__USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, last.TEXT, last.CREATED_AT,
            unreadCount)
        .from(CHAT)
        .join(mine).on(mine.CHAT_ID.eq(CHAT.ID))
        // Every chat is created with an event and belongs to exactly one.
        .leftJoin(EVENT).on(EVENT.CHAT_ID.eq(CHAT.ID))
        // The newest message: the one with no newer message in the same chat.
        .leftJoin(last).on(last.CHAT_ID.eq(CHAT.ID))
        .and(DSL.notExists(DSL.selectOne().from(newer)
            .where(newer.CHAT_ID.eq(last.CHAT_ID)).and(newer.ID.gt(last.ID))))
        .leftJoin(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(last.AUTHOR__USER_ACCOUNT_ID))
        .where(mine.USER_ACCOUNT_ID.eq(me))
        .and(mine.LEFT_AT.isNull())
        .orderBy(DSL.coalesce(last.CREATED_AT, CHAT.CREATED_AT).desc(), CHAT.ID.desc())
        .fetch(row -> new ChatDto(
            row.get(CHAT.ID),
            row.get(CHAT.TITLE),
            row.get(CHAT.CREATED_AT),
            row.get(memberCount),
            row.get(EVENT.ID),
            row.get(EVENT.EVENT_TYPE),
            row.get(last.ID) == null ? null : new ChatMessageDto(
                row.get(last.ID),
                row.get(last.AUTHOR__USER_ACCOUNT_ID),
                row.get(PROFILE.DISPLAY_NAME),
                row.get(last.TEXT),
                row.get(last.CREATED_AT),
                null), // a line of the chat list shows the message alone, not what it answers
            row.get(unreadCount)));
  }

  /** People in the chat now. */
  public List<ChatMemberDto> members(Long chatId) {
    return dsl
        .select(PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, PROFILE.TAG, PROFILE.AVATAR_URL)
        .from(CHAT_MEMBER)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(CHAT_MEMBER.USER_ACCOUNT_ID))
        .where(CHAT_MEMBER.CHAT_ID.eq(chatId))
        .and(CHAT_MEMBER.LEFT_AT.isNull())
        .orderBy(PROFILE.DISPLAY_NAME, PROFILE.USER_ACCOUNT_ID)
        .fetch(Records.mapping(ChatMemberDto::new));
  }

  /** How much of an answered message is shown above the answer. */
  private static final int REPLY_PREVIEW_CHARS = 140;

  /**
   * A page of messages, newest first.
   *
   * @param beforeId only messages older than this one; null = from the newest
   */
  public List<ChatMessageDto> messages(Long chatId, Long beforeId, int limit) {
    Condition older = beforeId == null ? DSL.noCondition() : CHAT_MESSAGE.ID.lt(beforeId);

    return selectMessages()
        .where(CHAT_MESSAGE.CHAT_ID.eq(chatId))
        .and(older)
        .orderBy(CHAT_MESSAGE.ID.desc())
        .limit(limit)
        .fetch(ChatQuery::toMessage);
  }

  public Optional<ChatMessageDto> message(Long messageId) {
    return selectMessages()
        .where(CHAT_MESSAGE.ID.eq(messageId))
        .fetchOptional(ChatQuery::toMessage);
  }

  // The answered message and its author, under names of their own: the same two tables are
  // already in the query as the message and its author.
  private static final ChatMessage ANSWERED = CHAT_MESSAGE.as("answered");
  private static final kz.nuraskhan.hangelo.jooq.tables.Profile ANSWERED_AUTHOR = PROFILE.as("answered_author");

  private org.jooq.SelectOnConditionStep<? extends org.jooq.Record> selectMessages() {
    return dsl
        .select(CHAT_MESSAGE.ID, CHAT_MESSAGE.AUTHOR__USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME,
            CHAT_MESSAGE.TEXT, CHAT_MESSAGE.CREATED_AT,
            ANSWERED.ID, ANSWERED.AUTHOR__USER_ACCOUNT_ID, ANSWERED_AUTHOR.DISPLAY_NAME, ANSWERED.TEXT)
        .from(CHAT_MESSAGE)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(CHAT_MESSAGE.AUTHOR__USER_ACCOUNT_ID))
        .leftJoin(ANSWERED).on(ANSWERED.ID.eq(CHAT_MESSAGE.REPLY_TO__CHAT_MESSAGE_ID))
        .leftJoin(ANSWERED_AUTHOR).on(ANSWERED_AUTHOR.USER_ACCOUNT_ID.eq(ANSWERED.AUTHOR__USER_ACCOUNT_ID));
  }

  private static ChatMessageDto toMessage(org.jooq.Record row) {
    ChatMessageDto.ReplyDto replyTo = null;
    if (row.get(ANSWERED.ID) != null) {
      String text = row.get(ANSWERED.TEXT);
      replyTo = new ChatMessageDto.ReplyDto(
          row.get(ANSWERED.ID),
          row.get(ANSWERED.AUTHOR__USER_ACCOUNT_ID),
          row.get(ANSWERED_AUTHOR.DISPLAY_NAME),
          text.length() <= REPLY_PREVIEW_CHARS ? text : text.substring(0, REPLY_PREVIEW_CHARS) + "…");
    }
    return new ChatMessageDto(
        row.get(CHAT_MESSAGE.ID),
        row.get(CHAT_MESSAGE.AUTHOR__USER_ACCOUNT_ID),
        row.get(PROFILE.DISPLAY_NAME),
        row.get(CHAT_MESSAGE.TEXT),
        row.get(CHAT_MESSAGE.CREATED_AT),
        replyTo);
  }
}
