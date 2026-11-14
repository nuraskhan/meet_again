package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** The list of chats: the newest message of each, how many are new for the reader, the order. */
class ChatListApiTest extends ApiTestBase {

  @Test
  void list__carriesTheNewestMessage_andCountsWhatOthersWroteAsUnread() {
    TestUser creator = register();
    TestUser friend = register();
    long chatId = chatOf(creator, friend, "Coffee");
    post("/chat/" + chatId + "/message", creator, Map.of("text", "Hi"));

    //
    //
    Response posted = post("/chat/" + chatId + "/message", creator, Map.of("text", "Hi, how are you?"));
    //
    //

    JsonNode forFriend = get("/chat/list", friend).body().get(0);
    assertThat(forFriend.get("unreadCount").asInt()).isEqualTo(2);
    assertThat(forFriend.get("lastMessage").get("id").asLong()).isEqualTo(posted.body().get("id").asLong());
    assertThat(forFriend.get("lastMessage").get("text").asString()).isEqualTo("Hi, how are you?");
    assertThat(forFriend.get("lastMessage").get("authorId").asLong()).isEqualTo(creator.id());
    assertThat(forFriend.get("lastMessage").get("authorName").asString()).isEqualTo(creator.displayName());
    assertThat(forFriend.get("lastMessage").get("createdAt").asString())
        .isEqualTo(posted.body().get("createdAt").asString());

    // One's own messages are never new to oneself.
    JsonNode forCreator = get("/chat/list", creator).body().get(0);
    assertThat(forCreator.get("unreadCount").asInt()).isZero();
    assertThat(forCreator.get("lastMessage").get("text").asString()).isEqualTo("Hi, how are you?");
  }

  @Test
  void list__ofAChatNobodyWroteIn_hasNoLastMessage() {
    TestUser creator = register();
    TestUser friend = register();
    chatOf(creator, friend, "Silent");

    //
    //
    JsonNode chat = get("/chat/list", friend).body().get(0);
    //
    //

    assertThat(chat.get("lastMessage").isNull()).isTrue();
    assertThat(chat.get("unreadCount").asInt()).isZero();
  }

  @Test
  void list__isOrderedByTheNewestMessage_notByWhenTheChatWasMade() {
    TestUser me = register();
    TestUser friend = register();
    long older = chatOf(me, friend, "Older chat");
    long newer = chatOf(me, friend, "Newer chat");
    long silent = chatOf(me, friend, "Newest chat, silent");

    //
    //
    List<Long> beforeAnyMessage = longs(get("/chat/list", me).body(), "id");
    post("/chat/" + newer + "/message", friend, Map.of("text", "First"));
    post("/chat/" + older + "/message", friend, Map.of("text", "Second"));
    List<Long> afterMessages = longs(get("/chat/list", me).body(), "id");
    //
    //

    assertThat(beforeAnyMessage).containsExactly(silent, newer, older);
    // The one written in last is on top; the silent one stands by the moment it was made, which
    // is before both messages.
    assertThat(afterMessages).containsExactly(older, newer, silent);
  }

  @Test
  void markRead__zeroesTheCount_andWhatComesAfterIsNewAgain() {
    TestUser creator = register();
    TestUser friend = register();
    long chatId = chatOf(creator, friend, "Walk");
    post("/chat/" + chatId + "/message", creator, Map.of("text", "One"));
    post("/chat/" + chatId + "/message", creator, Map.of("text", "Two"));

    //
    //
    Response response = post("/chat/" + chatId + "/read", friend);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(get("/chat/list", friend).body().get(0).get("unreadCount").asInt()).isZero();

    post("/chat/" + chatId + "/message", creator, Map.of("text", "Three"));
    assertThat(get("/chat/list", friend).body().get(0).get("unreadCount").asInt()).isEqualTo(1);
  }

  @Test
  void writing__marksWhatStoodAboveAsRead() {
    TestUser creator = register();
    TestUser friend = register();
    long chatId = chatOf(creator, friend, "Bar");
    post("/chat/" + chatId + "/message", creator, Map.of("text", "Are you coming?"));

    //
    //
    post("/chat/" + chatId + "/message", friend, Map.of("text", "Yes"));
    //
    //

    assertThat(get("/chat/list", friend).body().get(0).get("unreadCount").asInt()).isZero();
    assertThat(get("/chat/list", creator).body().get(0).get("unreadCount").asInt()).isEqualTo(1);
  }

  @Test
  void aNewcomer__findsNothingUnread_whateverWasWrittenBeforeTheyCame() {
    TestUser creator = register();
    TestUser newcomer = register();
    JsonNode event = post("/event/create", creator, Map.of("eventType", "WALK", "title", "Open walk")).body();
    long chatId = event.get("chatId").asLong();
    post("/chat/" + chatId + "/message", creator, Map.of("text", "Written before anyone came"));

    //
    //
    post("/event/join/" + event.get("shareToken").asString(), newcomer);
    //
    //

    JsonNode chat = get("/chat/list", newcomer).body().get(0);
    assertThat(chat.get("unreadCount").asInt()).isZero();
    // History is still there to read.
    assertThat(chat.get("lastMessage").get("text").asString()).isEqualTo("Written before anyone came");

    post("/chat/" + chatId + "/message", creator, Map.of("text", "Welcome"));
    assertThat(get("/chat/list", newcomer).body().get(0).get("unreadCount").asInt()).isEqualTo(1);
  }

  @Test
  void markRead__isOnlyForAMember() {
    TestUser creator = register();
    TestUser friend = register();
    TestUser stranger = register();
    long chatId = chatOf(creator, friend, "Private");
    post("/chat/" + chatId + "/message", creator, Map.of("text", "Hello"));

    //
    //
    Response byStranger = post("/chat/" + chatId + "/read", stranger);
    Response withoutToken = post("/chat/" + chatId + "/read", null);
    //
    //

    assertThat(byStranger.status()).isEqualTo(404);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(get("/chat/list", friend).body().get(0).get("unreadCount").asInt()).isEqualTo(1);
  }

  /** An event of {@code creator} with {@code friend} invited and coming. @return the id of its chat */
  private long chatOf(TestUser creator, TestUser friend, String title) {
    if (get("/friend/list", creator).body().valueStream()
        .noneMatch(row -> row.get("userId").asLong() == friend.id())) {
      makeFriends(creator, friend);
    }
    JsonNode event = post("/event/create", creator,
        Map.of("eventType", "COFFEE", "title", title, "inviteeIds", List.of(friend.id()))).body();
    assertThat(event.get("chatId")).isNotNull();
    return event.get("chatId").asLong();
  }
}
