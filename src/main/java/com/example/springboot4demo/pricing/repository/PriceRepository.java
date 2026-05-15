package com.example.springboot4demo.pricing.repository;

import com.example.springboot4demo.pricing.model.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {
    @Query("SELECT p FROM Price p WHERE p.product.id = :productId AND (p.validUntil IS NULL OR p.validUntil > :now) ORDER BY p.validFrom DESC")
    List<Price> findActiveByProductId(Long productId, LocalDateTime now);

    @Query("SELECT p FROM Price p WHERE p.product.sku = :productSku AND (p.validUntil IS NULL OR p.validUntil > :now) ORDER BY p.validFrom DESC")
    List<Price> findActiveByProductSku(String productSku, LocalDateTime now);
}

