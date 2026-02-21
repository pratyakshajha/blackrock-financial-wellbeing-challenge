package com.example.service;

import com.example.domain.ReturnType;
import com.example.dto.*;
import com.example.mapper.TransactionMapper;
import com.example.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Service
public class ReturnService {

    // Using MathContext for precision in BigDecimal power calculations
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final int RETIREMENT_AGE = 60;
    private static final int MIN_INVESTMENT_PERIOD = 5;
    private static final BigDecimal NPS_DEDUCTION_CAP = new BigDecimal("200000");

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private TaxCalculatorService taxCalculatorService;

    public ReturnResponse returns(ReturnRequest returnRequest, ReturnType type) {
        // 1. Validate all transactions and apply temporal rules to get the final list of valid transactions.
        List<ValidTransactionDto> validTransactions = getValidTransactions(returnRequest);

        // 2. Calculate overall totals for the response header.
        BigDecimal totalAmount = calculateTotal(validTransactions, ValidTransactionDto::getAmount);
        BigDecimal totalCeiling = calculateTotal(validTransactions, ValidTransactionDto::getCeiling);

        // 3. Calculate savings and returns for each k-period.
        List<Savings> savingsByDates = calculateSavingsForEachKPeriod(returnRequest, validTransactions, type);

        // 4. Build the final, structured response.
        return buildReturnResponse(totalAmount, totalCeiling, savingsByDates);
    }

    /**
     * Orchestrates the validation of transactions by calling the TransactionService.
     * @param returnRequest The original request containing expenses and constraints.
     * @return A list of transactions that are valid after all rules have been applied.
     */
    private List<ValidTransactionDto> getValidTransactions(ReturnRequest returnRequest) {
        // First, parse the expenses to calculate the initial remanent and ceiling for each transaction.
        List<Transaction> parsedTransactions = transactionService.parseExpenses(returnRequest.getTransactions());
        // Then, map the fully-formed Transaction entities to DTOs for the validation request.
        List<TransactionDto> transactionDtos = transactionMapper.toDtoList(parsedTransactions);

        ValidatorRequest validatorRequest = ValidatorRequest.builder()
                .wage(returnRequest.getWage())
                .transactions(transactionDtos)
                .p(returnRequest.getP())
                .q(returnRequest.getQ())
                .k(returnRequest.getK())
                .build();
        ValidatorResponse validationResult = transactionService.validateTemporalConstraints(validatorRequest);
        return validationResult.getValid();
    }

