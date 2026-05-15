package com.example.springboot4demo.pricing.service;

import com.example.springboot4demo.config.DbReadMetrics;
import com.example.springboot4demo.pricing.dto.PriceEvent;
import com.example.springboot4demo.pricing.model.Price;
import com.example.springboot4demo.pricing.model.Product;
import com.example.springboot4demo.pricing.repository.PriceRepository;
import com.example.springboot4demo.pricing.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceConsumerService {

    private final PriceRepository priceRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final DbReadMetrics dbReadMetrics;

    @KafkaListener(topics = "product-prices", groupId = "price-processor")
    @Transactional
    public void consumePriceUpdate(String message) {
        try {
            log.info("Received price update message: {}", message);

            PriceEvent priceEvent = objectMapper.readValue(message, PriceEvent.class);

            Product product = dbReadMetrics.record(
                            "PriceConsumerService",
                            "ProductRepository",
                            "findById",
                            () -> productRepository.findById(priceEvent.getProductId())
                    )
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + priceEvent.getProductId()));

            // Upsert: Create new price record
            Price price = Price.builder()
                    .product(product)
                    .amount(priceEvent.getAmount())
                    .currency(priceEvent.getCurrency())
                    .source(priceEvent.getSource())
                    .validFrom(priceEvent.getTimestamp())
                    .build();

            priceRepository.save(price);
            log.info("Price stored successfully for product ID: {} with amount: {}", priceEvent.getProductId(), priceEvent.getAmount());

        } catch (Exception e) {
            log.error("Error processing price update message", e);
        }
    }
}

