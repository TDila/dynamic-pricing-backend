package com.ecommerce.dynamic_pricing_backend.repository;

import com.ecommerce.dynamic_pricing_backend.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCode(String code);

    @Query("SELECT p FROM Promotion p WHERE p.active = true AND p.startDate <= :now AND p.endDate >= :now")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM Promotion p WHERE p.code = :code AND p.active = true AND " +
            "p.startDate <= :now AND p.endDate >= :now AND " +
            "(p.usageLimit IS NULL OR p.usedCount < p.usageLimit)")
    Optional<Promotion> findValidPromotionByCode(@Param("code") String code, @Param("now") LocalDateTime now);

    @Query("SELECT p FROM Promotion p WHERE p.active = true AND p.startDate <= :now AND p.endDate >= :now AND " +
            "(p.category IS NULL OR p.category = :category)")
    List<Promotion> findActivePromotionsByCategory(@Param("category") String category, @Param("now") LocalDateTime now);

    @Query("SELECT p FROM Promotion p WHERE p.active = true AND p.startDate <= :now AND p.endDate >= :now AND " +
            "(p.brand IS NULL OR p.brand = :brand)")
    List<Promotion> findActivePromotionsByBrand(@Param("brand") String brand, @Param("now") LocalDateTime now);

    List<Promotion> findByActiveTrue();

    @Query("SELECT p FROM Promotion p WHERE p.endDate < :now")
    List<Promotion> findExpiredPromotions(@Param("now") LocalDateTime now);
}
