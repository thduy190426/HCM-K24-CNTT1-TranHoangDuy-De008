package com.banking.controllers;

import com.banking.advice.ApiResponse;
import com.banking.models.dto.TermDepositRequest;
import com.banking.models.dto.TermDepositResponse;
import com.banking.models.services.TermDepositService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/term-deposits")
@RequiredArgsConstructor
public class TermDepositController {

    private final TermDepositService termDepositService;

    @PostMapping("/open")
    public ResponseEntity<ApiResponse<TermDepositResponse>> openTermDeposit(
            @Valid @RequestBody TermDepositRequest request) {
        TermDepositResponse response = termDepositService.openTermDeposit(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Mở sổ tiết kiệm thành công"));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<ApiResponse<TermDepositResponse>> settleTermDeposit(@PathVariable Long id) {
        TermDepositResponse response = termDepositService.settleTermDeposit(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Tất toán sổ tiết kiệm thành công"));
    }
}
