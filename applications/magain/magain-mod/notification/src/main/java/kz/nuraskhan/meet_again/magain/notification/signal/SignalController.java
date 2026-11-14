package kz.nuraskhan.meet_again.magain.notification.signal;

import java.security.Principal;
import kz.nuraskhan.meet_again.web.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/signal")
public class SignalController {

  private final SignalHub signalHub;

  public SignalController(SignalHub signalHub) {
    this.signalHub = signalHub;
  }

  /**
   * The stream of signals for the caller: a request that stays open, into which the server writes
   * events as they happen. Needs the token like everything else - so the page opens it with
   * {@code fetch} and reads the body as it comes (the browser's own EventSource cannot send a
   * token).
   *
   * <p>Events: {@code hello} when the stream opens; {@code notification} with a NotificationDto;
   * {@code chat.message} with a chat id and the message. A line starting with a colon is a
   * keep-alive and means nothing.
   */
  @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> stream(Principal principal) {
    return ResponseEntity.ok()
        // For proxies on the way: pass each line on as it comes, keep nothing.
        .header("Cache-Control", "no-cache, no-transform")
        .header("X-Accel-Buffering", "no")
        .body(signalHub.connect(CurrentUser.id(principal)));
  }
}
