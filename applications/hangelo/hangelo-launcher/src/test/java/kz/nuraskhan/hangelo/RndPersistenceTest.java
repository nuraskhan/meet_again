package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import kz.nuraskhan.hangelo.model.entity.ProfileInterest;
import kz.nuraskhan.hangelo.model.entity.ProfilePhoto;
import kz.nuraskhan.hangelo.model.entity.ProfilePrompt;
import kz.nuraskhan.hangelo.model.entity.Chat;
import kz.nuraskhan.hangelo.model.entity.ChatMember;
import kz.nuraskhan.hangelo.model.entity.ChatMessage;
import kz.nuraskhan.hangelo.model.entity.EmailVerification;
import kz.nuraskhan.hangelo.model.entity.Event;
import kz.nuraskhan.hangelo.model.entity.EventReminder;
import kz.nuraskhan.hangelo.model.entity.EventSuggestion;
import kz.nuraskhan.hangelo.model.entity.EventTimeOption;
import kz.nuraskhan.hangelo.model.entity.Friendship;
import kz.nuraskhan.hangelo.model.entity.Invitation;
import kz.nuraskhan.hangelo.model.entity.Notification;
import kz.nuraskhan.hangelo.model.entity.ParticipantPreference;
import kz.nuraskhan.hangelo.model.entity.Place;
import kz.nuraskhan.hangelo.model.entity.Profile;
import kz.nuraskhan.hangelo.model.entity.UserAccount;
import kz.nuraskhan.hangelo.model.enums.EventType;
import kz.nuraskhan.hangelo.model.enums.FriendshipStatus;
import kz.nuraskhan.hangelo.model.repository.ProfileInterestRepository;
import kz.nuraskhan.hangelo.model.repository.ProfilePhotoRepository;
import kz.nuraskhan.hangelo.model.repository.ProfilePromptRepository;
import kz.nuraskhan.hangelo.model.repository.ChatMemberRepository;
import kz.nuraskhan.hangelo.model.repository.ChatMessageRepository;
import kz.nuraskhan.hangelo.model.repository.ChatRepository;
import kz.nuraskhan.hangelo.model.repository.EmailVerificationRepository;
import kz.nuraskhan.hangelo.model.repository.EventReminderRepository;
import kz.nuraskhan.hangelo.model.repository.EventRepository;
import kz.nuraskhan.hangelo.model.repository.EventSuggestionRepository;
import kz.nuraskhan.hangelo.model.repository.EventTimeOptionRepository;
import kz.nuraskhan.hangelo.model.repository.FriendshipRepository;
import kz.nuraskhan.hangelo.model.repository.InvitationRepository;
import kz.nuraskhan.hangelo.model.repository.NotificationRepository;
import kz.nuraskhan.hangelo.model.repository.ParticipantPreferenceRepository;
import kz.nuraskhan.hangelo.model.repository.PlaceRepository;
import kz.nuraskhan.hangelo.model.repository.ProfileRepository;
import kz.nuraskhan.hangelo.model.repository.UserAccountRepository;
import kz.nuraskhan.hangelo.model.rnd.RND;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Guard: whatever RND creates must be accepted by the real schema - mandatory columns, check
 * constraints, foreign keys - and must be read back exactly as written.
 */
class RndPersistenceTest extends ApiTestBase {

  @Autowired UserAccountRepository userAccountRepository;
  @Autowired ProfileRepository profileRepository;
  @Autowired ProfilePhotoRepository profilePhotoRepository;
  @Autowired ProfileInterestRepository profileInterestRepository;
  @Autowired ProfilePromptRepository profilePromptRepository;
  @Autowired FriendshipRepository friendshipRepository;
  @Autowired ChatRepository chatRepository;
  @Autowired ChatMemberRepository chatMemberRepository;
  @Autowired ChatMessageRepository chatMessageRepository;
  @Autowired EventRepository eventRepository;
  @Autowired EmailVerificationRepository emailVerificationRepository;
  @Autowired EventReminderRepository eventReminderRepository;
  @Autowired EventTimeOptionRepository eventTimeOptionRepository;
  @Autowired InvitationRepository invitationRepository;
  @Autowired PlaceRepository placeRepository;
  @Autowired EventSuggestionRepository eventSuggestionRepository;
  @Autowired ParticipantPreferenceRepository participantPreferenceRepository;
  @Autowired NotificationRepository notificationRepository;

