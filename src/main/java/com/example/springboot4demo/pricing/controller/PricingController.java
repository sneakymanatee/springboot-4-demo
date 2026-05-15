package com.example.springboot4demo.pricing.controller;

import com.example.springboot4demo.pricing.dto.PriceEvent;
import com.example.springboot4demo.pricing.dto.PriceResponse;
import com.example.springboot4demo.pricing.dto.ProductPatchRequest;
import com.example.springboot4demo.pricing.dto.ProductRequest;
import com.example.springboot4demo.pricing.model.Product;
import com.example.springboot4demo.pricing.service.PriceProducerService;
import com.example.springboot4demo.pricing.service.PriceQueryService;
import com.example.springboot4demo.pricing.service.PricingService;
import com.example.springboot4demo.pricing.service.PricingQuoteService;
import com.example.springboot4demo.pricing.service.ProductService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@Slf4j
public class PricingController {

    private final ProductService productService;
    private final PriceProducerService priceProducerService;
    private final PriceQueryService priceQueryService;
    private final PricingQuoteService pricingQuoteService;
    private final PricingService pricingService;
    private final MeterRegistry meterRegistry;

    /**
     * POST /api/v1/pricing/products - Create a new product
     */
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request) {
        return recordHttp("/api/v1/pricing/products", "POST", () -> {
            log.info("Creating product with SKU: {}", request.getSku());

            Product product = productService.createOrUpdateProduct(
                    request.getSku(),
                    request.getName(),
                    request.getDescription(),
                    request.getBasePrice()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(product);
        });
    }

    /**
     * PATCH /api/v1/pricing/products/{sku} - Update product by SKU
     */
    @PatchMapping("/products/{sku}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable("sku") String sku,
            @RequestBody ProductPatchRequest request) {
        return recordHttp("/api/v1/pricing/products/{sku}", "PATCH", () -> {
            log.info("Updating product with SKU: {}", sku);

            Product product = productService.updateProductPartial(
                    sku,
                    request.getName(),
                    request.getDescription(),
                    request.getBasePrice()
            );

            return ResponseEntity.ok(product);
        });
    }

    /**
     * POST /api/v1/pricing/prices - Send price update asynchronously to Kafka
     */
    @PostMapping("/prices")
    public ResponseEntity<Void> publishPriceUpdate(@RequestBody PriceEvent priceEvent) {
        return recordHttp("/api/v1/pricing/prices", "POST", () -> {
            log.info("Publishing price update for product SKU: {}", priceEvent.getProductSku());

            Product product = productService.getProductBySku(priceEvent.getProductSku());
            priceEvent.setProductId(product.getId());
            priceEvent.setTimestamp(LocalDateTime.now());
            priceEvent.setSource("API");

            priceProducerService.sendPriceUpdate(priceEvent);

            return ResponseEntity.accepted().build();
        });
    }

    /**
     * GET /api/v1/pricing/products/{sku}/prices - List all prices for a product by SKU
     */
    @GetMapping("/products/{sku}/prices")
    public ResponseEntity<List<PriceResponse>> getPricesByProductSku(@PathVariable("sku") String sku) {
        return recordHttp("/api/v1/pricing/products/{sku}/prices", "GET", () -> {
            log.info("Fetching prices for product SKU: {}", sku);

            List<PriceResponse> prices = priceQueryService.getActivePricesByProductSku(sku);
            return ResponseEntity.ok(prices);
        });
    }

    /**
     * GET /api/v1/pricing/products/id/{id}/prices - List all prices for a product by ID
     */
    @GetMapping("/products/id/{id}/prices")
    public ResponseEntity<List<PriceResponse>> getPricesByProductId(@PathVariable("id") Long id) {
        return recordHttp("/api/v1/pricing/products/id/{id}/prices", "GET", () -> {
            log.info("Fetching prices for product ID: {}", id);

            List<PriceResponse> prices = priceQueryService.getActivePricesByProductId(id);
            return ResponseEntity.ok(prices);
        });
    }

    /**
     * GET /api/v1/pricing/products/{sku} - Get product by SKU
     */
    @GetMapping("/products/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable("sku") String sku) {
        return recordHttp("/api/v1/pricing/products/{sku}", "GET", () -> {
            log.info("Fetching product with SKU: {}", sku);

            Product product = productService.getProductBySku(sku);
            return ResponseEntity.ok(product);
        });
    }

    /**
     * GET /api/v1/pricing/products/{sku}/quote - Build a storefront quote by SKU.
     */
    @GetMapping("/products/{sku}/quote")
    public ResponseEntity<Map<String, Object>> getQuote(
            @PathVariable("sku") String sku,
            @RequestParam(name = "quantity", defaultValue = "1") int quantity,
            @RequestParam(name = "channel", defaultValue = "WEB") String channel) {
        return recordHttp("/api/v1/pricing/products/{sku}/quote", "GET", () -> {
            log.info("Fetching quote for product SKU: {}", sku);
            Map<String, Object> quote = pricingQuoteService.buildQuote(sku, quantity, channel);
            return ResponseEntity.ok(quote);
        });
    }

    /**
     * GET /api/v1/pricing/products/{sku}/quote/segment - Segment quote with unstable mapping behavior.
     */
    @GetMapping("/products/{sku}/quote/segment")
    public ResponseEntity<Map<String, Object>> getSegmentQuote(
            @PathVariable("sku") String sku,
            @RequestParam(name = "segment", defaultValue = "flash") String segment) {
        return recordHttp("/api/v1/pricing/products/{sku}/quote/segment", "GET", () -> {
            log.info("Fetching quote segment for product SKU: {}", sku);
            BigDecimal quoted = pricingService.computeQuote(sku, segment);
            return ResponseEntity.ok(Map.of(
                    "sku", sku,
                    "segment", segment,
                    "quotedPrice", quoted
            ));
        });
    }

    private <T> ResponseEntity<T> recordHttp(String path, String method, Supplier<ResponseEntity<T>> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ResponseEntity<T> response = action.get();
            String status = String.valueOf(response.getStatusCode().value());
            meterRegistry.counter("app.http.requests.total", "path", path, "method", method, "status", status).increment();
            sample.stop(Timer.builder("app.http.request.duration")
                    .tag("path", path)
                    .tag("method", method)
                    .tag("status", status)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
            return response;
        } catch (RuntimeException ex) {
            meterRegistry.counter("app.http.requests.total", "path", path, "method", method, "status", "500").increment();
            meterRegistry.counter("app.http.errors.total", "path", path, "method", method).increment();
            sample.stop(Timer.builder("app.http.request.duration")
                    .tag("path", path)
                    .tag("method", method)
                    .tag("status", "500")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
            throw ex;
        }
    }
}
