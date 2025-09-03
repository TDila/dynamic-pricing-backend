package com.ecommerce.dynamic_pricing_backend.repository;

import com.ecommerce.dynamic_pricing_backend.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    long countOrdersByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN false ELSE true END FROM Order o WHERE o.user.id = :userId")
    boolean isFirstTimeBuyer(@Param("userId") Long userId);
}