    /**
     * A generic helper to sum a BigDecimal property from a list of DTOs.
     * @param transactions The list of transactions.
     * @param mapper A function to extract the BigDecimal to be summed.
     * @return The total sum.
     */
    private BigDecimal calculateTotal(List<ValidTransactionDto> transactions, Function<ValidTransactionDto, BigDecimal> mapper) {
        return transactions.stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Iterates through each K-period, calculating the principal, profits, and tax benefits for each.
     * @param returnRequest The original request containing age and inflation data.
     * @param validTransactions The list of valid transactions to be considered.
     * @param type The type of investment (NPS or Index Fund).
     * @return A list of Savings objects, one for each K-period.
     */
    private List<Savings> calculateSavingsForEachKPeriod(ReturnRequest returnRequest, List<ValidTransactionDto> validTransactions, ReturnType type) {
        List<KConstraint> kPeriods = returnRequest.getK() != null ? returnRequest.getK() : Collections.emptyList();
        List<Savings> savingsByDates = new ArrayList<>();

        if (returnRequest.getAge() == null || returnRequest.getInflation() == null) {
            throw new IllegalArgumentException("User age and inflation rate must be provided for return calculations.");
        }
        int investmentPeriodYears = Math.max(MIN_INVESTMENT_PERIOD, RETIREMENT_AGE - returnRequest.getAge());

        // Normalize inflation rate. If a user provides a percentage like 5.5, convert it to a decimal 0.055.
        // This prevents the real return from incorrectly calculating as zero.
        BigDecimal inflationRate = returnRequest.getInflation();
        if (inflationRate.abs().compareTo(BigDecimal.ONE) > 0) {
            inflationRate = inflationRate.divide(new BigDecimal("100"), MC);
        }

        for (KConstraint kPeriod : kPeriods) {
            savingsByDates.add(processSingleKPeriod(kPeriod, validTransactions, type, inflationRate, investmentPeriodYears, returnRequest.getWage()));
        }
        return savingsByDates;
    }

    /**
     * Processes a single K-period to calculate its financial metrics.
     * @return A populated Savings object for the given period.
     */
    private Savings processSingleKPeriod(KConstraint kPeriod, List<ValidTransactionDto> validTransactions, ReturnType type, BigDecimal inflationRate, int investmentPeriodYears, BigDecimal annualWage) {
        // For the given k-period, sum the remanents of transactions that fall within it. This is the principal (P).
        BigDecimal principalForK = validTransactions.stream()
                .filter(tx -> isWithinPeriod(tx.getDate(), kPeriod.getStart(), kPeriod.getEnd()))
                .map(ValidTransactionDto::getRemanent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate the real (inflation-adjusted) return value.
        BigDecimal realReturn = calculateRealReturn(principalForK, type.getRate(), inflationRate, investmentPeriodYears);
        // The profit is the real return minus the initial principal.
        BigDecimal profits = realReturn.subtract(principalForK);

        // Calculate tax benefit only for NPS.
        BigDecimal taxBenefit = BigDecimal.ZERO;
        if (type == ReturnType.NPS) {
            taxBenefit = calculateNpsTaxBenefit(principalForK, annualWage);
        }

        return Savings.builder()
                .start(kPeriod.getStart())
                .end(kPeriod.getEnd())
                .amount(principalForK.setScale(2, RoundingMode.HALF_UP))
                .profits(profits.setScale(2, RoundingMode.HALF_UP))
                // Only include taxBenefit for NPS, otherwise it will be omitted from JSON.
                .taxBenefit(type == ReturnType.NPS ? taxBenefit.setScale(2, RoundingMode.HALF_UP) : null)
                .build();
    }
    
    /**
     * Builds the final response object.
     */
    private ReturnResponse buildReturnResponse(BigDecimal totalAmount, BigDecimal totalCeiling, List<Savings> savingsByDates) {
        return ReturnResponse.builder()
                .transactionsTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP))
                .transactionsTotalCeiling(totalCeiling.setScale(2, RoundingMode.HALF_UP))
                .savingsByDates(savingsByDates)
                .build();
    }

    /**
     * Calculates the real, inflation-adjusted return on an investment.
     */
    private BigDecimal calculateRealReturn(BigDecimal principal, BigDecimal rate, BigDecimal inflation, int years) {
        // A = P * (1 + r)^t
        BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
        BigDecimal nominalReturn = principal.multiply(onePlusRate.pow(years, MC));

        // A_real = A / (1 + inflation)^t
        BigDecimal onePlusInflation = BigDecimal.ONE.add(inflation);
        BigDecimal inflationDivisor = onePlusInflation.pow(years, MC);

        if (inflationDivisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return nominalReturn.divide(inflationDivisor, MC);
    }

    /**
     * Checks if a given date falls within a start and end date (inclusive).
     */
    private boolean isWithinPeriod(LocalDateTime date, LocalDateTime start, LocalDateTime end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private BigDecimal calculateNpsTaxBenefit(BigDecimal investedAmount, BigDecimal annualIncome) {
        if (annualIncome == null || annualIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // NPS deduction is the minimum of: invested amount, 10% of income, or 2L cap.
        BigDecimal tenPercentOfIncome = annualIncome.multiply(new BigDecimal("0.10"));

        BigDecimal eligibleDeduction = investedAmount
                .min(tenPercentOfIncome)
                .min(NPS_DEDUCTION_CAP);

        BigDecimal taxWithNoDeduction = taxCalculatorService.calculateTax(annualIncome);
        BigDecimal taxWithDeduction = taxCalculatorService.calculateTax(annualIncome.subtract(eligibleDeduction));

        return taxWithNoDeduction.subtract(taxWithDeduction);
    }
}
