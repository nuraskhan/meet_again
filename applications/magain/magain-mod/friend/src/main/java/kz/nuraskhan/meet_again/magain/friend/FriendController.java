package kz.nuraskhan.meet_again.magain.friend;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.meet_again.magain.friend.dto.FriendDto;
import kz.nuraskhan.meet_again.magain.friend.dto.FriendRequestDto;
import kz.nuraskhan.meet_again.magain.friend.dto.SendFriendRequest;
import kz.nuraskhan.meet_again.web.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/friend")
public class FriendController {

  private final FriendService friendService;
  private final FriendQuery friendQuery;

  public FriendController(FriendService friendService, FriendQuery friendQuery) {
    this.friendService = friendService;
    this.friendQuery = friendQuery;
  }

  @GetMapping("/list")
  public List<FriendDto> list(Principal principal) {
    return friendQuery.friends(CurrentUser.id(principal));
  }

  @PostMapping("/{userId}/remove")
  public void remove(Principal principal, @PathVariable("userId") Long userId) {
    friendService.remove(CurrentUser.id(principal), userId);
  }

  @PostMapping("/request")
  public Map<String, Long> sendRequest(Principal principal,
                                       @Valid @RequestBody SendFriendRequest request) {
    Long requestId = friendService.sendRequest(CurrentUser.id(principal), request.userId());
    return Map.of("requestId", requestId);
  }

  @GetMapping("/request/incoming")
  public List<FriendRequestDto> incoming(Principal principal) {
    return friendQuery.incoming(CurrentUser.id(principal));
  }

  @GetMapping("/request/outgoing")
  public List<FriendRequestDto> outgoing(Principal principal) {
    return friendQuery.outgoing(CurrentUser.id(principal));
  }

  @PostMapping("/request/{requestId}/accept")
  public void accept(Principal principal, @PathVariable("requestId") Long requestId) {
    friendService.accept(CurrentUser.id(principal), requestId);
  }

  @PostMapping("/request/{requestId}/decline")
  public void decline(Principal principal, @PathVariable("requestId") Long requestId) {
    friendService.decline(CurrentUser.id(principal), requestId);
  }
}
