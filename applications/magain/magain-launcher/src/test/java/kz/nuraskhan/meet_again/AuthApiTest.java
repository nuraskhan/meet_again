package kz.nuraskhan.meet_again;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import kz.nuraskhan.meet_again.model.rnd.RND;
import org.junit.jupiter.api.Test;

class AuthApiTest extends ApiTestBase {

  @Test
  void register__returnsTokenThatOpensOwnProfile() {
    String email = RND.email();

    //
    //
    Response registered = post("/auth/register", null,
        Map.of("email", email, "password", "secret1", "displayName", "Aigerim"));
    //
    //

    assertThat(registered.status()).isEqualTo(200);
    TestUser user = new TestUser(registered.body().get("userId").asLong(),
        registered.body().get("token").asString(), email, "secret1", "Aigerim");

    Response me = get("/profile/me", user);
    assertThat(me.status()).isEqualTo(200);
    assertThat(me.body().get("userId").asLong()).isEqualTo(user.id());
    assertThat(me.body().get("email").asString()).isEqualTo(email);
    assertThat(me.body().get("displayName").asString()).isEqualTo("Aigerim");
  }

  @Test
  void register__refusesEmailThatIsTaken_inAnyLetterCase() {
    TestUser existing = register();

    //
    //
    Response response = post("/auth/register", null, Map.of(
        "email", existing.email().toUpperCase(), "password", "secret1", "displayName", "Twin"));
    //
    //

    assertThat(response.status()).isEqualTo(409);
    assertThat(response.body().get("code").asString()).isEqualTo("email_taken");
  }

  @Test
  void register__refusesShortPassword() {
    //
    //
    Response response = post("/auth/register", null,
        Map.of("email", RND.email(), "password", "12345", "displayName", "Shorty"));
    //
    //

    assertThat(response.status()).isEqualTo(400);
    assertThat(response.body().get("code").asString()).isEqualTo("invalid_request");
    assertThat(response.body().get("message").asString()).contains("password");
  }

  @Test
  void login__returnsTokenForRightPassword() {
    TestUser user = register();

    //
    //
    Response response = post("/auth/login", null,
        Map.of("email", user.email(), "password", user.password()));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("userId").asLong()).isEqualTo(user.id());

    TestUser loggedIn = new TestUser(user.id(), response.body().get("token").asString(),
        user.email(), user.password(), user.displayName());
    assertThat(get("/profile/me", loggedIn).status()).isEqualTo(200);
  }

  @Test
  void login__refusesWrongPassword() {
    TestUser user = register();

    //
    //
    Response response = post("/auth/login", null,
        Map.of("email", user.email(), "password", user.password() + "x"));
    //
    //

    assertThat(response.status()).isEqualTo(401);
    assertThat(response.body().get("code").asString()).isEqualTo("bad_credentials");
  }

  @Test
  void anyOtherEndpoint__refusesRequestWithoutToken() {
    //
    //
    Response response = get("/profile/me", null);
    //
    //

    assertThat(response.status()).isEqualTo(401);
  }

  @Test
  void anyOtherEndpoint__refusesForgedToken() {
    TestUser user = register();
    TestUser forged = new TestUser(user.id(), user.token() + "x", user.email(), user.password(),
        user.displayName());

    //
    //
    Response response = get("/profile/me", forged);
    //
    //

    assertThat(response.status()).isEqualTo(401);
  }

  @Test
  void health__isOpenWithoutToken() {
    //
    //
    Response response = get("/actuator/health", null);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("status").asString()).isEqualTo("UP");
  }
}
