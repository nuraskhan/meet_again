package kz.nuraskhan.meet_again.web;

import java.security.Principal;

/**
 * Who is calling. Controllers take {@code Principal principal} as a parameter and pass it here.
 *
 * <p>The name of the principal is the subject of the token = {@code user_account.id}. It will stay
 * so when the tokens are issued by Keycloak instead of our own auth module.
 */
public final class CurrentUser {

  private CurrentUser() {}

  /** @return {@code user_account.id} of the caller */
  public static Long id(Principal principal) {
    return Long.valueOf(principal.getName());
  }
}
