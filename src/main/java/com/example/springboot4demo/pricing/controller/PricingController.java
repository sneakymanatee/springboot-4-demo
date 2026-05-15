package com.example.springboot4demo.pricing.controller;

import com.example.springboot4demo.pricing.dto.PriceEvent;
import com.example.springboot4demo.pricing.dto.PriceResponse;
import com.example.springboot4demo.pricing.dto.ProductPatchRequest;
import com.example.springboot4demo.pricing.dto.ProductRequest;
import com.example.springboot4demo.pricing.model.Product;
import com.example.springboot4demo.pricing.service.PriceProducerService;
import com.example.springboot4demo.pricing.service.PriceQueryService;
import com.example.springboot4demo.pricing.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@Slf4j
public class PricingController {

    private final ProductService productService;
    private final PriceProducerService priceProducerService;
    private final PriceQueryService priceQueryService;

    /**
     * POST /api/v1/pricing/products - Create a new product
     */
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request) {
        log.info("Creating product with SKU: {}", request.getSku());

        Product product = productService.createOrUpdateProduct(
                request.getSku(),
                request.getName(),
                request.getDescription(),
                request.getBasePrice()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    /**
     * PATCH /api/v1/pricing/products/{sku} - Update product by SKU
     */
    @PatchMapping("/products/{sku}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable("sku") String sku,
            @RequestBody ProductPatchRequest request) {
        log.info("Updating product with SKU: {}", sku);

        Product product = productService.updateProductPartial(
                sku,
                request.getName(),
                request.getDescription(),
                request.getBasePrice()
        );

        return ResponseEntity.ok(product);
    }

    /**
     * POST /api/v1/pricing/prices - Send price update asynchronously to Kafka
     */
    @PostMapping("/prices")
    public ResponseEntity<Void> publishPriceUpdate(@RequestBody PriceEvent priceEvent) {
        log.info("Publishing price update for product SKU: {}", priceEvent.getProductSku());

        // Verify product exists
        Product product = productService.getProductBySku(priceEvent.getProductSku());
        priceEvent.setProductId(product.getId());
        priceEvent.setTimestamp(LocalDateTime.now());
        priceEvent.setSource("API");

        // Send async to Kafka
        priceProducerService.sendPriceUpdate(priceEvent);

        return ResponseEntity.accepted().build();
    }

    /**
     * GET /api/v1/pricing/products/{sku}/prices - List all prices for a product by SKU
     */
    @GetMapping("/products/{sku}/prices")
    public ResponseEntity<List<PriceResponse>> getPricesByProductSku(@PathVariable("sku") String sku) {
        log.info("Fetching prices for product SKU: {}", sku);

        List<PriceResponse> prices = priceQueryService.getActivePricesByProductSku(sku);
        return ResponseEntity.ok(prices);
    }

    /**
     * GET /api/v1/pricing/products/id/{id}/prices - List all prices for a product by ID
     */
    @GetMapping("/products/id/{id}/prices")
    public ResponseEntity<List<PriceResponse>> getPricesByProductId(@PathVariable("id") Long id) {
        log.info("Fetching prices for product ID: {}", id);

        List<PriceResponse> prices = priceQueryService.getActivePricesByProductId(id);
        return ResponseEntity.ok(prices);
    }

    /**
     * GET /api/v1/pricing/products/{sku} - Get product by SKU
     */
    @GetMapping("/products/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable("sku") String sku) {
        log.info("Fetching product with SKU: {}", sku);

        Product product = productService.getProductBySku(sku);
        return ResponseEntity.ok(product);
    }
}

