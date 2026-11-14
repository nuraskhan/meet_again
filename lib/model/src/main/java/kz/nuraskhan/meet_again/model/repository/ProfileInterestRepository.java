package kz.nuraskhan.meet_again.model.repository;

import kz.nuraskhan.meet_again.model.entity.ProfileInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileInterestRepository extends JpaRepository<ProfileInterest, Long> {

  /**
   * Removes the interests of a person at once, in the database, before new ones are written (the
   * new rows reuse the same positions, which are unique per person).
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from ProfileInterest i where i.userAccountId = :userAccountId")
  void deleteAllOf(@Param("userAccountId") Long userAccountId);
}
