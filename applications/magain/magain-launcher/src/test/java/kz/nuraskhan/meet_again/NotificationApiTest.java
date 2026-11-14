package kz.nuraskhan.meet_again;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class NotificationApiTest extends ApiTestBase {

  @Test
  void friendRequest__tellsTheAddressee_andAcceptingTellsTheRequester() {
    TestUser asker = register();
    TestUser asked = register();

    //
    //
    long requestId = post("/friend/request", asker, Map.of("userId", asked.id()))
        .body().get("requestId").asLong();
    post("/friend/request/" + requestId + "/accept", asked);
    //
    //

    JsonNode forAsked = get("/notification/list", asked).body();
    assertThat(forAsked.size()).isEqualTo(1);
    assertThat(forAsked.get(0).get("kind").asString()).isEqualTo("FRIEND_REQUEST");
    assertThat(forAsked.get(0).get("text").asString())
        .isEqualTo(asker.displayName() + " wants to be your friend");
    assertThat(forAsked.get(0).get("actorId").asLong()).isEqualTo(asker.id());
    assertThat(forAsked.get(0).get("actorName").asString()).isEqualTo(asker.displayName());
    assertThat(forAsked.get(0).get("eventId").isNull()).isTrue();
    assertThat(forAsked.get(0).get("read").asBoolean()).isFalse();

    JsonNode forAsker = get("/notification/list", asker).body();
    assertThat(forAsker.size()).isEqualTo(1);
    assertThat(forAsker.get(0).get("kind").asString()).isEqualTo("FRIEND_ACCEPTED");
    assertThat(forAsker.get(0).get("text").asString())
        .isEqualTo(asked.displayName() + " accepted your friend request");
  }

  @Test
  void friendRequest__declined_tellsNobody() {
    TestUser asker = register();
    TestUser asked = register();
    long requestId = post("/friend/request", asker, Map.of("userId", asked.id()))
        .body().get("requestId").asLong();

    //
    //
    post("/friend/request/" + requestId + "/decline", asked);
    //
    //

    assertThat(get("/notification/list", asker).body().size()).isZero();
  }

  @Test
  void create__tellsEveryInvitee_andNotTheCreator() {
    TestUser creator = register();
    TestUser friend1 = register();
    TestUser friend2 = register();
    makeFriends(creator, friend1);
    makeFriends(creator, friend2);
    int creatorHadBefore = get("/notification/list", creator).body().size();

    //
    //
    JsonNode event = post("/event/create", creator, Map.of(
        "eventType", "FOOD", "title", "Lagman on Friday",
        "inviteeIds", List.of(friend1.id(), friend2.id()))).body();
    //
    //

    for (TestUser invitee : List.of(friend1, friend2)) {
      JsonNode newest = get("/notification/list", invitee).body().get(0);
      assertThat(newest.get("kind").asString()).isEqualTo("EVENT_INVITATION");
      assertThat(newest.get("text").asString())
          .isEqualTo(creator.displayName() + " invited you to Lagman on Friday");
      assertThat(newest.get("eventId").asLong()).isEqualTo(event.get("id").asLong());
    }
    assertThat(get("/notification/list", creator).body().size()).isEqualTo(creatorHadBefore);
  }

  @Test
  void answeringAnInvitation__tellsTheCreator() {
    TestUser creator = register();
    TestUser comer = register();
    TestUser decliner = register();
    makeFriends(creator, comer);
    makeFriends(creator, decliner);
    JsonNode event = post("/event/create", creator, Map.of(
        "eventType", "WALK", "title", "Sunday walk",
        "inviteeIds", List.of(comer.id(), decliner.id()))).body();

    //
    //
    post("/invitation/" + invitationId(event, comer) + "/accept", comer);
    post("/invitation/" + invitationId(event, decliner) + "/decline", decliner);
    //
    //

    JsonNode forCreator = get("/notification/list", creator).body();
    assertThat(forCreator.get(0).get("kind").asString()).isEqualTo("INVITATION_DECLINED");
    assertThat(forCreator.get(0).get("text").asString())
        .isEqualTo(decliner.displayName() + " can't make it to Sunday walk");
    assertThat(forCreator.get(1).get("kind").asString()).isEqualTo("INVITATION_ACCEPTED");
    assertThat(forCreator.get(1).get("text").asString())
        .isEqualTo(comer.displayName() + " is coming to Sunday walk");
  }

  @Test
  void publicPlan__joiningAndLeavingTellTheCreator_andRemovingTellsTheGuest() {
    TestUser creator = register();
    TestUser leaver = register();
    TestUser removed = register();
    long eventId = post("/event/create", creator,
        Map.of("eventType", "BAR", "title", "Open bar night", "visibility", "PUBLIC"))
        .body().get("id").asLong();

    //
    //
    post("/event/" + eventId + "/join", leaver);
    post("/event/" + eventId + "/leave", leaver);
    post("/event/" + eventId + "/join", removed);
    post("/event/" + eventId + "/guest/" + removed.id() + "/remove", creator);
    //
    //

    JsonNode forCreator = get("/notification/list", creator).body();
    assertThat(forCreator.valueStream().map(n -> n.get("kind").asString()).toList())
        .containsExactly("GUEST_JOINED", "GUEST_LEFT", "GUEST_JOINED");
    assertThat(forCreator.get(1).get("text").asString())
        .isEqualTo(leaver.displayName() + " left Open bar night");
    assertThat(forCreator.get(2).get("text").asString())
        .isEqualTo(leaver.displayName() + " joined Open bar night");

    JsonNode forRemoved = get("/notification/list", removed).body();
    assertThat(forRemoved.size()).isEqualTo(1);
    assertThat(forRemoved.get(0).get("kind").asString()).isEqualTo("REMOVED_FROM_EVENT");
    assertThat(forRemoved.get(0).get("text").asString())
        .isEqualTo("You were removed from Open bar night");
  }

  @Test
  void cancel__tellsThoseInvitedAndComing_andNotThoseWhoDeclined() {
    TestUser creator = register();
    TestUser pending = register();
    TestUser coming = register();
    TestUser decliner = register();
    for (TestUser friend : List.of(pending, coming, decliner)) {
      makeFriends(creator, friend);
    }
    JsonNode event = post("/event/create", creator, Map.of(
        "eventType", "COFFEE", "title", "Coffee",
        "inviteeIds", List.of(pending.id(), coming.id(), decliner.id()))).body();
    post("/invitation/" + invitationId(event, coming) + "/accept", coming);
    post("/invitation/" + invitationId(event, decliner) + "/decline", decliner);

    //
    //
    post("/event/" + event.get("id").asLong() + "/cancel", creator);
    //
    //

    for (TestUser told : List.of(pending, coming)) {
      JsonNode newest = get("/notification/list", told).body().get(0);
      assertThat(newest.get("kind").asString()).isEqualTo("EVENT_CANCELLED");
      assertThat(newest.get("text").asString()).isEqualTo(creator.displayName() + " cancelled Coffee");
    }
    assertThat(get("/notification/list", decliner).body().valueStream()
        .map(n -> n.get("kind").asString()).toList()).doesNotContain("EVENT_CANCELLED");
  }

  @Test
  void read__marksOne_readAll__marksTheRest_andTheCountFollows() {
    TestUser me = register();
    for (int i = 0; i < 3; i++) {
      post("/friend/request", register(), Map.of("userId", me.id()));
    }
    JsonNode list = get("/notification/list", me).body();
    long oldest = list.get(2).get("id").asLong();
    assertThat(get("/notification/unread-count", me).body().get("count").asInt()).isEqualTo(3);

    //
    //
    Response one = post("/notification/" + oldest + "/read", me);
    Response oneAgain = post("/notification/" + oldest + "/read", me);
    //
    //

    assertThat(one.status()).isEqualTo(200);
    assertThat(one.body().get("count").asInt()).isEqualTo(2);
    assertThat(oneAgain.status()).isEqualTo(200);
    assertThat(oneAgain.body().get("count").asInt()).isEqualTo(2);
    JsonNode afterOne = get("/notification/list", me).body();
    assertThat(afterOne.valueStream().map(n -> n.get("read").asBoolean()).toList())
        .containsExactly(false, false, true);

    //
    //
    Response all = post("/notification/read-all", me);
    //
    //

    assertThat(all.status()).isEqualTo(200);
    assertThat(get("/notification/unread-count", me).body().get("count").asInt()).isZero();
    assertThat(get("/notification/list", me).body().valueStream()
        .map(n -> n.get("read").asBoolean()).toList()).containsExactly(true, true, true);
  }

  @Test
  void list__isPagedNewestFirstByBeforeId() {
    TestUser me = register();
    for (int i = 0; i < 3; i++) {
      post("/friend/request", register(), Map.of("userId", me.id()));
    }
    List<Long> all = longs(get("/notification/list", me).body(), "id");

    //
    //
    Response firstPage = get("/notification/list?limit=2", me);
    Response secondPage = get("/notification/list?limit=2&beforeId=" + all.get(1), me);
    //
    //

    assertThat(all).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    assertThat(longs(firstPage.body(), "id")).containsExactly(all.get(0), all.get(1));
    assertThat(longs(secondPage.body(), "id")).containsExactly(all.get(2));
  }

  @Test
  void read__ofSomebodyElsesNotification_isNotFound_andChangesNothing() {
    TestUser owner = register();
    TestUser stranger = register();
    post("/friend/request", register(), Map.of("userId", owner.id()));
    long id = get("/notification/list", owner).body().get(0).get("id").asLong();

    //
    //
    Response byStranger = post("/notification/" + id + "/read", stranger);
    Response missing = post("/notification/" + Long.MAX_VALUE + "/read", owner);
    Response withoutToken = get("/notification/list", null);
    //
    //

    assertThat(byStranger.status()).isEqualTo(404);
    assertThat(byStranger.body().get("code").asString()).isEqualTo("notification_not_found");
    assertThat(missing.status()).isEqualTo(404);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(get("/notification/unread-count", owner).body().get("count").asInt()).isEqualTo(1);
  }

  private static long invitationId(JsonNode event, TestUser invitee) {
    return event.get("invitees").valueStream()
        .filter(row -> row.get("userId").asLong() == invitee.id())
        .findFirst().orElseThrow().get("invitationId").asLong();
  }
}
