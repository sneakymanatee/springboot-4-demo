package com.example.springboot4demo.demo.messaging.repository;

import com.example.springboot4demo.demo.messaging.model.Message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
}
