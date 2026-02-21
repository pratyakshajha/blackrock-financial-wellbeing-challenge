package com.example.controller;

import com.example.dto.Expense;
import com.example.dto.TransactionDto;
import com.example.dto.ValidatorRequest;
import com.example.dto.ValidatorResponse;
import com.example.mapper.TransactionMapper;
import com.example.model.Transaction;
import com.example.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class.getName());

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionMapper transactionMapper;

    @PostMapping("/transactions:parse")
    public List<TransactionDto> parse(@RequestBody List<Expense> expenses) {
        List<Transaction> transactions = transactionService.parseExpenses(expenses);
        return transactionMapper.toResponseList(transactions);
    }

    @PostMapping("/transactions:validator")
    public ValidatorResponse validator(@RequestBody ValidatorRequest validatorRequest) {
        return transactionService.validate(validatorRequest);
    }

    @PostMapping("/transactions:filter")
    public ValidatorResponse temporalValidator(@RequestBody ValidatorRequest validatorRequest) {
        return transactionService.validateTemporalConstraints(validatorRequest);
    }

}