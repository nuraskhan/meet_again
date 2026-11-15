package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.model.entity.Event;
import kz.nuraskhan.hangelo.model.enums.EventStatus;
import kz.nuraskhan.hangelo.model.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * A person's reliability: of what they said they would do, how much they did.
 *
 * <p>An event "takes place" when its date passes. Here the status is set directly: the timer that
 * does it looks at the whole database, and its own tests are elsewhere (EventExpiryApiTest).
 */
class ReliabilityApiTest extends ApiTestBase {

  @Autowired EventRepository eventRepository;

  @Test
  void aNewcomer__hasNoScore_untilThereIsEnoughToJudgeBy() {
    TestUser guest = register();
    comeTo(guest);
    comeTo(guest);

    //
    //
    JsonNode afterTwo = get("/profile/me", guest).body().get("reliability");
    comeTo(guest);
    JsonNode afterThree = get("/profile/me", guest).body().get("reliability");
    //
    //

    assertThat(afterTwo.get("score").isNull()).isTrue();
    assertThat(afterTwo.get("kept").asInt()).isEqualTo(2);
    assertThat(afterThree.get("score").asInt()).isEqualTo(100);
    assertThat(afterThree.get("kept").asInt()).isEqualTo(3);
    assertThat(afterThree.get("broken").asInt()).isZero();
  }

  @Test
  void leavingAfterSayingComing__countsAgainst_andDecliningAnInvitationDoesNot() {
    TestUser guest = register();
    comeTo(guest);
    comeTo(guest);
    // Said "coming", then left while it was still on: a promise broken.
    TestUser host = register();
    makeFriends(host, guest);
    JsonNode event = post("/event/create", host, Map.of("eventType", "WALK", "inviteeIds", List.of(guest.id()))).body();
    post("/invitation/" + event.get("invitees").get(0).get("invitationId").asLong() + "/accept", guest);
    // Never said "coming" to this one: nothing promised, nothing broken.
    JsonNode declined = post("/event/create", host, Map.of("eventType", "BAR", "inviteeIds", List.of(guest.id()))).body();
    post("/invitation/" + declined.get("invitees").get(0).get("invitationId").asLong() + "/decline", guest);

    //
    //
    post("/event/" + event.get("id").asLong() + "/leave", guest);
    //
    //

    JsonNode reliability = get("/profile/me", guest).body().get("reliability");
    assertThat(reliability.get("kept").asInt()).isEqualTo(2);
    assertThat(reliability.get("broken").asInt()).isEqualTo(1);
    assertThat(reliability.get("score").asInt()).isEqualTo(67); // 2 of 3
  }

  @Test
  void comingBackAfterLeaving__forgivesTheLeaving() {
    TestUser guest = register();
    TestUser host = register();
    makeFriends(host, guest);
    JsonNode event = post("/event/create", host, Map.of("eventType", "WALK", "inviteeIds", List.of(guest.id()))).body();
    long eventId = event.get("id").asLong();
    post("/invitation/" + event.get("invitees").get(0).get("invitationId").asLong() + "/accept", guest);
    post("/event/" + eventId + "/leave", guest);

    //
    //
    post("/event/join/" + get("/event/" + eventId, host).body().get("shareToken").asString(), guest);
    //
    //

    assertThat(get("/profile/me", guest).body().get("reliability").get("broken").asInt()).isZero();
  }

  @Test
  void markedAsNotHavingCome__countsAgainst_andTheMarkCanBeTakenBack() {
    TestUser guest = register();
    TestUser host = register();
    long eventId = heldEvent(host, guest);

    //
    //
    Response marked = post("/event/" + eventId + "/guest/" + guest.id() + "/no-show", host);
    JsonNode afterMark = get("/profile/me", guest).body().get("reliability");
    Response unmarked = post("/event/" + eventId + "/guest/" + guest.id() + "/came", host);
    JsonNode afterUnmark = get("/profile/me", guest).body().get("reliability");
    //
    //

    assertThat(marked.status()).isEqualTo(200);
    assertThat(marked.body().get("invitees").get(0).get("noShow").asBoolean()).isTrue();
    // The mark is the creator's to see: the guest's own view of the event does not carry it.
    assertThat(get("/event/" + eventId, guest).body().get("invitees").get(0).get("noShow").asBoolean()).isFalse();
    assertThat(afterMark.get("kept").asInt()).isZero();
    assertThat(afterMark.get("broken").asInt()).isEqualTo(1);

    assertThat(unmarked.body().get("invitees").get(0).get("noShow").asBoolean()).isFalse();
    assertThat(afterUnmark.get("kept").asInt()).isEqualTo(1);
    assertThat(afterUnmark.get("broken").asInt()).isZero();
  }

