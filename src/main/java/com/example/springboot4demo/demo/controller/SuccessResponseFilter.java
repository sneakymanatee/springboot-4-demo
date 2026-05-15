package com.example.springboot4demo.demo.controller;

import com.example.springboot4demo.demo.messaging.config.MetricsConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SuccessResponseFilter extends OncePerRequestFilter {
    private final MetricsConfig metricsConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        int status = response.getStatus();
        if (status >= 200 && status < 300) {
            String path = request.getRequestURI();
            metricsConfig.incrementSuccess(path);
        }
    }
}
