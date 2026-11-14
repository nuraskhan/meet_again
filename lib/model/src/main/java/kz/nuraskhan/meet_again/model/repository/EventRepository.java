package kz.nuraskhan.meet_again.model.repository;

import java.util.Optional;
import kz.nuraskhan.meet_again.model.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

  /** The event a link leads to. */
  Optional<Event> findByShareToken(String shareToken);
}
