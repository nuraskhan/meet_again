package kz.nuraskhan.hangelo.model.repository;

import kz.nuraskhan.hangelo.model.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
}
