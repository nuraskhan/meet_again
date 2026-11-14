package kz.nuraskhan.hangelo.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LetterTest {

  @Test
  void build__escapesWhatUsersTyped_soItCannotBecomeMarkup() {
    String nameFromUser = "<a href=\"https://evil.test\">Click</a> & \"friends\"";

    //
    //
    Email actual = Letter.to("ann@example.test")
        .subject("Hello")
        .heading(nameFromUser)
        .paragraph("Invited by " + nameFromUser)
        .build();
    //
    //

    assertThat(actual.html())
        .doesNotContain("<a href=\"https://evil.test\">")
        .contains("&lt;a href=&quot;https://evil.test&quot;&gt;Click&lt;/a&gt; &amp; &quot;friends&quot;");
    // Plain text has no markup to protect: it stays as typed.
    assertThat(actual.text()).contains(nameFromUser);
  }

  @Test
  void subject__isKeptOnOneLine() {
    //
    //
    Email actual = Letter.to("ann@example.test").subject("Coming up:\r\nBcc: someone@evil.test").build();
    //
    //

    assertThat(actual.subject()).isEqualTo("Coming up: Bcc: someone@evil.test");
  }

  @Test
  void button__appearsInBothBodiesWithTheSameAddress() {
    //
    //
    Email actual = Letter.to("ann@example.test")
        .subject("Confirm")
        .button("Confirm e-mail", "https://site.test/verify-email?token=abc_DEF-123")
        .footnote("Works for 24 hours.")
        .build();
    //
    //

    assertThat(actual.html()).contains("href=\"https://site.test/verify-email?token=abc_DEF-123\"");
    assertThat(actual.text())
        .contains("Confirm e-mail: https://site.test/verify-email?token=abc_DEF-123")
        .contains("Works for 24 hours.");
  }
}
