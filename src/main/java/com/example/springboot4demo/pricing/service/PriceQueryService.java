package com.example.springboot4demo.pricing.service;

import com.example.springboot4demo.pricing.dto.PriceResponse;
import com.example.springboot4demo.pricing.model.Price;
import com.example.springboot4demo.pricing.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceQueryService {

    private final PriceRepository priceRepository;

    public List<PriceResponse> getActivePricesByProductId(Long productId) {
        log.info("Fetching active prices for product ID: {}", productId);

        List<Price> prices = priceRepository.findActiveByProductId(productId, LocalDateTime.now());

        return prices.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PriceResponse> getActivePricesByProductSku(String productSku) {
        log.info("Fetching active prices for product SKU: {}", productSku);

        List<Price> prices = priceRepository.findActiveByProductSku(productSku, LocalDateTime.now());

        return prices.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PriceResponse mapToResponse(Price price) {
        return PriceResponse.builder()
                .id(price.getId())
                .productId(price.getProduct().getId())
                .amount(price.getAmount())
                .currency(price.getCurrency())
                .source(price.getSource())
                .validFrom(price.getValidFrom())
                .validUntil(price.getValidUntil())
                .createdAt(price.getCreatedAt())
                .build();
    }
}

