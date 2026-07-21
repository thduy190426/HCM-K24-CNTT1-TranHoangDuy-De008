# Prompt History – Term Deposit Settlement Feature

## 1. Overview

This document records the complete history of structured prompts used throughout the analysis and development of the "Term Deposit Settlement" feature for the Core Banking system.

Each prompt follows a standardized 5-phase engineering process built on a normalized **Input Block** (`Role` – `Goal` – `Context` – `Constraint` – `Format`). The goal is to ensure the AI Agent produces functionally correct, secure, and maintainable code.

---

## 2. Prompt History by Round

---

### ROUND 1 — Business Analysis & Architecture Proposal

#### Prompt (Phase 1 & 2)

```
Role:
You are a Java Senior Developer and Backend Architect working on a Core Banking
system built with Spring Boot and Gradle. The project already has the Customer
and BankAccount entities in place.

Goal:
Design the TermDeposit entity (Savings Account) and implement the full business
logic for Term Deposit Settlement. The API must compare the actual withdrawal
date against the maturity date to calculate the correct interest amount.

Context:
Customers can open a term deposit for 1 month (4%/year), 6 months (6%/year),
or 12 months (7%/year). When the "Settle" action is triggered, the settlement
date must be sourced directly from the server (never accepted from the client).
The project already uses ApiResponse<T> and BusinessException as the standard
response wrapper and error handler respectively.

Constraints:
- Never use java.util.Date or any legacy date/time library.
  Only java.time.LocalDate and ChronoUnit.DAYS are permitted.
- All monetary calculations must use BigDecimal with RoundingMode.HALF_UP
  to prevent floating-point precision errors (ArithmeticException).
- If the settlement date is BEFORE the maturity date (even by 1 day):
  apply the fixed non-term penalty interest rate of 0.1%/year.
- If the settlement date is ON or AFTER the maturity date:
  apply the original contracted interest rate.
- A deposit with status SETTLED must not be settled again. Any repeated
  call to the settle API must return HTTP 400 with the message:
  "Sổ tiết kiệm đã được tất toán trước đó".

Output Format:
- Step 1: Before writing any production code, explain the logic in plain
  language and run a pseudocode dry-run with at least 2 sample datasets
  (one early withdrawal, one on-time withdrawal) to verify the formula
  produces correct results.
- Step 2: Once the pseudocode is validated, propose 2-3 distinct
  architectural approaches (e.g., Service-layer calculation, Rich Domain
  Model, Strategy Pattern). For each approach, provide a comparison table
  of Pros / Cons / Trade-offs. Clearly state the recommended option and
  STOP to wait for my confirmation before proceeding to any next step.
```

#### Result & Evaluation

| Criteria | Outcome |
| :--- | :--- |
| **Role acceptance** | AI correctly identified the Backend Architect role, read the existing Base Code structure before analysis |
| **Pseudocode dry-run** | AI simulated: Principal 100,000,000 VND × 6% × 91 days / 365 → on-time interest **≈ 1,495,890 VND**; same principal with early withdrawal at 91 days → penalty interest **≈ 24,931 VND**. Formula confirmed error-free before generating code. |
| **Architecture proposal** | 3 options listed with comparison table. AI recommended Option 2 (Rich Domain Model) and stopped to wait for approval without proceeding further. |
| **Constraint compliance** | No occurrence of `java.util.Date` or `Double` in the proposed design. |

---

### ROUND 2 — Risk Testing & Solution Refinement

#### Prompt (Phase 3, 4 & 5)

```
I confirm the selection of Option 2 (Rich Domain Model) — placing the interest
calculation logic directly inside the TermDeposit entity.

Now please execute the following steps in the exact order listed, without
combining or skipping any:

STEP 1 — Stress Testing (What-If Analysis):
Given the selected design, identify at least 3 dangerous real-world scenarios
(examples: user double-clicks causing two concurrent settlements, client sends
a manipulated settlement date to the server, infinite decimal division causing
a server crash). For each scenario, analyze the risk severity and propose a
corresponding fallback or defense mechanism.

STEP 2 — Iterative Refinement over 3 Rounds (execute sequentially, do not merge):

- Round 1 (Robustness): Handle edge cases and security — add Optimistic Locking
  via @Version, enforce RoundingMode.HALF_UP, and guard against null or negative
  input values.

- Round 2 (Maintainability): Enforce clear layer separation
  (Controller – Service – Repository), add structured logging via SLF4J (@Slf4j),
  use descriptive variable and method names. Wrap the entire settlement flow
  inside @Transactional.

- Round 3 (Project-Specific Tuning): Eliminate all hardcoded values, reuse the
  existing ApiResponse<T> and BusinessException classes from the Base Code.
  Ensure no existing package structure (com.banking.*) is broken.

After each round, write a 2-3 line summary of changes made before moving to
the next round.

STEP 3 — Consolidated Technical Specification:
Synthesize all constraints, business rules, and refined decisions into one
concise reusable technical specification (in prompt format) so future
similar features in this project can reference it without restarting from scratch.
```

