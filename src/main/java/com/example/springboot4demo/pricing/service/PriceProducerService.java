package com.example.springboot4demo.pricing.service;

import com.example.springboot4demo.pricing.dto.PriceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "product-prices";

    @Async
    public void sendPriceUpdate(PriceEvent event) {
        try {
            log.info("Sending price update for product SKU: {}", event.getProductSku());

            String payload = objectMapper.writeValueAsString(event);

            Message<String> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC)
                    .setHeader(KafkaHeaders.KEY, event.getProductSku())
                    .build();

            kafkaTemplate.send(message);
            log.info("Price update sent successfully for SKU: {}", event.getProductSku());
        } catch (Exception e) {
            log.error("Error sending price update for SKU: {}", event.getProductSku(), e);
        }
    }
}

