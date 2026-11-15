package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import kz.nuraskhan.hangelo.model.rnd.RND;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Signing in as a demonstration account by a link, and the figures of visits for admins. */
class DemoAndAnalyticsApiTest extends ApiTestBase {

  // ------------------------------------------------------------------ demo login

  @Test
  void demoLogin__signsInAsTheDemoAccount_withoutAPassword_andTheTwoAreFriends() {
    //
    //
    Response jobs = post("/auth/demo/steve-jobs", null);
    Response wozniak = post("/auth/demo/steve-wozniak", null);
    //
    //

    assertThat(jobs.status()).isEqualTo(200);
    assertThat(wozniak.status()).isEqualTo(200);
    TestUser steve = demo(jobs);
    TestUser woz = demo(wozniak);

    JsonNode profile = get("/profile/me", steve).body();
    assertThat(profile.get("displayName").asString()).isEqualTo("Steve Jobs");
    assertThat(profile.get("email").asString()).isEqualTo("steve.jobs@hangelo.demo");
    assertThat(profile.get("emailVerified").asBoolean()).isTrue(); // no banner about a letter nobody gets
    assertThat(get("/profile/me", woz).body().get("displayName").asString()).isEqualTo("Steve Wozniak");

    // Friends from the first moment: one can invite the other at once.
    assertThat(longs(get("/friend/list", steve).body(), "userId")).contains(woz.id());
    assertThat(longs(get("/friend/list", woz).body(), "userId")).contains(steve.id());
  }

  @Test
  void demoLogin__again_isTheSameAccount_notANewOne() {
    long first = post("/auth/demo/steve-wozniak", null).body().get("userId").asLong();

    //
    //
    Response again = post("/auth/demo/steve-wozniak", null);
    //
    //

    assertThat(again.body().get("userId").asLong()).isEqualTo(first);
    // Still one friendship between the two, not one more per login.
    TestUser woz = demo(again);
    long jobsId = post("/auth/demo/steve-jobs", null).body().get("userId").asLong();
    assertThat(longs(get("/friend/list", woz).body(), "userId").stream().filter(id -> id == jobsId).count())
        .isEqualTo(1);
  }

  @Test
  void demoLogin__isOnlyForTheTwoAccounts_andNobodyGetsInWithAPassword() {
    TestUser real = register();

    //
    //
    Response unknown = post("/auth/demo/elon-musk", null);
    Response aRealPerson = post("/auth/demo/" + real.email(), null);
    Response byPassword = post("/auth/login", null,
        Map.of("email", "steve.jobs@hangelo.demo", "password", "password"));
    //
    //

    assertThat(unknown.status()).isEqualTo(404);
    assertThat(unknown.body().get("code").asString()).isEqualTo("demo_not_found");
    assertThat(aRealPerson.status()).isEqualTo(404);
    assertThat(byPassword.status()).isEqualTo(401);
  }

  // ------------------------------------------------------------------ visits and their figures

  @Test
  void analytics__countsVisitsVisitorsAndPages_forAnAdmin() {
    TestUser admin = demo(post("/auth/demo/steve-jobs", null)); // the admin unless configured otherwise
    TestUser someone = register();
    // A page pattern of this test alone: the table is shared with every other test.
    String page = "/test/" + RND.str(12).toLowerCase() + "/:id";
    String visitorA = "visitor-" + RND.str(16);
    String visitorB = "visitor-" + RND.str(16);
    JsonNode before = get("/admin/analytics?days=1", admin).body();

    //
    //
    Response anonymous = post("/public/visit", null, Map.of("visitorId", visitorA, "path", page));
    post("/public/visit", null, Map.of("visitorId", visitorA, "path", page));
    post("/public/visit", someone, Map.of("visitorId", visitorB, "path", page));
    //
    //

    assertThat(anonymous.status()).isEqualTo(200);
    JsonNode after = get("/admin/analytics?days=1", admin).body();
    assertThat(after.get("days").asInt()).isEqualTo(1);
    assertThat(after.get("visits").asLong() - before.get("visits").asLong()).isEqualTo(3);
    assertThat(after.get("visitors").asLong() - before.get("visitors").asLong()).isEqualTo(2);
    assertThat(after.get("signedInVisits").asLong() - before.get("signedInVisits").asLong()).isEqualTo(1);
    assertThat(after.get("users").asLong()).isGreaterThanOrEqualTo(2);

    // Today is a UTC day, whatever zone the server runs in.
    assertThat(after.get("byDay").size()).isEqualTo(1);
    assertThat(after.get("byDay").get(0).get("day").asString()).isEqualTo(LocalDate.now(ZoneOffset.UTC).toString());

    // The 30-day view has every day of the stretch, also the ones nobody came on.
    assertThat(get("/admin/analytics", admin).body().get("byDay").size()).isEqualTo(30);

    JsonNode thePage = get("/admin/analytics?days=90", admin).body().get("topPages").valueStream()
        .filter(row -> row.get("path").asString().equals(page)).findFirst().orElse(null);
    // (Present unless more than fifteen busier pages exist; in a test database it may be crowded out.)
    if (thePage != null) {
      assertThat(thePage.get("visits").asLong()).isEqualTo(3);
      assertThat(thePage.get("visitors").asLong()).isEqualTo(2);
    }
  }

  @Test
  void analytics__isClosedToEveryoneButAdmins() {
    TestUser admin = demo(post("/auth/demo/steve-jobs", null));
    TestUser someone = register();
    TestUser otherDemo = demo(post("/auth/demo/steve-wozniak", null));

    //
    //
    Response bySomeone = get("/admin/analytics", someone);
    Response byTheOtherDemo = get("/admin/analytics", otherDemo);
    Response withoutToken = get("/admin/analytics", null);
    //
    //

    // For anyone but an admin the page "does not exist".
    assertThat(bySomeone.status()).isEqualTo(404);
    assertThat(byTheOtherDemo.status()).isEqualTo(404);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(get("/admin/access", someone).body().get("admin").asBoolean()).isFalse();
    assertThat(get("/admin/access", admin).body().get("admin").asBoolean()).isTrue();
    assertThat(get("/admin/analytics", admin).status()).isEqualTo(200);
  }

  @Test
  void visit__refusesAnAddressThatCouldCarryASecret_orAnOddVisitorId() {
    //
    //
    Response withQuery = post("/public/visit", null,
        Map.of("visitorId", "visitor-12345678", "path", "/verify-email?token=secret"));
    Response notAPath = post("/public/visit", null,
        Map.of("visitorId", "visitor-12345678", "path", "https://evil.example/"));
    Response oddVisitor = post("/public/visit", null,
        Map.of("visitorId", "<script>", "path", "/feed"));
    Response tooLong = post("/public/visit", null,
        Map.of("visitorId", "visitor-12345678", "path", "/" + "a".repeat(200)));
    //
    //

    assertThat(withQuery.status()).isEqualTo(400);
    assertThat(notAPath.status()).isEqualTo(400);
    assertThat(oddVisitor.status()).isEqualTo(400);
    assertThat(tooLong.status()).isEqualTo(400);
  }

  private TestUser demo(Response login) {
    return new TestUser(login.body().get("userId").asLong(), login.body().get("token").asString(),
        "", "", "");
  }
}
