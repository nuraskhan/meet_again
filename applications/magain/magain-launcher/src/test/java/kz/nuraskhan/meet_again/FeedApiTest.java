package kz.nuraskhan.meet_again;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import kz.nuraskhan.meet_again.magain.event.expiry.EventExpiryJob;
import kz.nuraskhan.meet_again.model.entity.UserAccount;
import kz.nuraskhan.meet_again.model.enums.InvitationStatus;
import kz.nuraskhan.meet_again.model.repository.InvitationRepository;
import kz.nuraskhan.meet_again.model.repository.ProfileRepository;
import kz.nuraskhan.meet_again.model.repository.UserAccountRepository;
import kz.nuraskhan.meet_again.model.rnd.RND;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * Public events: the feed, joining from it, leaving, and the creator removing a guest.
 *
 * <p>The feed is one for everybody, so it holds the public events of other tests too. Every test
 * looks for its own events in it by id.
 */
class FeedApiTest extends ApiTestBase {

  @Autowired UserAccountRepository userAccountRepository;
  @Autowired ProfileRepository profileRepository;
  @Autowired InvitationRepository invitationRepository;
  @Autowired EventExpiryJob expiryJob;

  // ------------------------------------------------------------------ the feed

  @Test
  void feed__showsAPublicEventToAStranger_withWhoInvites() {
    TestUser creator = register();
    TestUser stranger = register();
    post("/profile/me", creator, Map.of("displayName", creator.displayName(), "city", "Queens"));

    //
    //
    long eventId = post("/event/create", creator, Map.of(
        "eventType", "BOARD_GAMES",
        "title", "Open game night",
        "description", "Everybody welcome",
        "visibility", "PUBLIC",
        "proposedTimes", List.of("2031-03-01T23:00:00Z"))).body().get("id").asLong();
    //
    //

    JsonNode item = feedItem(stranger, eventId);
    assertThat(item.get("title").asString()).isEqualTo("Open game night");
    assertThat(item.get("description").asString()).isEqualTo("Everybody welcome");
    assertThat(item.get("eventType").asString()).isEqualTo("BOARD_GAMES");
    assertThat(item.get("proposedTimes").get(0).asString()).isEqualTo("2031-03-01T23:00:00Z");
    assertThat(item.get("creatorId").asLong()).isEqualTo(creator.id());
    assertThat(item.get("creatorName").asString()).isEqualTo(creator.displayName());
    assertThat(item.get("creatorCity").asString()).isEqualTo("Queens");
    assertThat(item.get("goingCount").asInt()).isZero();
    assertThat(item.get("full").asBoolean()).isFalse();
    assertThat(item.get("myStanding").asString()).isEqualTo("NONE");
    // Not for someone who has not joined: the way into the chat and the link.
    assertThat(item.has("chatId")).isFalse();
    assertThat(item.has("shareToken")).isFalse();

    assertThat(feedItem(creator, eventId).get("myStanding").asString()).isEqualTo("CREATOR");
    // The inviter's profile opens for anyone logged in.
    assertThat(get("/profile/" + creator.id(), stranger).status()).isEqualTo(200);
  }

  @Test
  void feed__neverShowsAPrivateEvent_whichIsWhatAnEventIsUnlessSaidOtherwise() {
    TestUser creator = register();
    TestUser stranger = register();

    //
    //
    long saidPrivate = post("/event/create", creator,
        Map.of("eventType", "WALK", "visibility", "PRIVATE")).body().get("id").asLong();
    long saidNothing = post("/event/create", creator, Map.of("eventType", "WALK")).body().get("id").asLong();
    //
    //

    assertThat(feedIds(stranger, "")).doesNotContain(saidPrivate, saidNothing);
    assertThat(feedIds(creator, "")).doesNotContain(saidPrivate, saidNothing);
    assertThat(get("/event/" + saidNothing, creator).body().get("visibility").asString()).isEqualTo("PRIVATE");
  }

