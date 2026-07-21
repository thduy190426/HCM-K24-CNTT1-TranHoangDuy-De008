# Bảng Phân tích & Đặc tả Yêu cầu (SRS)
## Tính năng: API Quản lý & Tất Toán Sổ Tiết Kiệm (Term Deposit Settlement)

---

## 1. Giới thiệu (Introduction)

Dự án **Core Banking** bổ sung tính năng Quản lý Sổ tiết kiệm (Term Deposit). Khách hàng có thể gửi tiền vào một khoản tiền gửi có kỳ hạn nhằm hưởng lãi suất cao hơn tài khoản thông thường. Cốt lõi của tính năng là API **Tất toán sổ tiết kiệm (Settlement)** — cho phép khách hàng rút toàn bộ gốc và lãi tại bất kỳ thời điểm nào trong hoặc sau kỳ hạn, với quy tắc tính lãi khác nhau tùy vào thời điểm rút.

---

## 2. Thiết kế Entity (Database Design)

### Entity: `TermDeposit` — Sổ tiết kiệm

Thực thể `TermDeposit` ánh xạ với bảng `term_deposits` trong cơ sở dữ liệu. Quan hệ: Nhiều-1 (`@ManyToOne`) với thực thể `BankAccount` (một tài khoản ngân hàng có thể có nhiều sổ tiết kiệm).

**Danh sách các trường (Fields):**

| Thuộc tính | Kiểu dữ liệu | Mô tả | Ràng buộc |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Khóa chính | Bắt buộc, tự động tăng |
| `version` | `Long` | Phiên bản để chặn xung đột đồng thời (Optimistic Locking) | Tự động quản lý bởi JPA |
| `principalAmount` | `BigDecimal` | Số tiền gốc gửi ban đầu | Không null, `precision=19`, `scale=4` |
| `interestRate` | `BigDecimal` | Lãi suất theo kỳ hạn đã chọn (dạng thập phân, VD: `0.0600` = 6%/năm) | Không null, `precision=5`, `scale=4` |
| `termMonths` | `Integer` | Kỳ hạn gửi: 1, 6 hoặc 12 tháng | Không null |
| `depositDate` | `LocalDate` | Ngày mở sổ (ngày gửi tiền) | Không null |
| `maturityDate` | `LocalDate` | Ngày đáo hạn = `depositDate + termMonths tháng` | Không null |
| `status` | `Enum` | Trạng thái sổ: `ACTIVE` hoặc `SETTLED` | Mặc định `ACTIVE` khi mở |
| `bankAccount` | `BankAccount` | Tài khoản ngân hàng liên kết (nguồn nạp/rút tiền) | Không null |

**Bảng lãi suất theo kỳ hạn (quy định nghiệp vụ):**

| Kỳ hạn | Lãi suất/năm | Giá trị lưu trong DB (`interestRate`) |
| :---: | :---: | :---: |
| 1 tháng | 4% | `0.0400` |
| 6 tháng | 6% | `0.0600` |
| 12 tháng | 7% | `0.0700` |
| Không kỳ hạn (rút trước hạn) | 0.1% | `0.0010` (hằng số cố định trong code) |

---

## 3. Thuật toán Tính Lãi (Interest Calculation Algorithm)

### 3.1. Mô tả logic rẽ nhánh (Branching Logic)

Khi khách hàng bấm "Tất toán", hệ thống thực hiện theo đúng trình tự sau:

```
BƯỚC 1: Kiểm tra trạng thái sổ
   - Nếu status == SETTLED  →  Ném lỗi HTTP 400: "Sổ tiết kiệm đã được tất toán trước đó"
   - Nếu status == ACTIVE   →  Tiếp tục BƯỚC 2

BƯỚC 2: Xác định số ngày gửi thực tế
   settlementDate = LocalDate.now()   (lấy từ server, không nhận từ client)
   actualDays = DAYS.between(depositDate, settlementDate)
   Nếu actualDays < 0  →  Quy về 0 (bảo vệ khỏi lỗi hệ thống thời gian)

BƯỚC 3: Rẽ nhánh xác định lãi suất áp dụng
   Nếu settlementDate < maturityDate  (rút TRƯỚC ngày đáo hạn)
      →  applicableRate = 0.001  (lãi suất không kỳ hạn: 0.1%/năm)
   Nếu settlementDate >= maturityDate (rút ĐÚNG hoặc SAU ngày đáo hạn)
      →  applicableRate = interestRate (lãi suất kỳ hạn ban đầu)

BƯỚC 4: Tính tiền lãi và tổng tiền nhận về
   interest = principalAmount × applicableRate × actualDays / 365
   totalAmount = principalAmount + interest

BƯỚC 5: Cập nhật hệ thống
   BankAccount.balance += totalAmount
   TermDeposit.status  = SETTLED
   Lưu cả hai vào Database trong cùng một transaction (ACID)
```

### 3.2. Công thức tính tiền lãi (Interest Formula)

```
Tiền lãi = Tiền gốc  ×  Lãi suất/năm  ×  Số ngày gửi thực tế
           ─────────────────────────────────────────────────────
                                   365
```

**Trong đó:**
- **Tiền gốc** (`principalAmount`): Số tiền ban đầu gửi vào sổ.
- **Lãi suất/năm** (`applicableRate`): Lãi suất kỳ hạn nếu rút đúng/sau hạn; hoặc `0.001` (0.1%) nếu rút trước hạn.
- **Số ngày gửi thực tế** (`actualDays`): Tính bằng `ChronoUnit.DAYS.between(depositDate, settlementDate)`.
- **365**: Quy ước số ngày trong một năm.
- Kết quả làm tròn đến đồng nguyên (đơn vị VND) bằng `RoundingMode.HALF_UP`.

