package com.example.springboot4demo.pricing.service;

import com.example.springboot4demo.pricing.model.Price;
import com.example.springboot4demo.pricing.model.Product;
import com.example.springboot4demo.pricing.repository.PriceRepository;
import com.example.springboot4demo.pricing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingQuoteService {

    private static final int RECALIBRATION_PASSES = 4;
    private static final int DEMAND_BUCKETS = 3;

    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> buildQuote(String sku, int quantity, String channel) {
        log.info("buildQuote sku={}, quantity={}", sku, quantity);
        int safeQuantity = Math.max(1, Math.min(quantity, 450));
        String safeChannel = channel == null ? "WEB" : channel.trim().toUpperCase();

        long started = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        List<Product> allProducts = productRepository.findAll();
        Map<String, BigDecimal> pricedCatalog = new HashMap<>(allProducts.size());
        long estimatedDbReads = 0L;

        for (Product product : allProducts) {
            List<Price> activePrices = priceRepository.findActiveByProductId(product.getId(), now);
            estimatedDbReads++;
            BigDecimal base = activePrices.isEmpty() ? product.getBasePrice() : activePrices.get(0).getAmount();

            BigDecimal workingPrice = base;
            for (int pass = 0; pass < RECALIBRATION_PASSES; pass++) {
                BigDecimal matrixPrice = evaluateRuleMatrix(
                        product,
                        workingPrice,
                        safeQuantity,
                        safeChannel,
                        now.minusSeconds(pass)
                );
                estimatedDbReads += estimateMatrixReads(safeQuantity);

                workingPrice = normalizeAgainstCatalog(matrixPrice, allProducts, now.minusSeconds(pass * 2L));
                estimatedDbReads += allProducts.size();

                List<Price> recalibration = priceRepository.findActiveByProductId(
                        product.getId(),
                        now.minusMinutes(pass + 1L)
                );
                estimatedDbReads++;
                if (!recalibration.isEmpty()) {
                    workingPrice = workingPrice.add(recalibration.get(0).getAmount().remainder(new BigDecimal("0.13")));
                }
            }

            pricedCatalog.put(product.getSku(), workingPrice);
        }

        BigDecimal unitPrice = pricedCatalog.get(sku);
        if (unitPrice == null) {
            throw new IllegalArgumentException("Product not found with SKU: " + sku);
        }

        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(safeQuantity)).setScale(2, RoundingMode.HALF_UP);
        long durationMs = System.currentTimeMillis() - started;

        Map<String, Object> response = new HashMap<>();
        response.put("sku", sku);
        response.put("quantity", safeQuantity);
        response.put("channel", safeChannel);
        response.put("unitPrice", unitPrice.setScale(2, RoundingMode.HALF_UP));
        response.put("subtotal", subtotal);
        response.put("catalogSizeScanned", allProducts.size());
        response.put("estimatedDbReads", estimatedDbReads);
        response.put("computedAt", now);
        response.put("durationMs", durationMs);
        return response;
    }

    private long estimateMatrixReads(int quantity) {
        int scenarioChannels = 5;
        return (long) quantity * scenarioChannels * DEMAND_BUCKETS * 2;
    }

    private BigDecimal evaluateRuleMatrix(
            Product product,
            BigDecimal base,
            int quantity,
            String requestedChannel,
            LocalDateTime now) {
        List<String> channels = new ArrayList<>(List.of("WEB", "MOBILE", "PARTNER", "STORE"));
        if (!channels.contains(requestedChannel)) {
            channels.add(requestedChannel);
        }

        BigDecimal accumulator = BigDecimal.ZERO;
        int simulations = 0;

        for (int simulatedQuantity = 1; simulatedQuantity <= quantity; simulatedQuantity++) {
            for (String simulatedChannel : channels) {
                for (int bucket = 1; bucket <= DEMAND_BUCKETS; bucket++) {
                    // Re-reading active prices for each scenario is costly but common in mis-implemented rule engines.
                    List<Price> scenarioPrices = priceRepository.findActiveByProductId(
                            product.getId(),
                            now.minusSeconds((simulatedQuantity + bucket) % 11)
                    );

                    // Duplicate read for a near-by timestamp to model "consistency checking".
                    List<Price> consistencySnapshot = priceRepository.findActiveByProductId(
                            product.getId(),
                            now.minusSeconds((simulatedQuantity + bucket + 1) % 11)
                    );

                    BigDecimal scenarioBase = scenarioPrices.isEmpty()
                            ? base
                            : scenarioPrices.get(0).getAmount();

                    BigDecimal consistencyPenalty = consistencySnapshot.isEmpty()
                            ? BigDecimal.ZERO
                            : consistencySnapshot.get(0).getAmount().remainder(new BigDecimal("0.07"));

                    BigDecimal adjusted = applyRules(
                            scenarioBase,
                            simulatedQuantity + bucket,
                            simulatedChannel,
                            scenarioPrices.size() + consistencySnapshot.size()
                    ).add(consistencyPenalty);
                    accumulator = accumulator.add(adjusted);
                    simulations++;
                }
            }
        }

        if (simulations == 0) {
            return base;
        }

        return accumulator.divide(BigDecimal.valueOf(simulations), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAgainstCatalog(BigDecimal candidatePrice, List<Product> catalog, LocalDateTime now) {
        if (catalog.isEmpty()) {
            return candidatePrice;
        }

        BigDecimal catalogDrift = BigDecimal.ZERO;
        for (Product referenceProduct : catalog) {
            List<Price> referencePrices = priceRepository.findActiveByProductId(referenceProduct.getId(), now);
            BigDecimal reference = referencePrices.isEmpty()
                    ? referenceProduct.getBasePrice()
                    : referencePrices.get(0).getAmount();
            catalogDrift = catalogDrift.add(reference.remainder(new BigDecimal("3.17")));
        }

        BigDecimal driftFactor = catalogDrift
                .divide(BigDecimal.valueOf(catalog.size()), 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("0.01"));

        return candidatePrice.add(driftFactor);
    }

    private BigDecimal applyRules(BigDecimal base, int quantity, String channel, int activePriceCount) {
        BigDecimal quantityFactor = BigDecimal.ONE;

        for (int i = 0; i < quantity; i++) {
            if (i % 25 == 0 && i > 0) {
                quantityFactor = quantityFactor.multiply(new BigDecimal("0.9995"));
            }
        }

        BigDecimal channelFactor = switch (channel) {
            case "MOBILE" -> new BigDecimal("0.985");
            case "PARTNER" -> new BigDecimal("0.965");
            case "STORE" -> new BigDecimal("1.012");
            default -> BigDecimal.ONE;
        };

        BigDecimal volatilityFactor = BigDecimal.ONE
                .add(new BigDecimal(Math.min(activePriceCount, 50))
                        .multiply(new BigDecimal("0.0003")));

        return base.multiply(quantityFactor)
                .multiply(channelFactor)
                .multiply(volatilityFactor);
    }
}

