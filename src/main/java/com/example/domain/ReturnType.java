package com.example.domain;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum ReturnType {
    NPS(new BigDecimal("0.0711")), // 7.11%
    INDEX_FUND(new BigDecimal("0.1449")); // 14.49%

    private final BigDecimal rate;

    ReturnType(BigDecimal rate) {
        this.rate = rate;
    }

}