**Tổng tiền khách hàng nhận về:**
```
Tổng tiền = Tiền gốc + Tiền lãi
```

### 3.3. Ví dụ minh họa số liệu thực tế

**Giả sử:** Khách hàng mở sổ ngày `01/01/2024`, số tiền gốc `100.000.000₫`, kỳ hạn 6 tháng (6%/năm), ngày đáo hạn là `01/07/2024`.

**Trường hợp 1 — Rút đúng hạn (ngày 01/07/2024):**

| Tham số | Giá trị |
| :--- | :--- |
| Số ngày gửi thực tế | `182` ngày (từ 01/01 đến 01/07) |
| Lãi suất áp dụng | `6%/năm = 0.0600` |
| Tiền lãi | `100.000.000 × 0.0600 × 182 / 365` = **2.991.781₫** |
| **Tổng tiền nhận về** | **102.991.781₫** |

**Trường hợp 2 — Rút trước hạn (ngày 01/04/2024, trước hạn 91 ngày):**

| Tham số | Giá trị |
| :--- | :--- |
| Số ngày gửi thực tế | `91` ngày (từ 01/01 đến 01/04) |
| Lãi suất áp dụng | `0.1%/năm = 0.0010` (lãi phạt không kỳ hạn) |
| Tiền lãi | `100.000.000 × 0.0010 × 91 / 365` = **24.932₫** |
| **Tổng tiền nhận về** | **100.024.932₫** |

> **Nhận xét:** Chênh lệch tiền lãi giữa hai trường hợp là `2.991.781₫ - 24.932₫ ≈ 2.966.849₫`. Đây là chi phí thiệt thòi khi khách hàng rút trước hạn, hoàn toàn phù hợp với quy định nghiệp vụ ngân hàng.

---

## 4. Quy tắc Nghiệp vụ Bổ sung (Additional Business Rules)

### 4.1. Chặn tất toán nhiều lần (Double-Spend Prevention)

Hệ thống áp dụng 2 tầng bảo vệ để ngăn ngân hàng cộng tiền 2 lần khi khách hàng gọi API tất toán hai lần liên tiếp:

- **Tầng 1 — Kiểm tra trạng thái trong mã nguồn:** Phương thức `calculateSettlementAmount()` trong Entity kiểm tra `if (status == SETTLED)`, lập tức ném `IllegalArgumentException` với thông báo: `"Sổ tiết kiệm đã được tất toán trước đó"`. Service bắt lỗi này và trả về **HTTP 400 Bad Request**.
- **Tầng 2 — Optimistic Locking tại cơ sở dữ liệu:** Trường `@Version Long version` được JPA/Hibernate quản lý tự động. Nếu 2 request đồng thời vượt qua Tầng 1 (race condition), request thứ 2 khi ghi xuống DB sẽ gặp lỗi `ObjectOptimisticLockingFailureException` và bị rollback toàn bộ giao dịch, đảm bảo tiền không bị cộng khống.

### 4.2. Kiểm tra số dư khi mở sổ

Khi mở sổ tiết kiệm, hệ thống kiểm tra số dư tài khoản (`BankAccount.balance`). Nếu số dư nhỏ hơn số tiền muốn gửi (`principalAmount`), hệ thống từ chối và trả về **HTTP 400 Bad Request** với thông báo phù hợp. Số tiền gốc sẽ bị trừ khỏi tài khoản tức thì khi mở sổ.

### 4.3. Tính toán ngày tháng chuẩn xác

- Ngày tất toán (`settlementDate`) được lấy **trực tiếp từ server** qua `LocalDate.now()`, không nhận từ client để ngăn chặn hành vi gian lận.
- Toàn bộ thao tác tính ngày sử dụng `java.time.LocalDate` và `java.time.temporal.ChronoUnit.DAYS` — không sử dụng `java.util.Date` hoặc bất kỳ API thời gian cũ nào.
- Tiền tệ tính toán dùng `java.math.BigDecimal` kèm `RoundingMode.HALF_UP` để tránh sai số dấu phẩy động.

---

## 5. Đặc tả API (API Specifications)

### 5.1. Mở sổ tiết kiệm

- **Method & Endpoint:** `POST /api/v1/term-deposits/open`
- **Request Body:**

```json
{
  "bankAccountId": 1,
  "principalAmount": 100000000,
  "termMonths": 6
}
```

- **Kết quả thành công (HTTP 200):** Trả về thông tin sổ tiết kiệm vừa mở, bao gồm `depositDate`, `maturityDate`, `interestRate`, `status = ACTIVE`.
- **Lỗi (HTTP 400):** Nếu số dư tài khoản không đủ, hoặc `termMonths` không phải 1/6/12.

### 5.2. Tất toán sổ tiết kiệm

- **Method & Endpoint:** `POST /api/v1/term-deposits/{id}/settle`
- **Request Body:** Không cần (ngày tất toán do server tự lấy).
- **Kết quả thành công (HTTP 200):** Trả về thông tin sổ đã tất toán với `status = SETTLED`. Số dư `BankAccount` đã được cộng đủ gốc + lãi.
- **Lỗi (HTTP 400):** Nếu sổ đã có `status = SETTLED` trước đó, trả về: `"Sổ tiết kiệm đã được tất toán trước đó"`.
- **Lỗi (HTTP 404):** Nếu không tìm thấy sổ theo `id`.

---

*Tài liệu SRS này đáp ứng đầy đủ yêu cầu Nhiệm vụ 1 của đề bài: Thiết kế Entity TermDeposit, trình bày thuật toán tính lãi theo số ngày thực tế, và mô tả rõ ràng logic rẽ nhánh "trước hạn / đúng hạn" cùng các ràng buộc nghiệp vụ kèm ví dụ minh họa số liệu.*
