package com.banking.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "term_deposits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate; 

    @Column(name = "term_months", nullable = false)
    private Integer termMonths; 

    @Column(name = "deposit_date", nullable = false)
    private LocalDate depositDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DepositStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    // Lãi suất không kỳ hạn áp dụng khi tất toán TRƯỚC ngày đáo hạn (0.1%/năm)
    private static final BigDecimal NON_TERM_INTEREST_RATE = new BigDecimal("0.001");

    public enum DepositStatus {
        ACTIVE, SETTLED
    }

    public BigDecimal calculateSettlementAmount(LocalDate settlementDate) {
        if (this.status == DepositStatus.SETTLED) {
            throw new IllegalArgumentException("Sổ tiết kiệm đã được tất toán trước đó");
        }

        long actualDays = ChronoUnit.DAYS.between(this.depositDate, settlementDate);
        if (actualDays < 0) {
            actualDays = 0;
        }

        BigDecimal applicableRate = settlementDate.isBefore(this.maturityDate) 
                                    ? NON_TERM_INTEREST_RATE 
                                    : this.interestRate;

        BigDecimal interestAmount = this.principalAmount
                .multiply(applicableRate)
                .multiply(BigDecimal.valueOf(actualDays))
                .divide(BigDecimal.valueOf(365), 0, RoundingMode.HALF_UP); 

        return this.principalAmount.add(interestAmount);
    }
}
