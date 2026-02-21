package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ValidatorResponse {
    private List<ValidTransactionDto> valid;
    private List<InvalidTransactionDto> invalid;
}