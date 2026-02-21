package com.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Setter(AccessLevel.NONE) // prevent setting via builder
    private Long id;

    private BigDecimal amount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;
    private BigDecimal ceiling;
    private BigDecimal remanent;
}
