package com.example.springboot4demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("HelloWorld Controller Tests")
class HelloWorldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /hello without authentication should return 200 OK")
    void helloWorld_withoutAuthentication_shouldReturn200() throws Exception {
        mockMvc.perform(get("/hello"))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello, World!"));
    }

    @Test
    @DisplayName("GET /hello/admin without authentication should return 401 Unauthorized")
    void helloAdmin_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get("/hello/admin"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /hello/admin with USER role should return 403 Forbidden")
    void helloAdmin_withUserRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/hello/admin")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /hello/admin with ADMIN role should return 200 OK and admin greeting")
    void helloAdmin_withAdminRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/hello/admin")
                .with(jwt().jwt(builder -> builder.subject("admin"))
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello, Admin! Authenticated as: admin"));
    }
}
