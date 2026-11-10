# Fintech Chatbot — Backend (Spring Boot)

Backend cho đề tài **AI Chatbot RAG hỗ trợ quản lý và tư vấn tài chính**.
Đặc tả chức năng: [`../USE_CASES.md`](../USE_CASES.md). Mọi mã `UC-xxx`, `BR-xx`, `E-xx` trong code tham chiếu file đó.

## Công nghệ

| Thành phần | Phiên bản / lựa chọn |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | 3.5.16 (Web, Security, Validation, Data JPA) |
| ORM | Hibernate 6 — **tầng DAO viết tay** bằng `Session` + HQL, không dùng `JpaRepository` |
| AI | API tương thích OpenAI (mặc định Vilao AI), gọi bằng Spring `RestClient`; PDFBox, Jsoup cho RAG |
| Database | PostgreSQL 16 + pgvector (Docker); H2 chế độ PostgreSQL khi chạy test |
| Migration | Flyway (`src/main/resources/db/migration`), Hibernate `ddl-auto=validate` |
| Bảo mật | JWT (jjwt 0.12), BCrypt, role `USER` / `ADMIN` |
| Khác | Lombok, MapStruct, springdoc-openapi (Swagger UI), Apache POI (xuất Excel) |

## Kiến trúc

```
Controller  (REST, DTO, @Valid)                     controller/, controller/admin/
   ↓
Service     (interface + impl, @Transactional)      service/, service/impl/
   ↓
DAO         (interface + impl, Hibernate Session)   dao/, dao/impl/
   ↓
Entity      (@Entity, 18 thực thể E-01 → E-18)      entity/, entity/enums/
```

- `GenericDao<T, ID>` → `AbstractHibernateDao<T>`: CRUD chung bằng Hibernate `Session`.
- `UserOwnedDao<T, ID>` → `AbstractUserOwnedDao<T>`: mọi truy vấn có điều kiện `userId` (BR-05).
- DAO dùng `@Transactional(propagation = MANDATORY)`: **chỉ được gọi từ Service** có transaction.
- Entity thuộc user kế thừa `UserOwnedEntity` (cột `user_id`); quan hệ `@ManyToOne` kèm cột `*_id` chỉ đọc.

```
src/main/java/com/fintech/chatbot/
├── common/        ApiResponse, PageResponse, ErrorCode, BusinessException, GlobalExceptionHandler, MoneyParser
├── config/        SecurityConfig, OpenApiConfig, ClockConfig, AdminSeeder, properties/
├── security/      JwtService, JwtAuthFilter, PrincipalLoader, UserPrincipal, @CurrentUser
├── entity/        18 entity + enums/
├── dao/           interface DAO  +  impl/ (Hibernate)
├── dto/           request/, response/
├── mapper/        MapStruct
├── service/       interface  +  impl/  +  support/ (TransactionRecorder, BudgetAlertService, Recurrence…)
├── calculator/    FinancialCalculator (UC-B05)
├── scheduler/     DailyReminderJob (UC-D04)
└── controller/    REST API  +  admin/
```

## Chạy dự án

Yêu cầu: **JDK 21** và **Docker Desktop**. Không cần cài Maven vì đã có `mvnw`.

```bash
docker compose up -d postgres
```

Đặt biến môi trường cho tài khoản admin (chỉ cần trước lần chạy đầu), rồi chạy (PowerShell):

```powershell
$env:ADMIN_EMAIL="admin@fintech.local"; $env:ADMIN_PASSWORD="<mật khẩu của bạn>"; .\mvnw.cmd spring-boot:run
```

Mở Swagger UI: http://localhost:8080/swagger-ui.html → gọi `POST /api/auth/login` → copy `accessToken` → bấm **Authorize**.

Chạy test (không cần Docker, dùng H2):

```bash
./mvnw test
```

### Biến môi trường

| Biến | Mặc định (profile dev) | Ghi chú |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fintech` | |
| `DB_USERNAME` / `DB_PASSWORD` | `fintech` / `fintech` | Khớp `docker-compose.yml` |
| `JWT_SECRET` | chuỗi dev trong `application-dev.yml` | **Bắt buộc đặt** khi triển khai thật, ≥ 32 byte |
| `JWT_EXPIRATION_MINUTES` | `1440` | |
| `ADMIN_EMAIL` | `admin@fintech.local` | |
| `ADMIN_PASSWORD` | *(trống)* | Trống → không tạo admin |

## Giao diện (React + Vite + TypeScript)

Mã nguồn ở `frontend/`: React 19, React Router, Tailwind CSS 4, react-markdown. Hiện có:
- **Đăng nhập / đăng ký** và **khung chat** (lịch sử hội thoại, thẻ giao dịch nháp sửa được, trích dẫn nguồn,
  👍/👎 kèm lý do, micro bằng Web Speech API — Chrome / Edge).
- **Admin → Kho tri thức** (`/admin/knowledge`, UC-C01): thêm tài liệu bằng tải file / dán văn bản / URL công khai, xem trước
  các đoạn, sửa metadata, duyệt (tạo embedding), tắt, thay phiên bản mới, xóa, index lại toàn bộ, **thử truy vấn** xem điểm từng đoạn.

Profile `h2` tạo sẵn admin chạy thử `admin@fintech.local` (mật khẩu trong `application-h2.yml`, đổi bằng `ADMIN_PASSWORD`).

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173, tự chuyển /api sang backend :8080
npm run build    # build vào src/main/resources/static → Spring Boot phục vụ luôn giao diện tại :8080
```

