package kz.nuraskhan.meet_again;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.meet_again.model.rnd.RND;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** What a person says about themselves, and what the others see of it. */
class ProfileApiTest extends ApiTestBase {

  @Test
  void aNewProfile__hasTheNameFromRegistration_theStandardPrivilege_andNothingElse() {
    //
    //
    TestUser user = register();
    //
    //

    JsonNode me = get("/profile/me", user).body();
    assertThat(me.get("displayName").asString()).isEqualTo(user.displayName());
    assertThat(me.get("privilege").asString()).isEqualTo("STANDARD");
    assertThat(me.get("maxPhotos").asInt()).isEqualTo(3);
    assertThat(me.get("photos").size()).isZero();
    assertThat(me.get("interests").size()).isZero();
    assertThat(me.get("prompts").size()).isZero();
    assertThat(me.get("age").isNull()).isTrue();
    assertThat(me.get("bio").isNull()).isTrue();
  }

  @Test
  void updateMe__savesEverythingThePersonSaysAboutThemselves() {
    TestUser user = register();
    TestUser other = register();

    //
    //
    Response updated = post("/profile/me", user, Map.ofEntries(
        Map.entry("displayName", "Dauren"),
        Map.entry("birthDate", "1995-03-01"),
        Map.entry("pronouns", "he/him"),
        Map.entry("city", "Brooklyn"),
        Map.entry("hometown", "Almaty"),
        Map.entry("occupation", "Carpenter"),
        Map.entry("education", "Trade school"),
        Map.entry("languages", "Kazakh, Russian, English"),
        Map.entry("bio", "Likes bathhouses"),
        Map.entry("interests", List.of("banya", "chess", "hiking")),
        Map.entry("prompts", List.of(
            Map.of("question", "My perfect weekend", "answer", "A hike, then a nap."),
            Map.of("question", "Ask me about", "answer", "Dovetail joints.")))));
    //
    //

    assertThat(updated.status()).isEqualTo(200);
    // The owner gets the date of birth back as entered: a date, it must not shift with a time zone.
    assertThat(updated.body().get("birthDate").asString()).isEqualTo("1995-03-01");

    JsonNode seen = get("/profile/" + user.id(), other).body();
    assertThat(seen.get("displayName").asString()).isEqualTo("Dauren");
    assertThat(seen.get("pronouns").asString()).isEqualTo("he/him");
    assertThat(seen.get("city").asString()).isEqualTo("Brooklyn");
    assertThat(seen.get("hometown").asString()).isEqualTo("Almaty");
    assertThat(seen.get("occupation").asString()).isEqualTo("Carpenter");
    assertThat(seen.get("education").asString()).isEqualTo("Trade school");
    assertThat(seen.get("languages").asString()).isEqualTo("Kazakh, Russian, English");
    assertThat(seen.get("bio").asString()).isEqualTo("Likes bathhouses");
    assertThat(strings(seen.get("interests"))).containsExactly("banya", "chess", "hiking");
    assertThat(seen.get("prompts").get(0).get("question").asString()).isEqualTo("My perfect weekend");
    assertThat(seen.get("prompts").get(0).get("answer").asString()).isEqualTo("A hike, then a nap.");
    assertThat(seen.get("prompts").get(1).get("question").asString()).isEqualTo("Ask me about");
    assertThat(seen.get("privilege").asString()).isEqualTo("STANDARD");
  }

  @Test
  void othersSeeTheAge_neverTheDateOfBirthOrTheEmail() {
    TestUser user = register();
    TestUser other = register();
    // Thirty years and about half a year ago: thirty in every time zone, far from any birthday.
    LocalDate birthDate = LocalDate.now(ZoneOffset.UTC).minusYears(30).minusMonths(6);
    post("/profile/me", user, Map.of("displayName", "Aged", "birthDate", birthDate.toString()));

    //
    //
    Response response = get("/profile/" + user.id(), other);
    //
    //

    assertThat(response.body().get("age").asInt()).isEqualTo(30);
    assertThat(response.body().has("birthDate")).isFalse();
    assertThat(response.body().has("email")).isFalse();
    assertThat(response.body().toString()).doesNotContain(birthDate.toString()).doesNotContain(user.email());
  }

