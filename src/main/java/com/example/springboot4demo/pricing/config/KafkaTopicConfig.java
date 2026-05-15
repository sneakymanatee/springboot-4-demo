package com.example.springboot4demo.pricing.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic priceUpdateTopic() {
        return TopicBuilder.name("product-prices")
                .partitions(3)
                .replicas(1)
                .build();
    }
}

