package com.banking.models.services;

import com.banking.exceptions.BusinessException;
import com.banking.models.dto.TermDepositRequest;
import com.banking.models.dto.TermDepositResponse;
import com.banking.models.entities.BankAccount;
import com.banking.models.entities.TermDeposit;
import com.banking.models.repositories.BankAccountRepository;
import com.banking.models.repositories.TermDepositRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TermDepositService {

    private final TermDepositRepository termDepositRepository;
    private final BankAccountRepository bankAccountRepository;

    @Transactional
    public TermDepositResponse openTermDeposit(TermDepositRequest request) {
        BankAccount account = bankAccountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy tài khoản ngân hàng"));

        if (account.getBalance().compareTo(request.getPrincipalAmount()) < 0) {
            throw new BusinessException(400, "Số dư tài khoản không đủ để mở sổ tiết kiệm");
        }

        account.setBalance(account.getBalance().subtract(request.getPrincipalAmount()));
        bankAccountRepository.save(account);

        BigDecimal interestRate;
        switch (request.getTermMonths()) {
            case 1:  interestRate = new BigDecimal("0.0400"); break; 
            case 6:  interestRate = new BigDecimal("0.0600"); break;
            case 12: interestRate = new BigDecimal("0.0700"); break; 
            default: throw new BusinessException(400, "Kỳ hạn không hợp lệ. Chỉ chấp nhận 1, 6, hoặc 12 tháng.");
        }

        LocalDate depositDate  = LocalDate.now();
        LocalDate maturityDate = depositDate.plusMonths(request.getTermMonths());

        TermDeposit deposit = TermDeposit.builder()
                .principalAmount(request.getPrincipalAmount())
                .interestRate(interestRate)
                .termMonths(request.getTermMonths())
                .depositDate(depositDate)
                .maturityDate(maturityDate)
                .status(TermDeposit.DepositStatus.ACTIVE)
                .bankAccount(account)
                .build();

        deposit = termDepositRepository.save(deposit);
        log.info("Mở sổ tiết kiệm thành công | ID: {} | Tài khoản: {} | Tiền gốc: {} VND | Kỳ hạn: {} tháng",
                deposit.getId(), account.getId(), request.getPrincipalAmount(), request.getTermMonths());

        return mapToResponse(deposit, null, null);
    }

    @Transactional
    public TermDepositResponse settleTermDeposit(Long depositId) {
        TermDeposit deposit = termDepositRepository.findById(depositId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy sổ tiết kiệm"));

        try {
            LocalDate settlementDate = LocalDate.now();

            BigDecimal totalAmount = deposit.calculateSettlementAmount(settlementDate);
            BigDecimal interest    = totalAmount.subtract(deposit.getPrincipalAmount());

            BankAccount account = deposit.getBankAccount();
            account.setBalance(account.getBalance().add(totalAmount));
            bankAccountRepository.save(account);

            deposit.setStatus(TermDeposit.DepositStatus.SETTLED);
            deposit = termDepositRepository.save(deposit);

            log.info("Tất toán thành công | Sổ ID: {} | Tiền gốc: {} | Tiền lãi: {} | Tổng nhận: {} VND | Ngày: {}",
                    depositId, deposit.getPrincipalAmount(), interest, totalAmount, settlementDate);

            return mapToResponse(deposit, totalAmount, settlementDate);

        } catch (IllegalArgumentException ex) {
            log.error("Lỗi tất toán sổ ID: {} | Lý do: {}", depositId, ex.getMessage());
            throw new BusinessException(400, ex.getMessage());
        }
    }

    private TermDepositResponse mapToResponse(TermDeposit deposit,
                                              BigDecimal totalSettlementAmount,
                                              LocalDate settlementDate) {
        return TermDepositResponse.builder()
                .id(deposit.getId())
                .principalAmount(deposit.getPrincipalAmount())
                .interestRate(deposit.getInterestRate())
                .termMonths(deposit.getTermMonths())
                .depositDate(deposit.getDepositDate())
                .maturityDate(deposit.getMaturityDate())
                .status(deposit.getStatus())
                .bankAccountId(deposit.getBankAccount().getId())
                .totalSettlementAmount(totalSettlementAmount)
                .settlementDate(settlementDate)
                .build();
    }
}
