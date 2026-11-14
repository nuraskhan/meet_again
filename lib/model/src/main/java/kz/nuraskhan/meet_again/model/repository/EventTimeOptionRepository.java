package kz.nuraskhan.meet_again.model.repository;

import kz.nuraskhan.meet_again.model.entity.EventTimeOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTimeOptionRepository extends JpaRepository<EventTimeOption, Long> {
}
