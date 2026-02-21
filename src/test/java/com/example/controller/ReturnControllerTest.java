//package com.example.controller;
//
//import com.example.domain.ReturnType;
//import com.example.dto.ReturnRequest;
//import com.example.dto.ReturnResponse;
//import com.example.dto.Savings;
//import com.example.service.ReturnService;
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
//import java.util.Collections;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(ReturnController.class)
//class ReturnControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private ReturnService returnService;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private ReturnRequest testRequest;
//    private ReturnResponse testResponse;
//    private LocalDateTime testDateTime;
//
//    @BeforeEach
//    void setUp() {
//        testDateTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
//        testRequest = ReturnRequest.builder()
//                .age(30)
//                .inflation(new BigDecimal("5.0"))
//                .wage(new BigDecimal("1000000"))
//                .transactions(Collections.emptyList())
//                .k(Collections.emptyList())
//                .build();
//
//        testResponse = ReturnResponse.builder()
//                .transactionsTotalAmount(new BigDecimal("100.00"))
//                .transactionsTotalCeiling(new BigDecimal("200.00"))
//                .savingsByDates(Collections.singletonList(
//                        Savings.builder()
//                                .start(testDateTime)
//                                .end(testDateTime.plusYears(1))
//                                .amount(new BigDecimal("50.00"))
//                                .profits(new BigDecimal("25.00"))
//                                .taxBenefit(new BigDecimal("0.00"))
//                                .build()
//                ))
//                .build();
//    }
//
//    @Test
//    @DisplayName("POST /returns:nps should return NPS specific returns")
//    void parseNps_validRequest_returnsNPSResponse() throws Exception {
//        when(returnService.returns(any(ReturnRequest.class), eq(ReturnType.NPS))).thenReturn(testResponse);
//
//        mockMvc.perform(post("/returns:nps")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(testRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.transactionsTotalAmount").value(100.00))
//                .andExpect(jsonPath("$.savingsByDates[0].profits").value(25.00));
//    }
//
//    @Test
//    @DisplayName("POST /returns:index should return Index Fund specific returns")
//    void parseIndex_validRequest_returnsIndexFundResponse() throws Exception {
//        when(returnService.returns(any(ReturnRequest.class), eq(ReturnType.INDEX_FUND))).thenReturn(testResponse);
//
//        mockMvc.perform(post("/returns:index")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(testRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.transactionsTotalAmount").value(100.00))
//                .andExpect(jsonPath("$.savingsByDates[0].profits").value(25.00));
//    }
//}