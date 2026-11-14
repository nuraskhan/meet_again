package kz.nuraskhan.hangelo.model.repository;

import kz.nuraskhan.hangelo.model.entity.ProfilePrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfilePromptRepository extends JpaRepository<ProfilePrompt, Long> {

  /**
   * Removes the prompts of a person at once, in the database, before new ones are written (the new
   * rows reuse the same positions, which are unique per person).
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from ProfilePrompt p where p.userAccountId = :userAccountId")
  void deleteAllOf(@Param("userAccountId") Long userAccountId);
}
