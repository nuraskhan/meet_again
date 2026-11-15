package kz.nuraskhan.hangelo.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.auth.dto.AuthResponse;
import kz.nuraskhan.hangelo.model.entity.Friendship;
import kz.nuraskhan.hangelo.model.entity.Profile;
import kz.nuraskhan.hangelo.model.entity.UserAccount;
import kz.nuraskhan.hangelo.model.enums.FriendshipStatus;
import kz.nuraskhan.hangelo.model.repository.FriendshipRepository;
import kz.nuraskhan.hangelo.model.repository.ProfileRepository;
import kz.nuraskhan.hangelo.model.repository.UserAccountRepository;
import kz.nuraskhan.hangelo.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signing in as one of two demonstration accounts by a link, with no password - for showing the
 * product to someone without making them register.
 *
 * <p>The two accounts are created the first time either is used, and are friends with each other,
 * so that one can at once invite the other. They are ordinary accounts in every other respect.
 *
 * <p>WHAT THIS MEANS FOR SAFETY, stated plainly: anyone who knows the link is signed in as that
 * account and sees everything it sees. That is the point of a demonstration and the reason the
 * accounts are fixed and cannot be any other. Nothing private belongs in them. The whole thing is
 * switched off with {@code hangelo.demo-login.enabled=false} (HANGELO_DEMO_LOGIN).
 *
 * <p>Nobody can sign in to these accounts with a password: the password is random and thrown
 * away.
 */
@Service
public class DemoLoginService {

  /** The accounts there are: the word in the link, and who it is. */
  private record Demo(String email, String displayName, String city) {
  }

  private static final Map<String, Demo> DEMOS = Map.of(
      "steve-jobs", new Demo("steve.jobs@hangelo.demo", "Steve Jobs", "Cupertino"),
      "steve-wozniak", new Demo("steve.wozniak@hangelo.demo", "Steve Wozniak", "Los Gatos"));

  private static final SecureRandom RANDOM = new SecureRandom();

  private final boolean enabled;
  private final UserAccountRepository userAccountRepository;
  private final ProfileRepository profileRepository;
  private final FriendshipRepository friendshipRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  public DemoLoginService(@Value("${hangelo.demo-login.enabled:true}") boolean enabled,
                          UserAccountRepository userAccountRepository,
                          ProfileRepository profileRepository,
                          FriendshipRepository friendshipRepository,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService) {
    this.enabled = enabled;
    this.userAccountRepository = userAccountRepository;
    this.profileRepository = profileRepository;
    this.friendshipRepository = friendshipRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
  }

  /** @param who the word in the link: "steve-jobs" or "steve-wozniak" */
  @Transactional
  public AuthResponse login(String who) {
    // Off, or a word that is not one of the two: the link "does not exist".
    if (!enabled || !DEMOS.containsKey(who)) {
      throw ApiException.notFound("demo_not_found", "There is no such demo account");
    }

    // Both accounts exist from the first use of either, so the friendship can be made.
    Long jobs = accountOf(DEMOS.get("steve-jobs"));
    Long wozniak = accountOf(DEMOS.get("steve-wozniak"));
    befriend(jobs, wozniak);

    Long me = who.equals("steve-jobs") ? jobs : wozniak;
    return new AuthResponse(tokenService.issue(me), me);
  }

  private Long accountOf(Demo demo) {
    UserAccount existing = userAccountRepository.findByEmail(demo.email()).orElse(null);
    if (existing != null) {
      return existing.id;
    }

    Instant now = Instant.now();

    UserAccount account = new UserAccount();
    account.email = demo.email();
    // A password nobody knows: 32 random bytes, hashed and forgotten.
    byte[] secret = new byte[32];
    RANDOM.nextBytes(secret);
    account.passwordHash = passwordEncoder.encode(Base64.getEncoder().encodeToString(secret));
    account.createdAt = now;
    // Nothing is sent to an address that does not exist; "confirmed" keeps the banner away.
    account.emailVerifiedAt = now;
    account = userAccountRepository.save(account);

    Profile profile = new Profile();
    profile.userAccountId = account.id;
    profile.displayName = demo.displayName();
    profile.city = demo.city();
    profile.updatedAt = now;
    profileRepository.save(profile);

    return account.id;
  }

  private void befriend(Long first, Long second) {
    List<Friendship> live = friendshipRepository.findBetween(first, second,
        List.of(FriendshipStatus.PENDING, FriendshipStatus.ACCEPTED));
    if (!live.isEmpty()) {
      return; // friends already, or a request between them is waiting: left as it is
    }
    Instant now = Instant.now();
    Friendship friendship = new Friendship();
    friendship.requesterUserAccountId = first;
    friendship.addresseeUserAccountId = second;
    friendship.status = FriendshipStatus.ACCEPTED;
    friendship.createdAt = now;
    friendship.respondedAt = now;
    friendshipRepository.save(friendship);
  }
}
