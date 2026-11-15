package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import kz.nuraskhan.hangelo.model.entity.Profile;
import kz.nuraskhan.hangelo.model.enums.Privilege;
import kz.nuraskhan.hangelo.model.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/** "Upgrade to Plus": taking the Plus plan and giving it back. A demonstration, no payment. */
class PlusApiTest extends ApiTestBase {

  @Autowired ProfileRepository profileRepository;

  @Test
  void startPlus__movesTheFreePlanToPlus_andItStays() {
    TestUser user = register();
    assertThat(get("/profile/me", user).body().get("privilege").asString()).isEqualTo("STANDARD");

    //
    //
    Response response = post("/profile/plus/start", user);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("privilege").asString()).isEqualTo("PLUS");
    assertThat(response.body().get("maxPhotos").asInt()).isEqualTo(6);
    // Kept in the database, not only in the answer.
    assertThat(profileRepository.findById(user.id()).orElseThrow().privilege).isEqualTo(Privilege.PLUS);
    assertThat(get("/profile/me", user).body().get("privilege").asString()).isEqualTo("PLUS");
    // Others see it too.
    JsonNode seen = get("/profile/" + user.id(), register()).body();
    assertThat(seen.get("privilege").asString()).isEqualTo("PLUS");
  }

  @Test
  void startPlus__twice_changesNothing_andCancelGoesBackToTheFreePlan() {
    TestUser user = register();
    post("/profile/plus/start", user);

    //
    //
    Response again = post("/profile/plus/start", user);
    Response cancelled = post("/profile/plus/cancel", user);
    Response cancelledAgain = post("/profile/plus/cancel", user);
    //
    //

    assertThat(again.body().get("privilege").asString()).isEqualTo("PLUS");
    assertThat(cancelled.body().get("privilege").asString()).isEqualTo("STANDARD");
    assertThat(cancelledAgain.body().get("privilege").asString()).isEqualTo("STANDARD");
  }

  @Test
  void startPlus__neverLowersAHigherLevel_andCancelDoesNotDropOne() {
    TestUser user = register();
    Profile profile = profileRepository.findById(user.id()).orElseThrow();
    profile.privilege = Privilege.MAX;
    profileRepository.save(profile);

    //
    //
    Response started = post("/profile/plus/start", user);
    Response cancelled = post("/profile/plus/cancel", user);
    //
    //

    assertThat(started.body().get("privilege").asString()).isEqualTo("MAX");
    assertThat(cancelled.body().get("privilege").asString()).isEqualTo("MAX");
  }

  @Test
  void startPlus__needsAToken_andChangesOnlyTheCaller() {
    TestUser user = register();
    TestUser other = register();

    //
    //
    Response withoutToken = post("/profile/plus/start", null);
    post("/profile/plus/start", user);
    //
    //

    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(get("/profile/me", other).body().get("privilege").asString()).isEqualTo("STANDARD");
  }
}
