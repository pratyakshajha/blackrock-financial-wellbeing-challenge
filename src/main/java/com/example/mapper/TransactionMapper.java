package com.example.mapper;

import com.example.dto.InvalidTransactionDto;
import com.example.dto.ValidTransactionDto;
import com.example.dto.TransactionDto;
import com.example.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {

    TransactionDto toDto(Transaction transaction);
    List<TransactionDto> toDtoList(List<Transaction> transaction);
    List<TransactionDto> toResponseList(List<Transaction> transactions);
    Transaction toEntity(TransactionDto transactionDto);
    List<Transaction> toEntityList(List<TransactionDto> transactionDtos);

    @Mapping(target = "message", source = "message")
    InvalidTransactionDto toInvalidDto(Transaction transaction, String message);

    @Mapping(target = "inKPeriod", source = "inKPeriod")
    ValidTransactionDto toValidDto(Transaction transaction, Boolean inKPeriod);
    
    @Mapping(target = "inKPeriod", ignore = true)
    ValidTransactionDto toValidDto(Transaction transaction);
    
    List<ValidTransactionDto> toValidDtoList(List<Transaction> transactions);
}