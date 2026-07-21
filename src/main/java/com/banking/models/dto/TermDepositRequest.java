package com.banking.models.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermDepositRequest {

    @NotNull(message = "ID tài khoản ngân hàng không được để trống")
    private Long bankAccountId;

    @NotNull(message = "Số tiền gốc không được để trống")
    @Positive(message = "Số tiền gốc phải lớn hơn 0")
    private BigDecimal principalAmount;

    @NotNull(message = "Kỳ hạn không được để trống")
    @Min(value = 1, message = "Kỳ hạn tối thiểu là 1 tháng")
    private Integer termMonths; 
}
