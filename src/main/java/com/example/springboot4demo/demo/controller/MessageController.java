package com.example.springboot4demo.demo.controller;

import com.example.springboot4demo.demo.messaging.model.Message;
import com.example.springboot4demo.demo.messaging.repository.MessageRepository;
import com.example.springboot4demo.demo.messaging.service.KafkaProducerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final KafkaProducerService kafkaProducerService;
    private final MessageRepository messageRepository;

    public MessageController(KafkaProducerService kafkaProducerService, MessageRepository messageRepository) {
        this.kafkaProducerService = kafkaProducerService;
        this.messageRepository = messageRepository;
    }

    @PostMapping
    public String sendMessage(@RequestBody String content) {
        kafkaProducerService.sendMessage(content);
        return "Message sent to Kafka: " + content;
    }

    @GetMapping
    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
