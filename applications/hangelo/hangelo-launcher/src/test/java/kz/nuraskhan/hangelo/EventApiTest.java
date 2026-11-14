package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class EventApiTest extends ApiTestBase {

  @Test
  void types__offerPrefilledValuesForEveryType() {
    TestUser user = register();

    //
    //
    Response response = get("/event/type/list", user);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    JsonNode bath = response.body().valueStream()
        .filter(type -> type.get("eventType").asString().equals("BATH"))
        .findFirst().orElseThrow();
    assertThat(bath.get("defaultTitle").asString()).isEqualTo("Bathhouse");
    assertThat(bath.get("defaultDistanceMaxMi").asInt()).isEqualTo(10);
    assertThat(bath.get("defaultPriceMin").asInt()).isEqualTo(5000);
    assertThat(bath.get("defaultPriceMax").asInt()).isEqualTo(15000);
  }

  @Test
  void create__invitesEveryFriendAndPutsEveryoneIntoOneNewGroupChat() {
    TestUser creator = register();
    TestUser friend1 = register();
    TestUser friend2 = register();
    makeFriends(creator, friend1);
    makeFriends(friend2, creator);

    //
    //
    Response response = post("/event/create", creator, Map.of(
        "eventType", "FOOD",
        "title", "Lagman on Friday",
        "description", "The place near the office",
        "distanceMinMi", 1,
        "distanceMaxMi", 7,
        "priceMin", 2500,
        "priceMax", 6000,
        // 02:30Z is morning in Almaty and the previous calendar day in Los Angeles: the moments must
        // come back exactly as sent, whatever the time zone of the server is. Sent latest first and
        // one of them twice: they come back earliest first, each once.
        "proposedTimes", List.of("2030-01-02T15:00:00Z", "2030-01-01T02:30:00Z", "2030-01-02T15:00:00Z"),
        "inviteeIds", List.of(friend1.id(), friend2.id())));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    JsonNode event = response.body();
    assertThat(event.get("eventType").asString()).isEqualTo("FOOD");
    assertThat(event.get("title").asString()).isEqualTo("Lagman on Friday");
    assertThat(event.get("description").asString()).isEqualTo("The place near the office");
    assertThat(event.get("distanceMinMi").asInt()).isEqualTo(1);
    assertThat(event.get("distanceMaxMi").asInt()).isEqualTo(7);
    assertThat(event.get("priceMin").asInt()).isEqualTo(2500);
    assertThat(event.get("priceMax").asInt()).isEqualTo(6000);
    assertThat(event.get("proposedTimes").valueStream().map(JsonNode::asString).toList())
        .containsExactly("2030-01-01T02:30:00Z", "2030-01-02T15:00:00Z");
    assertThat(event.get("status").asString()).isEqualTo("ACTIVE");
    assertThat(event.get("creatorId").asLong()).isEqualTo(creator.id());
    assertThat(event.get("creatorName").asString()).isEqualTo(creator.displayName());
    assertThat(longs(event.get("invitees"), "userId")).containsExactly(friend1.id(), friend2.id());
    assertThat(event.get("invitees").get(0).get("status").asString()).isEqualTo("PENDING");

    long eventId = event.get("id").asLong();
    long chatId = event.get("chatId").asLong();

    // Each invitee got the invitation with the whole form in it.
    for (TestUser invitee : List.of(friend1, friend2)) {
      Response incoming = get("/invitation/incoming", invitee);
      assertThat(incoming.body().size()).isEqualTo(1);
      assertThat(incoming.body().get(0).get("event").get("id").asLong()).isEqualTo(eventId);
      assertThat(incoming.body().get(0).get("event").get("title").asString())
          .isEqualTo("Lagman on Friday");
    }

    // One chat, named after the event, with all three in it - and each of them sees it.
    for (TestUser member : List.of(creator, friend1, friend2)) {
      Response chats = get("/chat/list", member);
      assertThat(longs(chats.body(), "id")).containsExactly(chatId);
      assertThat(chats.body().get(0).get("title").asString()).isEqualTo("Lagman on Friday");
      assertThat(chats.body().get(0).get("memberCount").asInt()).isEqualTo(3);
      // The chat leads back to its event.
      assertThat(chats.body().get(0).get("eventId").asLong()).isEqualTo(eventId);
      assertThat(chats.body().get(0).get("eventType").asString()).isEqualTo("FOOD");
    }
    assertThat(longs(get("/chat/" + chatId + "/member/list", friend1).body(), "userId"))
        .containsExactlyInAnyOrder(creator.id(), friend1.id(), friend2.id());
  }

  @Test
  void create__fillsWhatTheFormLeftOutFromTheType() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);

    //
    //
    Response response = post("/event/create", creator,
        Map.of("eventType", "BATH", "inviteeIds", List.of(friend.id())));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    JsonNode event = response.body();
    assertThat(event.get("title").asString()).isEqualTo("Bathhouse");
    assertThat(event.get("distanceMinMi").asInt()).isEqualTo(0);
    assertThat(event.get("distanceMaxMi").asInt()).isEqualTo(10);
    assertThat(event.get("priceMin").asInt()).isEqualTo(5000);
    assertThat(event.get("priceMax").asInt()).isEqualTo(15000);
    assertThat(event.get("proposedTimes").size()).isZero();
  }

  @Test
  void create__refusesInviteeWhoIsNotFriend_andCreatesNothing() {
    TestUser creator = register();
    TestUser friend = register();
    TestUser stranger = register();
    makeFriends(creator, friend);

    //
    //
    Response response = post("/event/create", creator,
        Map.of("eventType", "WALK", "inviteeIds", List.of(friend.id(), stranger.id())));
    //
    //

    assertThat(response.status()).isEqualTo(400);
    assertThat(response.body().get("code").asString()).isEqualTo("invitee_not_friend");
    assertThat(get("/event/list", creator).body().size()).isZero();
    assertThat(get("/chat/list", creator).body().size()).isZero();
    assertThat(get("/invitation/incoming", friend).body().size()).isZero();
  }

  @Test
  void create__refusesFormWithReversedRange() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);

    //
    //
    Response reversedPrice = post("/event/create", creator, Map.of(
        "eventType", "WALK", "priceMin", 500, "priceMax", 100, "inviteeIds", List.of(friend.id())));
    //
    //

    assertThat(reversedPrice.status()).isEqualTo(400);
    assertThat(reversedPrice.body().get("code").asString()).isEqualTo("bad_price");
  }

  @Test
  void get__isOnlyForTheCreatorAndTheInvited() {
    TestUser creator = register();
    TestUser friend = register();
    TestUser stranger = register();
    makeFriends(creator, friend);
    long eventId = createEvent(creator, friend).get("id").asLong();

    //
    //
    Response byFriend = get("/event/" + eventId, friend);
    Response byStranger = get("/event/" + eventId, stranger);
    //
    //

    assertThat(byFriend.status()).isEqualTo(200);
    assertThat(byStranger.status()).isEqualTo(404);
    assertThat(longs(get("/event/list", friend).body(), "id")).containsExactly(eventId);
    assertThat(get("/event/list", stranger).body().size()).isZero();
  }

  @Test
  void accept__marksTheInvitationAndKeepsTheInviteeInTheChat() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);
    JsonNode event = createEvent(creator, friend);
    long invitationId = event.get("invitees").get(0).get("invitationId").asLong();

    //
    //
    Response response = post("/invitation/" + invitationId + "/accept", friend);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    JsonNode invitee = get("/event/" + event.get("id").asLong(), creator).body().get("invitees").get(0);
    assertThat(invitee.get("status").asString()).isEqualTo("ACCEPTED");
    assertThat(invitee.get("respondedAt").isNull()).isFalse();
    assertThat(get("/invitation/incoming", friend).body().size()).isZero();
    assertThat(get("/chat/list", friend).body().size()).isEqualTo(1);
  }

  @Test
  void decline__marksTheInvitationAndTakesTheInviteeOutOfTheChat() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);
    JsonNode event = createEvent(creator, friend);
    long chatId = event.get("chatId").asLong();
    long invitationId = event.get("invitees").get(0).get("invitationId").asLong();

    //
    //
    Response response = post("/invitation/" + invitationId + "/decline", friend);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    JsonNode invitee = get("/event/" + event.get("id").asLong(), creator).body().get("invitees").get(0);
    assertThat(invitee.get("status").asString()).isEqualTo("DECLINED");
    assertThat(get("/chat/list", friend).body().size()).isZero();
    assertThat(get("/chat/" + chatId + "/message/list", friend).status()).isEqualTo(404);
    assertThat(get("/chat/list", creator).body().get(0).get("memberCount").asInt()).isEqualTo(1);
  }

  @Test
  void accept__isOnlyForTheInvitee_andOnlyOnce() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);
    long invitationId = createEvent(creator, friend).get("invitees").get(0).get("invitationId").asLong();

    //
    //
    Response byCreator = post("/invitation/" + invitationId + "/accept", creator);
    Response first = post("/invitation/" + invitationId + "/accept", friend);
    Response second = post("/invitation/" + invitationId + "/decline", friend);
    //
    //

    assertThat(byCreator.status()).isEqualTo(404);
    assertThat(first.status()).isEqualTo(200);
    assertThat(second.status()).isEqualTo(409);
  }

  @Test
  void cancel__isOnlyForTheCreator_andStopsTheInvitations() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);
    JsonNode event = createEvent(creator, friend);
    long eventId = event.get("id").asLong();
    long invitationId = event.get("invitees").get(0).get("invitationId").asLong();

    //
    //
    Response byFriend = post("/event/" + eventId + "/cancel", friend);
    Response byCreator = post("/event/" + eventId + "/cancel", creator);
    //
    //

    assertThat(byFriend.status()).isEqualTo(404);
    assertThat(byCreator.status()).isEqualTo(200);
    assertThat(get("/event/" + eventId, friend).body().get("status").asString()).isEqualTo("CANCELLED");
    assertThat(get("/invitation/incoming", friend).body().size()).isZero();
    assertThat(post("/invitation/" + invitationId + "/accept", friend).status()).isEqualTo(409);
  }

  @Test
  void postMessage__isSeenByEveryMemberNewestFirst_andPagesByBeforeId() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);
    long chatId = createEvent(creator, friend).get("chatId").asLong();

    //
    //
    Response first = post("/chat/" + chatId + "/message", creator, Map.of("text", "Who is in?"));
    Response second = post("/chat/" + chatId + "/message", friend, Map.of("text", "I am!"));
    //
    //

    assertThat(first.status()).isEqualTo(200);
    assertThat(second.status()).isEqualTo(200);
    assertThat(second.body().get("authorId").asLong()).isEqualTo(friend.id());
    assertThat(second.body().get("authorName").asString()).isEqualTo(friend.displayName());

    Response all = get("/chat/" + chatId + "/message/list", creator);
    assertThat(all.body().size()).isEqualTo(2);
    assertThat(all.body().get(0).get("text").asString()).isEqualTo("I am!");
    assertThat(all.body().get(1).get("text").asString()).isEqualTo("Who is in?");

    long newestId = all.body().get(0).get("id").asLong();
    Response older = get("/chat/" + chatId + "/message/list?beforeId=" + newestId + "&limit=10", friend);
    assertThat(older.body().size()).isEqualTo(1);
    assertThat(older.body().get(0).get("text").asString()).isEqualTo("Who is in?");
  }

  @Test
  void postMessage__isRefusedForSomeoneOutsideTheChat() {
    TestUser creator = register();
    TestUser friend = register();
    TestUser stranger = register();
    makeFriends(creator, friend);
    long chatId = createEvent(creator, friend).get("chatId").asLong();

    //
    //
    Response post = post("/chat/" + chatId + "/message", stranger, Map.of("text", "Hello?"));
    Response read = get("/chat/" + chatId + "/message/list", stranger);
    //
    //

    assertThat(post.status()).isEqualTo(404);
    assertThat(read.status()).isEqualTo(404);
    assertThat(get("/chat/" + chatId + "/message/list", creator).body().size()).isZero();
  }

  /** An event of {@code creator} with the one invitee; they must be friends already. */
  private JsonNode createEvent(TestUser creator, TestUser invitee) {
    Response response = post("/event/create", creator,
        Map.of("eventType", "COFFEE", "inviteeIds", List.of(invitee.id())));
    assertThat(response.status()).isEqualTo(200);
    return response.body();
  }
}
