package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.mail.Email;
import kz.nuraskhan.hangelo.model.entity.EmailVerification;
import kz.nuraskhan.hangelo.model.repository.EmailVerificationRepository;
import kz.nuraskhan.hangelo.model.rnd.RND;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Confirming an e-mail address by the link in a letter. */
class EmailVerificationApiTest extends ApiTestBase {

  @Autowired CapturingEmailSender mail;
  @Autowired EmailVerificationRepository emailVerificationRepository;

  @Test
  void register__sendsOneConfirmationLetter_andTheAddressIsNotConfirmedYet() {
    //
    //
    TestUser user = registerUnconfirmed();
    //
    //

    List<Email> letters = mail.to(user.email());
    assertThat(letters).hasSize(1);
    assertThat(letters.get(0).subject()).isEqualTo("Confirm your e-mail for Hangelo");
    assertThat(letters.get(0).text())
        .contains("Hi " + user.displayName())
        .contains("https://site.test/verify-email?token=");

    assertThat(get("/profile/me", user).body().get("emailVerified").asBoolean()).isFalse();
  }

  @Test
  void register__succeedsEvenWhenTheLetterCannotBeSent() {
    String email = RND.email();
    mail.refuse(email);
    try {
      //
      //
      Response response = post("/auth/register", null,
          Map.of("email", email, "password", "secret1", "displayName", "Unlucky"));
      //
      //

      assertThat(response.status()).isEqualTo(200);
      assertThat(response.body().get("token").asString()).isNotBlank();
      assertThat(mail.to(email)).isEmpty();
    } finally {
      mail.refuseNobody();
    }
  }

  @Test
  void verifyEmail__confirmsTheAddress_withoutBeingLoggedIn() {
    TestUser user = registerUnconfirmed();
    String token = mail.verificationToken(user.email());

    //
    //
    Response response = post("/auth/verify-email", null, Map.of("token", token));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(get("/profile/me", user).body().get("emailVerified").asBoolean()).isTrue();
  }

  @Test
  void verifyEmail__worksOnce() {
    TestUser user = registerUnconfirmed();
    String token = mail.verificationToken(user.email());
    post("/auth/verify-email", null, Map.of("token", token));

    //
    //
    Response second = post("/auth/verify-email", null, Map.of("token", token));
    //
    //

    assertThat(second.status()).isEqualTo(400);
    assertThat(second.body().get("code").asString()).isEqualTo("bad_link");
  }

  @Test
  void verifyEmail__refusesUnknownAndExpiredLink_withTheSameAnswer() {
    TestUser user = registerUnconfirmed();
    String token = mail.verificationToken(user.email());

    // Make the link a day and a minute old.
    EmailVerification verification = emailVerificationRepository
        .findFirstByUserAccountIdOrderByCreatedAtDesc(user.id()).orElseThrow();
    verification.expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES);
    emailVerificationRepository.save(verification);

    //
    //
    Response expired = post("/auth/verify-email", null, Map.of("token", token));
    Response unknown = post("/auth/verify-email", null, Map.of("token", "no-such-token"));
    //
    //