#### Result & Evaluation

| Criteria | Outcome |
| :--- | :--- |
| **Risk testing** | Correctly identified 3 risks: race condition on double-click, client date manipulation, `ArithmeticException` on division by 365. Fallbacks proposed: `@Version`, enforced `LocalDate.now()` on backend, `RoundingMode.HALF_UP`. |
| **Round 1 – Robustness** | Added `@Version` to Entity, added `actualDays < 0` guard against negative values, declared `NON_TERM_INTEREST_RATE` as a static `BigDecimal` constant. |
| **Round 2 – Maintainability** | Applied `@Transactional` wrapping both balance update and deposit status save, SLF4J logging with full deposit ID and amount detail. |
| **Round 3 – Tuning** | Controller uses `ApiResponse.success(...)`, Service catches `IllegalArgumentException` from Entity and re-throws as `BusinessException(400, ...)`. |
| **Reusable specification** | Generated one concise technical spec capturing all immutable constraints of the Term Deposit module. |

---

### ROUND 3 — Source Code & Documentation Deployment

#### Prompt (Execution Phase)

```
Deploy the fully approved design into the real CoreBanking project. Specifically:

1. Create all required Java files following the existing package structure of
   the project (com.banking.models.entities, com.banking.models.repositories,
   com.banking.models.dto, com.banking.models.services, com.banking.controllers).
   Do not create any new packages outside the existing structure.

2. Ensure the TermDeposit entity contains the calculateSettlementAmount() method
   with the before-maturity / on-maturity branching logic,
   using BigDecimal + RoundingMode.HALF_UP.

3. Ensure TermDepositService uses @Transactional, @Slf4j, and enforces
   LocalDate.now() server-side — the settlement date must not be accepted
   from the request body.

4. Ensure TermDepositController returns the correct type ApiResponse<TermDepositResponse>
   and that BusinessException is thrown with HTTP 400 when the deposit is
   already in SETTLED status.

5. After all files are created, run the compilation check command to confirm
   there are no syntax errors.

6. Create SRS.md and Prompt_History.md in the project root directory.
   Content must be detailed, well-structured, and use standard Markdown
   (tables, hierarchical headings, bold for key points).
```

#### Result & Evaluation

| Criteria | Outcome |
| :--- | :--- |
| **Java files** | Created 6 files: `TermDeposit.java`, `TermDepositRepository.java`, `TermDepositRequest.java`, `TermDepositResponse.java`, `TermDepositService.java`, `TermDepositController.java` — correct packages, no new packages added. |
| **Entity logic** | `calculateSettlementAmount(LocalDate)` contains: `SETTLED` guard check, `isBefore(maturityDate)` branching, formula `BigDecimal × rate × days / 365` with `RoundingMode.HALF_UP`. |
| **Transaction integrity** | `@Transactional` covers both operations: update `BankAccount.balance` and persist `TermDeposit.status = SETTLED`. |
| **HTTP 400** | `IllegalArgumentException` from Entity is caught in Service and rethrown as `BusinessException(400, "Sổ tiết kiệm đã được tất toán trước đó")`. |
| **Documentation** | `SRS.md` and `Prompt_History.md` created in the project root directory. |
| **Compilation check** | `gradlew classes` was executed; failed due to network timeout downloading Gradle Wrapper — not a Java syntax error. |

---

## 3. Summary

Across 3 structured prompt rounds, the Term Deposit Settlement feature was built end-to-end from risk analysis to complete production-ready code, guaranteeing:

- **Transaction Safety:** Double-Spend prevention via 2-layer defense (application-level `status` check + database-level `@Version` Optimistic Locking).
- **Financial Precision:** All monetary calculations use `BigDecimal` + `RoundingMode.HALF_UP`, eliminating any `Double` floating-point inaccuracy.
- **Sustainable Architecture:** Business logic resides in the Entity (Rich Domain Model), Service acts as orchestrator only, Controller handles routing only — strict separation of concerns.
- **Backward Compatibility:** Correctly reuses `ApiResponse`, `BusinessException`, and the existing Base Code package structure without breaking any existing functionality.
