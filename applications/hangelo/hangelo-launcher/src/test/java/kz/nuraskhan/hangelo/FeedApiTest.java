package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.event.expiry.EventExpiryJob;
import kz.nuraskhan.hangelo.model.entity.UserAccount;
import kz.nuraskhan.hangelo.model.enums.InvitationStatus;
import kz.nuraskhan.hangelo.model.repository.InvitationRepository;
import kz.nuraskhan.hangelo.model.repository.ProfileRepository;
import kz.nuraskhan.hangelo.model.repository.UserAccountRepository;
import kz.nuraskhan.hangelo.model.rnd.RND;
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
  void join__asksTheCreator_andTheStrangerIsNotInYet() {
    TestUser creator = register();
    TestUser stranger = register();
    long eventId = publicEvent(creator);
    long chatId = get("/event/" + eventId, creator).body().get("chatId").asLong();

    //
    //
    Response response = post("/event/" + eventId + "/join", stranger);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("standing").asString()).isEqualTo("REQUESTED");

    // Not a guest yet: no chat, and the event is not theirs to open.
    assertThat(get("/chat/list", stranger).body().size()).isZero();
    assertThat(get("/chat/" + chatId + "/message/list", stranger).status()).isEqualTo(404);
    assertThat(get("/event/" + eventId, stranger).status()).isEqualTo(404);
    assertThat(get("/event/list", stranger).body().size()).isZero();

    // The feed says the request is waiting, and counts nobody as coming.
    JsonNode item = feedItem(stranger, eventId);
    assertThat(item.get("myStanding").asString()).isEqualTo("REQUESTED");
    assertThat(item.get("goingCount").asInt()).isZero();
    assertThat(item.get("going").size()).isZero();

    // The creator sees who is asking.
    JsonNode asking = get("/event/" + eventId, creator).body().get("invitees");
    assertThat(asking.size()).isEqualTo(1);
    assertThat(asking.get(0).get("userId").asLong()).isEqualTo(stranger.id());
    assertThat(asking.get(0).get("status").asString()).isEqualTo("REQUESTED");
  }

  @Test
  void join__twice_isOneRequest() {
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
    assertThat(response.body().get("standing").asString()).isEqualTo("REQUESTED");
    assertThat(get("/event/" + eventId, creator).body().get("invitees").size()).isEqualTo(1);
    // The creator was told once, not twice.
    assertThat(get("/notification/list", creator).body().valueStream()
        .filter(n -> n.get("kind").asString().equals("JOIN_REQUESTED")).count()).isEqualTo(1);
  }

  @Test
  void join__ofSomeoneInvitedByName_isTheirYes_withoutAsking() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);
    long eventId = post("/event/create", creator, Map.of(
        "eventType", "WALK", "visibility", "PUBLIC", "inviteeIds", List.of(friend.id())))
        .body().get("id").asLong();

    //
    //
    Response response = post("/event/" + eventId + "/join", friend);
    //
    //

    assertThat(response.body().get("standing").asString()).isEqualTo("ACCEPTED");
    assertThat(get("/chat/list", friend).body().size()).isEqualTo(1);
    assertThat(feedItem(friend, eventId).get("goingCount").asInt()).isEqualTo(1);
  }

  @Test
  void approveRequest__makesTheStrangerAGuestWhoIsComing_andPutsThemIntoTheChat() {
    TestUser creator = register();
    TestUser stranger = register();
    long eventId = publicEvent(creator);
    post("/event/" + eventId + "/join", stranger);

    //
    //
    Response response = post("/event/" + eventId + "/request/" + stranger.id() + "/approve", creator);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    long chatId = response.body().get("chatId").asLong();
    assertThat(response.body().get("invitees").get(0).get("userId").asLong()).isEqualTo(stranger.id());
    assertThat(response.body().get("invitees").get(0).get("status").asString()).isEqualTo("ACCEPTED");
    assertThat(response.body().get("invitees").get(0).get("respondedAt").isNull()).isFalse();

    // In the group chat with the creator, and able to write there.
    assertThat(longs(get("/chat/list", stranger).body(), "id")).containsExactly(chatId);
    assertThat(post("/chat/" + chatId + "/message", stranger, Map.of("text", "Hi all")).status()).isEqualTo(200);
    assertThat(get("/chat/" + chatId + "/message/list", creator).body().get(0).get("text").asString())
        .isEqualTo("Hi all");

    // The event is theirs to open now.
    assertThat(get("/event/" + eventId, stranger).status()).isEqualTo(200);

    // The feed shows a face and where the reader stands.
    JsonNode item = feedItem(stranger, eventId);
    assertThat(item.get("myStanding").asString()).isEqualTo("ACCEPTED");
    assertThat(item.get("goingCount").asInt()).isEqualTo(1);
    assertThat(item.get("going").get(0).get("userId").asLong()).isEqualTo(stranger.id());
    assertThat(item.get("going").get(0).get("displayName").asString()).isEqualTo(stranger.displayName());
  }

  @Test
  void declineRequest__leavesThemOut_andTheyMayAskAgain() {
    TestUser creator = register();
    TestUser stranger = register();
    long eventId = publicEvent(creator);
    post("/event/" + eventId + "/join", stranger);

    //
    //
    Response response = post("/event/" + eventId + "/request/" + stranger.id() + "/decline", creator);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("invitees").get(0).get("status").asString()).isEqualTo("DECLINED");
    assertThat(get("/chat/list", stranger).body().size()).isZero();
    assertThat(feedItem(stranger, eventId).get("myStanding").asString()).isEqualTo("DECLINED");
    assertThat(feedItem(creator, eventId).get("goingCount").asInt()).isZero();

    assertThat(post("/event/" + eventId + "/join", stranger).body().get("standing").asString())
        .isEqualTo("REQUESTED");
  }

  @Test
  void aRequest__isAnsweredOnlyByTheCreator_andOnlyWhileItWaits() {
    TestUser creator = register();
    TestUser asking = register();
    TestUser guest = register();
    TestUser stranger = register();
    long eventId = publicEvent(creator);
    post("/event/" + eventId + "/join", asking);
    joinApproved(creator, eventId, guest);

    //
    //
    Response byAGuest = post("/event/" + eventId + "/request/" + asking.id() + "/approve", guest);
    Response byThemselves = post("/event/" + eventId + "/request/" + asking.id() + "/approve", asking);
    Response ofAStranger = post("/event/" + eventId + "/request/" + stranger.id() + "/approve", creator);
    Response ofAGuest = post("/event/" + eventId + "/request/" + guest.id() + "/decline", creator);
    Response withoutToken = post("/event/" + eventId + "/request/" + asking.id() + "/approve", null);
    //
    //

    assertThat(byAGuest.status()).isEqualTo(404);
    assertThat(byThemselves.status()).isEqualTo(404);
    assertThat(ofAStranger.status()).isEqualTo(404);
    assertThat(ofAStranger.body().get("code").asString()).isEqualTo("request_not_found");
    assertThat(ofAGuest.status()).isEqualTo(404);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(feedItem(asking, eventId).get("myStanding").asString()).isEqualTo("REQUESTED");
    assertThat(feedItem(guest, eventId).get("myStanding").asString()).isEqualTo("ACCEPTED");
  }

  @Test
  void aRequest__isSeenByTheCreatorOnly_andGivesNoSayInTheEvent() {
    TestUser creator = register();
    TestUser asking = register();
    TestUser guest = register();
    long eventId = publicEvent(creator);
    joinApproved(creator, eventId, guest);
    post("/event/" + eventId + "/join", asking);

    //
    //
    JsonNode seenByCreator = get("/event/" + eventId, creator).body().get("invitees");
    JsonNode seenByGuest = get("/event/" + eventId, guest).body().get("invitees");
    Response preference = post("/event/" + eventId + "/preference", asking,
        Map.of("locationText", "Somewhere", "travelRadiusMi", 5));
    Response suggestions = post("/event/" + eventId + "/suggestion/refresh", asking);
    //
    //

    assertThat(longs(seenByCreator, "userId")).containsExactly(guest.id(), asking.id());
    assertThat(longs(seenByGuest, "userId")).containsExactly(guest.id());
    assertThat(preference.status()).isEqualTo(409);
    assertThat(suggestions.status()).isEqualTo(404);
  }

  @Test
  void leave__takesBackARequestThatIsWaiting() {
    TestUser creator = register();
    TestUser asking = register();
    long eventId = publicEvent(creator);
    post("/event/" + eventId + "/join", asking);

    //
    //
    Response response = post("/event/" + eventId + "/leave", asking);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(feedItem(asking, eventId).get("myStanding").asString()).isEqualTo("DECLINED");
    // Nothing is left for the creator to answer.
    assertThat(post("/event/" + eventId + "/request/" + asking.id() + "/approve", creator).status())
        .isEqualTo(404);
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
    long chatId = joinApproved(creator, eventId, guest);

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

    // Coming back is asking again: the creator decides once more.
    assertThat(post("/event/" + eventId + "/join", guest).body().get("standing").asString())
        .isEqualTo("REQUESTED");
    assertThat(joinApproved(creator, eventId, guest)).isEqualTo(chatId);
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
    long chatId = joinApproved(creator, eventId, unwanted);

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
    joinApproved(creator, eventId, guest);
    joinApproved(creator, eventId, otherGuest);

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
    joinApproved(creator, eventId, removed);
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

  /** The guest asks to join and the creator lets them in. @return the id of the event's chat */
  private long joinApproved(TestUser creator, long eventId, TestUser guest) {
    assertThat(post("/event/" + eventId + "/join", guest).status()).isEqualTo(200);
    Response approved = post("/event/" + eventId + "/request/" + guest.id() + "/approve", creator);
    assertThat(approved.status()).isEqualTo(200);
    return approved.body().get("chatId").asLong();
  }

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
