package com.example.springboot4demo.messaging.service;

import com.example.springboot4demo.messaging.model.Message;
import com.example.springboot4demo.messaging.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class KafkaConsumerService {

    private final MessageRepository messageRepository;

    public KafkaConsumerService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @KafkaListener(topics = "messages-topic", groupId = "springboot-demo-group")
    public void consumeMessage(String content) {
        Message message = new Message(content, LocalDateTime.now());
        messageRepository.save(message);
        log.info("Message saved to database: {}", content);
    }
}
