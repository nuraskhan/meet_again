package kz.nuraskhan.hangelo.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import kz.nuraskhan.hangelo.mail.EmailSender;
import kz.nuraskhan.hangelo.mail.Letter;
import kz.nuraskhan.hangelo.mail.MailProperties;
import kz.nuraskhan.hangelo.model.entity.EmailVerification;
import kz.nuraskhan.hangelo.model.entity.Profile;
import kz.nuraskhan.hangelo.model.entity.UserAccount;
import kz.nuraskhan.hangelo.model.repository.EmailVerificationRepository;
import kz.nuraskhan.hangelo.model.repository.ProfileRepository;
import kz.nuraskhan.hangelo.model.repository.UserAccountRepository;
import kz.nuraskhan.hangelo.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirming that an e-mail address belongs to the person who registered with it: a letter with a
 * six-digit code and a link. The person types the code into the page they are on; the link does
 * the same from wherever the letter is opened.
 *
 * <p>Until an address is confirmed we send it nothing but this letter - otherwise anyone could make
 * us write to a stranger by registering with their address.
 */
@Service
public class EmailVerificationService {

  /** How long a link works. */
  static final Duration LINK_LIFETIME = Duration.ofHours(24);

  /** How soon another letter may be asked for: keeps the button from flooding a mailbox. */
  static final Duration PAUSE_BETWEEN_LETTERS = Duration.ofSeconds(60);

  /**
   * How many wrong codes one letter forgives. A code is one of a million: without a limit it could
   * simply be tried through. After the limit a new letter has to be asked for - and that is allowed
   * once a minute.
   */
  static final int MAX_WRONG_CODES = 5;

  /** 32 random bytes: the link cannot be guessed. */
  private static final int TOKEN_BYTES = 32;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserAccountRepository userAccountRepository;
  private final ProfileRepository profileRepository;
  private final EmailVerificationRepository emailVerificationRepository;
  private final EmailSender emailSender;
  private final MailProperties mailProperties;

  public EmailVerificationService(UserAccountRepository userAccountRepository,
                                  ProfileRepository profileRepository,
                                  EmailVerificationRepository emailVerificationRepository,
                                  EmailSender emailSender,
                                  MailProperties mailProperties) {
    this.userAccountRepository = userAccountRepository;
    this.profileRepository = profileRepository;
    this.emailVerificationRepository = emailVerificationRepository;
    this.emailSender = emailSender;
    this.mailProperties = mailProperties;
  }

  /**
   * Sends a letter with a fresh link. If the letter cannot be handed to the provider, nothing is
   * saved either, and the exception goes to the caller.
   */
  @Transactional
  public void send(Long userAccountId) {
    UserAccount account = userAccountRepository.findById(userAccountId)
        .orElseThrow(() -> ApiException.notFound("user_not_found", "User not found"));

    if (account.emailVerifiedAt != null) {
      throw ApiException.conflict("already_verified", "Your e-mail is already confirmed");
    }

    Instant now = Instant.now();

    EmailVerification last = emailVerificationRepository
        .findFirstByUserAccountIdOrderByCreatedAtDesc(userAccountId).orElse(null);
    if (last != null && last.createdAt.plus(PAUSE_BETWEEN_LETTERS).isAfter(now)) {
      throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_soon",
          "A letter was just sent. Check your inbox, or try again in a minute");
    }

    String token = newToken();
    String code = newCode();

    EmailVerification verification = new EmailVerification();
    verification.userAccountId = userAccountId;
    verification.tokenHash = hash(token);
    verification.codeHash = hashCode(userAccountId, code);
    verification.createdAt = now;
    verification.expiresAt = now.plus(LINK_LIFETIME);
    emailVerificationRepository.save(verification);

    String name = profileRepository.findById(userAccountId).map((Profile p) -> p.displayName).orElse("there");

    emailSender.send(Letter.to(account.email)
        .subject("Confirm your e-mail for Hangelo")
        .heading("One more step")
        .paragraph("Hi " + name + ", please confirm that this address is yours.")
        .paragraph("Your confirmation code: " + code)
        .paragraph("Type it on the page that asked for it. Or press the button: it does the same.")
        .button("Confirm e-mail", mailProperties.link("/verify-email?token=" + token))
        .footnote("The code and the link work for 24 hours. If you did not register at Hangelo, ignore this letter.")
        .build());
  }

  /**
   * The link came back. Needs no login: the letter may be opened on another device.
   *
   * <p>One answer for a wrong, a used and an expired token: which of the three it is, is nobody's
   * business but ours.
   */
  @Transactional
  public void verify(String token) {
    Instant now = Instant.now();

    EmailVerification verification = emailVerificationRepository.findByTokenHash(hash(token)).orElse(null);

    if (verification == null || verification.usedAt != null || verification.expiresAt.isBefore(now)) {
      throw ApiException.badRequest("bad_link", "This link does not work any more. Ask for a new one");
    }

    verification.usedAt = now;

    UserAccount account = userAccountRepository.findById(verification.userAccountId).orElseThrow();
    if (account.emailVerifiedAt == null) {
      account.emailVerifiedAt = now;
    }
  }

  /**
   * The code from the newest letter, typed by the person themselves (they are logged in, with a
   * token that opens nothing else yet).
   *
   * <p>WHY a wrong code is counted even though the request fails: the count is the only thing
   * between a six-digit code and someone trying all of them. So the refusal must not undo it -
   * hence "noRollbackFor".
   */
  @Transactional(noRollbackFor = ApiException.class)
  public void confirmByCode(Long userAccountId, String code) {
    UserAccount account = userAccountRepository.findById(userAccountId)
        .orElseThrow(() -> ApiException.notFound("user_not_found", "User not found"));
    if (account.emailVerifiedAt != null) {
      return; // confirmed already (by the link, in another tab): nothing to do, and no error
    }

    Instant now = Instant.now();
    EmailVerification verification = emailVerificationRepository
        .findFirstByUserAccountIdOrderByCreatedAtDesc(userAccountId).orElse(null);

    if (verification == null || verification.codeHash == null || verification.usedAt != null
        || verification.expiresAt.isBefore(now)) {
      throw ApiException.badRequest("code_expired", "This code does not work any more. Ask for a new one");
    }
    if (verification.failedAttempts >= MAX_WRONG_CODES) {
      throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts",
          "Too many wrong codes. Ask for a new one");
    }
    if (!verification.codeHash.equals(hashCode(userAccountId, code.trim()))) {
      verification.failedAttempts = verification.failedAttempts + 1;
      emailVerificationRepository.save(verification);
      int left = MAX_WRONG_CODES - verification.failedAttempts;
      throw ApiException.badRequest("wrong_code", left > 0
          ? "That is not the code from the letter. Check it and try again"
          : "That is not the code from the letter. Ask for a new one");
    }

    verification.usedAt = now;
    account.emailVerifiedAt = now;
  }

  /** Six digits, every one of the million equally likely; "000042" is a code too. */
  private static String newCode() {
    return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
  }

  /**
   * What is stored instead of the code. The account id is mixed in, so the same code in two
   * letters does not give the same stored value.
   */
  private static String hashCode(Long userAccountId, String code) {
    return hash(userAccountId + ":" + code);
  }

  private static String newToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** What is stored instead of the token: reading the table gives nobody a working link. */
  private static String hash(String token) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e); // SHA-256 is in every JDK
    }
  }
}
