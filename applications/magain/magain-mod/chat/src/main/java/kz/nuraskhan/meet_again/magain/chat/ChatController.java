package kz.nuraskhan.meet_again.magain.chat;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import kz.nuraskhan.meet_again.magain.chat.dto.ChatDto;
import kz.nuraskhan.meet_again.magain.chat.dto.ChatMemberDto;
import kz.nuraskhan.meet_again.magain.chat.dto.ChatMessageDto;
import kz.nuraskhan.meet_again.magain.chat.dto.PostMessageRequest;
import kz.nuraskhan.meet_again.web.CurrentUser;
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

  private final ChatService chatService;
  private final ChatQuery chatQuery;

  public ChatController(ChatService chatService, ChatQuery chatQuery) {
    this.chatService = chatService;
    this.chatQuery = chatQuery;
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

  @PostMapping("/{chatId}/message")
  public ChatMessageDto postMessage(Principal principal,
                                    @PathVariable("chatId") Long chatId,
                                    @Valid @RequestBody PostMessageRequest request) {
    Long messageId = chatService.postMessage(CurrentUser.id(principal), chatId, request.text());
    return chatQuery.message(messageId).orElseThrow();
  }
}
