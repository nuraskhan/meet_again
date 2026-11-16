package kz.nuraskhan.hangelo.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import kz.nuraskhan.hangelo.model.entity.UserAccount;
import kz.nuraskhan.hangelo.model.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Until a person has confirmed their e-mail, the application is closed to them: every request made
 * with their token is refused with {@code 403 email_not_verified}.
 *
 * <p>Three things stay open, because without them the confirmation could not be finished:
 * <ul>
 *   <li>{@code GET /profile/me} - the page asks it to learn whether the address is confirmed yet
 *       (the letter may be opened on another device);</li>
 *   <li>{@code POST /account/email-verification/confirm} - the code from the letter;</li>
 *   <li>{@code POST /account/email-verification/send} - "send the letter again".</li>
 * </ul>
 *
 * <p>WHY a filter and not a check in each controller: a new endpoint is closed by default. One that
 * forgets a check would be open.
 *
 * <p>Not a Spring bean on purpose: as a bean it would also be put in front of the security chain
 * by Spring Boot, run there before anyone is authenticated, and - being once-per-request - not run
 * again in the place where it matters. SecurityConfig creates it and puts it after the token filter.
 */
public class EmailVerifiedFilter extends OncePerRequestFilter {

  private static final String REFUSAL =
      "{\"code\":\"email_not_verified\",\"message\":\"Confirm your e-mail first: open the link in the letter we sent you\"}";

  private final UserAccountRepository userAccountRepository;

  /**
   * Accounts known to be confirmed: a confirmation is never taken back, so the answer "yes" can be
   * remembered and costs no query afterwards. "No" is never remembered - it changes.
   */
  private final Set<Long> confirmed = ConcurrentHashMap.newKeySet();

  public EmailVerifiedFilter(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // No token: either an open address, or the chain will refuse it itself.
    if (!(authentication instanceof JwtAuthenticationToken) || isOpenToUnconfirmed(request)) {
      chain.doFilter(request, response);
      return;
    }

    Long userId = Long.valueOf(authentication.getName());

    if (!confirmed.contains(userId)) {
      boolean isConfirmed = userAccountRepository.findById(userId)
          .map((UserAccount account) -> account.emailVerifiedAt != null)
          .orElse(false);

      if (!isConfirmed) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(REFUSAL);
        return;
      }

      confirmed.add(userId);
    }

    chain.doFilter(request, response);
  }

  private static boolean isOpenToUnconfirmed(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    String method = request.getMethod();

    return path.startsWith("/auth/")
        || path.startsWith("/public/")
        || path.startsWith("/actuator/health")
        || ("GET".equals(method) && path.equals("/profile/me"))
        || ("POST".equals(method) && path.equals("/account/email-verification/confirm"))
        || ("POST".equals(method) && path.equals("/account/email-verification/send"));
  }
}
