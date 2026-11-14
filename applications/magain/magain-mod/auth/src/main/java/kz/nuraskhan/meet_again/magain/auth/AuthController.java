package kz.nuraskhan.meet_again.magain.auth;

import jakarta.validation.Valid;
import kz.nuraskhan.meet_again.magain.auth.dto.AuthResponse;
import kz.nuraskhan.meet_again.magain.auth.dto.LoginRequest;
import kz.nuraskhan.meet_again.magain.auth.dto.RegisterRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The only endpoints open without a token. */
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
