package com.agripulse.backend.security;

import com.agripulse.backend.user.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Lets the token check ask one cheap question: is this account still enabled, and which token version is current? */
public interface TokenStateRepository extends Repository<User, Long> {
    @Query("select u.tokenVersion from User u where u.id = :id and u.enabled = true")
    Optional<Integer> activeTokenVersion(@Param("id") Long id);
}
