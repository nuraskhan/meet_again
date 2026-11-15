package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Deleting an event (its creator), and clearing a closed one from one's own list (a guest). */
class EventDeleteApiTest extends ApiTestBase {

  @Test
  void delete__removesTheEventAndItsChatForEveryone() {
    TestUser creator = register();
    TestUser guest = register();
    makeFriends(creator, guest);
    JsonNode event = post("/event/create", creator, Map.of(
        "eventType", "FOOD", "title", "Dinner", "inviteeIds", List.of(guest.id()))).body();
    long eventId = event.get("id").asLong();
    long chatId = event.get("chatId").asLong();
    post("/chat/" + chatId + "/message", creator, Map.of("text", "Hello"));

    //
    //
    Response response = post("/event/" + eventId + "/delete", creator);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    for (TestUser person : List.of(creator, guest)) {
      assertThat(get("/event/" + eventId, person).status()).isEqualTo(404);
      assertThat(get("/event/list", person).body().size()).isZero();
      assertThat(get("/chat/list", person).body().size()).isZero();
      assertThat(get("/chat/" + chatId + "/message/list", person).status()).isEqualTo(404);
      assertThat(post("/chat/" + chatId + "/message", person, Map.of("text", "Anyone?")).status())
          .isEqualTo(404);
    }
    assertThat(get("/invitation/incoming", guest).body().size()).isZero();
    // The link no longer opens anything.
    assertThat(get("/public/event/" + event.get("shareToken").asString(), null).status()).isEqualTo(404);
    assertThat(post("/event/join/" + event.get("shareToken").asString(), register()).status())
        .isEqualTo(409);
  }

  @Test
  void delete__ofAnEventThatIsStillOn_tellsTheGuestsItIsOff() {
    TestUser creator = register();
    TestUser guest = register();
    makeFriends(creator, guest);
    long eventId = post("/event/create", creator, Map.of(
        "eventType", "WALK", "title", "Walk", "inviteeIds", List.of(guest.id()))).body().get("id").asLong();

    //
    //
    post("/event/" + eventId + "/delete", creator);
    //
    //

    JsonNode newest = get("/notification/list", guest).body().get(0);
    assertThat(newest.get("kind").asString()).isEqualTo("EVENT_CANCELLED");
    assertThat(newest.get("text").asString()).isEqualTo(creator.displayName() + " cancelled Walk");
  }

  @Test
  void delete__ofAPublicEvent_takesItOutOfTheFeed() {
    TestUser creator = register();
    long eventId = post("/event/create", creator,
        Map.of("eventType", "BAR", "visibility", "PUBLIC")).body().get("id").asLong();

    //
    //
    post("/event/" + eventId + "/delete", creator);
    //
    //

    assertThat(longs(get("/feed/list", register()).body(), "id")).doesNotContain(eventId);
  }

  @Test
  void delete__worksForACancelledEventToo_andOnlyOnce() {
    TestUser creator = register();
    long eventId = post("/event/create", creator, Map.of("eventType", "COFFEE")).body().get("id").asLong();
    post("/event/" + eventId + "/cancel", creator);

    //
    //
    Response first = post("/event/" + eventId + "/delete", creator);
    Response second = post("/event/" + eventId + "/delete", creator);
    //
    //

    assertThat(first.status()).isEqualTo(200);
    assertThat(second.status()).isEqualTo(404);
    assertThat(get("/event/list", creator).body().size()).isZero();
  }

  @Test
  void delete__isOnlyForTheCreator() {
    TestUser creator = register();
    TestUser guest = register();
    TestUser stranger = register();
    makeFriends(creator, guest);
    long eventId = post("/event/create", creator, Map.of(
        "eventType", "COFFEE", "inviteeIds", List.of(guest.id()))).body().get("id").asLong();

    //
    //
    Response byGuest = post("/event/" + eventId + "/delete", guest);
    Response byStranger = post("/event/" + eventId + "/delete", stranger);
    Response unknown = post("/event/" + Long.MAX_VALUE + "/delete", creator);
    Response withoutToken = post("/event/" + eventId + "/delete", null);
    //
    //

    assertThat(byGuest.status()).isEqualTo(404);
    assertThat(byStranger.status()).isEqualTo(404);
    assertThat(unknown.status()).isEqualTo(404);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(get("/event/" + eventId, creator).status()).isEqualTo(200);
    assertThat(get("/chat/list", guest).body().size()).isEqualTo(1);
  }

  @Test
  void leave__ofAnEventThatIsCancelled_clearsItAndItsChatFromTheGuestsLists_andTellsNobody() {
    TestUser creator = register();
    TestUser guest = register();
    TestUser other = register();
    makeFriends(creator, guest);
    makeFriends(creator, other);
    long eventId = post("/event/create", creator, Map.of(
        "eventType", "FOOD", "title", "Lunch", "inviteeIds", List.of(guest.id(), other.id())))
        .body().get("id").asLong();
    post("/event/" + eventId + "/cancel", creator);
    int creatorHad = get("/notification/list", creator).body().size();

    //
    //
    Response response = post("/event/" + eventId + "/leave", guest);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(get("/event/list", guest).body().size()).isZero();
    assertThat(get("/event/" + eventId, guest).status()).isEqualTo(404);
    assertThat(get("/chat/list", guest).body().size()).isZero();
    // The others keep it.
    assertThat(get("/event/list", creator).body().size()).isEqualTo(1);
    assertThat(get("/event/list", other).body().size()).isEqualTo(1);
    assertThat(get("/chat/list", other).body().size()).isEqualTo(1);
    assertThat(get("/notification/list", creator).body().size()).isEqualTo(creatorHad);
  }
}