Chạy thử toàn bộ **không cần Docker** (dữ liệu H2 lưu ở `data/`):

```powershell
cd frontend; npm run build; cd ..; .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,h2"
```

## API

### Giai đoạn 1 — Tài khoản & quản trị

| Use case | Method & path | Quyền |
|---|---|---|
| UC-D01 Đăng ký | `POST /api/auth/register` | public |
| UC-D02 Đăng nhập | `POST /api/auth/login` | public |
| UC-D02 Đăng xuất | `POST /api/auth/logout` | đăng nhập |
| UC-D03 Xem / sửa hồ sơ | `GET`, `PUT /api/me` | đăng nhập |
| UC-D03 Đổi mật khẩu | `PUT /api/me/password` | đăng nhập |
| UC-D03 Xuất dữ liệu | `GET /api/me/export` (ZIP) | đăng nhập |
| UC-D03 Xóa tài khoản | `DELETE /api/me` | USER |
| UC-C05 Quản lý tài khoản | `GET /api/admin/users`, `POST /api/admin/users/{id}/lock` · `/unlock` · `/reset-password` | ADMIN |
| UC-C06 Danh mục mặc định | `GET`, `POST /api/admin/categories`, `PUT`, `DELETE /{id}`, `POST /{id}/hide` · `/unhide` | ADMIN |

Hành vi đáng chú ý:
- Đăng ký tạo sẵn ví **"Tiền mặt"** (mặc định) và quyền dữ liệu AI bật hết (UC-D05).
- Sai mật khẩu **5 lần** → khóa đăng nhập **15 phút** (`app.security.*`).
- Đăng xuất / đổi mật khẩu / bị khóa / reset mật khẩu → mọi JWT cũ mất hiệu lực (cơ chế `token_version`).
- Quên mật khẩu qua email **chưa làm** (cần SMTP); tạm thời Admin reset và cấp mật khẩu tạm.

### Giai đoạn 2 — Quản lý & tư vấn tài chính

| Use case | Method & path |
|---|---|
| UC-A01 Ví | `GET`, `POST /api/wallets` · `GET`, `PUT`, `DELETE /api/wallets/{id}` · `POST /{id}/adjust-balance` · `/archive` · `/unarchive` |
| UC-A02 Giao dịch | `POST /api/transactions` · `GET`, `PUT`, `DELETE /api/transactions/{id}` |
| UC-A03 Danh mục | `GET`, `POST /api/categories` · `PUT`, `DELETE /{id}` · `POST /{id}/hide` · `/unhide` · `/merge` |
| UC-A04 Ngân sách | `GET /api/budgets?month=yyyy-MM`, `POST` · `GET`, `PUT`, `DELETE /{id}` · `POST /copy-previous?month=` |
| UC-A05 Mục tiêu tiết kiệm | `GET`, `POST /api/saving-goals` · `GET`, `PUT /{id}` · `POST /{id}/deposit` · `/withdraw` · `/cancel` · `GET /{id}/transactions` |
| UC-A06 Sổ nợ | `GET`, `POST /api/debts` · `GET /summary` · `GET`, `PUT`, `DELETE /{id}` · `POST /{id}/payments` · `GET /{id}/transactions` |
| UC-A07 Định kỳ | `GET`, `POST /api/recurring-rules` · `GET`, `PUT`, `DELETE /{id}` · `POST /{id}/pause` · `/resume` · `/execute` |
| UC-A08 Lọc giao dịch | `GET /api/transactions?from&to&walletId&categoryId&type&minAmount&maxAmount&source&keyword&page&size` |
| UC-A09 Báo cáo | `GET /api/reports/overview?from&to` |
| UC-A10 Xuất file | `GET /api/transactions/export?from&to&format=XLSX\|CSV` |
| UC-B04 Hồ sơ tài chính | `GET`, `PUT /api/financial-profile` · `GET /risk-questions` · `POST /risk-assessment` |
| UC-B05 Công cụ mô phỏng | `POST /api/tools/compound-interest` · `/loan/annuity` · `/loan/equal-principal` · `/loan/flat` · `/loan/compare` · `/saving-target` |
| UC-B06 Cảnh báo ngân sách | tự động sau mỗi giao dịch chi; xem ở `GET /api/notifications`, `GET /unread-count`, `POST /{id}/read`, `POST /read-all` |
| UC-B08 Hội thoại | `GET`, `POST /api/conversations` · `GET`, `PATCH`, `DELETE /{id}` |
| UC-D04 Nhắc nhở tự động | job `DailyReminderJob` chạy 07:00 hằng ngày (`app.scheduler.daily-cron`) |
| UC-D05 Quyền dữ liệu AI | `GET`, `PUT /api/me/ai-permissions` |