  @Test
  void updateMe__replacesTheWhole_whatIsLeftOutBecomesEmpty() {
    TestUser user = register();
    post("/profile/me", user, Map.of(
        "displayName", "First",
        "bio", "To be removed",
        "interests", List.of("one", "two"),
        "prompts", List.of(Map.of("question", "Q", "answer", "A"))));

    //
    //
    Response response = post("/profile/me", user, Map.of("displayName", "Second", "interests", List.of("three")));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("displayName").asString()).isEqualTo("Second");
    assertThat(response.body().get("bio").isNull()).isTrue();
    assertThat(strings(response.body().get("interests"))).containsExactly("three");
    assertThat(response.body().get("prompts").size()).isZero();
  }

  @Test
  void updateMe__keepsAnInterestOnce_whateverTheLetterCase() {
    TestUser user = register();

    //
    //
    Response response = post("/profile/me", user,
        Map.of("displayName", "Tidy", "interests", List.of(" Chess ", "chess", "CHESS", "go")));
    //
    //

    assertThat(strings(response.body().get("interests"))).containsExactly("Chess", "go");
  }

  @Test
  void updateMe__refusesTooMuchAndTooLong() {
    TestUser user = register();
    List<String> elevenInterests = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
    List<Map<String, String>> fourPrompts = List.of(
        Map.of("question", "Q1", "answer", "A"), Map.of("question", "Q2", "answer", "A"),
        Map.of("question", "Q3", "answer", "A"), Map.of("question", "Q4", "answer", "A"));

    //
    //
    Response tooManyInterests = post("/profile/me", user, Map.of("displayName", "X", "interests", elevenInterests));
    Response tooLongInterest = post("/profile/me", user,
        Map.of("displayName", "X", "interests", List.of("a".repeat(31))));
    Response tooManyPrompts = post("/profile/me", user, Map.of("displayName", "X", "prompts", fourPrompts));
    Response promptWithoutAnswer = post("/profile/me", user,
        Map.of("displayName", "X", "prompts", List.of(Map.of("question", "Q", "answer", " "))));
    Response birthInTheFuture = post("/profile/me", user, Map.of("displayName", "X", "birthDate", "2999-01-01"));
    Response noName = post("/profile/me", user, Map.of("displayName", " "));
    //
    //

    for (Response refused : List.of(tooManyInterests, tooLongInterest, tooManyPrompts, promptWithoutAnswer,
        birthInTheFuture, noName)) {
      assertThat(refused.status()).isEqualTo(400);
      assertThat(refused.body().get("code").asString()).isEqualTo("invalid_request");
    }
  }

  @Test
  void updateMe__cannotRaiseOnesOwnPrivilege_norSetAPhotoAddress() {
    TestUser user = register();
    Map<String, Object> body = new HashMap<>();
    body.put("displayName", "Sneaky");
    body.put("privilege", "MAX");
    body.put("maxPhotos", 99);
    body.put("avatarUrl", "https://tracker.example.test/pixel.png");
    body.put("photos", List.of(Map.of("id", 1, "url", "https://tracker.example.test/pixel.png")));

    //
    //
    Response response = post("/profile/me", user, body);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("privilege").asString()).isEqualTo("STANDARD");
    assertThat(response.body().get("maxPhotos").asInt()).isEqualTo(3);
    assertThat(response.body().get("avatarUrl").isNull()).isTrue();
    assertThat(response.body().get("photos").size()).isZero();
  }

  @Test
  void byId__answersNotFoundForUnknownUser() {
    TestUser user = register();

    //
    //
    Response response = get("/profile/" + Long.MAX_VALUE, user);
    //
    //

    assertThat(response.status()).isEqualTo(404);
    assertThat(response.body().get("code").asString()).isEqualTo("profile_not_found");
  }

  @Test
  void search__findsByPartOfNameIgnoringCase_andNeverReturnsTheCaller() {
    String marker = RND.str(12);
    TestUser me = register();
    TestUser wanted = register();
    post("/profile/me", me, Map.of("displayName", "Me " + marker));
    post("/profile/me", wanted, Map.of("displayName", "Wanted " + marker, "city", "Queens"));

    //
    //
    Response response = get("/profile/search?text=" + marker.toUpperCase(), me);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(longs(response.body(), "userId")).containsExactly(wanted.id());
    // A card for a list: the name and the city, not the whole profile.
    assertThat(response.body().get(0).get("city").asString()).isEqualTo("Queens");
    assertThat(response.body().get(0).has("bio")).isFalse();
  }

  @Test
  void search__findsByExactEmail() {
    TestUser me = register();
    TestUser wanted = register();

    //
    //
    Response response = get("/profile/search?text=" + wanted.email(), me);
    //
    //

    assertThat(longs(response.body(), "userId")).containsExactly(wanted.id());
  }

  private static List<String> strings(JsonNode array) {
    return array.valueStream().map(JsonNode::asString).toList();
  }
}
