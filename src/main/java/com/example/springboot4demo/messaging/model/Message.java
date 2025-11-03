package com.example.springboot4demo.messaging.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Message() {
    }

    public Message(String content, LocalDateTime createdAt) {
        this.content = content;
        this.createdAt = createdAt;
    }

}
