package com.banking.models.dto;

import com.banking.models.entities.TermDeposit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermDepositResponse {
    private Long id;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private LocalDate depositDate;
    private LocalDate maturityDate;
    private TermDeposit.DepositStatus status;
    private Long bankAccountId;

    private BigDecimal totalSettlementAmount;

    private LocalDate settlementDate;
}
