package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.model.entity.Profile;
import kz.nuraskhan.hangelo.model.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/** Answering a message in a chat, and the tag by a person's name that is set in the database. */
class ChatReplyAndTagApiTest extends ApiTestBase {

  @Autowired ProfileRepository profileRepository;

  // ------------------------------------------------------------------ answering a message

  @Test
  void postMessage__withReplyTo_carriesTheAnsweredMessageInShort_toTheAuthorTheListAndTheSignal() {
    TestUser creator = register();
    TestUser friend = register();
    long chatId = chatOf(creator, friend);
    long questionId = post("/chat/" + chatId + "/message", creator, Map.of("text", "Who brings the snacks?"))
        .body().get("id").asLong();

    try (SignalStream creatorPage = listen(creator)) {
      //
      //
      Response answer = post("/chat/" + chatId + "/message", friend,
          Map.of("text", "Me. Again.", "replyToId", questionId));
      //
      //

      assertThat(answer.status()).isEqualTo(200);
      JsonNode replyTo = answer.body().get("replyTo");
      assertThat(replyTo.get("id").asLong()).isEqualTo(questionId);
      assertThat(replyTo.get("authorId").asLong()).isEqualTo(creator.id());
      assertThat(replyTo.get("authorName").asString()).isEqualTo(creator.displayName());
      assertThat(replyTo.get("text").asString()).isEqualTo("Who brings the snacks?");

      // The list, newest first: the answer with what it answers, the question answering nothing.
      JsonNode list = get("/chat/" + chatId + "/message/list", creator).body();
      assertThat(list.get(0).get("replyTo").get("id").asLong()).isEqualTo(questionId);
      assertThat(list.get(1).get("replyTo").isNull()).isTrue();

      // The other person's open chat gets the same thing pushed.
      Signal signal = nextOfType(creatorPage, "chat.message");
      assertThat(signal.data().get("message").get("replyTo").get("text").asString())
          .isEqualTo("Who brings the snacks?");
    }
  }

  @Test
  void postMessage__cutsALongAnsweredMessage_toItsBeginning() {
    TestUser creator = register();
    TestUser friend = register();
    long chatId = chatOf(creator, friend);
    long longId = post("/chat/" + chatId + "/message", creator, Map.of("text", "a".repeat(500)))
        .body().get("id").asLong();

    //
    //
    Response answer = post("/chat/" + chatId + "/message", friend, Map.of("text", "ok", "replyToId", longId));
    //
    //

    assertThat(answer.body().get("replyTo").get("text").asString()).isEqualTo("a".repeat(140) + "…");
  }

  @Test
  void postMessage__refusesAnAnswerToAMessageOfAnotherChat_orToNoMessage() {
    TestUser creator = register();
    TestUser friend = register();
    long chatId = chatOf(creator, friend);
    long otherChatId = chatOf(creator, friend);
    long elsewhereId = post("/chat/" + otherChatId + "/message", creator, Map.of("text", "elsewhere"))
        .body().get("id").asLong();

    //
    //
    Response toAnotherChat = post("/chat/" + chatId + "/message", friend,
        Map.of("text", "?", "replyToId", elsewhereId));
    Response toNothing = post("/chat/" + chatId + "/message", friend,
        Map.of("text", "?", "replyToId", Long.MAX_VALUE));
    //
    //

    assertThat(toAnotherChat.status()).isEqualTo(400);
    assertThat(toAnotherChat.body().get("code").asString()).isEqualTo("reply_not_found");
    assertThat(toNothing.status()).isEqualTo(400);
    // Nothing was written.
    assertThat(get("/chat/" + chatId + "/message/list", creator).body().size()).isZero();
  }

  // ------------------------------------------------------------------ the tag

  @Test
  void tag__setInTheDatabase_isShownWithTheNameEverywhere() {
    TestUser tagged = register();
    TestUser friend = register();
    long chatId = chatOf(tagged, friend);
    assertThat(get("/profile/me", tagged).body().get("tag").isNull()).isTrue();

    //
    //
    tag(tagged, "Team");
    //
    //

    assertThat(get("/profile/me", tagged).body().get("tag").asString()).isEqualTo("Team");
    assertThat(get("/profile/" + tagged.id(), friend).body().get("tag").asString()).isEqualTo("Team");
    assertThat(get("/friend/list", friend).body().get(0).get("tag").asString()).isEqualTo("Team");
    JsonNode members = get("/chat/" + chatId + "/member/list", friend).body();
    assertThat(members.valueStream()
        .filter(member -> member.get("userId").asLong() == tagged.id())
        .findFirst().orElseThrow().get("tag").asString()).isEqualTo("Team");
    // Whoever has no tag has none.
    assertThat(get("/friend/list", tagged).body().get(0).get("tag").isNull()).isTrue();
  }

  @Test
  void tag__cannotBeSetByThePersonThemselves() {
    TestUser user = register();
    JsonNode profile = get("/profile/me", user).body();

    //
    //
    Response update = post("/profile/me", user,
        Map.of("displayName", profile.get("displayName").asString(), "tag", "Founder"));
    //
    //

    assertThat(update.status()).isEqualTo(200);
    assertThat(get("/profile/me", user).body().get("tag").isNull()).isTrue();
  }

  private void tag(TestUser user, String tag) {
    Profile profile = profileRepository.findById(user.id()).orElseThrow();
    profile.tag = tag;
    profileRepository.save(profile);
  }

  /** The first signal of this type; others (a notification about the invitation) are passed over. */
  private Signal nextOfType(SignalStream stream, String type) {
    for (int i = 0; i < 10; i++) {
      Signal signal = stream.next();
      if (signal.type().equals(type)) {
        return signal;
      }
    }
    throw new AssertionError("No signal of type " + type);
  }

  /** An event of {@code creator} with {@code friend} invited. @return the id of its chat */
  private long chatOf(TestUser creator, TestUser friend) {
    if (get("/friend/list", creator).body().valueStream()
        .noneMatch(row -> row.get("userId").asLong() == friend.id())) {
      makeFriends(creator, friend);
    }
    JsonNode event = post("/event/create", creator,
        Map.of("eventType", "COFFEE", "title", "Coffee", "inviteeIds", List.of(friend.id()))).body();
    assertThat(event.get("chatId")).isNotNull();
    return event.get("chatId").asLong();
  }
}
