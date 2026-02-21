//package com.example.controller;
//
//import com.example.dto.*;
//import com.example.mapper.TransactionMapper;
//import com.example.model.Transaction;
//import com.example.service.TransactionService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import tools.jackson.databind.ObjectMapper;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(TransactionController.class)
//class TransactionControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private TransactionService transactionService;
//
//    @MockitoBean
//    private TransactionMapper transactionMapper;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private LocalDateTime testDateTime;
//
//    @BeforeEach
//    void setUp() {
//        testDateTime = LocalDateTime.of(2023, 1, 1, 10, 0, 0);
//    }
//
//    @Test
//    @DisplayName("POST /transactions:parse should return parsed transactions")
//    void parse_validExpenses_returnsTransactionDtos() throws Exception {
//        Expense expense = Expense.builder().amount(new BigDecimal("123.45")).date(testDateTime).build();
//        Transaction transaction = Transaction.builder().amount(new BigDecimal("123.45")).date(testDateTime)
//                .ceiling(new BigDecimal("200.0")).remanent(new BigDecimal("76.55")).build();
//        TransactionDto transactionDto = TransactionDto.builder().amount(new BigDecimal("123.45")).date(testDateTime)
//                .ceiling(new BigDecimal("200.0")).remanent(new BigDecimal("76.55")).build();
//
//        when(transactionService.parseExpenses(anyList())).thenReturn(Collections.singletonList(transaction));
//        when(transactionMapper.toResponseList(anyList())).thenReturn(Collections.singletonList(transactionDto));
//
//        mockMvc.perform(post("/transactions:parse")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(Collections.singletonList(expense))))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].amount").value(123.45))
//                .andExpect(jsonPath("$[0].remanent").value(76.55));
//    }
//
//    @Test
//    @DisplayName("POST /transactions:validator should return validation response")
//    void validator_validRequest_returnsValidatorResponse() throws Exception {
//        ValidatorRequest request = ValidatorRequest.builder()
//                .transactions(Collections.singletonList(TransactionDto.builder().amount(new BigDecimal("100")).date(testDateTime).build()))
//                .wage(new BigDecimal("1000"))
//                .build();
//        ValidatorResponse response = ValidatorResponse.builder()
//                .valid(Collections.singletonList(ValidTransactionDto.builder().amount(new BigDecimal("100")).date(testDateTime).build()))
//                .invalid(Collections.emptyList())
//                .build();
//
//        when(transactionService.validate(any(ValidatorRequest.class))).thenReturn(response);
//
//        mockMvc.perform(post("/transactions:validator")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.valid[0].amount").value(100.0))
//                .andExpect(jsonPath("$.invalid").isEmpty());
//    }
//
//    @Test
//    @DisplayName("POST /transactions:filter should return temporal validation response")
//    void temporalValidator_validRequest_returnsValidatorResponse() throws Exception {
//        ValidatorRequest request = ValidatorRequest.builder()
//                .transactions(Collections.singletonList(TransactionDto.builder().amount(new BigDecimal("100")).date(testDateTime).build()))
//                .wage(new BigDecimal("1000"))
//                .k(Collections.singletonList(KConstraint.builder().start(testDateTime.minusDays(1)).end(testDateTime.plusDays(1)).build()))
//                .build();
//        ValidatorResponse response = ValidatorResponse.builder()
//                .valid(Collections.singletonList(ValidTransactionDto.builder().amount(new BigDecimal("100")).date(testDateTime).inKPeriod(true).build()))
//                .invalid(Collections.emptyList())
//                .build();
//
//        when(transactionService.validateTemporalConstraints(any(ValidatorRequest.class))).thenReturn(response);
//
//        mockMvc.perform(post("/transactions:filter")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.valid[0].amount").value(100.0))
//                .andExpect(jsonPath("$.valid[0].inKPeriod").value(true));
//    }
//}