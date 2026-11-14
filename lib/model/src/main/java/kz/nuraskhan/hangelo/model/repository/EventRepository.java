package kz.nuraskhan.hangelo.model.repository;

import java.util.Optional;
import kz.nuraskhan.hangelo.model.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

  /** The event a link leads to. */
  Optional<Event> findByShareToken(String shareToken);
}
