package kz.nuraskhan.hangelo.event;

import java.security.Principal;
import java.util.List;
import kz.nuraskhan.hangelo.event.dto.InvitationDto;
import kz.nuraskhan.hangelo.web.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invitation")
public class InvitationController {

  private final EventService eventService;
  private final EventQuery eventQuery;

  public InvitationController(EventService eventService, EventQuery eventQuery) {
    this.eventService = eventService;
    this.eventQuery = eventQuery;
  }

  @GetMapping("/incoming")
  public List<InvitationDto> incoming(Principal principal) {
    return eventQuery.incoming(CurrentUser.id(principal));
  }

  @PostMapping("/{invitationId}/accept")
  public void accept(Principal principal, @PathVariable("invitationId") Long invitationId) {
    eventService.accept(CurrentUser.id(principal), invitationId);
  }

  @PostMapping("/{invitationId}/decline")
  public void decline(Principal principal, @PathVariable("invitationId") Long invitationId) {
    eventService.decline(CurrentUser.id(principal), invitationId);
  }
}
