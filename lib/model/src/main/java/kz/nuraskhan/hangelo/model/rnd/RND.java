package kz.nuraskhan.hangelo.model.rnd;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;
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
import kz.nuraskhan.hangelo.model.entity.ProfileInterest;
import kz.nuraskhan.hangelo.model.entity.ProfilePhoto;
import kz.nuraskhan.hangelo.model.entity.ProfilePrompt;
import kz.nuraskhan.hangelo.model.entity.UserAccount;
import kz.nuraskhan.hangelo.model.enums.EventStatus;
import kz.nuraskhan.hangelo.model.enums.EventType;
import kz.nuraskhan.hangelo.model.enums.EventVisibility;
import kz.nuraskhan.hangelo.model.enums.FriendshipStatus;
import kz.nuraskhan.hangelo.model.enums.InvitationStatus;
import kz.nuraskhan.hangelo.model.enums.NotificationKind;
import kz.nuraskhan.hangelo.model.enums.Privilege;

/**
 * Random values and entities filled with them, for tests and demo data.
 *
 * <p>An entity from here is valid and ready to save: every mandatory field is filled, every check
 * constraint holds. It has no id, and references to other rows are the arguments - the caller
 * decides what it belongs to. Change afterwards only the fields the test is about:
 *
 * <pre>
 *   UserAccount user = userAccountRepository.save(RND.userAccount());
 *   Profile profile = RND.profile(user.id);
 *   profile.city = "Almaty";
 *   profileRepository.save(profile);
 * </pre>
 */
public final class RND {

  private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
  private static final String LETTERS_AND_DIGITS = LETTERS + "0123456789";

  private RND() {}

  // ------------------------------------------------------------------ values