  @Test
  void feed__dropsAnEventThatIsCancelledOrOver() {
    TestUser creator = register();
    TestUser stranger = register();
    java.time.Instant now = java.time.Instant.now();
    long cancelled = publicEvent(creator);
    long over = post("/event/create", creator, Map.of(
        "eventType", "WALK", "visibility", "PUBLIC",
        "proposedTimes", List.of(now.plus(1, java.time.temporal.ChronoUnit.HOURS).toString())))
        .body().get("id").asLong();
    long stillOn = publicEvent(creator);

    //
    //
    post("/event/" + cancelled + "/cancel", creator);
    expiryJob.expireDue(now.plus(5, java.time.temporal.ChronoUnit.HOURS));
    //
    //

    assertThat(feedIds(stranger, "")).contains(stillOn).doesNotContain(cancelled, over);
  }

  @Test
  void feed__filtersByType_andPagesFromNewestToOlder() {
    TestUser creator = register();
    TestUser reader = register();
    long first = post("/event/create", creator, Map.of("eventType", "CINEMA", "visibility", "PUBLIC")).body().get("id").asLong();
    long second = post("/event/create", creator, Map.of("eventType", "BAR", "visibility", "PUBLIC")).body().get("id").asLong();
    long third = post("/event/create", creator, Map.of("eventType", "CINEMA", "visibility", "PUBLIC")).body().get("id").asLong();

    //
    //
    List<Long> newestTwo = feedIds(reader, "?limit=2");
    List<Long> afterThem = feedIds(reader, "?limit=1&beforeId=" + second);
    List<Long> cinemaOnly = feedIds(reader, "?eventType=CINEMA&limit=50");
    //
    //

    assertThat(newestTwo).containsExactly(third, second);
    assertThat(afterThem).containsExactly(first);
    assertThat(cinemaOnly).contains(third, first).doesNotContain(second);
    assertThat(cinemaOnly.indexOf(third)).isLessThan(cinemaOnly.indexOf(first));
  }

  @Test
  void feed__needsToken() {
    //
    //
    Response response = get("/feed/list", null);
    //
    //

    assertThat(response.status()).isEqualTo(401);
  }

  // ------------------------------------------------------------------ joining

  @Test
  void join__makesAStrangerAGuestWhoIsComing_andPutsThemIntoTheChat() {
    TestUser creator = register();
    TestUser stranger = register();
    long eventId = publicEvent(creator);

    //
    //
    Response response = post("/event/" + eventId + "/join", stranger);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    long chatId = response.body().get("chatId").asLong();
    assertThat(response.body().get("visibility").asString()).isEqualTo("PUBLIC");
    assertThat(response.body().get("invitees").get(0).get("userId").asLong()).isEqualTo(stranger.id());
    assertThat(response.body().get("invitees").get(0).get("status").asString()).isEqualTo("ACCEPTED");

    // In the group chat with the creator, and able to write there.
    assertThat(longs(get("/chat/list", stranger).body(), "id")).containsExactly(chatId);
    assertThat(post("/chat/" + chatId + "/message", stranger, Map.of("text", "Hi all")).status()).isEqualTo(200);
    assertThat(get("/chat/" + chatId + "/message/list", creator).body().get(0).get("text").asString())
        .isEqualTo("Hi all");

    // The feed shows a face and where the reader stands.
    JsonNode item = feedItem(stranger, eventId);
    assertThat(item.get("myStanding").asString()).isEqualTo("ACCEPTED");
    assertThat(item.get("goingCount").asInt()).isEqualTo(1);
    assertThat(item.get("going").get(0).get("userId").asLong()).isEqualTo(stranger.id());
    assertThat(item.get("going").get(0).get("displayName").asString()).isEqualTo(stranger.displayName());
  }