Quy tắc nghiệp vụ chính (đều có test trong `FinanceFlowTest`):
- **BR-07 số dư:** mọi thêm / sửa / xóa giao dịch đều tính lại số dư từ đầu (`TransactionRecorder`), kể cả ví cũ khi đổi ví.
  Điều chỉnh số dư tạo giao dịch `ADJUST` mang dấu (migration V3).
- Giao dịch sinh từ **khoản nợ / mục tiêu** chỉ sửa được qua chức năng đó, và **không tính vào thu chi** trong báo cáo.
- **Ngân sách danh mục cha** tính cả chi của danh mục con. Cảnh báo mỗi mức (ngưỡng, vượt) **chỉ gửi 1 lần** / ngân sách.
- **Định kỳ:** lần lặp tính từ ngày bắt đầu (ngày 31 → cuối tháng ngắn → quay lại 31); server tắt lâu thì ghi bù tối đa 31 kỳ.
- **Báo cáo:** tài sản ròng = Σ số dư ví + cho vay còn lại − đi vay còn lại; lịch sử số dư = cuối mỗi tháng trong 6 tháng.
- **Công cụ B05:** tính bằng `BigDecimal` 34 chữ số; lịch trả nợ làm tròn lãi từng kỳ, kỳ cuối tự điều chỉnh để dư nợ về 0.

Khác biệt nhỏ so với USE_CASES.md (đã chọn cho gọn, có thể đổi):
- User **không ẩn được danh mục mặc định** (chỉ ẩn danh mục riêng); Admin ẩn danh mục mặc định cho mọi người ở UC-C06.
- Giao dịch định kỳ chỉ hỗ trợ **thu / chi** (chưa hỗ trợ chuyển tiền định kỳ).
- Bổ sung **xóa khoản nợ** (xóa luôn giao dịch liên quan); mục tiêu tiết kiệm chỉ **hủy**, không xóa.
- Cảnh báo ngân sách dựa trên số **đã chi thực tế**; dự báo chi tiêu nằm ở Backlog (§11).

### Giai đoạn 3 — AI (chatbot, RAG)

| Use case | Method & path |
|---|---|
| A11, A13, B01, B02, B07 (qua Intent Router) | `POST /api/chat/messages` `{conversationId?, message, inputMode: TEXT\|VOICE}` |
| A11 bước 6 — lưu thẻ giao dịch đã xác nhận | `POST /api/chat/transactions` |
| A12, B03 — giọng nói | Mặc định: Web Speech API ở trình duyệt → gửi transcript với `inputMode=VOICE`. Tùy chọn: `POST /api/chat/transcribe` (multipart `audio`) |
| B09 — đánh giá câu trả lời | `POST /api/chat/messages/{id}/feedback` |
| C01 — kho tri thức | `GET`, `POST /api/admin/knowledge/documents` (multipart `file` + `metadata`) · `POST /documents/from-url` · `GET`, `PUT`, `DELETE /documents/{id}` · `POST /{id}/approve` · `/disable` · `/content` · `POST /reindex` · `GET /test-query?q=` |
| C02 — kiểm duyệt báo cáo | `GET /api/admin/feedback` · `GET`, `PUT /api/admin/feedback/{id}` |
| C03 — chất lượng chatbot | `GET /api/admin/chat-quality` · `POST`, `GET /api/admin/chat-quality/evaluations` · `GET /evaluations/{id}` |
| C04 — cấu hình AI | `GET`, `PUT /api/admin/ai-config` · `POST /reset` |

Thiết kế chính (USE_CASES.md §9):
- **Nhà cung cấp AI:** **Vilao AI** cho cả chat, embedding và giọng nói, gọi qua API tương thích OpenAI (`OpenAiCompatibleClient`);
  đổi nhà cung cấp chỉ cần đổi `AI_*_BASE_URL`. Mỗi model phải được **đăng ký (subscribe) cho API key trong Vilao console**, nếu không
  Vilao trả `403 Please subscribe to model`. Chưa có key → app vẫn chạy, chức năng AI trả `503 AI_NOT_CONFIGURED`.
