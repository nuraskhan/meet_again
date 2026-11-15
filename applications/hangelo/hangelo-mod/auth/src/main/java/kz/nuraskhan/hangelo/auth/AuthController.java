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

  private final DemoLoginService demoLoginService;

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthService authService;
  private final EmailVerificationService emailVerificationService;

  public AuthController(AuthService authService, EmailVerificationService emailVerificationService,
                        DemoLoginService demoLoginService) {
    this.demoLoginService = demoLoginService;
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
    return authService.login(request);
  }

  /** The link from the confirmation letter. No token: the link itself is the proof. */
  /**
   * Signs in as a demonstration account, with no password: "steve-jobs" or "steve-wozniak".
   * See DemoLoginService for what that means and how it is switched off.
   */
  @PostMapping("/demo/{who}")
  public AuthResponse demoLogin(@org.springframework.web.bind.annotation.PathVariable("who") String who) {
    return demoLoginService.login(who);
  }

  @PostMapping("/verify-email")
  public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    emailVerificationService.verify(request.token());
  }
}
