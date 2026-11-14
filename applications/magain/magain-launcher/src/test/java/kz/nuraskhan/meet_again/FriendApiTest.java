package kz.nuraskhan.meet_again;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FriendApiTest extends ApiTestBase {

  @Test
  void sendRequest__appearsAsIncomingForTheOtherAndOutgoingForTheSender() {
    TestUser sender = register();
    TestUser addressee = register();

    //
    //
    Response response = post("/friend/request", sender, Map.of("userId", addressee.id()));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    long requestId = response.body().get("requestId").asLong();

    Response incoming = get("/friend/request/incoming", addressee);
    assertThat(longs(incoming.body(), "requestId")).containsExactly(requestId);
    assertThat(longs(incoming.body(), "userId")).containsExactly(sender.id());

    Response outgoing = get("/friend/request/outgoing", sender);
    assertThat(longs(outgoing.body(), "requestId")).containsExactly(requestId);
    assertThat(longs(outgoing.body(), "userId")).containsExactly(addressee.id());

    // Not friends until it is accepted.
    assertThat(get("/friend/list", sender).body().size()).isZero();
  }

  @Test
  void sendRequest__refusesSecondRequestBetweenTheSameTwo_inEitherDirection() {
    TestUser a = register();
    TestUser b = register();
    post("/friend/request", a, Map.of("userId", b.id()));

    //
    //
    Response response = post("/friend/request", b, Map.of("userId", a.id()));
    //
    //

    assertThat(response.status()).isEqualTo(409);
    assertThat(response.body().get("code").asString()).isEqualTo("request_exists");
  }

  @Test
  void sendRequest__refusesYourselfAndUnknownUser() {
    TestUser user = register();

    //
    //
    Response toSelf = post("/friend/request", user, Map.of("userId", user.id()));
    Response toNobody = post("/friend/request", user, Map.of("userId", Long.MAX_VALUE));
    //
    //

    assertThat(toSelf.status()).isEqualTo(400);
    assertThat(toNobody.status()).isEqualTo(404);
  }

  @Test
  void accept__makesBothSeeEachOtherAsFriends() {
    TestUser sender = register();
    TestUser addressee = register();
    long requestId = post("/friend/request", sender, Map.of("userId", addressee.id()))
        .body().get("requestId").asLong();

    //
    //
    Response response = post("/friend/request/" + requestId + "/accept", addressee);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(longs(get("/friend/list", sender).body(), "userId")).containsExactly(addressee.id());
    assertThat(longs(get("/friend/list", addressee).body(), "userId")).containsExactly(sender.id());
    assertThat(get("/friend/request/incoming", addressee).body().size()).isZero();
  }

  @Test
  void accept__isOnlyForTheAddressee() {
    TestUser sender = register();
    TestUser addressee = register();
    TestUser stranger = register();
    long requestId = post("/friend/request", sender, Map.of("userId", addressee.id()))
        .body().get("requestId").asLong();

    //
    //
    Response bySender = post("/friend/request/" + requestId + "/accept", sender);
    Response byStranger = post("/friend/request/" + requestId + "/accept", stranger);
    //
    //

    assertThat(bySender.status()).isEqualTo(404);
    assertThat(byStranger.status()).isEqualTo(404);
    assertThat(get("/friend/list", sender).body().size()).isZero();
  }

  @Test
  void decline__leavesThemNotFriends_andAllowsToAskAgain() {
    TestUser sender = register();
    TestUser addressee = register();
    long requestId = post("/friend/request", sender, Map.of("userId", addressee.id()))
        .body().get("requestId").asLong();

    //
    //
    Response response = post("/friend/request/" + requestId + "/decline", addressee);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(get("/friend/list", sender).body().size()).isZero();
    assertThat(post("/friend/request", sender, Map.of("userId", addressee.id())).status())
        .isEqualTo(200);
  }

  @Test
  void remove__endsTheFriendshipForBoth() {
    TestUser a = register();
    TestUser b = register();
    makeFriends(a, b);

    //
    //
    Response response = post("/friend/" + a.id() + "/remove", b);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(get("/friend/list", a).body().size()).isZero();
    assertThat(get("/friend/list", b).body().size()).isZero();
  }
}
