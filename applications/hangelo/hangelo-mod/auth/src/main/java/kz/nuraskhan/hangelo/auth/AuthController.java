package kz.nuraskhan.hangelo.auth;

import jakarta.validation.Valid;
import kz.nuraskhan.hangelo.auth.dto.AuthResponse;
import kz.nuraskhan.hangelo.auth.dto.LoginRequest;
import kz.nuraskhan.hangelo.auth.dto.RegisterRequest;
import kz.nuraskhan.hangelo.auth.dto.VerifyEmailRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The endpoints of the auth module that are open without a token. */
@RestController
@RequestMapping("/auth")
public class AuthController {


  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthService authService;
  private final EmailVerificationService emailVerificationService;

  public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
    this.authService = authService;
    this.emailVerificationService = emailVerificationService;
  }

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);

    // The account exists whatever happens to the letter: a mail provider that is down must not
    // stop people from registering. The letter can be asked for again from the site.
    try {
      emailVerificationService.send(response.userId());
    } catch (RuntimeException e) {
      log.warn("The confirmation letter for user {} was not sent: {}", response.userId(), e.getMessage());
    }

    return response;
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    // Logged in with an e-mail that is not confirmed: the page will ask for the code at once, so
    // a fresh letter goes out now. If one went out less than a minute ago, or the provider is
    // down, the login is not spoiled by it: the page has "Send again".
    if (!response.emailVerified()) {
      try {
        emailVerificationService.send(response.userId());
      } catch (RuntimeException e) {
        log.info("No new confirmation letter at the login of user {}: {}", response.userId(), e.getMessage());
      }
    }
    return response;
  }

  /** The link from the confirmation letter. No token: the link itself is the proof. */

  @PostMapping("/verify-email")
  public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    emailVerificationService.verify(request.token());
  }
}
