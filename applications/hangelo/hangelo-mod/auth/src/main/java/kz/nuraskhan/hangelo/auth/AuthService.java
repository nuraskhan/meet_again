package kz.nuraskhan.hangelo.auth;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import kz.nuraskhan.hangelo.auth.dto.AuthResponse;
import kz.nuraskhan.hangelo.auth.dto.LoginRequest;
import kz.nuraskhan.hangelo.auth.dto.RegisterRequest;
import kz.nuraskhan.hangelo.model.entity.Profile;
import kz.nuraskhan.hangelo.model.entity.UserAccount;
import kz.nuraskhan.hangelo.model.repository.ProfileRepository;
import kz.nuraskhan.hangelo.model.repository.UserAccountRepository;
import kz.nuraskhan.hangelo.web.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserAccountRepository userAccountRepository;
  private final ProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  public AuthService(UserAccountRepository userAccountRepository,
                     ProfileRepository profileRepository,
                     PasswordEncoder passwordEncoder,
                     TokenService tokenService) {
    this.userAccountRepository = userAccountRepository;
    this.profileRepository = profileRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
  }

  /** Creates the account and its profile; the new user is logged in at once. */
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());

    if (userAccountRepository.existsByEmail(email)) {
      throw ApiException.conflict("email_taken", "This e-mail is already registered");
    }

    Instant now = Instant.now();

    UserAccount account = new UserAccount();
    account.email = email;
    account.passwordHash = passwordEncoder.encode(request.password());
    account.createdAt = now;
    account = userAccountRepository.save(account);

    Profile profile = new Profile();
    profile.userAccountId = account.id;
    profile.displayName = request.displayName().trim();
    profile.updatedAt = now;
    profileRepository.save(profile);

    return new AuthResponse(tokenService.issue(account.id), account.id);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    Optional<UserAccount> found = userAccountRepository.findByEmail(normalizeEmail(request.email()));

    // One answer for "no such e-mail", "wrong password" and "deactivated": the caller must not be
    // able to find out which e-mails are registered.
    boolean ok = found.isPresent()
        && found.get().deactivatedAt == null
        && passwordEncoder.matches(request.password(), found.get().passwordHash);

    if (!ok) {
      throw ApiException.unauthorized("bad_credentials", "Wrong e-mail or password");
    }

    Long userAccountId = found.get().id;
    return new AuthResponse(tokenService.issue(userAccountId), userAccountId);
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
