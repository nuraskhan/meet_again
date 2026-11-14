package kz.nuraskhan.meet_again;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kz.nuraskhan.meet_again.mail.Email;
import kz.nuraskhan.meet_again.mail.EmailNotSentException;
import kz.nuraskhan.meet_again.mail.EmailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The mail of the tests: nothing leaves the machine, every letter is kept here to be looked at.
 * Being {@code @Primary}, it takes the place of the real sender in the whole server under test.
 *
 * <p>Letters are found by address. Every test registers its own people with random addresses, so
 * tests do not see each other's letters.
 */
@Component
@Primary
public class CapturingEmailSender implements EmailSender {

  private static final Pattern VERIFY_TOKEN = Pattern.compile("/verify-email\\?token=([A-Za-z0-9_-]+)");

  private final List<Email> sent = new CopyOnWriteArrayList<>();

  /** An address the "provider" refuses, to test what happens when a letter cannot be sent. */
  private volatile String refusedAddress;

  @Override
  public void send(Email email) {
    if (email.to().equals(refusedAddress)) {
      throw new EmailNotSentException("refused by the test", null);
    }
    sent.add(email);
  }

  public void refuse(String address) {
    refusedAddress = address;
  }

  public void refuseNobody() {
    refusedAddress = null;
  }

  /** Every letter sent to this address, oldest first. */
  public List<Email> to(String address) {
    return sent.stream().filter(email -> email.to().equals(address)).toList();
  }

  /** The token from the link of the newest confirmation letter sent to this address. */
  public String verificationToken(String address) {
    List<Email> letters = to(address);
    for (int i = letters.size() - 1; i >= 0; i--) {
      Matcher matcher = VERIFY_TOKEN.matcher(letters.get(i).text());
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    throw new AssertionError("No confirmation letter was sent to " + address);
  }
}
