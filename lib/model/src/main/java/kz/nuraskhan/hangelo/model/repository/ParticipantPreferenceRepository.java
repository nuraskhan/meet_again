package kz.nuraskhan.hangelo.model.repository;

import java.util.Optional;
import kz.nuraskhan.hangelo.model.entity.ParticipantPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantPreferenceRepository extends JpaRepository<ParticipantPreference, Long> {

  Optional<ParticipantPreference> findByEventIdAndUserAccountId(Long eventId, Long userAccountId);
}
