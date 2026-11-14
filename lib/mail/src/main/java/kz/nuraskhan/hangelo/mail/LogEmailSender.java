package kz.nuraskhan.hangelo.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends nothing: writes the letter to the log. Used when there is no Resend key - on a developer's
 * machine, where the link of a confirmation letter is then copied from the console.
 */
class LogEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

  @Override
  public void send(Email email) {
    log.info("E-mail NOT sent (no Resend key). To: {} | Subject: {}\n{}",
        email.to(), email.subject(), email.text());
  }
}
