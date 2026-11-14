package kz.nuraskhan.meet_again;

import static org.assertj.core.api.Assertions.assertThat;

import kz.nuraskhan.meet_again.model.entity.Chat;
import kz.nuraskhan.meet_again.model.entity.ChatMember;
import kz.nuraskhan.meet_again.model.entity.ChatMessage;
import kz.nuraskhan.meet_again.model.entity.Event;
import kz.nuraskhan.meet_again.model.entity.Friendship;
import kz.nuraskhan.meet_again.model.entity.Invitation;
import kz.nuraskhan.meet_again.model.entity.Profile;
import kz.nuraskhan.meet_again.model.entity.UserAccount;
import kz.nuraskhan.meet_again.model.enums.FriendshipStatus;
import kz.nuraskhan.meet_again.model.repository.ChatMemberRepository;
import kz.nuraskhan.meet_again.model.repository.ChatMessageRepository;
import kz.nuraskhan.meet_again.model.repository.ChatRepository;
import kz.nuraskhan.meet_again.model.repository.EventRepository;
import kz.nuraskhan.meet_again.model.repository.FriendshipRepository;
import kz.nuraskhan.meet_again.model.repository.InvitationRepository;
import kz.nuraskhan.meet_again.model.repository.ProfileRepository;
import kz.nuraskhan.meet_again.model.repository.UserAccountRepository;
import kz.nuraskhan.meet_again.model.rnd.RND;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Guard: whatever RND creates must be accepted by the real schema - mandatory columns, check
 * constraints, foreign keys - and must be read back exactly as written.
 */
class RndPersistenceTest extends ApiTestBase {

  @Autowired UserAccountRepository userAccountRepository;
  @Autowired ProfileRepository profileRepository;
  @Autowired FriendshipRepository friendshipRepository;
  @Autowired ChatRepository chatRepository;
  @Autowired ChatMemberRepository chatMemberRepository;
  @Autowired ChatMessageRepository chatMessageRepository;
  @Autowired EventRepository eventRepository;
  @Autowired InvitationRepository invitationRepository;

  @Test
  void everyRndEntity__isSavedAndReadBackUnchanged() {
    UserAccount user1 = RND.userAccount();
    UserAccount user2 = RND.userAccount();

    //
    //
    user1 = userAccountRepository.save(user1);
    user2 = userAccountRepository.save(user2);
    Profile profile = profileRepository.save(RND.profile(user1.id));
    Friendship friendship =
        friendshipRepository.save(RND.friendship(user1.id, user2.id, FriendshipStatus.ACCEPTED));
    Chat chat = chatRepository.save(RND.chat());
    ChatMember member = chatMemberRepository.save(RND.chatMember(chat.id, user1.id));
    ChatMessage message = chatMessageRepository.save(RND.chatMessage(chat.id, user1.id));
    Event event = eventRepository.save(RND.event(user1.id, chat.id));
    Invitation invitation = invitationRepository.save(RND.invitation(event.id, user2.id));
    //
    //

    assertThat(userAccountRepository.findById(user1.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(user1);
    assertThat(profileRepository.findById(user1.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(profile);
    assertThat(friendshipRepository.findById(friendship.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(friendship);
    assertThat(chatRepository.findById(chat.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(chat);
    assertThat(chatMemberRepository.findById(member.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(member);
    assertThat(chatMessageRepository.findById(message.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(message);
    // The moments in particular: startsAt must survive the round trip in any time zone.
    assertThat(eventRepository.findById(event.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(event);
    assertThat(invitationRepository.findById(invitation.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(invitation);

    assertThat(friendshipRepository.countFriendsAmong(user1.id, java.util.List.of(user2.id)))
        .isEqualTo(1);
  }
}
