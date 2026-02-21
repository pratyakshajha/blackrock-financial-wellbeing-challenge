package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReturnResponse {
    private BigDecimal transactionsTotalAmount;
    private BigDecimal transactionsTotalCeiling;
    private List<Savings> savingsByDates;
}
