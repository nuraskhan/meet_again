package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** An event opened and joined by its link. */
class EventLinkApiTest extends ApiTestBase {

  @Test
  void create__worksWithoutInvitees_andGivesTheLinkToTheCreator() {
    TestUser creator = register();

    //
    //
    Response response = post("/event/create", creator, Map.of("eventType", "WALK"));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("invitees").size()).isZero();
    assertThat(response.body().get("shareToken").asString()).hasSizeGreaterThanOrEqualTo(24);
    // The chat exists from the start, with the creator alone in it.
    assertThat(get("/chat/list", creator).body().get(0).get("memberCount").asInt()).isEqualTo(1);
  }

  @Test
  void publicEvent__isReadWithoutToken_andShowsNoGuestNames() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);
    String shareToken = post("/event/create", creator, Map.of(
        "eventType", "CINEMA",
        "title", "New Nolan",
        "description", "IMAX if we are lucky",
        "proposedTimes", List.of("2030-05-01T14:00:00Z", "2030-05-02T14:00:00Z"),
        "inviteeIds", List.of(friend.id()))).body().get("shareToken").asString();

    //
    //
    Response response = get("/public/event/" + shareToken, null);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    JsonNode event = response.body();
    assertThat(event.get("eventType").asString()).isEqualTo("CINEMA");
    assertThat(event.get("title").asString()).isEqualTo("New Nolan");
    assertThat(event.get("description").asString()).isEqualTo("IMAX if we are lucky");
    assertThat(event.get("proposedTimes").valueStream().map(JsonNode::asString).toList())
        .containsExactly("2030-05-01T14:00:00Z", "2030-05-02T14:00:00Z");
    assertThat(event.get("status").asString()).isEqualTo("ACTIVE");
    assertThat(event.get("creatorName").asString()).isEqualTo(creator.displayName());
    assertThat(event.get("invitedCount").asInt()).isEqualTo(1);
    assertThat(event.get("goingCount").asInt()).isZero();

    // Nothing that identifies the event inside the system or its guests.
    assertThat(response.body().toString()).doesNotContain(friend.displayName());
    assertThat(event.has("id")).isFalse();
    assertThat(event.has("chatId")).isFalse();
    assertThat(event.has("invitees")).isFalse();
    assertThat(event.has("shareToken")).isFalse();
  }

  @Test
  void publicEvent__answersNotFoundForUnknownLink() {
    //
    //
    Response response = get("/public/event/no-such-token", null);
    //
    //

    assertThat(response.status()).isEqualTo(404);
    assertThat(response.body().get("code").asString()).isEqualTo("event_not_found");
  }

  @Test
  void join__needsToken() {
    TestUser creator = register();
    String shareToken = createByLinkOnly(creator).get("shareToken").asString();

    //
    //
    Response response = post("/event/join/" + shareToken, null);
    //
    //

    assertThat(response.status()).isEqualTo(401);
    assertThat(get("/public/event/" + shareToken, null).body().get("invitedCount").asInt()).isZero();
  }

  @Test
  void join__makesAStrangerAGuestWhoSaidYes_andPutsThemIntoTheChat() {
    TestUser creator = register();
    TestUser stranger = register(); // not a friend of the creator: the link is enough
    JsonNode created = createByLinkOnly(creator);
    long eventId = created.get("id").asLong();
    long chatId = created.get("chatId").asLong();

    //
    //
    Response response = post("/event/join/" + created.get("shareToken").asString(), stranger);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("id").asLong()).isEqualTo(eventId);
    // A guest sees the event but does not get the link to hand out.
    assertThat(response.body().get("shareToken").isNull()).isTrue();

    JsonNode guest = get("/event/" + eventId, creator).body().get("invitees").get(0);
    assertThat(guest.get("userId").asLong()).isEqualTo(stranger.id());
    assertThat(guest.get("status").asString()).isEqualTo("ACCEPTED");

    assertThat(longs(get("/event/list", stranger).body(), "id")).containsExactly(eventId);
    assertThat(longs(get("/chat/list", stranger).body(), "id")).containsExactly(chatId);
    assertThat(longs(get("/chat/" + chatId + "/member/list", creator).body(), "userId"))
        .containsExactlyInAnyOrder(creator.id(), stranger.id());
    assertThat(get("/invitation/incoming", stranger).body().size()).isZero();
  }

  @Test
  void join__twice_changesNothing() {
    TestUser creator = register();
    TestUser guest = register();
    JsonNode created = createByLinkOnly(creator);
    String shareToken = created.get("shareToken").asString();
    post("/event/join/" + shareToken, guest);

    //
    //
    Response response = post("/event/join/" + shareToken, guest);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("invitees").size()).isEqualTo(1);
    assertThat(get("/chat/list", creator).body().get(0).get("memberCount").asInt()).isEqualTo(2);
  }

  @Test
  void join__acceptsAnInvitationNotAnsweredYet() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);
    JsonNode created = post("/event/create", creator,
        Map.of("eventType", "COFFEE", "inviteeIds", List.of(friend.id()))).body();

    //
    //
    Response response = post("/event/join/" + created.get("shareToken").asString(), friend);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("invitees").size()).isEqualTo(1);
    assertThat(response.body().get("invitees").get(0).get("status").asString()).isEqualTo("ACCEPTED");
    assertThat(get("/invitation/incoming", friend).body().size()).isZero();
  }

  @Test
  void join__bringsBackTheOneWhoDeclined() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);
    JsonNode created = post("/event/create", creator,
        Map.of("eventType", "COFFEE", "inviteeIds", List.of(friend.id()))).body();
    long chatId = created.get("chatId").asLong();
    post("/invitation/" + created.get("invitees").get(0).get("invitationId").asLong() + "/decline", friend);

    //
    //
    Response response = post("/event/join/" + created.get("shareToken").asString(), friend);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("invitees").get(0).get("status").asString()).isEqualTo("ACCEPTED");
    assertThat(longs(get("/chat/list", friend).body(), "id")).containsExactly(chatId);
    assertThat(get("/chat/" + chatId + "/message/list", friend).status()).isEqualTo(200);
  }

  @Test
  void join__byTheCreator_changesNothing() {
    TestUser creator = register();
    JsonNode created = createByLinkOnly(creator);

    //
    //
    Response response = post("/event/join/" + created.get("shareToken").asString(), creator);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("invitees").size()).isZero();
  }

  @Test
  void join__isRefusedForCancelledEventAndUnknownLink() {
    TestUser creator = register();
    TestUser guest = register();
    JsonNode created = createByLinkOnly(creator);
    post("/event/" + created.get("id").asLong() + "/cancel", creator);

    //
    //
    Response cancelled = post("/event/join/" + created.get("shareToken").asString(), guest);
    Response unknown = post("/event/join/no-such-token", guest);
    //
    //

    assertThat(cancelled.status()).isEqualTo(409);
    assertThat(cancelled.body().get("code").asString()).isEqualTo("event_cancelled");
    assertThat(unknown.status()).isEqualTo(404);
    assertThat(get("/event/list", guest).body().size()).isZero();
    // The link still opens, and says the event is off.
    assertThat(get("/public/event/" + created.get("shareToken").asString(), null)
        .body().get("status").asString()).isEqualTo("CANCELLED");
  }

  /** An event nobody is invited to by name. */
  private JsonNode createByLinkOnly(TestUser creator) {
    Response response = post("/event/create", creator, Map.of("eventType", "BOARD_GAMES"));
    assertThat(response.status()).isEqualTo(200);
    return response.body();
  }
}
