package com.example.team3Project.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByEmail(String email);

    Optional<User> findByUsernameAndEmail(String username, String email);

    Optional<User> findByLoginIdAndEmail(String loginId, String email);

    Optional<User> findByProviderId(String providerId);

    boolean existsByUsername(String username);
}
