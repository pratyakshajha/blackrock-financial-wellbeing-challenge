package com.example.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TaxCalculatorService {

    // For production, these values should be moved to a configuration file (e.g., application.yml)
    private static final BigDecimal SLAB_1_THRESHOLD = new BigDecimal("700000");
    private static final BigDecimal SLAB_2_THRESHOLD = new BigDecimal("1000000");
    private static final BigDecimal SLAB_3_THRESHOLD = new BigDecimal("1200000");
    private static final BigDecimal SLAB_4_THRESHOLD = new BigDecimal("1500000");

    private static final BigDecimal SLAB_2_RATE = new BigDecimal("0.10");
    private static final BigDecimal SLAB_3_RATE = new BigDecimal("0.15");
    private static final BigDecimal SLAB_4_RATE = new BigDecimal("0.20");
    private static final BigDecimal SLAB_5_RATE = new BigDecimal("0.30");

    /**
     * Calculates the total income tax based on a progressive slab system.
     * @param income The total annual income.
     * @return The calculated total tax liability.
     */
    public BigDecimal calculateTax(BigDecimal income) {
        if (income == null || income.compareTo(SLAB_1_THRESHOLD) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal taxableAmount;

        // Slab > 15L
        if (income.compareTo(SLAB_4_THRESHOLD) > 0) {
            taxableAmount = income.subtract(SLAB_4_THRESHOLD);
            tax = tax.add(taxableAmount.multiply(SLAB_5_RATE));
            income = SLAB_4_THRESHOLD; // Cap income for next slab calculation
        }

        // Slab 12L to 15L
        if (income.compareTo(SLAB_3_THRESHOLD) > 0) {
            taxableAmount = income.subtract(SLAB_3_THRESHOLD);
            tax = tax.add(taxableAmount.multiply(SLAB_4_RATE));
            income = SLAB_3_THRESHOLD;
        }

        // Slab 10L to 12L
        if (income.compareTo(SLAB_2_THRESHOLD) > 0) {
            taxableAmount = income.subtract(SLAB_2_THRESHOLD);
            tax = tax.add(taxableAmount.multiply(SLAB_3_RATE));
            income = SLAB_2_THRESHOLD;
        }

        // Slab 7L to 10L
        if (income.compareTo(SLAB_1_THRESHOLD) > 0) {
            taxableAmount = income.subtract(SLAB_1_THRESHOLD);
            tax = tax.add(taxableAmount.multiply(SLAB_2_RATE));
        }

        return tax;
    }
}