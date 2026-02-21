package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxCalculatorServiceTest {

    private TaxCalculatorService taxCalculatorService;

    @BeforeEach
    void setUp() {
        taxCalculatorService = new TaxCalculatorService();
    }

    @Test
    @DisplayName("Should return 0 tax for income below or equal to 7L")
    void calculateTax_incomeBelowOrEqual7L_returnsZero() {
        assertEquals(BigDecimal.ZERO, taxCalculatorService.calculateTax(new BigDecimal("0")));
        assertEquals(BigDecimal.ZERO, taxCalculatorService.calculateTax(new BigDecimal("699999")));
        assertEquals(BigDecimal.ZERO, taxCalculatorService.calculateTax(new BigDecimal("700000")));
        assertEquals(BigDecimal.ZERO, taxCalculatorService.calculateTax(null));
    }

    @Test
    @DisplayName("Should calculate tax correctly for income in 7L-10L slab")
    void calculateTax_incomeIn7Lto10L_calculatesCorrectly() {
        // Income = 800,000. Taxable: 100,000 @ 10% = 10,000
        BigDecimal expectedTax = new BigDecimal("10000.00").setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedTax, taxCalculatorService.calculateTax(new BigDecimal("800000")));
    }

    @Test
    @DisplayName("Should calculate tax correctly for income in 10L-12L slab")
    void calculateTax_incomeIn10Lto12L_calculatesCorrectly() {
        // Income = 1,100,000
        // 7L-10L: 300,000 @ 10% = 30,000
        // 10L-12L: 100,000 @ 15% = 15,000
        // Total = 45,000
        BigDecimal expectedTax = new BigDecimal("45000.00").setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedTax, taxCalculatorService.calculateTax(new BigDecimal("1100000")));
    }

    @Test
    @DisplayName("Should calculate tax correctly for income in 12L-15L slab")
    void calculateTax_incomeIn12Lto15L_calculatesCorrectly() {
        // Income = 1,300,000
        // 7L-10L: 300,000 @ 10% = 30,000
        // 10L-12L: 200,000 @ 15% = 30,000
        // 12L-15L: 100,000 @ 20% = 20,000
        // Total = 80,000
        BigDecimal expectedTax = new BigDecimal("80000.00").setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedTax, taxCalculatorService.calculateTax(new BigDecimal("1300000")));
    }

    @Test
    @DisplayName("Should calculate tax correctly for income above 15L slab")
    void calculateTax_incomeAbove15L_calculatesCorrectly() {
        // Income = 1,600,000
        // 7L-10L: 300,000 @ 10% = 30,000
        // 10L-12L: 200,000 @ 15% = 30,000
        // 12L-15L: 300,000 @ 20% = 60,000
        // >15L: 100,000 @ 30% = 30,000
        // Total = 150,000
        BigDecimal expectedTax = new BigDecimal("150000.00").setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedTax, taxCalculatorService.calculateTax(new BigDecimal("1600000")));
    }
}