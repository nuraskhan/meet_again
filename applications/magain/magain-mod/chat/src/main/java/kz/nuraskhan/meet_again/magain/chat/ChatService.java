package kz.nuraskhan.meet_again.magain.chat;

import java.time.Instant;
import java.util.Collection;
import kz.nuraskhan.meet_again.model.entity.Chat;
import kz.nuraskhan.meet_again.model.entity.ChatMember;
import kz.nuraskhan.meet_again.model.entity.ChatMessage;
import kz.nuraskhan.meet_again.model.repository.ChatMemberRepository;
import kz.nuraskhan.meet_again.model.repository.ChatMessageRepository;
import kz.nuraskhan.meet_again.model.repository.ChatRepository;
import kz.nuraskhan.meet_again.web.ApiException;
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
    requireMember(me, chatId);

    ChatMessage message = new ChatMessage();
    message.chatId = chatId;
    message.authorUserAccountId = me;
    message.text = text;
    message.createdAt = Instant.now();

    return chatMessageRepository.save(message).id;
  }

  /** A chat the caller is not in "does not exist" for them. */
  @Transactional(readOnly = true)
  public void requireMember(Long me, Long chatId) {
    ChatMember member = chatMemberRepository.findByChatIdAndUserAccountId(chatId, me).orElse(null);

    if (member == null || member.leftAt != null) {
      throw ApiException.notFound("chat_not_found", "Chat not found");
    }
  }
}
