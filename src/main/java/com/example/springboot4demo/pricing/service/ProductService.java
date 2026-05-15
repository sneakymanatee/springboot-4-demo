package com.example.springboot4demo.pricing.service;

import com.example.springboot4demo.pricing.model.Product;
import com.example.springboot4demo.pricing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product createOrUpdateProduct(String sku, String name, String description, java.math.BigDecimal basePrice) {
        log.info("Creating or updating product with SKU: {}", sku);

        Product product = productRepository.findBySku(sku)
                .map(existing -> {
                    existing.setName(name);
                    existing.setDescription(description);
                    existing.setBasePrice(basePrice);
                    return existing;
                })
                .orElseGet(() -> Product.builder()
                        .sku(sku)
                        .name(name)
                        .description(description)
                        .basePrice(basePrice)
                        .build());

        return productRepository.save(product);
    }

    public Product getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + sku));
    }

    @Transactional
    public Product updateProductPartial(String sku, String name, String description, java.math.BigDecimal basePrice) {
        log.info("Partially updating product with SKU: {}", sku);

        Product existing = getProductBySku(sku);

        if (name != null) {
            existing.setName(name);
        }
        if (description != null) {
            existing.setDescription(description);
        }
        if (basePrice != null) {
            existing.setBasePrice(basePrice);
        }

        return productRepository.save(existing);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
    }
}

