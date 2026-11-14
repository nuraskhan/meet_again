package kz.nuraskhan.meet_again.magain.auth;

import java.security.Principal;
import kz.nuraskhan.meet_again.mail.EmailNotSentException;
import kz.nuraskhan.meet_again.web.ApiException;
import kz.nuraskhan.meet_again.web.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** What a logged-in person does with their own account. Needs a token, unlike {@code /auth}. */
@RestController
@RequestMapping("/account")
public class AccountController {

  private final EmailVerificationService emailVerificationService;

  public AccountController(EmailVerificationService emailVerificationService) {
    this.emailVerificationService = emailVerificationService;
  }

  /** "Send the confirmation letter again." At most once a minute. */
  @PostMapping("/email-verification/send")
  public void sendEmailVerification(Principal principal) {
    try {
      emailVerificationService.send(CurrentUser.id(principal));
    } catch (EmailNotSentException e) {
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "email_not_sent",
          "We could not send the letter right now. Please try again later");
    }
  }
}
