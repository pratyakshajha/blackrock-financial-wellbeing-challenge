package com.example.service;

import com.example.dto.*;
import com.example.mapper.TransactionMapper;
import com.example.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    private static final BigDecimal HUNDRED = new BigDecimal("100.0");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final String ERROR_DUPLICATE_TRANSACTION = "Duplicate transaction";
    private static final String ERROR_NEGATIVE_AMOUNT = "Negative amounts are not allowed";
    private static final String ERROR_INVESTMENT_EXCEEDS_CAP = "Investment exceeds maximum allowed amount";

    @Autowired
    private TransactionMapper transactionMapper;

    public List<Transaction> parseExpenses(List<Expense> expenses) {
        return expenses
                .parallelStream()
                .map(this::parseExpense)
                .collect(Collectors.toList());
    }

    private Transaction parseExpense(Expense expense) {
        Transaction transaction = Transaction.builder().amount(expense.getAmount()).date(expense.getDate()).build();
        BigDecimal ceiling = transaction.getAmount().divide(HUNDRED, 0, RoundingMode.CEILING).multiply(HUNDRED);
        transaction.setCeiling(ceiling);
        transaction.setRemanent(ceiling.subtract(transaction.getAmount()));
        return transaction;
    }

    /** Internal record to hold the results of the initial validation pass. */
    record PreValidationResult(List<Transaction> candidates, List<InvalidTransactionDto> invalids) {}

    /** Internal record to pass processed transactions to the final validation step. */
    record FinalValidationInput(Transaction transaction, Boolean inKPeriod) {}

    public ValidatorResponse validate(ValidatorRequest validatorRequest) {
        List<Transaction> allTransactions = transactionMapper.toEntityList(validatorRequest.getTransactions());
        allTransactions.forEach(this::ensureTransactionIsInitialized);

        // 1. Perform initial validation (duplicates, negatives)
        PreValidationResult preValidation = findInvalidDuplicatesAndNegatives(allTransactions);

        // 2. Prepare for final validation (inKPeriod is null for this simple validator)
        List<FinalValidationInput> finalCandidates = preValidation.candidates().stream()
                .map(tx -> new FinalValidationInput(tx, null))
                .collect(Collectors.toList());

        // 3. Apply investment cap and build response
        return applyInvestmentCap(finalCandidates, validatorRequest.getWage(), preValidation.invalids());
    }

    public ValidatorResponse validateTemporalConstraints(ValidatorRequest validatorRequest) {
        List<Transaction> allTransactions = transactionMapper.toEntityList(validatorRequest.getTransactions());
        allTransactions.forEach(this::ensureTransactionIsInitialized);

        // 1. Perform initial validation (duplicates, negatives)
        PreValidationResult preValidation = findInvalidDuplicatesAndNegatives(allTransactions);
        List<Transaction> candidates = preValidation.candidates();
        List<InvalidTransactionDto> invalidTransactions = preValidation.invalids();

        // 2. Apply temporal rules to the valid candidates
        List<QConstraint> qPeriods = validatorRequest.getQ() != null ? validatorRequest.getQ() : Collections.emptyList();
        List<PConstraint> pPeriods = validatorRequest.getP() != null ? validatorRequest.getP() : Collections.emptyList();
        List<KConstraint> kPeriods = validatorRequest.getK() != null ? validatorRequest.getK() : Collections.emptyList();

        List<FinalValidationInput> processedTransactions = new ArrayList<>();
        for (Transaction transaction : candidates) {
            applyTemporalRules(transaction, pPeriods, qPeriods);

            boolean isInKPeriod = kPeriods.stream()
                    .anyMatch(k -> isWithinPeriod(transaction.getDate(), k.getStart(), k.getEnd()));

            processedTransactions.add(new FinalValidationInput(transaction, isInKPeriod));
        }

        // 3. Apply investment cap and build response
        return applyInvestmentCap(processedTransactions, validatorRequest.getWage(), invalidTransactions);
    }

    /**
     * Ensures a transaction has its remanent and ceiling calculated.
     * This makes the service robust against incomplete DTOs from clients.
     * @param transaction The transaction to initialize.
     */
    private void ensureTransactionIsInitialized(Transaction transaction) {
        if (transaction.getAmount() != null && (transaction.getRemanent() == null || transaction.getCeiling() == null)) {
            BigDecimal ceiling = transaction.getAmount().divide(HUNDRED, 0, RoundingMode.CEILING).multiply(HUNDRED);
            transaction.setCeiling(ceiling);
            transaction.setRemanent(ceiling.subtract(transaction.getAmount()));
        }
    }

    /**
     * Applies all temporal (P and Q) rules to a single transaction, modifying its remanent.
     * Q-rules (override) are applied before P-rules (addition).
     */
    private void applyTemporalRules(Transaction transaction, List<PConstraint> pPeriods, List<QConstraint> qPeriods) {
        LocalDateTime txDate = transaction.getDate();

        // --- Q-Period Logic: Override remanent ---
        qPeriods.stream()
                .filter(q -> isWithinPeriod(txDate, q.getStart(), q.getEnd()))
                .max(Comparator.comparing(QConstraint::getStart)) // Find the latest start date
                .ifPresent(chosenQ -> transaction.setRemanent(chosenQ.getFixed()));

        // --- P-Period Logic: Add extra amount ---
        BigDecimal extraFromP = pPeriods.stream()
                .filter(p -> isWithinPeriod(txDate, p.getStart(), p.getEnd()))
                .map(PConstraint::getExtra)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        transaction.setRemanent(transaction.getRemanent().add(extraFromP));
    }

    public boolean isWithinPeriod(LocalDateTime date, LocalDateTime start, LocalDateTime end) {
        // A date is within a period if it's not before the start and not after the end.
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * First validation pass to identify and segregate duplicate and negative-amount transactions.
     * @param allTransactions The initial list of all transactions.
     * @return A {@link PreValidationResult} containing valid candidates and a list of invalid transactions.
     */
    PreValidationResult findInvalidDuplicatesAndNegatives(List<Transaction> allTransactions) {
        List<InvalidTransactionDto> currentInvalids = new ArrayList<>();

        // 1. Handle duplicates
        record TransactionKey(BigDecimal amount, LocalDateTime date) {}
        Map<TransactionKey, List<Transaction>> grouped = allTransactions.stream()
                .collect(Collectors.groupingBy(t -> new TransactionKey(t.getAmount(), t.getDate())));

        List<Transaction> uniqueTransactions = new ArrayList<>();
        for (List<Transaction> group : grouped.values()) {
            uniqueTransactions.add(group.get(0)); // First is original
            if (group.size() > 1) {
                group.subList(1, group.size()).forEach(dup ->
                        currentInvalids.add(transactionMapper.toInvalidDto(dup, ERROR_DUPLICATE_TRANSACTION)));
            }
        }

        // 2. Handle negatives from the unique list
        List<Transaction> finalCandidates = new ArrayList<>();
        for (Transaction transaction : uniqueTransactions) {
            if (transaction.getAmount() != null && transaction.getAmount().compareTo(ZERO) < 0) {
                currentInvalids.add(transactionMapper.toInvalidDto(transaction, ERROR_NEGATIVE_AMOUNT));
            } else {
                finalCandidates.add(transaction);
            }
        }

        return new PreValidationResult(finalCandidates, currentInvalids);
    }

    /**
     * Final validation pass that sorts transactions by date and applies the investment cap.
     * @param candidates The list of transactions to validate.
     * @param maxAmountToInvest The maximum amount that can be invested (the wage).
     * @param initialInvalids A list of transactions already deemed invalid.
     * @return The final {@link ValidatorResponse}.
     */
    public ValidatorResponse applyInvestmentCap(
            List<FinalValidationInput> candidates,
            BigDecimal maxAmountToInvest,
            List<InvalidTransactionDto> initialInvalids) {

        List<ValidTransactionDto> validTransactions = new ArrayList<>();
        List<InvalidTransactionDto> invalidTransactions = new ArrayList<>(initialInvalids);

        // Sort candidates by date for chronological investment validation
        candidates.sort(Comparator.comparing(c -> c.transaction().getDate()));

        BigDecimal investedAmount = BigDecimal.ZERO;

        for (FinalValidationInput candidate : candidates) {
            Transaction transaction = candidate.transaction();
            if (maxAmountToInvest != null && investedAmount.add(transaction.getAmount()).compareTo(maxAmountToInvest) <= 0) {
                validTransactions.add(transactionMapper.toValidDto(transaction, candidate.inKPeriod()));
                investedAmount = investedAmount.add(transaction.getAmount());
            } else {
                invalidTransactions.add(transactionMapper.toInvalidDto(transaction, ERROR_INVESTMENT_EXCEEDS_CAP));
            }
        }

        return ValidatorResponse.builder()
                .valid(validTransactions)
                .invalid(invalidTransactions)
                .build();
    }
}
