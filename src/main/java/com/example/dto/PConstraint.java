package com.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PConstraint {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime start;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime end;
    private BigDecimal extra;
}