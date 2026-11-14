package kz.nuraskhan.hangelo.chat;

import java.time.Instant;
import java.util.Collection;
import kz.nuraskhan.hangelo.model.entity.Chat;
import kz.nuraskhan.hangelo.model.entity.ChatMember;
import kz.nuraskhan.hangelo.model.entity.ChatMessage;
import kz.nuraskhan.hangelo.model.repository.ChatMemberRepository;
import kz.nuraskhan.hangelo.model.repository.ChatMessageRepository;
import kz.nuraskhan.hangelo.model.repository.ChatRepository;
import kz.nuraskhan.hangelo.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

  private final ChatRepository chatRepository;
  private final ChatMemberRepository chatMemberRepository;
  private final ChatMessageRepository chatMessageRepository;

  public ChatService(ChatRepository chatRepository,
                     ChatMemberRepository chatMemberRepository,
                     ChatMessageRepository chatMessageRepository) {
    this.chatRepository = chatRepository;
    this.chatMemberRepository = chatMemberRepository;
    this.chatMessageRepository = chatMessageRepository;
  }

  /**
   * Creates a chat with these people in it. Called by other modules (an event creates its chat).
   *
   * @param memberIds without repetitions
   * @return id of the chat
   */
  @Transactional
  public Long createGroupChat(String title, Collection<Long> memberIds) {
    Instant now = Instant.now();

    Chat chat = new Chat();
    chat.title = title;
    chat.createdAt = now;
    chat = chatRepository.save(chat);

    for (Long memberId : memberIds) {
      ChatMember member = new ChatMember();
      member.chatId = chat.id;
      member.userAccountId = memberId;
      member.joinedAt = now;
      chatMemberRepository.save(member);
    }

    return chat.id;
  }

  /** Puts the person into the chat: a new member, or one who left and comes back. */
  @Transactional
  public void join(Long chatId, Long userId) {
    ChatMember member = chatMemberRepository.findByChatIdAndUserAccountId(chatId, userId).orElse(null);

    if (member == null) {
      member = new ChatMember();
      member.chatId = chatId;
      member.userAccountId = userId;
      member.joinedAt = Instant.now();
      // What was written before they came is history, not news: nothing is unread at the door.
      member.lastReadChatMessageId = chatMessageRepository.findNewestId(chatId);
      chatMemberRepository.save(member);
    } else if (member.leftAt != null) {
      member.leftAt = null;
      member.lastReadChatMessageId = chatMessageRepository.findNewestId(chatId);
    }
  }

  /**
   * The caller has seen the chat up to its newest message: nothing in it is unread for them now.
   * Called by the page when the chat is opened and when a message arrives while it is open.
   */
  @Transactional
  public void markRead(Long me, Long chatId) {
    ChatMember member = requireMember(me, chatId);
    member.lastReadChatMessageId = chatMessageRepository.findNewestId(chatId);
  }

  /** Takes the person out of the chat. Does nothing if they are not in it. */
  @Transactional
  public void leave(Long chatId, Long userId) {
    ChatMember member = chatMemberRepository.findByChatIdAndUserAccountId(chatId, userId).orElse(null);

    if (member != null && member.leftAt == null) {
      member.leftAt = Instant.now();
    }
  }

  /** @return id of the message */
  @Transactional
  public Long postMessage(Long me, Long chatId, String text) {
    ChatMember author = requireMember(me, chatId);

    ChatMessage message = new ChatMessage();
    message.chatId = chatId;
    message.authorUserAccountId = me;
    message.text = text;
    message.createdAt = Instant.now();
    message = chatMessageRepository.save(message);

    // Whoever writes has read what stands above their own words.
    author.lastReadChatMessageId = message.id;

    return message.id;
  }

  /**
   * A chat the caller is not in "does not exist" for them.
   *
   * @return the caller's membership
   */
  @Transactional(readOnly = true)
  public ChatMember requireMember(Long me, Long chatId) {
    ChatMember member = chatMemberRepository.findByChatIdAndUserAccountId(chatId, me).orElse(null);

    if (member == null || member.leftAt != null) {
      throw ApiException.notFound("chat_not_found", "Chat not found");
    }
    return member;
  }
}
