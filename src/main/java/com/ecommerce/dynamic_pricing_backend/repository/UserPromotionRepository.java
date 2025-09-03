package com.ecommerce.dynamic_pricing_backend.repository;

import com.ecommerce.dynamic_pricing_backend.entity.UserPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserPromotionRepository extends JpaRepository<UserPromotion, Long> {
    List<UserPromotion> findByUserId(Long userId);

    List<UserPromotion> findByPromotionId(Long promotionId);

    @Query("SELECT COUNT(up) FROM UserPromotion up WHERE up.user.id = :userId AND up.promotion.id = :promotionId")
    long countByUserIdAndPromotionId(@Param("userId") Long userId, @Param("promotionId") Long promotionId);

    @Query("SELECT up FROM UserPromotion up WHERE up.user.id = :userId AND up.promotion.code = :code")
    List<UserPromotion> findByUserIdAndPromotionCode(@Param("userId") Long userId, @Param("code") String code);

    @Query("SELECT up FROM UserPromotion up WHERE up.usedAt BETWEEN :startDate AND :endDate")
    List<UserPromotion> findUsageBetweenDates(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    boolean existsByUserIdAndPromotionId(Long userId, Long promotionId);
}
