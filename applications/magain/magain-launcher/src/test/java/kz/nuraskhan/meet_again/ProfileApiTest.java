package kz.nuraskhan.meet_again;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import kz.nuraskhan.meet_again.model.rnd.RND;
import org.junit.jupiter.api.Test;

class ProfileApiTest extends ApiTestBase {

  @Test
  void updateMe__changesWhatOthersSee() {
    TestUser user = register();
    TestUser other = register();

    //
    //
    Response updated = post("/profile/me", user, Map.of(
        "displayName", "Dauren",
        "bio", "Likes bathhouses",
        "city", "Almaty",
        "birthDate", "1995-03-01"));
    //
    //

    assertThat(updated.status()).isEqualTo(200);

    Response seen = get("/profile/" + user.id(), other);
    assertThat(seen.status()).isEqualTo(200);
    assertThat(seen.body().get("displayName").asString()).isEqualTo("Dauren");
    assertThat(seen.body().get("bio").asString()).isEqualTo("Likes bathhouses");
    assertThat(seen.body().get("city").asString()).isEqualTo("Almaty");
    // A date of birth is a date, not a moment: it must not shift with the time zone.
    assertThat(seen.body().get("birthDate").asString()).isEqualTo("1995-03-01");
    assertThat(seen.body().has("email")).isFalse();
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
    post("/profile/me", wanted, Map.of("displayName", "Wanted " + marker));

    //
    //
    Response response = get("/profile/search?text=" + marker.toUpperCase(), me);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(longs(response.body(), "userId")).containsExactly(wanted.id());
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
}
