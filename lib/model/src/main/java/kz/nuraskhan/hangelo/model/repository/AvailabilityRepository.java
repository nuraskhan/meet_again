package kz.nuraskhan.hangelo.model.repository;

import kz.nuraskhan.hangelo.model.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

  /** How many marks a person has that are not removed. */
  long countByUserAccountIdAndDeletedAtIsNull(Long userAccountId);
}
