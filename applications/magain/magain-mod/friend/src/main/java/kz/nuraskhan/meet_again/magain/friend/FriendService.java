package kz.nuraskhan.meet_again.magain.friend;

import java.time.Instant;
import java.util.List;
import kz.nuraskhan.meet_again.magain.notification.NotificationService;
import kz.nuraskhan.meet_again.model.entity.Friendship;
import kz.nuraskhan.meet_again.model.entity.UserAccount;
import kz.nuraskhan.meet_again.model.enums.FriendshipStatus;
import kz.nuraskhan.meet_again.model.enums.NotificationKind;
import kz.nuraskhan.meet_again.model.repository.FriendshipRepository;
import kz.nuraskhan.meet_again.model.repository.UserAccountRepository;
import kz.nuraskhan.meet_again.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {

  private static final List<FriendshipStatus> LIVE =
      List.of(FriendshipStatus.PENDING, FriendshipStatus.ACCEPTED);

  private final FriendshipRepository friendshipRepository;
  private final UserAccountRepository userAccountRepository;
  private final NotificationService notificationService;

  public FriendService(FriendshipRepository friendshipRepository,
                       UserAccountRepository userAccountRepository,
                       NotificationService notificationService) {
    this.friendshipRepository = friendshipRepository;
    this.userAccountRepository = userAccountRepository;
    this.notificationService = notificationService;
  }

  /** @return id of the created request */
  @Transactional
  public Long sendRequest(Long me, Long userId) {
    if (me.equals(userId)) {
      throw ApiException.badRequest("friend_self", "You cannot add yourself");
    }

    UserAccount target = userAccountRepository.findById(userId).orElse(null);
    if (target == null || target.deactivatedAt != null) {
      throw ApiException.notFound("user_not_found", "User not found");
    }

    List<Friendship> live = friendshipRepository.findBetween(me, userId, LIVE);
    if (!live.isEmpty()) {
      boolean friends = live.getFirst().status == FriendshipStatus.ACCEPTED;
      throw friends
          ? ApiException.conflict("already_friends", "You are already friends")
          : ApiException.conflict("request_exists", "A friend request between you already exists");
    }

    Friendship friendship = new Friendship();
    friendship.requesterUserAccountId = me;
    friendship.addresseeUserAccountId = userId;
    friendship.status = FriendshipStatus.PENDING;
    friendship.createdAt = Instant.now();

    Long requestId = friendshipRepository.save(friendship).id;

    notificationService.tell(userId, NotificationKind.FRIEND_REQUEST, me, null);

    return requestId;
  }

  @Transactional
  public void accept(Long me, Long requestId) {
    answer(me, requestId, FriendshipStatus.ACCEPTED);
  }

  @Transactional
  public void decline(Long me, Long requestId) {
    answer(me, requestId, FriendshipStatus.DECLINED);
  }

  /** Ends the friendship. The row stays, with the status REMOVED. */
  @Transactional
  public void remove(Long me, Long userId) {
    List<Friendship> accepted =
        friendshipRepository.findBetween(me, userId, List.of(FriendshipStatus.ACCEPTED));

    if (accepted.isEmpty()) {
      throw ApiException.notFound("friend_not_found", "You are not friends");
    }

    Friendship friendship = accepted.getFirst();
    friendship.status = FriendshipStatus.REMOVED;
    friendship.respondedAt = Instant.now();
  }

  private void answer(Long me, Long requestId, FriendshipStatus answer) {
    Friendship friendship = friendshipRepository.findById(requestId).orElse(null);

    // Only the addressee answers. For anyone else the request "does not exist".
    if (friendship == null || !friendship.addresseeUserAccountId.equals(me)) {
      throw ApiException.notFound("request_not_found", "Friend request not found");
    }
    if (friendship.status != FriendshipStatus.PENDING) {
      throw ApiException.conflict("request_answered", "This friend request is already answered");
    }

    friendship.status = answer;
    // The one who asked learns of a yes. A no is not announced.
    if (answer == FriendshipStatus.ACCEPTED) {
      notificationService.tell(friendship.requesterUserAccountId, NotificationKind.FRIEND_ACCEPTED, me, null);
    }
    friendship.respondedAt = Instant.now();
  }
}
