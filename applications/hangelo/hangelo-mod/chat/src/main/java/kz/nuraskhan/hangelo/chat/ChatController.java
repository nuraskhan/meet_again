package kz.nuraskhan.hangelo.chat;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.notification.signal.SignalHub;
import kz.nuraskhan.hangelo.chat.dto.ChatDto;
import kz.nuraskhan.hangelo.chat.dto.ChatMemberDto;
import kz.nuraskhan.hangelo.chat.dto.ChatMessageDto;
import kz.nuraskhan.hangelo.chat.dto.PostMessageRequest;
import kz.nuraskhan.hangelo.web.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

  private static final int MAX_PAGE = 100;

  /** The type of the signal that carries a new chat message; the page listens for this name. */
  static final String SIGNAL_CHAT_MESSAGE = "chat.message";

  private final ChatService chatService;
  private final ChatQuery chatQuery;
  private final SignalHub signalHub;

  public ChatController(ChatService chatService, ChatQuery chatQuery, SignalHub signalHub) {
    this.chatService = chatService;
    this.chatQuery = chatQuery;
    this.signalHub = signalHub;
  }

  @GetMapping("/list")
  public List<ChatDto> list(Principal principal) {
    return chatQuery.myChats(CurrentUser.id(principal));
  }

  @GetMapping("/{chatId}/member/list")
  public List<ChatMemberDto> members(Principal principal, @PathVariable("chatId") Long chatId) {
    chatService.requireMember(CurrentUser.id(principal), chatId);
    return chatQuery.members(chatId);
  }

  /** Newest first; the next page is asked with {@code beforeId} = id of the last message got. */
  @GetMapping("/{chatId}/message/list")
  public List<ChatMessageDto> messages(
      Principal principal,
      @PathVariable("chatId") Long chatId,
      @RequestParam(name = "beforeId", required = false) Long beforeId,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    chatService.requireMember(CurrentUser.id(principal), chatId);
    return chatQuery.messages(chatId, beforeId, Math.clamp(limit, 1, MAX_PAGE));
  }

  /** The caller has seen the chat up to its newest message: its unread count becomes zero. */
  @PostMapping("/{chatId}/read")
  public void markRead(Principal principal, @PathVariable("chatId") Long chatId) {
    chatService.markRead(CurrentUser.id(principal), chatId);
  }

  @PostMapping("/{chatId}/message")
  public ChatMessageDto postMessage(Principal principal,
                                    @PathVariable("chatId") Long chatId,
                                    @Valid @RequestBody PostMessageRequest request) {
    Long me = CurrentUser.id(principal);
    Long messageId = chatService.postMessage(me, chatId, request.text());
    ChatMessageDto message = chatQuery.message(messageId).orElseThrow();

    // The others in the chat get the message itself, pushed, so an open chat shows it at once.
    // The author already has it: it is the answer to this request. A message is not a
    // notification - nothing is stored for it beyond the message.
    Map<String, Object> signal = Map.of("chatId", chatId, "message", message);
    for (ChatMemberDto member : chatQuery.members(chatId)) {
      if (!member.userId().equals(me)) {
        signalHub.send(member.userId(), SIGNAL_CHAT_MESSAGE, signal);
      }
    }

    return message;
  }
}
