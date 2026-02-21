//package com.example.service;
//
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
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class TransactionServiceTest {
//
//    @Mock
//    private TransactionMapper transactionMapper;
//
//    @InjectMocks
//    private TransactionService transactionService;
//
//    private LocalDateTime now;
//    private TransactionMapper realMapper; // Use a real mapper for some tests where DTO conversion is critical
//
//    @BeforeEach
//    void setUp() {
//        now = LocalDateTime.now();
//        // Initialize a real mapper for parseExpenses and other methods that need actual mapping
//        realMapper = new TransactionMapperImpl();
//        // When mocking, ensure to provide a default behavior for toEntityList and toDtoList
//        when(transactionMapper.toEntityList(anyList())).thenAnswer(invocation -> {
//            List<TransactionDto> dtos = invocation.getArgument(0);
//            return dtos.stream().map(realMapper::toEntity).collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
//        });
//        when(transactionMapper.toDtoList(anyList())).thenAnswer(invocation -> {
//            List<Transaction> entities = invocation.getArgument(0);
//            return entities.stream().map(realMapper::toDto).collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
//        });
//        when(transactionMapper.toInvalidDto(any(Transaction.class), anyString())).thenAnswer(invocation -> {
//            Transaction tx = invocation.getArgument(0);
//            String msg = invocation.getArgument(1);
//            return InvalidTransactionDto.builder()
//                    .amount(tx.getAmount()).date(tx.getDate()).ceiling(tx.getCeiling()).remanent(tx.getRemanent())
//                    .message(msg).build();
//        });
//        when(transactionMapper.toValidDto(any(Transaction.class), anyBoolean())).thenAnswer(invocation -> {
//            Transaction tx = invocation.getArgument(0);
//            Boolean inK = invocation.getArgument(1);
//            return ValidTransactionDto.builder()
//                    .amount(tx.getAmount()).date(tx.getDate()).ceiling(tx.getCeiling()).remanent(tx.getRemanent())
//                    .inKPeriod(inK).build();
//        });
//        when(transactionMapper.toValidDtoList(anyList())).thenAnswer(invocation -> {
//            List<Transaction> entities = invocation.getArgument(0);
//            return entities.stream().map(tx -> realMapper.toValidDto(tx, null)).collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
//        });
//    }
//
//    @Test
//    @DisplayName("parseExpenses should correctly calculate ceiling and remanent")
//    void parseExpenses_validInput_calculatesCorrectly() {
//        Expense expense1 = Expense.builder().amount(new BigDecimal("250")).date(now).build();
//        Expense expense2 = Expense.builder().amount(new BigDecimal("375")).date(now.plusDays(1)).build();
//
//        List<Transaction> transactions = transactionService.parseExpenses(Arrays.asList(expense1, expense2));
//
//        assertEquals(2, transactions.size());
//        assertEquals(new BigDecimal("300.0"), transactions.get(0).getCeiling());
//        assertEquals(new BigDecimal("50.0"), transactions.get(0).getRemanent());
//        assertEquals(new BigDecimal("400.0"), transactions.get(1).getCeiling());
//        assertEquals(new BigDecimal("25.0"), transactions.get(1).getRemanent());
//    }
//
//    @Test
//    @DisplayName("validate should identify duplicates and negative amounts")
//    void validate_duplicatesAndNegatives_identifiedCorrectly() {
//        TransactionDto tx1 = TransactionDto.builder().amount(new BigDecimal("100")).date(now).remanent(new BigDecimal("10")).ceiling(new BigDecimal("110")).build();
//        TransactionDto tx2 = TransactionDto.builder().amount(new BigDecimal("100")).date(now).remanent(new BigDecimal("10")).ceiling(new BigDecimal("110")).build(); // Duplicate
//        TransactionDto tx3 = TransactionDto.builder().amount(new BigDecimal("-50")).date(now.plusDays(1)).remanent(new BigDecimal("-5")).ceiling(new BigDecimal("-45")).build(); // Negative
//        TransactionDto tx4 = TransactionDto.builder().amount(new BigDecimal("200")).date(now.plusDays(2)).remanent(new BigDecimal("20")).ceiling(new BigDecimal("220")).build();
//
//        ValidatorRequest request = ValidatorRequest.builder()
//                .transactions(Arrays.asList(tx1, tx2, tx3, tx4))
//                .wage(new BigDecimal("1000"))
//                .build();
//
//        ValidatorResponse response = transactionService.validate(request);
//
//        assertEquals(2, response.getValid().size()); // tx1, tx4
//        assertEquals(2, response.getInvalid().size()); // tx2 (duplicate), tx3 (negative)
//
//        assertTrue(response.getInvalid().stream().anyMatch(inv -> inv.getMessage().equals("Duplicate transaction")));
//        assertTrue(response.getInvalid().stream().anyMatch(inv -> inv.getMessage().equals("Negative amounts are not allowed")));
//    }
//
//    @Test
//    @DisplayName("validate should correctly apply investment cap")
//    void validate_exceedsInvestmentCap_identifiedCorrectly() {
//        TransactionDto tx1 = TransactionDto.builder().amount(new BigDecimal("100")).date(now).remanent(new BigDecimal("10")).ceiling(new BigDecimal("110")).build();
//        TransactionDto tx2 = TransactionDto.builder().amount(new BigDecimal("200")).date(now.plusDays(1)).remanent(new BigDecimal("20")).ceiling(new BigDecimal("220")).build();
//        TransactionDto tx3 = TransactionDto.builder().amount(new BigDecimal("300")).date(now.plusDays(2)).remanent(new BigDecimal("30")).ceiling(new BigDecimal("330")).build();
//
//        ValidatorRequest request = ValidatorRequest.builder()
//                .transactions(Arrays.asList(tx1, tx2, tx3))
//                .wage(new BigDecimal("25")) // Max investment is 25
//                .build();
//
//        ValidatorResponse response = transactionService.validate(request);
//
//        assertEquals(2, response.getValid().size()); // tx1 (10), tx2 (20) -> total 30, but cap is 25. So only tx1 and part of tx2.
//        // The current logic adds transactions until the cap is exceeded.
//        // tx1 (10) is valid. invested = 10
//        // tx2 (20) is invalid because 10 + 20 = 30 > 25.
//        // This means the sorting by date is crucial.
//
//        // Let's re-evaluate based on the current implementation:
//        // tx1 (remanent 10) -> valid, invested = 10
//        // tx2 (remanent 20) -> 10 + 20 = 30 > 25, invalid
//        // tx3 (remanent 30) -> invalid
//        // So, only tx1 should be valid.
//
//        assertEquals(1, response.getValid().size()); // Only tx1
//        assertEquals(2, response.getInvalid().size()); // tx2, tx3
//        assertTrue(response.getInvalid().stream().allMatch(inv -> inv.getMessage().equals("Investment exceeds maximum allowed amount")));
//    }
//
//    @Test
//    @DisplayName("validateTemporalConstraints should apply Q-period override")
//    void validateTemporalConstraints_QPeriod_overridesRemanent() {
//        LocalDateTime txDate = now.plusMonths(6); // Mid-year
//        TransactionDto tx1 = TransactionDto.builder().amount(new BigDecimal("250")).date(txDate).remanent(new BigDecimal("50")).ceiling(new BigDecimal("300")).build();
//
//        QConstraint q1 = QConstraint.builder().start(txDate.minusDays(5)).end(txDate.plusDays(5)).fixed(new BigDecimal("0")).build();
//        QConstraint q2 = QConstraint.builder().start(txDate.minusDays(10)).end(txDate.plusDays(10)).fixed(new BigDecimal("100")).build(); // Earlier start, should be overridden
//
//        ValidatorRequest request = ValidatorRequest.builder()
//                .transactions(Collections.singletonList(tx1))
//                .q(Arrays.asList(q1, q2))
//                .wage(new BigDecimal("1000"))
//                .build();
//
//        ValidatorResponse response = transactionService.validateTemporalConstraints(request);
//
//        assertEquals(1, response.getValid().size());
//        assertEquals(new BigDecimal("0"), response.getValid().get(0).getRemanent()); // Remanent should be 0 from q1
//    }
//
//    @Test
//    @DisplayName("validateTemporalConstraints should apply P-period addition")
//    void validateTemporalConstraints_PPeriod_addsToRemanent() {
//        LocalDateTime txDate = now.plusMonths(3);
//        TransactionDto tx1 = TransactionDto.builder().amount(new BigDecimal("250")).date(txDate).remanent(new BigDecimal("50")).ceiling(new BigDecimal("300")).build();
//
//        PConstraint p1 = PConstraint.builder().start(txDate.minusDays(5)).end(txDate.plusDays(5)).extra(new BigDecimal("25")).build();
//        PConstraint p2 = PConstraint.builder().start(txDate.minusDays(2)).end(txDate.plusDays(2)).extra(new BigDecimal("10")).build(); // Overlapping
//
//        ValidatorRequest request = ValidatorRequest.builder()
//                .transactions(Collections.singletonList(tx1))
//                .p(Arrays.asList(p1, p2))
//                .wage(new BigDecimal("1000"))
//                .build();
//
//        ValidatorResponse response = transactionService.validateTemporalConstraints(request);
//
//        assertEquals(1, response.getValid().size());
//        // Original remanent 50 + 25 (from p1) + 10 (from p2) = 85
//        assertEquals(new BigDecimal("85.0"), response.getValid().get(0).getRemanent());
//    }
//
//    @Test
//    @DisplayName("validateTemporalConstraints should apply Q then P periods")
//    void validateTemporalConstraints_QthenP_appliedCorrectly() {
//        LocalDateTime txDate = now.plusMonths(6);
//        TransactionDto tx1 = TransactionDto.builder().amount(new BigDecimal("250")).date(txDate).remanent(new BigDecimal("50")).ceiling(new BigDecimal("300")).build();
//
//        QConstraint q = QConstraint.builder().start(txDate.minusDays(5)).end(txDate.plusDays(5)).fixed(new BigDecimal("0")).build();
//        PConstraint p = PConstraint.builder().start(txDate.minusDays(2)).end(txDate.plusDays(2)).extra(new BigDecimal("25")).build();
//
//        ValidatorRequest request = ValidatorRequest.builder()
//                .transactions(Collections.singletonList(tx1))
//                .q(Collections.singletonList(q))
//                .p(Collections.singletonList(p))
//                .wage(new BigDecimal("1000"))
//                .build();
//
//        ValidatorResponse response = transactionService.validateTemporalConstraints(request);
//
//        assertEquals(1, response.getValid().size());
//        // Original remanent 50 -> Q sets to 0 -> P adds 25 = 25
//        assertEquals(new BigDecimal("25.0"), response.getValid().get(0).getRemanent());
//    }
//
//    @Test
//    @DisplayName("validateTemporalConstraints should correctly set inKPeriod flag")
//    void validateTemporalConstraints_KPeriod_setsFlag() {
//        LocalDateTime txDate = now.plusMonths(6);
//        TransactionDto tx1 = TransactionDto.builder().amount(new BigDecimal("250")).date(txDate).remanent(new BigDecimal("50")).ceiling(new BigDecimal("300")).build();
//
//        KConstraint k1 = KConstraint.builder().start(txDate.minusDays(5)).end(txDate.plusDays(5)).build(); // Transaction is in this K-period
//        KConstraint k2 = KConstraint.builder().start(txDate.plusMonths(1)).end(txDate.plusMonths(2)).build(); // Transaction is NOT in this K-period
//
//        ValidatorRequest request = ValidatorRequest.builder()
//                .transactions(Collections.singletonList(tx1))
//                .k(Arrays.asList(k1, k2))
//                .wage(new BigDecimal("1000"))
//                .build();
//
//        ValidatorResponse response = transactionService.validateTemporalConstraints(request);
//
//        assertEquals(1, response.getValid().size());
//        assertTrue(response.getValid().get(0).getInKPeriod());
//    }
//
//    @Test
//    @DisplayName("ensureTransactionIsInitialized should calculate remanent and ceiling if null")
//    void ensureTransactionIsInitialized_nullRemanentAndCeiling_calculatesThem() {
//        Transaction tx = Transaction.builder().amount(new BigDecimal("123")).date(now).build();
//        // Call the private method via reflection or by calling a public method that uses it
//        // For simplicity, we'll call validate with this transaction
//        ValidatorRequest request = ValidatorRequest.builder()
//                .transactions(Collections.singletonList(realMapper.toDto(tx)))
//                .wage(new BigDecimal("1000"))
//                .build();
//
//        ValidatorResponse response = transactionService.validate(request);
//
//        assertEquals(1, response.getValid().size());
//        assertEquals(new BigDecimal("200.0"), response.getValid().get(0).getCeiling());
//        assertEquals(new BigDecimal("77.0"), response.getValid().get(0).getRemanent());
//    }
//
//    @Test
//    @DisplayName("isWithinPeriod should return true for dates within the period")
//    void isWithinPeriod_dateWithinPeriod_returnsTrue() {
//        LocalDateTime start = now.minusDays(5);
//        LocalDateTime end = now.plusDays(5);
//        assertTrue(transactionService.isWithinPeriod(now, start, end));
//        assertTrue(transactionService.isWithinPeriod(start, start, end));
//        assertTrue(transactionService.isWithinPeriod(end, start, end));
//    }
//
//    @Test
//    @DisplayName("isWithinPeriod should return false for dates outside the period")
//    void isWithinPeriod_dateOutsidePeriod_returnsFalse() {
//        LocalDateTime start = now.minusDays(5);
//        LocalDateTime end = now.plusDays(5);
//        assertFalse(transactionService.isWithinPeriod(now.minusDays(6), start, end));
//        assertFalse(transactionService.isWithinPeriod(now.plusDays(6), start, end));
//    }
//
//    @Test
//    @DisplayName("applyInvestmentCap should correctly handle null maxAmountToInvest")
//    void applyInvestmentCap_nullMaxAmountToInvest_allValid() {
//        Transaction tx1 = Transaction.builder().amount(new BigDecimal("100")).date(now).remanent(new BigDecimal("10")).ceiling(new BigDecimal("110")).build();
//        List<Transaction> transactions = Collections.singletonList(tx1);
//        List<TransactionService.FinalValidationInput> candidates = transactions.stream()
//                .map(tx -> new TransactionService.FinalValidationInput(tx, false))
//                .collect(Collectors.toList());
//
//        ValidatorResponse response = transactionService.applyInvestmentCap(candidates, null, Collections.emptyList());
//
//        assertEquals(1, response.getValid().size());
//        assertEquals(0, response.getInvalid().size());
//    }
//
//    @Test
//    @DisplayName("findInvalidDuplicatesAndNegatives should handle empty list")
//    void findInvalidDuplicatesAndNegatives_emptyList_returnsEmpty() {
//        TransactionService.PreValidationResult result = transactionService.findInvalidDuplicatesAndNegatives(Collections.emptyList());
//        assertTrue(result.candidates().isEmpty());
//        assertTrue(result.invalids().isEmpty());
//    }
//}