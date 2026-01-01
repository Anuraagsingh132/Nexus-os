package com.nexusos.api.identity.repository;

import com.nexusos.api.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query(value = "SELECT TO_CHAR(created_at, 'YYYY-MM-DD') as label, COUNT(id) as value " +
           "FROM users " +
           "WHERE created_at > current_date - interval '7 days' " +
           "GROUP BY TO_CHAR(created_at, 'YYYY-MM-DD') " +
           "ORDER BY TO_CHAR(created_at, 'YYYY-MM-DD') DESC", nativeQuery = true)
    List<Object[]> countUsersPerDayNative();
}
