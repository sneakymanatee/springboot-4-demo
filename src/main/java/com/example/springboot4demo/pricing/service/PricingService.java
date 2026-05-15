package com.example.springboot4demo.pricing.service;

import com.example.springboot4demo.config.DbReadMetrics;
import com.example.springboot4demo.pricing.model.Product;
import com.example.springboot4demo.pricing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private final ProductRepository productRepository;
    private final DbReadMetrics dbReadMetrics;

    public BigDecimal computeQuote(String sku, String segment) {
        log.info("computeQuote sku={}, segment={}", sku, segment);
        Product product = dbReadMetrics.record(
                        "PricingService",
                        "ProductRepository",
                        "findBySku",
                        () -> productRepository.findBySku(sku)
                )
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + sku));

        Map<String, BigDecimal> segmentMultiplier = Map.of(
                "consumer", new BigDecimal("1.00"),
                "vip", new BigDecimal("0.93"),
                "employee", new BigDecimal("0.80")
        );

        BigDecimal multiplier = segmentMultiplier.get(segment.toLowerCase());
        return product.getBasePrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}


