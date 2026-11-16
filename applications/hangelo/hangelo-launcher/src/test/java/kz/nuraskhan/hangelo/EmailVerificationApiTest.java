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

  // ------------------------------------------------------------------ the code typed into the page

  @Test
  void confirmByCode__opensTheApplication_withTheCodeFromTheLetter() {
    TestUser user = registerUnconfirmed();
    String code = mail.verificationCode(user.email());
    assertThat(code).matches("\\d{6}");
    assertThat(get("/friend/list", user).status()).isEqualTo(403);

    //
    //
    Response response = post("/account/email-verification/confirm", user, Map.of("code", code));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(get("/profile/me", user).body().get("emailVerified").asBoolean()).isTrue();
    assertThat(get("/friend/list", user).status()).isEqualTo(200);
    // The link of the same letter is used up with the code.
    assertThat(post("/auth/verify-email", null, Map.of("token", mail.verificationToken(user.email()))).status())
        .isEqualTo(400);
  }

  @Test
  void confirmByCode__refusesAWrongCode_andAfterFiveWrongOnesEvenTheRightOne() {
    TestUser user = registerUnconfirmed();
    String right = mail.verificationCode(user.email());
    String wrong = right.equals("000000") ? "000001" : "000000";

    //
    //
    Response first = post("/account/email-verification/confirm", user, Map.of("code", wrong));
    for (int i = 0; i < 4; i++) {
      post("/account/email-verification/confirm", user, Map.of("code", wrong));
    }
    Response rightAfterFiveWrong = post("/account/email-verification/confirm", user, Map.of("code", right));
    //
    //

    assertThat(first.status()).isEqualTo(400);
    assertThat(first.body().get("code").asString()).isEqualTo("wrong_code");
    assertThat(rightAfterFiveWrong.status()).isEqualTo(429);
    assertThat(rightAfterFiveWrong.body().get("code").asString()).isEqualTo("too_many_attempts");
    assertThat(get("/profile/me", user).body().get("emailVerified").asBoolean()).isFalse();
  }

  @Test
  void confirmByCode__refusesSomethingThatIsNotSixDigits_andNeedsAToken() {
    TestUser user = registerUnconfirmed();

    //
    //
    Response letters = post("/account/email-verification/confirm", user, Map.of("code", "abcdef"));
    Response tooShort = post("/account/email-verification/confirm", user, Map.of("code", "123"));
    Response withoutToken = post("/account/email-verification/confirm", null, Map.of("code", "123456"));
    //
    //

    assertThat(letters.status()).isEqualTo(400);
    assertThat(tooShort.status()).isEqualTo(400);
    assertThat(withoutToken.status()).isEqualTo(401);
  }

  @Test
  void confirmByCode__ofOnePerson_doesNotConfirmAnother() {
    TestUser owner = registerUnconfirmed();
    TestUser other = registerUnconfirmed();

    //
    //
    Response response = post("/account/email-verification/confirm", other,
        Map.of("code", mail.verificationCode(owner.email())));
    //
    //

    // (One chance in a million that the two codes are the same: then this is a right code.)
    if (!mail.verificationCode(owner.email()).equals(mail.verificationCode(other.email()))) {
      assertThat(response.status()).isEqualTo(400);
      assertThat(get("/profile/me", other).body().get("emailVerified").asBoolean()).isFalse();
    }
    assertThat(get("/profile/me", owner).body().get("emailVerified").asBoolean()).isFalse();
  }

  @Test
  void login__ofAnUnconfirmedAccount_sendsAFreshLetter_onceTheMinuteHasPassed() {
    TestUser user = registerUnconfirmed();
    // The letter of the registration went out a moment ago: move it back past the pause.
    EmailVerification first = emailVerificationRepository
        .findFirstByUserAccountIdOrderByCreatedAtDesc(user.id()).orElseThrow();
    first.createdAt = Instant.now().minus(2, ChronoUnit.MINUTES);
    emailVerificationRepository.save(first);

    //
    //
    Response login = post("/auth/login", null, Map.of("email", user.email(), "password", user.password()));
    //
    //

    assertThat(login.status()).isEqualTo(200);
    assertThat(login.body().get("emailVerified").asBoolean()).isFalse();
    assertThat(mail.to(user.email())).hasSize(2);
    // The new code works with the token the login gave.
    TestUser loggedIn = new TestUser(user.id(), login.body().get("token").asString(), user.email(),
        user.password(), user.displayName());
    assertThat(post("/account/email-verification/confirm", loggedIn,
        Map.of("code", mail.verificationCode(user.email()))).status()).isEqualTo(200);
  }

  @Test
  void login__ofAnUnconfirmedAccount_succeedsWithoutALetter_whenOneWasJustSent() {
    TestUser user = registerUnconfirmed(); // the first letter has just gone out

    //
    //
    Response login = post("/auth/login", null, Map.of("email", user.email(), "password", user.password()));
    //
    //

    assertThat(login.status()).isEqualTo(200);
    assertThat(mail.to(user.email())).hasSize(1);
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
    // What stays open: seeing whether the address is confirmed, typing the code (a wrong one here),
    // and asking for the letter again (refused here only because one has just been sent).
    assertThat(get("/profile/me", user).status()).isEqualTo(200);
    assertThat(post("/account/email-verification/send", user).status()).isEqualTo(429);
    assertThat(post("/account/email-verification/confirm", user, Map.of("code", "12345")).status())
        .isEqualTo(400);
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
