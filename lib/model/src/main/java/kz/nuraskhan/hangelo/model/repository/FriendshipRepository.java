package kz.nuraskhan.hangelo.model.repository;

import java.util.Collection;
import java.util.List;
import kz.nuraskhan.hangelo.model.entity.Friendship;
import kz.nuraskhan.hangelo.model.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

  /** Rows between two people, whoever sent the request, in the given statuses. */
  @Query("""
      select f from Friendship f
      where f.status in :statuses
        and ((f.requesterUserAccountId = :a and f.addresseeUserAccountId = :b)
          or (f.requesterUserAccountId = :b and f.addresseeUserAccountId = :a))
      """)
  List<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b,
                               @Param("statuses") Collection<FriendshipStatus> statuses);

  /** How many of {@code others} are friends of {@code me}. */
  @Query("""
      select count(f) from Friendship f
      where f.status = kz.nuraskhan.hangelo.model.enums.FriendshipStatus.ACCEPTED
        and ((f.requesterUserAccountId = :me and f.addresseeUserAccountId in :others)
          or (f.addresseeUserAccountId = :me and f.requesterUserAccountId in :others))
      """)
  long countFriendsAmong(@Param("me") Long me, @Param("others") Collection<Long> others);
}