    assertThat(expired.status()).isEqualTo(400);
    assertThat(unknown.status()).isEqualTo(400);
    assertThat(expired.body().toString()).isEqualTo(unknown.body().toString());
    assertThat(get("/profile/me", user).body().get("emailVerified").asBoolean()).isFalse();
  }

  @Test
  void sendAgain__needsToken() {
    //
    //
    Response response = post("/account/email-verification/send", null);
    //
    //

    assertThat(response.status()).isEqualTo(401);
  }

  @Test
  void sendAgain__isRefusedWithinAMinuteOfTheLastLetter() {
    TestUser user = registerUnconfirmed(); // the first letter has just gone out

    //
    //
    Response response = post("/account/email-verification/send", user);
    //
    //

    assertThat(response.status()).isEqualTo(429);
    assertThat(response.body().get("code").asString()).isEqualTo("too_soon");
    assertThat(mail.to(user.email())).hasSize(1);
  }

  @Test
  void sendAgain__sendsANewWorkingLink_onceTheMinuteHasPassed() {
    TestUser user = registerUnconfirmed();
    String firstToken = mail.verificationToken(user.email());

    // Make the first letter two minutes old.
    EmailVerification first = emailVerificationRepository
        .findFirstByUserAccountIdOrderByCreatedAtDesc(user.id()).orElseThrow();
    first.createdAt = Instant.now().minus(2, ChronoUnit.MINUTES);
    emailVerificationRepository.save(first);

    //
    //
    Response response = post("/account/email-verification/send", user);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(mail.to(user.email())).hasSize(2);

    String secondToken = mail.verificationToken(user.email());
    assertThat(secondToken).isNotEqualTo(firstToken);
    assertThat(post("/auth/verify-email", null, Map.of("token", secondToken)).status()).isEqualTo(200);
  }

  @Test
  void sendAgain__isRefusedWhenTheAddressIsAlreadyConfirmed() {
    TestUser user = registerUnconfirmed();
    post("/auth/verify-email", null, Map.of("token", mail.verificationToken(user.email())));

    //
    //
    Response response = post("/account/email-verification/send", user);
    //
    //

    assertThat(response.status()).isEqualTo(409);
    assertThat(response.body().get("code").asString()).isEqualTo("already_verified");
  }

  // ------------------------------------------------------------------ closed until confirmed

  @Test
  void unconfirmed__everythingIsRefused_exceptFinishingTheConfirmation() {
    TestUser friend = register();

    //
    //
    TestUser user = registerUnconfirmed();
    //
    //

    // Reading and writing alike, whatever the module.
    for (Response refused : List.of(
        get("/friend/list", user),
        get("/event/list", user),
        get("/chat/list", user),
        get("/notification/list", user),
        get("/profile/" + friend.id(), user),
        post("/friend/request", user, Map.of("userId", friend.id())),
        post("/profile/me", user, Map.of("displayName", "New name")),
        post("/event/create", user, Map.of("eventType", "COFFEE", "inviteeIds", List.of())))) {
      assertThat(refused.status()).isEqualTo(403);
      assertThat(refused.body().get("code").asString()).isEqualTo("email_not_verified");
    }
    // What stays open: seeing whether the address is confirmed, and asking for the letter again
    // (refused here only because one has just been sent).
    assertThat(get("/profile/me", user).status()).isEqualTo(200);
    assertThat(post("/account/email-verification/send", user).status()).isEqualTo(429);
    // The refused request really did nothing.
    assertThat(get("/friend/request/incoming", friend).body().size()).isZero();
  }

  @Test
  void unconfirmed__theSameTokenOpensEverything_onceTheLinkWasOpened() {
    TestUser user = registerUnconfirmed();
    assertThat(get("/friend/list", user).status()).isEqualTo(403);

    //
    //
    post("/auth/verify-email", null, Map.of("token", mail.verificationToken(user.email())));
    //
    //

    assertThat(get("/friend/list", user).status()).isEqualTo(200);
    assertThat(get("/event/list", user).status()).isEqualTo(200);
  }

  @Test
  void registerAndLogin__sayWhetherTheEmailIsConfirmed() {
    String email = RND.email();

    //
    //
    Response registered = post("/auth/register", null,
        Map.of("email", email, "password", "secret1", "displayName", "Someone"));
    Response loginBefore = post("/auth/login", null, Map.of("email", email, "password", "secret1"));
    post("/auth/verify-email", null, Map.of("token", mail.verificationToken(email)));
    Response loginAfter = post("/auth/login", null, Map.of("email", email, "password", "secret1"));
    //
    //

    assertThat(registered.body().get("emailVerified").asBoolean()).isFalse();
    assertThat(loginBefore.body().get("emailVerified").asBoolean()).isFalse();
    assertThat(loginAfter.body().get("emailVerified").asBoolean()).isTrue();
  }

  @Test
  void theLetter__escapesTheNameTheUserTyped() {
    String email = RND.email();

    //
    //
    post("/auth/register", null, Map.of(
        "email", email, "password", "secret1", "displayName", "<b>Bold</b> & <a href=x>link</a>"));
    //
    //

    Email letter = mail.to(email).get(0);
    assertThat(letter.html())
        .doesNotContain("<b>Bold</b>")
        .contains("&lt;b&gt;Bold&lt;/b&gt; &amp; &lt;a href=x&gt;link&lt;/a&gt;");
  }
}