  @Test
  void noShow__isOnlyForTheCreator_onlyAfterTheEvent_onlyOfSomeoneWhoWasComing() {
    TestUser host = register();
    TestUser coming = register();
    TestUser invited = register();
    TestUser stranger = register();
    makeFriends(host, coming);
    makeFriends(host, invited);
    JsonNode event = post("/event/create", host, Map.of(
        "eventType", "FOOD", "inviteeIds", List.of(coming.id(), invited.id()))).body();
    long eventId = event.get("id").asLong();
    post("/invitation/" + event.get("invitees").get(0).get("invitationId").asLong() + "/accept", coming);

    //
    //
    Response whileItIsOn = post("/event/" + eventId + "/guest/" + coming.id() + "/no-show", host);
    hold(eventId);
    Response byAGuest = post("/event/" + eventId + "/guest/" + coming.id() + "/no-show", invited);
    Response ofSomeoneNotComing = post("/event/" + eventId + "/guest/" + invited.id() + "/no-show", host);
    Response ofAStranger = post("/event/" + eventId + "/guest/" + stranger.id() + "/no-show", host);
    Response withoutToken = post("/event/" + eventId + "/guest/" + coming.id() + "/no-show", null);
    //
    //

    assertThat(whileItIsOn.status()).isEqualTo(409);
    assertThat(whileItIsOn.body().get("code").asString()).isEqualTo("event_not_over");
    assertThat(byAGuest.status()).isEqualTo(404);
    assertThat(ofSomeoneNotComing.status()).isEqualTo(404);
    assertThat(ofAStranger.status()).isEqualTo(404);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(get("/profile/me", coming).body().get("reliability").get("broken").asInt()).isZero();
  }

  @Test
  void theCreator__keepsAPromiseByHoldingTheEvent_andBreaksOneByCancellingWhatPeopleWereComingTo() {
    TestUser host = register();
    TestUser guest = register();
    heldEvent(host, guest);                                                     // held: kept
    heldEvent(host, guest);                                                     // held: kept
    long emptyOne = post("/event/create", host, Map.of("eventType", "WALK")).body().get("id").asLong();
    post("/event/" + emptyOne + "/cancel", host);                               // nobody was coming: nothing
    JsonNode awaited = post("/event/create", host, Map.of("eventType", "BAR", "inviteeIds", List.of(guest.id()))).body();
    post("/invitation/" + awaited.get("invitees").get(0).get("invitationId").asLong() + "/accept", guest);

    //
    //
    post("/event/" + awaited.get("id").asLong() + "/cancel", host);             // someone was coming: broken
    //
    //

    JsonNode reliability = get("/profile/me", host).body().get("reliability");
    assertThat(reliability.get("kept").asInt()).isEqualTo(2);
    assertThat(reliability.get("broken").asInt()).isEqualTo(1);
    assertThat(reliability.get("score").asInt()).isEqualTo(67);
  }

  @Test
  void theScore__isOnThePersonsPage_andOnTheirPublicPlanInTheFeed() {
    TestUser host = register();
    TestUser guest = register();
    TestUser reader = register();
    heldEvent(host, guest);
    heldEvent(host, guest);
    long third = heldEvent(host, guest);
    post("/event/" + third + "/guest/" + guest.id() + "/no-show", host);
    long publicEvent = post("/event/create", guest, Map.of("eventType", "COFFEE", "visibility", "PUBLIC"))
        .body().get("id").asLong();

    //
    //
    JsonNode onThePage = get("/profile/" + guest.id(), reader).body().get("reliability");
    JsonNode inTheFeed = get("/feed/list", reader).body().valueStream()
        .filter(item -> item.get("id").asLong() == publicEvent).findFirst().orElseThrow();
    //
    //

    assertThat(onThePage.get("score").asInt()).isEqualTo(67); // came to two of three
    assertThat(inTheFeed.get("creatorReliability").asInt()).isEqualTo(67);
  }

  /** An event of {@code host} that {@code guest} said "coming" to, and that has taken place. */
  private long heldEvent(TestUser host, TestUser guest) {
    if (get("/friend/list", host).body().valueStream().noneMatch(row -> row.get("userId").asLong() == guest.id())) {
      makeFriends(host, guest);
    }
    JsonNode event = post("/event/create", host, Map.of("eventType", "COFFEE", "inviteeIds", List.of(guest.id()))).body();
    assertThat(post("/invitation/" + event.get("invitees").get(0).get("invitationId").asLong() + "/accept", guest)
        .status()).isEqualTo(200);
    long eventId = event.get("id").asLong();
    hold(eventId);
    return eventId;
  }

  /** {@code guest} came to one more event of somebody. */
  private void comeTo(TestUser guest) {
    heldEvent(register(), guest);
  }

  /** The date of the event has passed. */
  private void hold(long eventId) {
    Event event = eventRepository.findById(eventId).orElseThrow();
    event.status = EventStatus.EXPIRED;
    eventRepository.save(event);
  }
}
