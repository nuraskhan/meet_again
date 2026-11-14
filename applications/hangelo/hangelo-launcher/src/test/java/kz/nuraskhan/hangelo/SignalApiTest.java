package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** The server pushing to a listening page: the stream is opened over real HTTP and read as it comes. */
class SignalApiTest extends ApiTestBase {

  @Test
  void stream__carriesANotificationTheMomentItHappens() {
    TestUser asker = register();
    TestUser asked = register();

    try (SignalStream stream = listen(asked)) {
      //
      //
      post("/friend/request", asker, Map.of("userId", asked.id()));
      //
      //

      Signal signal = stream.next();
      assertThat(signal.type()).isEqualTo("notification");
      assertThat(signal.data().get("kind").asString()).isEqualTo("FRIEND_REQUEST");
      assertThat(signal.data().get("text").asString())
          .isEqualTo(asker.displayName() + " wants to be your friend");
      assertThat(signal.data().get("actorId").asLong()).isEqualTo(asker.id());
      assertThat(signal.data().get("read").asBoolean()).isFalse();
      // The same thing the list gives: a page may show the signal as it is.
      assertThat(signal.data().get("id").asLong())
          .isEqualTo(get("/notification/list", asked).body().get(0).get("id").asLong());
    }
  }

  @Test
  void stream__carriesAChatMessageToTheOtherMembers_andNotToTheAuthor() {
    TestUser creator = register();
    TestUser friend = register();
    makeFriends(creator, friend);
    JsonNode event = post("/event/create", creator,
        Map.of("eventType", "COFFEE", "inviteeIds", List.of(friend.id()))).body();
    long chatId = event.get("chatId").asLong();

    try (SignalStream authorStream = listen(creator); SignalStream friendStream = listen(friend)) {
      //
      //
      Response posted = post("/chat/" + chatId + "/message", creator, Map.of("text", "Who is in?"));
      //
      //

      Signal signal = friendStream.next();
      assertThat(signal.type()).isEqualTo("chat.message");
      assertThat(signal.data().get("chatId").asLong()).isEqualTo(chatId);
      assertThat(signal.data().get("message").get("id").asLong())
          .isEqualTo(posted.body().get("id").asLong());
      assertThat(signal.data().get("message").get("text").asString()).isEqualTo("Who is in?");
      assertThat(signal.data().get("message").get("authorId").asLong()).isEqualTo(creator.id());
      assertThat(authorStream.staysSilent()).isTrue();
    }
  }

  @Test
  void stream__reachesEveryOpenPageOfThePerson_andNobodyElse() {
    TestUser asker = register();
    TestUser asked = register();
    TestUser bystander = register();

    try (SignalStream tab1 = listen(asked); SignalStream tab2 = listen(asked);
         SignalStream other = listen(bystander)) {
      //
      //
      post("/friend/request", asker, Map.of("userId", asked.id()));
      //
      //

      assertThat(tab1.next().type()).isEqualTo("notification");
      assertThat(tab2.next().type()).isEqualTo("notification");
      assertThat(other.staysSilent()).isTrue();
    }
  }

  @Test
  void stream__failedAction_signalsNothing() {
    TestUser asker = register();
    TestUser asked = register();
    post("/friend/request", asker, Map.of("userId", asked.id()));

    try (SignalStream stream = listen(asked)) {
      //
      //
      Response again = post("/friend/request", asker, Map.of("userId", asked.id()));
      //
      //

      assertThat(again.status()).isEqualTo(409);
      assertThat(stream.staysSilent()).isTrue();
    }
  }

  @Test
  void stream__needsAToken() {
    //
    //
    Response response = get("/signal/stream", null);
    //
    //

    assertThat(response.status()).isEqualTo(401);
  }
}
