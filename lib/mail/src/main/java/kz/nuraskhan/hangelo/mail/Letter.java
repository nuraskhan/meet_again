package kz.nuraskhan.hangelo.mail;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a letter in the one look all our letters share: a heading, paragraphs, one button, a note
 * at the bottom. Produces the HTML and the plain text from the same content, so the two cannot
 * drift apart.
 *
 * <p>WHY escaping lives here: a letter contains what users typed (a name, the title of an event).
 * Put into HTML as is, it would let one user plant a link or markup into a letter sent to another.
 * Every piece of text given to this class is escaped by it; callers never write HTML.
 *
 * <pre>
 *   Email email = Letter.to("ann@example.com")
 *       .subject("Confirm your e-mail")
 *       .heading("One more step")
 *       .paragraph("Hi " + name + ", confirm that this address is yours.")
 *       .button("Confirm e-mail", link)
 *       .footnote("The link works for 24 hours.")
 *       .build();
 * </pre>
 */
public final class Letter {

  private final String to;
  private String subject = "";
  private String heading = "";
  private final List<String> paragraphs = new ArrayList<>();
  private String buttonLabel;
  private String buttonUrl;
  private String footnote;

  private Letter(String to) {
    this.to = to;
  }

  public static Letter to(String address) {
    return new Letter(address);
  }

  public Letter subject(String subject) {
    // One line: a subject is a header of the letter.
    this.subject = subject.replaceAll("[\\r\\n]+", " ").trim();
    return this;
  }

  public Letter heading(String heading) {
    this.heading = heading;
    return this;
  }

  public Letter paragraph(String text) {
    paragraphs.add(text);
    return this;
  }

  /** @param url an address of ours, built by the caller from MailProperties.link(...) */
  public Letter button(String label, String url) {
    this.buttonLabel = label;
    this.buttonUrl = url;
    return this;
  }

  public Letter footnote(String footnote) {
    this.footnote = footnote;
    return this;
  }

  public Email build() {
    return new Email(to, subject, html(), text());
  }

  private String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<!doctype html><html><body style=\"margin:0;padding:24px;background:#fbf6ef;")
        .append("font-family:system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif;color:#221c15\">")
        .append("<div style=\"max-width:520px;margin:0 auto;padding:28px;background:#ffffff;")
        .append("border:1px solid #e7dccd;border-radius:16px\">")
        .append("<div style=\"font-family:Georgia,serif;font-size:18px;font-weight:700;margin-bottom:20px\">")
        .append("hangelo</div>");

    if (!heading.isEmpty()) {
      sb.append("<h1 style=\"font-family:Georgia,serif;font-size:24px;line-height:1.2;margin:0 0 16px\">")
          .append(escape(heading)).append("</h1>");
    }
    for (String paragraph : paragraphs) {
      sb.append("<p style=\"font-size:16px;line-height:1.5;margin:0 0 14px\">")
          .append(escape(paragraph)).append("</p>");
    }
    if (buttonUrl != null) {
      sb.append("<p style=\"margin:22px 0\"><a href=\"").append(escape(buttonUrl))
          .append("\" style=\"display:inline-block;padding:13px 24px;border-radius:999px;")
          .append("background:#e4572e;color:#ffffff;font-weight:700;text-decoration:none\">")
          .append(escape(buttonLabel)).append("</a></p>")
          .append("<p style=\"font-size:13px;line-height:1.5;color:#6d6257;margin:0 0 14px\">")
          .append("If the button does not work, open this address:<br>")
          .append("<a href=\"").append(escape(buttonUrl)).append("\" style=\"color:#e4572e\">")
          .append(escape(buttonUrl)).append("</a></p>");
    }
    if (footnote != null) {
      sb.append("<p style=\"font-size:13px;line-height:1.5;color:#6d6257;margin:18px 0 0;")
          .append("padding-top:14px;border-top:1px solid #e7dccd\">").append(escape(footnote)).append("</p>");
    }

    return sb.append("</div></body></html>").toString();
  }

  private String text() {
    StringBuilder sb = new StringBuilder();
    if (!heading.isEmpty()) {
      sb.append(heading).append("\n\n");
    }
    for (String paragraph : paragraphs) {
      sb.append(paragraph).append("\n\n");
    }
    if (buttonUrl != null) {
      sb.append(buttonLabel).append(": ").append(buttonUrl).append("\n\n");
    }
    if (footnote != null) {
      sb.append(footnote).append("\n");
    }
    return sb.toString().stripTrailing() + "\n";
  }

  /** Text as it may stand inside HTML, between tags or in a quoted attribute. */
  static String escape(String text) {
    StringBuilder sb = new StringBuilder(text.length() + 16);
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '&' -> sb.append("&amp;");
        case '<' -> sb.append("&lt;");
        case '>' -> sb.append("&gt;");
        case '"' -> sb.append("&quot;");
        case '\'' -> sb.append("&#39;");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }
}
