package com.example.dto;

import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReturnRequest {
    private Integer age;
    private BigDecimal inflation;
    private BigDecimal wage;
    private List<PConstraint> p;
    private List<QConstraint> q;
    private List<KConstraint> k;
    private List<Expense> transactions;
}