- **Intent Router** (`ai/IntentRouter`): LLM phân loại `{intent, confidence}`; confidence < 0,5 → hỏi lại; LLM trả sai định dạng → luật dự phòng.
- **BR-06 — số liệu do code tính:** A13 LLM chỉ sinh truy vấn JSON theo schema cố định (không sinh SQL), code chạy truy vấn có `userId`;
  B02 tóm tắt tài chính do code tính; B07 tỷ giá lấy từ open.er-api.com và phép quy đổi do code tính.
- **RAG:** PDF/DOCX/HTML/TXT/MD → làm sạch → chia đoạn ~2000 ký tự (~500 token), chồng lấn ~200 → embedding → **pgvector**
  (bảng `chunk_embeddings`, migration `db/vendor/postgresql/V5`). Truy xuất top_k, giữ top_n, so ngưỡng điểm; không đạt → trả "không đủ thông tin" mà **không gọi LLM** (BR-02).
  Câu trả lời luôn kèm trích dẫn và disclaimer (BR-01). Test chạy trên H2 dùng bảng vector dạng văn bản + cosine trong Java.
- **UC-C02:** mỗi câu trả lời lưu kèm các đoạn đã truy xuất và điểm, để Admin xem lý do trả lời sai.
- **Nhập URL tài liệu** có chống SSRF (chặn localhost, mạng nội bộ, metadata cloud, không theo redirect).
- Giá vàng SJC và lãi suất NHNN **chưa tích hợp** (chưa có endpoint công khai ổn định); chatbot nói rõ và chỉ nguồn để tự tra.

### Model AI cho từng tác vụ (Vilao AI)

Mỗi model phải được **subscribe cho API key** trong Vilao console. Đo với API thật trên prompt thật của app
(23 và 26/10/2026, `LiveVilaoSmokeTest`). `gemini-3.1-flash-lite` trả `EMPTY_OUTPUT` khoảng 5/6 lần nên không dùng.
Lỗi tạm thời của nhà cung cấp (5xx, phản hồi rỗng) được tự thử lại 1 lần.

| Tác vụ (`AiTask`) | Use case | Model mặc định | Biến môi trường | Lý do |
|---|---|---|---|---|
| Embedding | C01, B01, B02 | `dg/text-embedding-3-large` (3072 chiều) | `AI_EMBEDDING_MODEL` | Đoạn liên quan ≈ 0,57–0,72, không liên quan ≈ 0,23–0,33 → ngưỡng `rag.score_threshold` = 0,45 |
| `ROUTER` | §9.1 | `vgpt/gpt-6-sol` | `AI_MODEL_ROUTER` | Đúng 13/13 như gemini-3.8-flash nhưng nhanh và đều hơn (trung vị 3,2s, chậm nhất 5,8s so với 4,3s / 10,6s) |
| `EXTRACTION` | A11, A13 (lập truy vấn), B07 | `vgpt/gpt-6-sol` | `AI_MODEL_EXTRACTION` | Trả JSON nhanh: ghi giao dịch 37s → 4,6s so với gemini |
| `ANSWER` | B01, A13 (diễn đạt) | `chib/gemini-3.8-flash` | `AI_MODEL_ANSWER` | Trả lời tiếng Việt tốt, trích dẫn đúng [1] |
| `ADVICE` | B02 | `chib/gemini-3.8-flash` | `AI_MODEL_ADVICE` | |
| `JUDGE` | C03 (bộ đánh giá) | `vgpt/gpt-6-sol` | `AI_MODEL_JUDGE` | Khác họ model trả lời, tránh tự chấm cho mình |

Thứ tự ưu tiên: cấu hình Admin ở UC-C04 (`llm.model.<tác vụ>`) → biến môi trường của tác vụ → `AI_CHAT_MODEL` (mặc định `chib/gemini-3.8-flash`).
Đổi model embedding → bắt buộc `POST /api/admin/knowledge/reindex`.

| Biến khác | Ghi chú |
|---|---|
| `VILAO_API_KEY` | **Không** commit, **không** dán vào chat |
| `AI_STT_MODEL` | Tùy chọn, cho `POST /api/chat/transcribe` (mặc định dùng Web Speech API ở trình duyệt) |

Chạy thử với API thật (tốn vài chục đồng, dùng H2): đặt `VILAO_LIVE=1` và `VILAO_API_KEY` rồi `./mvnw test -Dtest=LiveVilaoSmokeTest`.

## Lộ trình

- [x] **Giai đoạn 1 — Nền tảng:** khung project, bảo mật, 18 entity + schema, DAO, D01–D03, C05, C06
- [x] **Giai đoạn 2 — Quản lý tài chính:** A01–A10, B04, B05, B06, B08, D04, D05
- [x] **Giai đoạn 3 — AI:** Intent Router, RAG (pgvector), A11–A13, B01–B03, B07, B09, C01–C04 — đã test với AI giả lập,
      **chưa chạy với API key thật và PostgreSQL thật**
