//package com.example.service;
//
//import com.example.domain.ReturnType;
//import com.example.dto.*;
//import com.example.mapper.TransactionMapper;
//import com.example.mapper.TransactionMapperImpl;
//import com.example.model.Transaction;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class ReturnServiceTest {
//
//    @Mock
//    private TransactionService transactionService;
//    @Mock
//    private TransactionMapper transactionMapper; // Mocked, but real mapper behavior is often needed for DTOs
//    @Mock
//    private TaxCalculatorService taxCalculatorService;
//
//    @InjectMocks
//    private ReturnService returnService;
//
//    private LocalDateTime now;
//
//    @BeforeEach
//    void setUp() {
//        now = LocalDateTime.now();
//
//        // Mock mapper behavior for DTO to Entity conversion and vice-versa
//        TransactionMapper realMapper = new TransactionMapperImpl(); // Use real implementation for DTO conversions
//        when(transactionMapper.toDtoList(anyList())).thenAnswer(invocation -> {
//            List<Transaction> entities = invocation.getArgument(0);
//            return entities.stream().map(realMapper::toDto).collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
//        });
//    }
//
//    @Test
//    @DisplayName("Should calculate NPS returns correctly with tax benefit")
//    void returns_NPS_calculatesCorrectly() {
//        // Given
//        BigDecimal expenseAmount = new BigDecimal("250");
//        BigDecimal remanent = new BigDecimal("50");
//        BigDecimal ceiling = new BigDecimal("300");
//        BigDecimal wage = new BigDecimal("600000"); // Annual income
//        BigDecimal inflation = new BigDecimal("5.5"); // 5.5%
//        int age = 29;
//        int investmentPeriod = 60 - age; // 31 years
//
//        Expense expense = Expense.builder().amount(expenseAmount).date(now).build();
//        Transaction parsedTransaction = Transaction.builder().amount(expenseAmount).date(now).remanent(remanent).ceiling(ceiling).build();
//        ValidTransactionDto validTx = ValidTransactionDto.builder().amount(expenseAmount).date(now).remanent(remanent).ceiling(ceiling).inKPeriod(true).build();
//
//        KConstraint kPeriod = KConstraint.builder().start(now.minusYears(1)).end(now.plusYears(1)).build();
//
//        ReturnRequest request = ReturnRequest.builder()
//                .age(age)
//                .inflation(inflation)
//                .wage(wage)
//                .transactions(Collections.singletonList(expense))
//                .k(Collections.singletonList(kPeriod))
//                .build();
//
//        // Mock TransactionService behavior
//        when(transactionService.parseExpenses(anyList())).thenReturn(Collections.singletonList(parsedTransaction));
//        when(transactionService.validateTemporalConstraints(any(ValidatorRequest.class)))
//                .thenReturn(ValidatorResponse.builder().valid(Collections.singletonList(validTx)).invalid(Collections.emptyList()).build());
//
//        // Mock TaxCalculatorService behavior
//        when(taxCalculatorService.calculateTax(any(BigDecimal.class))).thenReturn(BigDecimal.ZERO); // Simplified for test
//
//        // When
//        ReturnResponse response = returnService.returns(request, ReturnType.NPS);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(expenseAmount.setScale(2, RoundingMode.HALF_UP), response.getTransactionsTotalAmount());
//        assertEquals(ceiling.setScale(2, RoundingMode.HALF_UP), response.getTransactionsTotalCeiling());
//        assertEquals(1, response.getSavingsByDates().size());
//
//        Savings savings = response.getSavingsByDates().get(0);
//        assertEquals(remanent.setScale(2, RoundingMode.HALF_UP), savings.getAmount());
//        assertEquals(kPeriod.getStart(), savings.getStart());
//        assertEquals(kPeriod.getEnd(), savings.getEnd());
//
//        // Expected values based on example:
//        // Principal = 50
//        // Rate = 0.0711 (NPS)
//        // Years = 31
//        // Nominal = 50 * (1 + 0.0711)^31 = 50 * 8.41 = 420.5
//        // Real = 420.5 / (1 + 0.055)^31 = 420.5 / 5.258 = 79.97
//        // Profit = 79.97 - 50 = 29.97
//        // Tax Benefit = 0 (since income is 6L, no tax)
//
//        assertEquals(new BigDecimal("29.97").setScale(2, RoundingMode.HALF_UP), savings.getProfits());
//        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), savings.getTaxBenefit());
//    }
//
//    @Test
//    @DisplayName("Should calculate Index Fund returns correctly without tax benefit")
//    void returns_IndexFund_calculatesCorrectly() {
//        // Given
//        BigDecimal expenseAmount = new BigDecimal("250");
//        BigDecimal remanent = new BigDecimal("50");
//        BigDecimal ceiling = new BigDecimal("300");
//        BigDecimal wage = new BigDecimal("600000"); // Annual income
//        BigDecimal inflation = new BigDecimal("5.5"); // 5.5%
//        int age = 29;
//        int investmentPeriod = 60 - age; // 31 years
//
//        Expense expense = Expense.builder().amount(expenseAmount).date(now).build();
//        Transaction parsedTransaction = Transaction.builder().amount(expenseAmount).date(now).remanent(remanent).ceiling(ceiling).build();
//        ValidTransactionDto validTx = ValidTransactionDto.builder().amount(expenseAmount).date(now).remanent(remanent).ceiling(ceiling).inKPeriod(true).build();
//
//        KConstraint kPeriod = KConstraint.builder().start(now.minusYears(1)).end(now.plusYears(1)).build();
//
//        ReturnRequest request = ReturnRequest.builder()
//                .age(age)
//                .inflation(inflation)
//                .wage(wage)
//                .transactions(Collections.singletonList(expense))
//                .k(Collections.singletonList(kPeriod))
//                .build();
//
//        // Mock TransactionService behavior
//        when(transactionService.parseExpenses(anyList())).thenReturn(Collections.singletonList(parsedTransaction));
//        when(transactionService.validateTemporalConstraints(any(ValidatorRequest.class)))
//                .thenReturn(ValidatorResponse.builder().valid(Collections.singletonList(validTx)).invalid(Collections.emptyList()).build());
//
//        // When
//        ReturnResponse response = returnService.returns(request, ReturnType.INDEX_FUND);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(expenseAmount.setScale(2, RoundingMode.HALF_UP), response.getTransactionsTotalAmount());
//        assertEquals(ceiling.setScale(2, RoundingMode.HALF_UP), response.getTransactionsTotalCeiling());
//        assertEquals(1, response.getSavingsByDates().size());
//
//        Savings savings = response.getSavingsByDates().get(0);
//        assertEquals(remanent.setScale(2, RoundingMode.HALF_UP), savings.getAmount());
//        assertEquals(kPeriod.getStart(), savings.getStart());
//        assertEquals(kPeriod.getEnd(), savings.getEnd());
//
//        // Expected values based on example:
//        // Principal = 50
//        // Rate = 0.1449 (Index Fund)
//        // Years = 31
//        // Nominal = 50 * (1 + 0.1449)^31 = 50 * 66.34 = 3317
//        // Real = 3317 / (1 + 0.055)^31 = 3317 / 5.258 = 630.85
//        // Profit = 630.85 - 50 = 580.85
//
//        assertEquals(new BigDecimal("580.85").setScale(2, RoundingMode.HALF_UP), savings.getProfits());
//        assertEquals(null, savings.getTaxBenefit()); // No tax benefit for Index Fund
//    }
//
//    @Test
//    @DisplayName("Should throw IllegalArgumentException if age or inflation is null")
//    void returns_nullAgeOrInflation_throwsException() {
//        ReturnRequest request = ReturnRequest.builder().transactions(Collections.emptyList()).build();
//        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
//                returnService.returns(request, ReturnType.NPS));
//    }
//}