  @Test
  void everyRndEntity__isSavedAndReadBackUnchanged() {
    UserAccount user1 = RND.userAccount();
    UserAccount user2 = RND.userAccount();

    //
    //
    user1 = userAccountRepository.save(user1);
    user2 = userAccountRepository.save(user2);
    Profile profile = profileRepository.save(RND.profile(user1.id));
    ProfilePhoto photo = profilePhotoRepository.save(RND.profilePhoto(user1.id, 1));
    ProfileInterest interest = profileInterestRepository.save(RND.profileInterest(user1.id, 1));
    ProfilePrompt prompt = profilePromptRepository.save(RND.profilePrompt(user1.id, 1));
    Friendship friendship =
        friendshipRepository.save(RND.friendship(user1.id, user2.id, FriendshipStatus.ACCEPTED));
    Chat chat = chatRepository.save(RND.chat());
    ChatMember member = chatMemberRepository.save(RND.chatMember(chat.id, user1.id));
    ChatMessage message = chatMessageRepository.save(RND.chatMessage(chat.id, user1.id));
    Event event = eventRepository.save(RND.event(user1.id, chat.id));
    EventTimeOption timeOption = eventTimeOptionRepository.save(RND.eventTimeOption(event.id));
    EmailVerification verification =
        emailVerificationRepository.save(RND.emailVerification(user1.id));
    EventReminder reminder = eventReminderRepository.save(RND.eventReminder(event.id, user1.id));
    Invitation invitation = invitationRepository.save(RND.invitation(event.id, user2.id));
    Place place = placeRepository.save(RND.place(EventType.COFFEE));
    EventSuggestion suggestion =
        eventSuggestionRepository.save(RND.eventSuggestion(event.id, place.id, 1));
    ParticipantPreference preference =
        participantPreferenceRepository.save(RND.participantPreference(event.id, user2.id));
    Notification notification = notificationRepository.save(RND.notification(user1.id, user2.id));
    //
    //

    assertThat(userAccountRepository.findById(user1.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(user1);
    assertThat(profileRepository.findById(user1.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(profile);
    assertThat(profilePhotoRepository.findById(photo.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(photo);
    assertThat(profileInterestRepository.findById(interest.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(interest);
    assertThat(profilePromptRepository.findById(prompt.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(prompt);
    assertThat(friendshipRepository.findById(friendship.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(friendship);
    assertThat(chatRepository.findById(chat.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(chat);
    assertThat(chatMemberRepository.findById(member.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(member);
    assertThat(chatMessageRepository.findById(message.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(message);
    assertThat(eventRepository.findById(event.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(event);
    // The moment in particular: it must survive the round trip in any time zone.
    assertThat(eventTimeOptionRepository.findById(timeOption.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(timeOption);
    assertThat(emailVerificationRepository.findById(verification.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(verification);
    assertThat(eventReminderRepository.findById(reminder.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(reminder);
    assertThat(invitationRepository.findById(invitation.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(invitation);
    assertThat(placeRepository.findById(place.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(place);
    assertThat(eventSuggestionRepository.findById(suggestion.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(suggestion);
    assertThat(participantPreferenceRepository.findById(preference.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(preference);
    assertThat(notificationRepository.findById(notification.id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(notification);

    assertThat(friendshipRepository.countFriendsAmong(user1.id, java.util.List.of(user2.id)))
        .isEqualTo(1);
  }
}
