package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import kz.nuraskhan.hangelo.model.entity.UserAccount;
import kz.nuraskhan.hangelo.model.repository.UserAccountRepository;
import kz.nuraskhan.hangelo.model.rnd.RND;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/** Counting visits, and the figures of them for the people marked as administrators. */
class AnalyticsApiTest extends ApiTestBase {

  @Autowired UserAccountRepository userAccountRepository;

  @Test
  void analytics__countsVisitsVisitorsAndPages_forAnAdmin() {
    TestUser admin = registerAdmin();
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
  void analytics__isClosedToEveryoneButThoseMarkedInTheDatabase() {
    TestUser admin = registerAdmin();
    TestUser someone = register();

    //
    //
    Response bySomeone = get("/admin/analytics", someone);
    Response withoutToken = get("/admin/analytics", null);
    Response byAdmin = get("/admin/analytics", admin);
    //
    //

    // For anyone but an admin the page "does not exist".
    assertThat(bySomeone.status()).isEqualTo(404);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(byAdmin.status()).isEqualTo(200);
    assertThat(get("/admin/access", someone).body().get("admin").asBoolean()).isFalse();
    assertThat(get("/admin/access", admin).body().get("admin").asBoolean()).isTrue();
  }

  @Test
  void analytics__closesAtOnce_whenTheMarkIsTakenAway() {
    TestUser admin = registerAdmin();
    assertThat(get("/admin/analytics", admin).status()).isEqualTo(200);
    mark(admin, false);

    //
    //
    Response after = get("/admin/analytics", admin);
    //
    //

    assertThat(after.status()).isEqualTo(404);
    assertThat(get("/admin/access", admin).body().get("admin").asBoolean()).isFalse();
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

  /** A person marked as an administrator the only way there is: in the database. */
  private TestUser registerAdmin() {
    TestUser user = register();
    mark(user, true);
    return user;
  }

  private void mark(TestUser user, boolean admin) {
    UserAccount account = userAccountRepository.findById(user.id()).orElseThrow();
    account.admin = admin;
    userAccountRepository.save(account);
  }
}
