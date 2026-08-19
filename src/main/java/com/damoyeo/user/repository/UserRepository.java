package com.damoyeo.user.repository;

import com.damoyeo.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleSubject(String googleSubject);

    Optional<User> findByEmail(String email);
}
