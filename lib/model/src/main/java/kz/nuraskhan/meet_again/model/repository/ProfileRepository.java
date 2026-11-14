package kz.nuraskhan.meet_again.model.repository;

import kz.nuraskhan.meet_again.model.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
}
