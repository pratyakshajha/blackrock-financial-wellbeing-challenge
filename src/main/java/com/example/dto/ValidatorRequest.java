package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatorRequest {
    private List<PConstraint> p;
    private List<QConstraint> q;
    private List<KConstraint> k;
    private BigDecimal wage;
    private List<TransactionDto> transactions;
}
