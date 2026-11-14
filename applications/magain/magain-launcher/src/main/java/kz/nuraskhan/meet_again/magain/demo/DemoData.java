package kz.nuraskhan.meet_again.magain.demo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import kz.nuraskhan.meet_again.magain.auth.AuthService;
import kz.nuraskhan.meet_again.magain.auth.dto.RegisterRequest;
import kz.nuraskhan.meet_again.magain.chat.ChatService;
import kz.nuraskhan.meet_again.magain.event.EventService;
import kz.nuraskhan.meet_again.magain.event.dto.CreateEventRequest;
import kz.nuraskhan.meet_again.magain.friend.FriendService;
import kz.nuraskhan.meet_again.model.entity.Event;
import kz.nuraskhan.meet_again.model.entity.Invitation;
import kz.nuraskhan.meet_again.model.entity.UserAccount;
import kz.nuraskhan.meet_again.model.enums.EventType;
import kz.nuraskhan.meet_again.model.repository.EventRepository;
import kz.nuraskhan.meet_again.model.repository.InvitationRepository;
import kz.nuraskhan.meet_again.model.repository.PlaceRepository;
import kz.nuraskhan.meet_again.model.repository.ProfileRepository;
import kz.nuraskhan.meet_again.model.repository.UserAccountRepository;
import kz.nuraskhan.meet_again.model.rnd.RND;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Demo data for the local database: `make data`. Exists only when the server is started with
 * {@code --magain.demo-data=true}; it loads the data and stops the process.
 *
 * <p>Idempotent, in two parts checked separately (the people, the places): what is already there
 * is not touched - so whatever a
 * developer changed by hand survives.
 *
 * <p>What appears (password of every named user: {@value #PASSWORD}):
 * <ul>
 *   <li>alice, bob, carol, dan, eve {@code @demo.test};
 *   <li>alice is friends with bob, carol and dan; eve has sent alice a request;
 *   <li>alice invited bob and carol to a bathhouse; bob accepted, carol has not answered; the chat
 *       of the event has two messages;
 *   <li>ten more people with random names, to have something to search;
 *   <li>twenty invented places around New York City (see {@link SamplePlaces}), so that "suggest
 *       places" has something to suggest.
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "magain.demo-data", havingValue = "true")
public class DemoData implements ApplicationRunner {

  static final String PASSWORD = "demo123";
  private static final String FIRST_EMAIL = "alice@demo.test";

  private static final Logger log = LoggerFactory.getLogger(DemoData.class);

  private final ApplicationContext context;
  private final AuthService authService;
  private final FriendService friendService;
  private final EventService eventService;
  private final ChatService chatService;
  private final UserAccountRepository userAccountRepository;
  private final ProfileRepository profileRepository;
  private final EventRepository eventRepository;
  private final InvitationRepository invitationRepository;
  private final PlaceRepository placeRepository;

  public DemoData(ApplicationContext context,
                  AuthService authService,
                  FriendService friendService,
                  EventService eventService,
                  ChatService chatService,
                  UserAccountRepository userAccountRepository,
                  ProfileRepository profileRepository,
                  EventRepository eventRepository,
                  InvitationRepository invitationRepository,
                  PlaceRepository placeRepository) {
    this.context = context;
    this.authService = authService;
    this.friendService = friendService;
    this.eventService = eventService;
    this.chatService = chatService;
    this.userAccountRepository = userAccountRepository;
    this.profileRepository = profileRepository;
    this.eventRepository = eventRepository;
    this.invitationRepository = invitationRepository;
    this.placeRepository = placeRepository;
  }

  @Override
  public void run(ApplicationArguments args) {
    // Two parts, each checked on its own: a database that got its people before places existed
    // still gets the places.
    if (userAccountRepository.existsByEmail(FIRST_EMAIL)) {
      log.info("Demo people are already there, nothing to do");
    } else {
      load();
      log.info("Demo people loaded. Log in as {} / {}", FIRST_EMAIL, PASSWORD);
    }

    if (placeRepository.existsBySampleTrue()) {
      log.info("Sample places are already there, nothing to do");
    } else {
      placeRepository.saveAll(SamplePlaces.all());
      log.info("Sample places loaded (invented, around New York City)");
    }

    int exitCode = SpringApplication.exit(context);
    System.exit(exitCode);
  }

  private void load() {
    Long alice = register("alice", "Alice");
    Long bob = register("bob", "Bob");
    Long carol = register("carol", "Carol");
    Long dan = register("dan", "Dan");
    Long eve = register("eve", "Eve");

    makeFriends(alice, bob);
    makeFriends(alice, carol);
    makeFriends(dan, alice);
    friendService.sendRequest(eve, alice);

    Instant inFiveDays = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    Long eventId = eventService.create(alice, new CreateEventRequest(
        EventType.BATH, null, "Let's go this week", null, null, null, null,
        List.of(inFiveDays, inFiveDays.plus(1, ChronoUnit.DAYS)), "America/New_York",
        List.of(bob, carol)));

    Event event = eventRepository.findById(eventId).orElseThrow();
    Invitation bobsInvitation =
        invitationRepository.findByEventIdAndInviteeUserAccountId(eventId, bob).orElseThrow();
    eventService.accept(bob, bobsInvitation.id);

    chatService.postMessage(alice, event.chatId, "Who is in?");
    chatService.postMessage(bob, event.chatId, "I am!");

    for (int i = 0; i < 10; i++) {
      UserAccount random = userAccountRepository.save(RND.userAccount());
      profileRepository.save(RND.profile(random.id));
    }
  }

  private Long register(String name, String displayName) {
    return authService.register(new RegisterRequest(name + "@demo.test", PASSWORD, displayName))
        .userId();
  }

  private void makeFriends(Long requester, Long addressee) {
    Long requestId = friendService.sendRequest(requester, addressee);
    friendService.accept(addressee, requestId);
  }
}