  /** Lower-case latin letters and digits, starting with a letter. */
  public static String str(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      String alphabet = i == 0 ? LETTERS : LETTERS_AND_DIGITS;
      sb.append(alphabet.charAt(random().nextInt(alphabet.length())));
    }
    return sb.toString();
  }

  /** Both ends included. */
  public static int intBetween(int min, int max) {
    return random().nextInt(min, max + 1);
  }

  public static boolean bool() {
    return random().nextBoolean();
  }

  @SafeVarargs
  public static <T> T oneOf(T... values) {
    return values[random().nextInt(values.length)];
  }

  public static <E extends Enum<E>> E oneOf(Class<E> enumClass) {
    return oneOf(enumClass.getEnumConstants());
  }

  /** Unique for all practical purposes, in lower case. */
  public static String email() {
    return str(16) + "@rnd.test";
  }

  /** A moment within the last year, with the precision the database keeps (microseconds). */
  public static Instant pastInstant() {
    return now().minus(intBetween(1, 365 * 24 * 60), ChronoUnit.MINUTES);
  }

  /** A moment within the next month, with the precision the database keeps (microseconds). */
  public static Instant futureInstant() {
    return now().plus(intBetween(1, 30 * 24 * 60), ChronoUnit.MINUTES);
  }

  /** A date of birth of an adult. */
  public static LocalDate birthDate() {
    return LocalDate.of(intBetween(1960, 2005), intBetween(1, 12), intBetween(1, 28));
  }

  // ---------------------------------------------------------------- entities

  public static UserAccount userAccount() {
    UserAccount x = new UserAccount();
    x.email = email();
    x.passwordHash = "{rnd}" + str(40); // matches no password: nobody logs in as a random user
    x.createdAt = pastInstant();
    return x;
  }

  public static Profile profile(Long userAccountId) {
    Profile x = new Profile();
    x.userAccountId = userAccountId;
    x.displayName = "Name " + str(8);
    x.bio = "Bio " + str(30);
    x.city = "City " + str(6);
    x.birthDate = birthDate();
    x.privilege = oneOf(Privilege.class);
    x.hometown = "Hometown " + str(6);
    x.pronouns = oneOf("she/her", "he/him", "they/them");
    x.occupation = "Job " + str(8);
    x.education = "School " + str(8);
    x.languages = "English, " + str(6);
    x.avatarUrl = "/public/photo/" + str(24); // the address of an uploaded photo, never a link
    x.updatedAt = pastInstant();
    return x;
  }

  public static Friendship friendship(Long requesterUserAccountId, Long addresseeUserAccountId,
                                      FriendshipStatus status) {
    Friendship x = new Friendship();
    x.requesterUserAccountId = requesterUserAccountId;
    x.addresseeUserAccountId = addresseeUserAccountId;
    x.status = status;
    x.createdAt = pastInstant();
    x.respondedAt = status == FriendshipStatus.PENDING ? null : now();
    return x;
  }

  public static Chat chat() {
    Chat x = new Chat();
    x.title = "Chat " + str(10);
    x.createdAt = pastInstant();
    return x;
  }

  public static ChatMember chatMember(Long chatId, Long userAccountId) {
    ChatMember x = new ChatMember();
    x.chatId = chatId;
    x.userAccountId = userAccountId;
    x.joinedAt = pastInstant();
    return x;
  }

  public static ChatMessage chatMessage(Long chatId, Long authorUserAccountId) {
    ChatMessage x = new ChatMessage();
    x.chatId = chatId;
    x.authorUserAccountId = authorUserAccountId;
    x.text = "Message " + str(40);
    x.createdAt = now();
    return x;
  }

  public static Event event(Long creatorUserAccountId, Long chatId) {
    Event x = new Event();
    x.creatorUserAccountId = creatorUserAccountId;
    x.chatId = chatId;
    x.shareToken = str(32);
    x.visibility = oneOf(EventVisibility.class);
    x.timeZone = oneOf("America/New_York", "America/Los_Angeles", "Asia/Almaty");
    x.eventType = oneOf(EventType.class);
    x.title = "Event " + str(10);
    x.description = "Description " + str(30);
    x.distanceMinMi = intBetween(0, 5);
    x.distanceMaxMi = x.distanceMinMi + intBetween(0, 20);
    x.priceMin = intBetween(0, 5000);
    x.priceMax = x.priceMin + intBetween(0, 20000);
    x.status = EventStatus.ACTIVE;
    x.createdAt = pastInstant();
    return x;
  }

  public static EmailVerification emailVerification(Long userAccountId) {
    EmailVerification x = new EmailVerification();
    x.userAccountId = userAccountId;
    x.tokenHash = str(64);
    x.createdAt = now();
    x.expiresAt = futureInstant();
    return x;
  }

  public static EventReminder eventReminder(Long eventId, Long userAccountId) {
    EventReminder x = new EventReminder();
    x.eventId = eventId;
    x.userAccountId = userAccountId;
    x.sentAt = now();
    return x;
  }

  public static EventTimeOption eventTimeOption(Long eventId) {
    EventTimeOption x = new EventTimeOption();
    x.eventId = eventId;
    x.startsAt = futureInstant();
    return x;
  }

  public static Invitation invitation(Long eventId, Long inviteeUserAccountId) {
    Invitation x = new Invitation();
    x.eventId = eventId;
    x.inviteeUserAccountId = inviteeUserAccountId;
    x.status = InvitationStatus.PENDING;
    x.createdAt = pastInstant();
    return x;
  }

  /** Somewhere on the globe, away from the poles and the date line. Not marked as a sample. */
  public static Place place(EventType category) {
    Place x = new Place();
    x.name = "Place " + str(10);
    x.category = category;
    x.description = "About " + str(30);
    x.address = intBetween(1, 999) + " " + str(8) + " St";
    x.latitude = intBetween(-60_000, 60_000) / 1000.0;
    x.longitude = intBetween(-170_000, 170_000) / 1000.0;
    x.rating = intBetween(0, 50) / 10.0;
    x.priceMin = intBetween(0, 3000);
    x.priceMax = x.priceMin + intBetween(0, 10000);
    x.website = "https://rnd.test/place/" + str(10);
    x.phone = "+1 555 " + intBetween(100, 999) + " " + intBetween(1000, 9999);
    x.sample = false;
    x.createdAt = now();
    return x;
  }

  public static EventSuggestion eventSuggestion(Long eventId, Long placeId, int rank) {
    EventSuggestion x = new EventSuggestion();
    x.eventId = eventId;
    x.placeId = placeId;
    x.rank = rank;
    x.averageDistanceMi = intBetween(0, 200) / 10.0;
    x.farthestDistanceMi = x.averageDistanceMi + intBetween(0, 100) / 10.0;
    x.withinEveryonesReach = bool();
    x.fitsBudget = bool();
    x.participantsConsidered = intBetween(1, 8);
    x.createdAt = now();
    return x;
  }

  /** The content is random bytes, not a picture: enough for storing and reading back. */
  public static ProfilePhoto profilePhoto(Long userAccountId, int position) {
    ProfilePhoto x = new ProfilePhoto();
    x.userAccountId = userAccountId;
    x.position = position;
    x.token = str(32);
    x.contentType = oneOf("image/jpeg", "image/png");
    x.content = new byte[intBetween(1, 64)];
    random().nextBytes(x.content);
    x.createdAt = now();
    return x;
  }

  public static ProfileInterest profileInterest(Long userAccountId, int position) {
    ProfileInterest x = new ProfileInterest();
    x.userAccountId = userAccountId;
    x.position = position;
    x.label = "interest " + str(6);
    return x;
  }

  public static ProfilePrompt profilePrompt(Long userAccountId, int position) {
    ProfilePrompt x = new ProfilePrompt();
    x.userAccountId = userAccountId;
    x.position = position;
    x.question = "Question " + str(12) + "?";
    x.answer = "Answer " + str(30);
    return x;
  }

  /** Unread, about no event in particular. */
  public static Notification notification(Long userAccountId, Long actorUserAccountId) {
    Notification x = new Notification();
    x.userAccountId = userAccountId;
    x.kind = oneOf(NotificationKind.class);
    x.actorUserAccountId = actorUserAccountId;
    x.text = "Notification " + str(20);
    x.createdAt = now();
    return x;
  }

  /** With coordinates and a typed place both filled. */
  public static ParticipantPreference participantPreference(Long eventId, Long userAccountId) {
    ParticipantPreference x = new ParticipantPreference();
    x.eventId = eventId;
    x.userAccountId = userAccountId;
    x.latitude = intBetween(-89_000, 89_000) / 1000.0;
    x.longitude = intBetween(-179_000, 179_000) / 1000.0;
    x.locationText = "Place " + str(10);
    x.travelRadiusMi = intBetween(1, 50);
    x.updatedAt = now();
    return x;
  }

  // ------------------------------------------------------------------------

  private static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  private static ThreadLocalRandom random() {
    return ThreadLocalRandom.current();
  }
}