  @Test
  void join__twice_changesNothing() {
    TestUser creator = register();
    TestUser guest = register();
    long eventId = publicEvent(creator);
    post("/event/" + eventId + "/join", guest);

    //
    //
    Response response = post("/event/" + eventId + "/join", guest);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("invitees").size()).isEqualTo(1);
    assertThat(feedItem(creator, eventId).get("goingCount").asInt()).isEqualTo(1);
  }

  @Test
  void join__ofAPrivateEventByItsId_answersNotFound() {
    TestUser creator = register();
    TestUser stranger = register();
    long privateEvent = post("/event/create", creator, Map.of("eventType", "WALK")).body().get("id").asLong();

    //
    //
    Response byId = post("/event/" + privateEvent + "/join", stranger);
    Response unknown = post("/event/" + Long.MAX_VALUE + "/join", stranger);
    Response withoutToken = post("/event/" + publicEvent(creator) + "/join", null);
    //
    //

    assertThat(byId.status()).isEqualTo(404);
    assertThat(unknown.status()).isEqualTo(404);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(get("/event/list", stranger).body().size()).isZero();
  }

  @Test
  void join__isRefusedWhenTheEventIsFull() {
    TestUser creator = register();
    TestUser late = register();
    long eventId = publicEvent(creator);
    // Fifty guests already: put there directly, registering fifty people would take a while.
    for (int i = 0; i < 50; i++) {
      UserAccount guest = userAccountRepository.save(RND.userAccount());
      profileRepository.save(RND.profile(guest.id));
      var place = RND.invitation(eventId, guest.id);
      place.status = InvitationStatus.ACCEPTED;
      invitationRepository.save(place);
    }

    //
    //
    Response response = post("/event/" + eventId + "/join", late);
    //
    //

    assertThat(response.status()).isEqualTo(409);
    assertThat(response.body().get("code").asString()).isEqualTo("event_full");
    JsonNode item = feedItem(late, eventId);
    assertThat(item.get("full").asBoolean()).isTrue();
    assertThat(item.get("goingCount").asInt()).isEqualTo(50);
    assertThat(item.get("going").size()).isEqualTo(5); // faces, not the whole crowd
  }

  // ------------------------------------------------------------------ leaving

  @Test
  void leave__takesTheGuestOutOfTheEventAndTheChat_andTheyMayComeBack() {
    TestUser creator = register();
    TestUser guest = register();
    long eventId = publicEvent(creator);
    long chatId = post("/event/" + eventId + "/join", guest).body().get("chatId").asLong();

    //
    //
    Response response = post("/event/" + eventId + "/leave", guest);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(get("/chat/list", guest).body().size()).isZero();
    assertThat(get("/chat/" + chatId + "/message/list", guest).status()).isEqualTo(404);
    assertThat(feedItem(guest, eventId).get("myStanding").asString()).isEqualTo("DECLINED");
    assertThat(feedItem(creator, eventId).get("goingCount").asInt()).isZero();

    assertThat(post("/event/" + eventId + "/join", guest).status()).isEqualTo(200);
    assertThat(longs(get("/chat/list", guest).body(), "id")).containsExactly(chatId);
  }

  @Test
  void leave__isOnlyForAGuestWhoIsComing() {
    TestUser creator = register();
    TestUser stranger = register();
    long eventId = publicEvent(creator);

    //
    //
    Response byCreator = post("/event/" + eventId + "/leave", creator);
    Response byStranger = post("/event/" + eventId + "/leave", stranger);
    //
    //

    assertThat(byCreator.status()).isEqualTo(404);
    assertThat(byStranger.status()).isEqualTo(404);
  }

  // ------------------------------------------------------------------ removing a guest

  @Test
  void removeGuest__takesThemOut_andNeitherTheFeedNorTheLinkLetsThemBack() {
    TestUser creator = register();
    TestUser unwanted = register();
    JsonNode created = post("/event/create", creator, Map.of("eventType", "BAR", "visibility", "PUBLIC")).body();
    long eventId = created.get("id").asLong();
    long chatId = post("/event/" + eventId + "/join", unwanted).body().get("chatId").asLong();

    //
    //
    Response response = post("/event/" + eventId + "/guest/" + unwanted.id() + "/remove", creator);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("invitees").get(0).get("status").asString()).isEqualTo("REMOVED");

    // Out of the chat, and the event is no longer theirs to open.
    assertThat(get("/chat/" + chatId + "/message/list", unwanted).status()).isEqualTo(404);
    assertThat(get("/event/" + eventId, unwanted).status()).isEqualTo(404);
    assertThat(get("/event/list", unwanted).body().size()).isZero();

    Response fromTheFeed = post("/event/" + eventId + "/join", unwanted);
    Response byTheLink = post("/event/join/" + created.get("shareToken").asString(), unwanted);
    assertThat(fromTheFeed.status()).isEqualTo(409);
    assertThat(fromTheFeed.body().get("code").asString()).isEqualTo("removed_from_event");
    assertThat(byTheLink.status()).isEqualTo(409);

    assertThat(feedItem(unwanted, eventId).get("myStanding").asString()).isEqualTo("REMOVED");
    assertThat(feedItem(creator, eventId).get("goingCount").asInt()).isZero();
  }

  @Test
  void removeGuest__isOnlyForTheCreator_andOnlyOfSomeoneWhoIsAGuest() {
    TestUser creator = register();
    TestUser guest = register();
    TestUser otherGuest = register();
    TestUser stranger = register();
    long eventId = publicEvent(creator);
    post("/event/" + eventId + "/join", guest);
    post("/event/" + eventId + "/join", otherGuest);

    //
    //
    Response byAnotherGuest = post("/event/" + eventId + "/guest/" + guest.id() + "/remove", otherGuest);
    Response ofAStranger = post("/event/" + eventId + "/guest/" + stranger.id() + "/remove", creator);
    Response ofOneself = post("/event/" + eventId + "/guest/" + creator.id() + "/remove", creator);
    //
    //

    assertThat(byAnotherGuest.status()).isEqualTo(404);
    assertThat(ofAStranger.status()).isEqualTo(404);
    assertThat(ofOneself.status()).isEqualTo(404);
    assertThat(feedItem(creator, eventId).get("goingCount").asInt()).isEqualTo(2);
  }

  @Test
  void aRemovedGuest__hasNoSayInTheEventAnyMore() {
    TestUser creator = register();
    TestUser removed = register();
    long eventId = publicEvent(creator);
    post("/event/" + eventId + "/join", removed);
    post("/event/" + eventId + "/guest/" + removed.id() + "/remove", creator);

    //
    //
    Response preference = post("/event/" + eventId + "/preference", removed,
        Map.of("locationText", "Somewhere", "travelRadiusMi", 5));
    Response suggestions = post("/event/" + eventId + "/suggestion/refresh", removed);
    Response leave = post("/event/" + eventId + "/leave", removed);
    //
    //

    assertThat(preference.status()).isEqualTo(409);
    assertThat(suggestions.status()).isEqualTo(404);
    assertThat(leave.status()).isEqualTo(404);
  }

  // ------------------------------------------------------------------ helpers

  private long publicEvent(TestUser creator) {
    Response response = post("/event/create", creator, Map.of("eventType", "WALK", "visibility", "PUBLIC"));
    assertThat(response.status()).isEqualTo(200);
    return response.body().get("id").asLong();
  }

  /** The ids of a page of the feed, in its order. {@code query} is "" or starts with "?". */
  private List<Long> feedIds(TestUser reader, String query) {
    Response response = get("/feed/list" + (query.isEmpty() ? "?limit=50" : query), reader);
    assertThat(response.status()).isEqualTo(200);
    return longs(response.body(), "id");
  }

  /** The card of one event in the feed as this reader gets it. */
  private JsonNode feedItem(TestUser reader, long eventId) {
    return get("/feed/list?limit=50", reader).body().valueStream()
        .filter(item -> item.get("id").asLong() == eventId)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Event " + eventId + " is not in the feed"));
  }
}
