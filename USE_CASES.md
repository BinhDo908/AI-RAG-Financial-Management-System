# USE CASE SPECIFICATION — AI Chatbot RAG hỗ trợ Quản lý & Tư vấn Tài chính

> **Phiên bản:** 1.0 · **Ngày:** 2026-10-23 · **Trạng thái:** Đã chốt danh sách use case, chưa triển khai
>
> File này là **nguồn sự thật duy nhất (single source of truth)** về chức năng của hệ thống. Mọi tài liệu khác (SRS, thiết kế DB, API, code, test) phải bám theo các mã ID trong file này.

---

## 0. Hướng dẫn sử dụng file (dành cho người và AI)

| Muốn làm gì | Đọc mục |
|---|---|
| Hiểu đề tài trong 1 phút | §1 Tổng quan |
| Biết ai dùng hệ thống | §2 Actor |
| Kiểm tra một quyết định có vi phạm quy tắc không | §3 Business Rules |
| Biết dữ liệu / tài liệu / thư viện lấy từ đâu | §4 Nguồn mở & công nghệ |
| Thiết kế database | §5 Thực thể dữ liệu |
| Xem toàn bộ use case ngắn gọn | §6 Danh sách UC + §7 Ma trận |
| Viết code / test cho một chức năng | §8 Đặc tả chi tiết (tìm theo mã `UC-xxx`) |
| Hiểu luồng chatbot hoạt động | §9 Kiến trúc luồng AI |
| Yêu cầu phi chức năng | §10 NFR |
| Những gì **chưa làm** | §11 Backlog |

**Quy ước mã ID** (không đổi ID đã cấp; thêm mới thì tăng số tiếp theo):

| Tiền tố | Ý nghĩa |
|---|---|
| `UC-Axx` | Use case nhóm A — Quản lý tài chính |
| `UC-Bxx` | Use case nhóm B — Tư vấn tài chính |
| `UC-Cxx` | Use case nhóm C — Quản trị & Kho tri thức |
| `UC-Dxx` | Use case nhóm D — Chung / Tài khoản / Tự động |
| `BR-xx` | Business Rule |
| `NFR-xx` | Non-functional Requirement |
| `E-xx` | Thực thể dữ liệu (Entity) |
| `SRC-xx` | Nguồn dữ liệu / tài liệu mở |

**Quy ước trong đặc tả:** `PRE` = tiền điều kiện, `POST` = hậu điều kiện, `AF` = luồng thay thế, `EX` = luồng ngoại lệ. Mức ưu tiên: **High** (bắt buộc cho bản demo), **Medium** (nên có), **Low** (có thì tốt).

---

## 1. Tổng quan

### 1.1 Bối cảnh
Người dùng cá nhân thường ghi chép chi tiêu rời rạc, bỏ dở vì nhập liệu mất công, và khi cần kiến thức tài chính (lập ngân sách, vay trả góp, tiết kiệm…) thì phải tự tìm trên mạng với nguồn kém tin cậy. Các ứng dụng hiện có (Money Lover, MISA MoneyKeeper, Cleo, Monarch Money) mạnh về quản lý/phân tích dữ liệu cá nhân nhưng **chưa nhấn mạnh việc trả lời kiến thức tài chính có trích dẫn nguồn kiểm chứng được**.

### 1.2 Mục tiêu
- **BO-01:** Cung cấp công cụ quản lý thu chi đầy đủ như ứng dụng quản lý tài chính thông thường.
- **BO-02:** Giảm công nhập liệu bằng **chat ngôn ngữ tự nhiên** và **giọng nói (voice-to-text)**.
- **BO-03:** Chatbot **RAG** trả lời kiến thức tài chính **có trích dẫn nguồn mở**, kết hợp tư vấn cá nhân hóa dựa trên dữ liệu thật của người dùng.
- **BO-04:** Toàn bộ dữ liệu tri thức, mô hình, thư viện đều **mở / miễn phí**, người làm đề tài tự triển khai và truy cập được.

### 1.3 Hai tác vụ chính
1. **Quản lý tài chính** (Nhóm A): ví, giao dịch, danh mục, ngân sách, tiết kiệm, nợ, định kỳ, báo cáo + nhập liệu bằng chat/giọng nói + hỏi đáp dữ liệu cá nhân.
2. **Tư vấn tài chính** (Nhóm B): hỏi đáp kiến thức (RAG), tư vấn cá nhân hóa, công cụ mô phỏng, cảnh báo, tra cứu thị trường.

### 1.4 Phạm vi
**Trong phạm vi:** nhập liệu thủ công/chat/giọng nói, quản lý tiền như app thông thường, chatbot RAG trên kho tri thức nguồn mở, tư vấn cá nhân hóa mang tính giáo dục.

**Ngoài phạm vi:** liên kết tài khoản ngân hàng thật, thực hiện giao dịch tài chính (chuyển tiền, mua bán chứng khoán), khuyến nghị đầu tư cụ thể, dự báo chi tiêu (xem §11 Backlog).

---

## 2. Actor

| ID | Actor | Loại | Mô tả | Use case chính |
|---|---|---|---|---|
| ACT-01 | **Người dùng (User)** | Con người | Chủ thể trung tâm. Tự quản lý tiền, nhập liệu bằng form / chat / giọng nói, hỏi chatbot để được tư vấn. | A01–A13, B01–B09, D01–D03, D05 |
| ACT-02 | **Admin (kiêm Chuyên viên tài chính)** | Con người | Quản trị hệ thống **và** quản lý kho tri thức RAG: thu thập tài liệu nguồn mở, duyệt, index; kiểm duyệt câu trả lời bị báo sai; cấu hình AI; quản lý tài khoản. Không tham gia quản lý tiền của user. | C01–C06, D02 |
| ACT-03 | **Hệ thống AI** | Tự động | Chuyển giọng nói → văn bản, phân loại ý định, trích xuất giao dịch, gợi ý danh mục, truy xuất & sinh câu trả lời RAG, gửi cảnh báo/nhắc nhở. | A03, A11–A13, B01–B03, B06, B07, C01, D04 |
| ACT-04 | *Nguồn dữ liệu mở (External)* | Hệ thống ngoài, phụ | API/trang công khai cung cấp tỷ giá, lãi suất, giá vàng. | B07 |

**Phân quyền:** chỉ có 2 role đăng nhập: `USER` và `ADMIN`. Admin **không** xem được giao dịch chi tiết của user (chỉ xem thống kê ẩn danh và nội dung hội thoại bị user chủ động báo cáo — xem BR-05, BR-09).

---

## 3. Business Rules

| ID | Quy tắc | Loại | Áp dụng cho |
|---|---|---|---|
| BR-01 | Chatbot chỉ tư vấn mang tính **giáo dục, tham khảo**. Không khuyến nghị mua/bán một tài sản cụ thể (mã cổ phiếu, coin, sản phẩm của một ngân hàng cụ thể). Mọi câu trả lời tư vấn có **disclaimer**. | Static | B01, B02, C04 |
| BR-02 | Câu trả lời kiến thức phải **trích dẫn tài liệu trong kho tri thức**. Nếu không truy xuất được đoạn tài liệu đạt ngưỡng liên quan → trả lời "Không đủ thông tin trong nguồn tham khảo", không tự bịa. | Static | B01, B02 |
| BR-03 | Chỉ tài liệu từ **nguồn mở** (§4.1) và có trạng thái **Đã duyệt** mới được đưa vào index RAG. | Static | C01 |
| BR-04 | Giao dịch do AI trích xuất (từ chat hoặc giọng nói) **phải được user xác nhận** trước khi lưu. | Static | A11, A12 |
| BR-05 | AI chỉ truy cập dữ liệu tài chính **của chính user đang đăng nhập**, và chỉ những nhóm dữ liệu user cho phép (UC-D05). | Static | A13, B02, D05 |
| BR-06 | **Mọi con số** trong câu trả lời của AI (tổng chi, số dư, kết quả mô phỏng, tỷ giá) phải lấy từ **truy vấn dữ liệu / module tính toán / API**, LLM không được tự sinh số liệu. | Static | A13, B02, B05, B07 |
| BR-07 | Số dư ví = số dư ban đầu + Σ thu − Σ chi ± chuyển khoản. Số dư được **tính lại** mỗi khi giao dịch thêm/sửa/xóa, không nhập tay trực tiếp (trừ điều chỉnh số dư — tạo giao dịch "Điều chỉnh"). | Static | A01, A02 |
| BR-08 | Đơn vị tiền mặc định **VND**, số tiền là số nguyên dương. Hỗ trợ cách viết tắt tiếng Việt khi nhập bằng chat/giọng nói: `k`/`nghìn`/`ngàn` = ×1.000; `tr`/`triệu`/`củ` = ×1.000.000; `tỷ` = ×1.000.000.000. | Static | A02, A11, A12 |
| BR-09 | File âm thanh ghi âm **không lưu** sau khi chuyển thành văn bản (chỉ lưu transcript). | Static | A12, B03 |
| BR-10 | Không xóa cứng danh mục đã có giao dịch; chỉ được **ẩn** hoặc **gộp** sang danh mục khác. | Static | A03, C06 |
| BR-11 | Ngưỡng cảnh báo ngân sách mặc định: **80%** (cảnh báo) và **100%** (vượt). User có thể đổi ngưỡng cảnh báo trong khoảng 50–95%. | Dynamic | A04, B06 |
| BR-12 | Dữ liệu thị trường (tỷ giá, giá vàng, lãi suất) luôn hiển thị **kèm nguồn và thời điểm cập nhật**. | Static | B07 |

---

## 4. Nguồn mở & công nghệ

### 4.1 Nguồn tài liệu cho kho tri thức RAG (Admin thu thập ở UC-C01)

| ID | Nguồn | Ngôn ngữ | Nội dung dùng được | Ghi chú |
|---|---|---|---|---|
| SRC-01 | Ngân hàng Nhà nước Việt Nam — sbv.gov.vn | VI | Giáo dục tài chính, thẻ, tín dụng tiêu dùng, lãi suất điều hành | Văn bản nhà nước công khai |
| SRC-02 | Ủy ban Chứng khoán Nhà nước — ssc.gov.vn | VI | Kiến thức cơ bản cho nhà đầu tư, cảnh báo lừa đảo | Công khai |
| SRC-03 | Cơ sở dữ liệu văn bản pháp luật quốc gia — vbpl.vn | VI | Luật Các tổ chức tín dụng, quy định cho vay tiêu dùng, thẻ ngân hàng | Công khai |
| SRC-04 | Bảo hiểm xã hội Việt Nam — baohiemxahoi.gov.vn | VI | BHXH, lương hưu, BHYT | Công khai |
| SRC-05 | CFPB "Your Money, Your Goals" — consumerfinance.gov | EN | Lập ngân sách, quỹ khẩn cấp, quản lý nợ, tín dụng | Tài liệu chính phủ Mỹ — public domain |
| SRC-06 | OECD/INFE — tài liệu giáo dục tài chính | EN | Khung kiến thức tài chính cá nhân | PDF miễn phí |
| SRC-07 | World Bank Open Knowledge Repository | EN | Báo cáo tài chính toàn diện | Giấy phép CC BY |

> Mỗi tài liệu khi đưa vào kho phải ghi: `source_id`, URL gốc, ngày truy cập, giấy phép (xem E-12).

### 4.2 Nguồn dữ liệu thị trường (UC-B07)

| ID | Nguồn | Dữ liệu | Ghi chú |
|---|---|---|---|
| SRC-10 | Tỷ giá trung tâm — sbv.gov.vn | USD/VND | Công khai |
| SRC-11 | Bảng tỷ giá công khai Vietcombank | Tỷ giá mua/bán các ngoại tệ | **Cần kiểm tra lại endpoint khi code** |
| SRC-12 | open.er-api.com / Frankfurter API | Tỷ giá quốc tế | Miễn phí, không cần API key |
| SRC-13 | Giá vàng niêm yết SJC | Giá vàng miếng | **Cần kiểm tra lại endpoint khi code** |

### 4.3 Dữ liệu đánh giá chatbot (UC-C03)

| ID | Dataset | Dùng để |
|---|---|---|
| SRC-20 | FiQA (Hugging Face) | Đánh giá truy xuất / QA tài chính |
| SRC-21 | finance-alpaca (Hugging Face) | Bộ câu hỏi mẫu kiểm thử |
| SRC-22 | Bộ câu hỏi tiếng Việt tự soạn (≥ 50 câu, có đáp án + nguồn) | Đánh giá chính cho tiếng Việt |

### 4.4 Thành phần kỹ thuật gợi ý (đều mở/miễn phí)

| Thành phần | Lựa chọn | Dùng ở |
|---|---|---|
| Voice-to-Text | **Web Speech API** (trình duyệt, demo nhanh) · **PhoWhisper** (VinAI, tối ưu tiếng Việt) · **OpenAI Whisper** (open-source) | A12, B03 |
| Embedding | **bge-m3** · multilingual-e5 | B01, C01 |
| Reranker | bge-reranker-v2-m3 | B01 |
| Vector DB | ChromaDB · FAISS · Qdrant | B01, C01 |
| Trích xuất văn bản | PyMuPDF (PDF) · BeautifulSoup / trafilatura (HTML) | C01 |
| LLM | Qwen / Llama qua Ollama (chạy local) hoặc LLM qua API | Toàn bộ nhóm AI |
| Biểu đồ | Chart.js / Recharts | A09 |

---

## 5. Thực thể dữ liệu (gợi ý cho thiết kế DB)

| ID | Thực thể | Thuộc tính chính | Liên quan UC |
|---|---|---|---|
| E-01 | `User` | id, email, password_hash, full_name, role (`USER`/`ADMIN`), status (`ACTIVE`/`LOCKED`), created_at | D01–D03, C05 |
| E-02 | `Wallet` | id, user_id, name, type (`CASH`/`BANK`/`EWALLET`), initial_balance, current_balance, is_archived | A01 |
| E-03 | `Category` | id, user_id (null = mặc định hệ thống), name, type (`INCOME`/`EXPENSE`), icon, parent_id, is_hidden | A03, C06 |
| E-04 | `Transaction` | id, user_id, wallet_id, category_id, type (`INCOME`/`EXPENSE`/`TRANSFER`/`ADJUST`), amount, to_wallet_id, date, note, source (`MANUAL`/`CHAT`/`VOICE`/`RECEIPT`/`RECURRING`), debt_id, goal_id | A02, A08, A11, A12, A14 |
| E-05 | `Budget` | id, user_id, category_id, month (YYYY-MM), limit_amount, warn_threshold (%) | A04, B06 |
| E-06 | `SavingGoal` | id, user_id, name, target_amount, current_amount, deadline, status | A05 |
| E-07 | `Debt` | id, user_id, type (`BORROW`/`LEND`), counterparty, principal, remaining, due_date, status | A06 |
| E-08 | `RecurringRule` | id, user_id, template (wallet, category, type, amount, note), frequency (`DAILY`/`WEEKLY`/`MONTHLY`/`YEARLY`), next_run_date, auto_create (bool), is_active | A07, D04 |
| E-09 | `FinancialProfile` | user_id, monthly_income, dependents, goals_text, risk_level (`LOW`/`MEDIUM`/`HIGH`), updated_at | B04, B02 |
| E-10 | `Conversation` | id, user_id, title, created_at, updated_at | B08 |
| E-11 | `Message` | id, conversation_id, role (`user`/`assistant`), content, input_mode (`TEXT`/`VOICE`/`IMAGE`), intent, citations (JSON), created_at | A11–A14, B01–B03 |
| E-12 | `Document` | id, title, source_id (SRC-xx), url, license, language, accessed_at, status (`DRAFT`/`APPROVED`/`DISABLED`), uploaded_by | C01 |
| E-13 | `Chunk` | id, document_id, content, position, embedding (vector), metadata (heading, page) | C01, B01 |
| E-14 | `Feedback` | id, message_id, user_id, rating (`UP`/`DOWN`), reason, comment, review_status (`OPEN`/`RESOLVED`/`REJECTED`), admin_note | B09, C02 |
| E-15 | `Notification` | id, user_id, type (`BUDGET_WARN`/`BUDGET_OVER`/`DEBT_DUE`/`RECURRING`), content, is_read, created_at | B06, D04 |
| E-16 | `AIConfig` | key, value (model, temperature, top_k, score_threshold, system_prompt, disclaimer, blocked_topics) | C04 |
| E-17 | `AIDataPermission` | user_id, allow_transactions, allow_budgets, allow_debts, allow_goals, allow_profile | D05 |
| E-18 | `MarketRate` | id, kind (`FX`/`GOLD`/`INTEREST`), symbol, buy, sell, source_id, fetched_at | B07 |

---

## 6. Danh sách Use Case

### Nhóm A — Quản lý tài chính

| ID | Tên | Actor | Ưu tiên |
|---|---|---|---|
| UC-A01 | Quản lý ví | User | High |
| UC-A02 | Quản lý giao dịch (thu / chi / chuyển tiền) | User | High |
| UC-A03 | Quản lý danh mục thu chi | User, AI | High |
| UC-A04 | Quản lý ngân sách | User | High |
| UC-A05 | Quản lý mục tiêu tiết kiệm | User | Medium |
| UC-A06 | Quản lý khoản nợ / cho vay | User | Medium |
| UC-A07 | Quản lý giao dịch định kỳ | User | Medium |
| UC-A08 | Tìm kiếm & lọc giao dịch | User | Medium |
| UC-A09 | Xem báo cáo & biểu đồ | User | High |
| UC-A10 | Xuất dữ liệu Excel / CSV | User | Low |
| UC-A11 | Ghi giao dịch bằng chat | User, AI | High |
| UC-A12 | Ghi giao dịch bằng giọng nói | User, AI | High |
| UC-A13 | Hỏi đáp dữ liệu tài chính cá nhân | User, AI | High |
| UC-A14 | Quét hóa đơn bằng ảnh | User, AI | Medium |

### Nhóm B — Tư vấn tài chính

| ID | Tên | Actor | Ưu tiên |
|---|---|---|---|
| UC-B01 | Chat tư vấn kiến thức tài chính (RAG) | User, AI | High |
| UC-B02 | Tư vấn tài chính cá nhân hóa | User, AI | High |
| UC-B03 | Đặt câu hỏi bằng giọng nói | User, AI | High |
| UC-B04 | Thiết lập hồ sơ tài chính | User | Medium |
| UC-B05 | Công cụ mô phỏng tài chính | User | Medium |
| UC-B06 | Nhận cảnh báo ngân sách | User, AI | Medium |
| UC-B07 | Tra cứu tỷ giá, giá vàng, lãi suất | User, AI, Nguồn mở | Low |
| UC-B08 | Quản lý lịch sử hội thoại | User | Medium |
| UC-B09 | Đánh giá / báo cáo câu trả lời AI | User | Medium |

### Nhóm C — Quản trị & Kho tri thức

| ID | Tên | Actor | Ưu tiên |
|---|---|---|---|
| UC-C01 | Quản lý kho tri thức RAG | Admin, AI | High |
| UC-C02 | Kiểm duyệt câu trả lời bị báo cáo | Admin | Medium |
| UC-C03 | Xem dashboard chất lượng chatbot | Admin | Medium |
| UC-C04 | Cấu hình AI & guardrail | Admin | Medium |
| UC-C05 | Quản lý tài khoản người dùng | Admin | Medium |
| UC-C06 | Quản lý danh mục mặc định | Admin | Low |

### Nhóm D — Chung / Tài khoản / Tự động

| ID | Tên | Actor | Ưu tiên |
|---|---|---|---|
| UC-D01 | Đăng ký | User | High |
| UC-D02 | Đăng nhập / Đăng xuất | User, Admin | High |
| UC-D03 | Quản lý hồ sơ cá nhân | User | Medium |
| UC-D04 | Nhắc nhở tự động | AI | Medium |
| UC-D05 | Cài đặt quyền dữ liệu cho AI | User | Medium |

**Tổng: 33 use case** (A: 13 · B: 9 · C: 6 · D: 5).

---

## 7. Ma trận Actor – Use Case

| Use Case | User | Admin | AI | Nguồn mở |
|---|:-:|:-:|:-:|:-:|
| **Nhóm A — Quản lý tài chính** | | | | |
| UC-A01 Quản lý ví | ✓ | | | |
| UC-A02 Quản lý giao dịch | ✓ | | | |
| UC-A03 Quản lý danh mục | ✓ | | ✓ | |
| UC-A04 Quản lý ngân sách | ✓ | | | |
| UC-A05 Mục tiêu tiết kiệm | ✓ | | | |
| UC-A06 Nợ / cho vay | ✓ | | | |
| UC-A07 Giao dịch định kỳ | ✓ | | | |
| UC-A08 Tìm kiếm & lọc | ✓ | | | |
| UC-A09 Báo cáo & biểu đồ | ✓ | | | |
| UC-A10 Xuất dữ liệu | ✓ | | | |
| UC-A11 Ghi giao dịch bằng chat | ✓ | | ✓ | |
| UC-A12 Ghi giao dịch bằng giọng nói | ✓ | | ✓ | |
| UC-A13 Hỏi đáp dữ liệu cá nhân | ✓ | | ✓ | |
| UC-A14 Quét hóa đơn | ✓ | | ✓ | |
| **Nhóm B — Tư vấn tài chính** | | | | |
| UC-B01 Chat tư vấn kiến thức (RAG) | ✓ | | ✓ | |
| UC-B02 Tư vấn cá nhân hóa | ✓ | | ✓ | |
| UC-B03 Hỏi bằng giọng nói | ✓ | | ✓ | |
| UC-B04 Hồ sơ tài chính | ✓ | | | |
| UC-B05 Công cụ mô phỏng | ✓ | | | |
| UC-B06 Cảnh báo ngân sách | ✓ | | ✓ | |
| UC-B07 Tỷ giá, vàng, lãi suất | ✓ | | ✓ | ✓ |
| UC-B08 Lịch sử hội thoại | ✓ | | | |
| UC-B09 Đánh giá câu trả lời | ✓ | | | |
| **Nhóm C — Quản trị & Kho tri thức** | | | | |
| UC-C01 Kho tri thức RAG | | ✓ | ✓ | |
| UC-C02 Kiểm duyệt câu trả lời | | ✓ | | |
| UC-C03 Dashboard chất lượng | | ✓ | | |
| UC-C04 Cấu hình AI & guardrail | | ✓ | | |
| UC-C05 Quản lý tài khoản | | ✓ | | |
| UC-C06 Danh mục mặc định | | ✓ | | |
| **Nhóm D — Chung** | | | | |
| UC-D01 Đăng ký | ✓ | | | |
| UC-D02 Đăng nhập / Đăng xuất | ✓ | ✓ | | |
| UC-D03 Hồ sơ cá nhân | ✓ | | | |
| UC-D04 Nhắc nhở tự động | | | ✓ | |
| UC-D05 Quyền dữ liệu cho AI | ✓ | | | |

### Quan hệ giữa các use case

| Quan hệ | Mô tả |
|---|---|
| UC-A12 `<<include>>` UC-B03 (thành phần Voice-to-Text) | Ghi giao dịch bằng giọng nói dùng chung bộ chuyển giọng nói |
| UC-A12 `<<include>>` UC-A11 | Sau khi có transcript, xử lý như ghi giao dịch bằng chat |
| UC-A11 `<<include>>` UC-A03 | AI gợi ý danh mục cho giao dịch |
| UC-A02 `<<extend>>` UC-B06 | Lưu giao dịch có thể kích hoạt cảnh báo ngân sách |
| UC-B02 `<<include>>` UC-B01 | Tư vấn cá nhân hóa dùng lại pipeline RAG |
| UC-B02 `<<include>>` UC-A13 | Tư vấn cá nhân hóa dùng số liệu cá nhân |
| UC-A14 `<<include>>` UC-A03 | AI gợi ý danh mục cho từng món trên hóa đơn; lưu như UC-A11 bước 6 |
| UC-B09 → UC-C02 | Báo cáo của user tạo hàng chờ kiểm duyệt cho Admin |
| UC-A07 → UC-D04 | Quy tắc định kỳ sinh nhắc nhở / giao dịch tự động |

---

## 8. Đặc tả chi tiết Use Case

> Mẫu chung mỗi UC: **ID · Tên · Actor chính · Actor phụ · Mô tả · Trigger · PRE · POST · Normal Flow · AF · EX · Business Rules · Dữ liệu · Ưu tiên**.

---

### NHÓM A — QUẢN LÝ TÀI CHÍNH

#### UC-A01: Quản lý ví

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | User tạo, xem, sửa, lưu trữ (archive) các ví tiền: tiền mặt, tài khoản ngân hàng, ví điện tử. Số dư nhập tay ban đầu, **không liên kết ngân hàng thật**. |
| **Trigger** | User mở màn hình "Ví" và chọn Thêm / Sửa / Lưu trữ. |
| **PRE** | PRE-1: User đã đăng nhập. |
| **POST** | POST-1: Ví được lưu. POST-2: Tổng số dư toàn bộ ví được cập nhật trên trang chủ. |
| **Normal Flow** | 1. User mở "Ví" → hệ thống hiển thị danh sách ví + số dư từng ví + tổng số dư. 2. User bấm "Thêm ví". 3. User nhập: tên, loại (Tiền mặt / Ngân hàng / Ví điện tử), số dư ban đầu, đặt làm ví mặc định (tùy chọn). 4. User bấm "Lưu" → hệ thống kiểm tra hợp lệ và lưu. |
| **AF** | AF-1 Sửa ví: chỉ sửa tên/loại/ví mặc định; muốn đổi số dư → tạo giao dịch "Điều chỉnh số dư" (BR-07). AF-2 Lưu trữ ví: ví ẩn khỏi danh sách chọn khi nhập giao dịch nhưng giao dịch cũ vẫn giữ trong báo cáo. AF-3 Xóa ví: chỉ được xóa nếu ví chưa có giao dịch nào. |
| **EX** | EX-1: Tên ví trùng với ví khác của cùng user → báo lỗi. EX-2: Số dư ban đầu âm hoặc không phải số → báo lỗi. |
| **BR** | BR-07, BR-08 |
| **Dữ liệu** | E-02 Wallet, E-04 Transaction (ADJUST) |
| **Ưu tiên** | High |

#### UC-A02: Quản lý giao dịch (thu / chi / chuyển tiền)

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | User thêm, xem, sửa, xóa giao dịch bằng form. Có 3 loại: **Thu**, **Chi**, **Chuyển tiền** giữa 2 ví của mình. |
| **Trigger** | User bấm nút "+" hoặc chọn một giao dịch trong danh sách. |
| **PRE** | PRE-1: Đã đăng nhập. PRE-2: Có ít nhất 1 ví. |
| **POST** | POST-1: Giao dịch được lưu với `source = MANUAL`. POST-2: Số dư ví liên quan được tính lại (BR-07). POST-3: Tiến độ ngân sách của danh mục được cập nhật, có thể kích hoạt UC-B06. |
| **Normal Flow** | 1. User bấm "+". 2. Chọn loại: Thu / Chi / Chuyển tiền. 3. Nhập: số tiền, danh mục (với Thu/Chi), ví (với Chuyển tiền: ví nguồn + ví đích), ngày (mặc định hôm nay), ghi chú (tùy chọn). 4. (Tùy chọn) Gắn với khoản nợ (UC-A06) hoặc mục tiêu tiết kiệm (UC-A05). 5. Bấm "Lưu" → hệ thống lưu, tính lại số dư, kiểm tra ngân sách. |
| **AF** | AF-1 Sửa giao dịch: số dư ví cũ và mới đều được tính lại. AF-2 Xóa giao dịch: hệ thống hỏi xác nhận → xóa → tính lại số dư. AF-3 Số dư ví sau khi chi bị âm: cho phép lưu nhưng hiển thị cảnh báo "Số dư ví sẽ âm". |
| **EX** | EX-1: Số tiền ≤ 0 → báo lỗi. EX-2: Chuyển tiền với ví nguồn = ví đích → báo lỗi. EX-3: Thiếu danh mục với Thu/Chi → báo lỗi. |
| **BR** | BR-07, BR-08 |
| **Dữ liệu** | E-04 Transaction, E-02 Wallet, E-05 Budget |
| **Ưu tiên** | High |

#### UC-A03: Quản lý danh mục thu chi

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Actor phụ** | Hệ thống AI |
| **Mô tả** | User dùng bộ danh mục mặc định (do Admin quản lý ở UC-C06) và có thể tạo danh mục riêng, danh mục con, ẩn hoặc gộp danh mục. AI gợi ý danh mục khi nhập giao dịch dựa trên ghi chú. |
| **Trigger** | User mở "Danh mục" hoặc nhập giao dịch có ghi chú. |
| **PRE** | PRE-1: Đã đăng nhập. |
| **POST** | POST-1: Danh mục được tạo / sửa / ẩn / gộp. |
| **Normal Flow** | 1. User mở "Danh mục" → hệ thống hiển thị 2 tab Chi / Thu, gồm danh mục mặc định + danh mục riêng. 2. User bấm "Thêm" → nhập tên, loại, biểu tượng, danh mục cha (tùy chọn). 3. Bấm "Lưu". |
| **AF** | AF-1 Gợi ý danh mục (AI): khi user nhập ghi chú (vd "grab về nhà"), AI gợi ý danh mục phù hợp nhất ("Di chuyển"); user có thể chọn lại. AF-2 Ẩn danh mục: danh mục không còn hiện khi nhập liệu, giao dịch cũ giữ nguyên. AF-3 Gộp danh mục: chọn danh mục đích → toàn bộ giao dịch và ngân sách chuyển sang danh mục đích. |
| **EX** | EX-1: Xóa danh mục đã có giao dịch → không cho phép, gợi ý Ẩn hoặc Gộp (BR-10). EX-2: Trùng tên trong cùng loại → báo lỗi. |
| **BR** | BR-10 |
| **Dữ liệu** | E-03 Category |
| **Ưu tiên** | High |

#### UC-A04: Quản lý ngân sách

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | User đặt hạn mức chi tiêu theo **danh mục** và **tháng**, theo dõi đã chi / còn lại / % sử dụng. |
| **Trigger** | User mở "Ngân sách". |
| **PRE** | PRE-1: Đã đăng nhập. PRE-2: Có danh mục Chi. |
| **POST** | POST-1: Ngân sách được lưu. POST-2: Hệ thống bắt đầu theo dõi để cảnh báo (UC-B06). |
| **Normal Flow** | 1. User mở "Ngân sách" → hệ thống hiển thị danh sách ngân sách tháng hiện tại: danh mục, hạn mức, đã chi, còn lại, thanh % (xanh < ngưỡng cảnh báo, vàng ≥ ngưỡng cảnh báo, đỏ ≥ 100%). 2. User bấm "Thêm ngân sách" → chọn danh mục, tháng, hạn mức, ngưỡng cảnh báo (mặc định 80%). 3. Bấm "Lưu". |
| **AF** | AF-1 Sao chép ngân sách tháng trước sang tháng này. AF-2 Xem chi tiết: bấm vào một ngân sách → danh sách giao dịch thuộc danh mục đó trong tháng. AF-3 Sửa / xóa ngân sách. |
| **EX** | EX-1: Đã có ngân sách cho cùng danh mục + tháng → báo lỗi, gợi ý sửa ngân sách cũ. EX-2: Hạn mức ≤ 0 → báo lỗi. |
| **BR** | BR-11 |
| **Dữ liệu** | E-05 Budget, E-04 Transaction |
| **Ưu tiên** | High |

#### UC-A05: Quản lý mục tiêu tiết kiệm

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | User tạo mục tiêu (vd "Mua laptop 25 triệu trước 06/2027"), nạp tiền vào mục tiêu, theo dõi tiến độ. |
| **Trigger** | User mở "Mục tiêu tiết kiệm". |
| **PRE** | PRE-1: Đã đăng nhập. |
| **POST** | POST-1: Mục tiêu được lưu / cập nhật. POST-2: Khi `current_amount ≥ target_amount` → trạng thái `COMPLETED` + thông báo chúc mừng. |
| **Normal Flow** | 1. User bấm "Thêm mục tiêu" → nhập tên, số tiền mục tiêu, hạn (tùy chọn). 2. Hệ thống hiển thị: % hoàn thành, số tiền còn thiếu, **số tiền cần để dành mỗi tháng** = (mục tiêu − hiện có) / số tháng còn lại. 3. User bấm "Nạp tiền" → chọn ví nguồn, số tiền → hệ thống tạo giao dịch Chi gắn `goal_id` và cộng vào mục tiêu. |
| **AF** | AF-1 Rút tiền khỏi mục tiêu → tạo giao dịch Thu về ví. AF-2 Sửa / hủy mục tiêu. |
| **EX** | EX-1: Hạn đã qua → hiển thị "Quá hạn", vẫn cho nạp tiền. |
| **BR** | BR-06 (số tiền/tháng do code tính) |
| **Dữ liệu** | E-06 SavingGoal, E-04 Transaction |
| **Ưu tiên** | Medium |

#### UC-A06: Quản lý khoản nợ / cho vay

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | Ghi nhận khoản **đi vay** (mình nợ người khác) và **cho vay** (người khác nợ mình), ghi nhận các lần trả, theo dõi dư nợ và hạn trả. |
| **Trigger** | User mở "Sổ nợ". |
| **PRE** | PRE-1: Đã đăng nhập. |
| **POST** | POST-1: Khoản nợ được lưu. POST-2: Khi dư nợ = 0 → trạng thái `PAID`. POST-3: Nếu có hạn trả → đăng ký nhắc nhở (UC-D04). |
| **Normal Flow** | 1. User bấm "Thêm" → chọn Đi vay / Cho vay, nhập người liên quan, số tiền gốc, ngày, hạn trả (tùy chọn), ví nhận/chi. 2. Hệ thống tạo giao dịch tương ứng (Đi vay = Thu vào ví, Cho vay = Chi ra ví) gắn `debt_id`. 3. Khi trả/thu nợ: user bấm "Ghi nhận thanh toán" → nhập số tiền → hệ thống tạo giao dịch và trừ dư nợ. |
| **AF** | AF-1 Xem tổng: tổng đang nợ, tổng đang cho vay, khoản sắp đến hạn. |
| **EX** | EX-1: Số tiền thanh toán > dư nợ còn lại → báo lỗi. |
| **BR** | BR-07 |
| **Dữ liệu** | E-07 Debt, E-04 Transaction |
| **Ưu tiên** | Medium |

#### UC-A07: Quản lý giao dịch định kỳ

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | Tạo quy tắc lặp cho các khoản cố định: tiền nhà, điện nước, internet, lương. Hệ thống tự tạo giao dịch hoặc nhắc user xác nhận. |
| **Trigger** | User mở "Giao dịch định kỳ". |
| **PRE** | PRE-1: Đã đăng nhập. PRE-2: Có ví và danh mục. |
| **POST** | POST-1: Quy tắc được lưu với `next_run_date`. |
| **Normal Flow** | 1. User bấm "Thêm" → nhập như một giao dịch (ví, danh mục, loại, số tiền, ghi chú) + tần suất (Hằng ngày / Hằng tuần / Hằng tháng / Hằng năm) + ngày bắt đầu + chế độ: **Tự động tạo** hoặc **Nhắc để xác nhận**. 2. Bấm "Lưu". 3. Đến `next_run_date`, UC-D04 xử lý. |
| **AF** | AF-1 Tạm dừng / bật lại quy tắc. AF-2 Sửa / xóa quy tắc (không ảnh hưởng giao dịch đã sinh). |
| **EX** | EX-1: Ngày 31 với tháng không có ngày 31 → chạy vào ngày cuối tháng. |
| **BR** | BR-07 |
| **Dữ liệu** | E-08 RecurringRule, E-04 Transaction |
| **Ưu tiên** | Medium |

#### UC-A08: Tìm kiếm & lọc giao dịch

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | Xem lịch sử giao dịch nhóm theo ngày, lọc theo nhiều điều kiện, tìm theo ghi chú. |
| **Trigger** | User mở "Giao dịch". |
| **PRE** | PRE-1: Đã đăng nhập. |
| **POST** | Không thay đổi dữ liệu. |
| **Normal Flow** | 1. Hệ thống hiển thị giao dịch tháng hiện tại, nhóm theo ngày, kèm tổng thu / tổng chi / chênh lệch. 2. User chọn bộ lọc: khoảng thời gian, ví, danh mục, loại, khoảng số tiền, nguồn nhập (`MANUAL`/`CHAT`/`VOICE`/`RECEIPT`/`RECURRING`). 3. User nhập từ khóa → tìm trong ghi chú. 4. Hệ thống trả kết quả + tổng của kết quả lọc. |
| **AF** | AF-1 Không có kết quả → "Không tìm thấy giao dịch phù hợp". |
| **Dữ liệu** | E-04 Transaction |
| **Ưu tiên** | Medium |

#### UC-A09: Xem báo cáo & biểu đồ

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | Dashboard tổng quan tài chính cá nhân. |
| **Trigger** | User mở "Báo cáo" hoặc trang chủ. |
| **PRE** | PRE-1: Đã đăng nhập. |
| **POST** | Không thay đổi dữ liệu. |
| **Normal Flow** | 1. User chọn kỳ báo cáo (tháng này / tháng trước / tùy chọn). 2. Hệ thống hiển thị: **(a)** Tổng thu, tổng chi, chênh lệch; **(b)** Biểu đồ tròn chi theo danh mục; **(c)** Biểu đồ cột thu–chi 6 tháng gần nhất; **(d)** Biểu đồ đường số dư theo thời gian; **(e)** Top 5 danh mục chi nhiều nhất; **(f)** **Tài sản ròng** = Σ số dư các ví + Σ khoản cho vay còn lại − Σ khoản đi vay còn lại. 3. User bấm một phần biểu đồ → chuyển sang UC-A08 với bộ lọc tương ứng. |
| **AF** | AF-1 Chưa có dữ liệu → hiển thị hướng dẫn thêm giao dịch đầu tiên. |
| **BR** | BR-06 |
| **Dữ liệu** | E-04, E-02, E-07 |
| **Ưu tiên** | High |

#### UC-A10: Xuất dữ liệu Excel / CSV

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | Xuất danh sách giao dịch theo bộ lọc ra file `.xlsx` hoặc `.csv`. |
| **Trigger** | User bấm "Xuất" ở màn hình Giao dịch hoặc Báo cáo. |
| **PRE** | PRE-1: Đã đăng nhập. |
| **POST** | POST-1: File được tải về máy user. |
| **Normal Flow** | 1. User chọn khoảng thời gian + định dạng. 2. Hệ thống tạo file với cột: Ngày, Loại, Danh mục, Ví, Số tiền, Ghi chú, Nguồn nhập. 3. Trình duyệt tải file. |
| **EX** | EX-1: Không có giao dịch trong khoảng đã chọn → báo "Không có dữ liệu". |
| **Dữ liệu** | E-04 |
| **Ưu tiên** | Low |

#### UC-A11: Ghi giao dịch bằng chat

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Actor phụ** | Hệ thống AI |
| **Mô tả** | User gõ câu tự nhiên trong khung chat; AI nhận diện ý định "ghi giao dịch", trích xuất thông tin, hiển thị **thẻ xác nhận** để user lưu. |
| **Trigger** | User gửi tin nhắn mô tả khoản thu/chi (vd "trưa nay ăn phở 50k", "nhận lương 15 triệu"). |
| **PRE** | PRE-1: Đã đăng nhập. PRE-2: Có ít nhất 1 ví. |
| **POST** | POST-1: Giao dịch được lưu với `source = CHAT`. POST-2: Như POST-2, POST-3 của UC-A02. |
| **Normal Flow** | 1. User gửi tin nhắn. 2. **Intent Router** (§9.1) phân loại ý định = `RECORD_TRANSACTION`. 3. AI trích xuất JSON theo schema: `{type, amount, category, wallet, date, note}`. 4. Hệ thống **chuẩn hóa bằng code**: quy đổi số tiền theo BR-08, quy đổi ngày tương đối ("hôm qua", "thứ 2 tuần trước") thành ngày cụ thể, khớp tên danh mục/ví với dữ liệu của user (thiếu ví → dùng ví mặc định). 5. Hệ thống hiển thị **thẻ xác nhận** có thể sửa từng trường. 6. User bấm "Lưu" → hệ thống lưu như UC-A02. 7. Chatbot phản hồi: đã lưu + ngân sách còn lại của danh mục (nếu có). |
| **AF** | AF-1 Thiếu số tiền → AI hỏi lại "Khoản này bao nhiêu tiền?". AF-2 Một câu chứa nhiều giao dịch ("ăn sáng 30k, cafe 45k") → hiển thị nhiều thẻ, có nút "Lưu tất cả". AF-3 User sửa trường trên thẻ trước khi lưu. AF-4 User bấm "Hủy" → không lưu. |
| **EX** | EX-1: Không trích xuất được → "Mình chưa hiểu, bạn có thể ghi lại theo dạng: [nội dung] [số tiền]?". EX-2: JSON trả về sai schema → thử lại 1 lần, vẫn sai → EX-1. |
| **BR** | BR-04, BR-06, BR-08 |
| **Dữ liệu** | E-04, E-11 Message (intent = `RECORD_TRANSACTION`) |
| **Ưu tiên** | High |

#### UC-A12: Ghi giao dịch bằng giọng nói

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Actor phụ** | Hệ thống AI |
| **Mô tả** | User bấm giữ nút micro và nói; hệ thống chuyển **giọng nói → văn bản (Voice-to-Text)**, hiển thị transcript để user kiểm tra, sau đó xử lý như UC-A11. |
| **Trigger** | User bấm nút 🎤 trong khung chat hoặc nút "Ghi nhanh bằng giọng nói" ở trang chủ. |
| **PRE** | PRE-1: Đã đăng nhập. PRE-2: Trình duyệt được cấp quyền micro. PRE-3: Có ít nhất 1 ví. |
| **POST** | POST-1: Giao dịch được lưu với `source = VOICE`. POST-2: Transcript được lưu vào Message với `input_mode = VOICE`; **file âm thanh không lưu** (BR-09). |
| **Normal Flow** | 1. User bấm 🎤 → hệ thống bắt đầu ghi, hiển thị trạng thái "Đang nghe…". 2. User nói (vd "hôm qua đổ xăng tám mươi nghìn"). 3. User bấm dừng (hoặc im lặng ≥ 2 giây). 4. Thành phần **STT** (§9.3) chuyển âm thanh → văn bản. 5. Hệ thống hiển thị transcript trong ô nhập, user có thể sửa. 6. User bấm "Gửi" → tiếp tục từ bước 2 của UC-A11. |
| **AF** | AF-1 User bấm "Hủy" khi đang ghi → không xử lý. AF-2 Số đọc bằng chữ ("tám mươi nghìn") → chuẩn hóa thành 80.000 ở bước 4 của UC-A11. |
| **EX** | EX-1: Không có quyền micro → hướng dẫn cấp quyền. EX-2: Không nhận diện được tiếng nói / quá ồn → "Mình chưa nghe rõ, bạn thử lại nhé". EX-3: Ghi âm quá 60 giây → tự dừng. |
| **BR** | BR-04, BR-08, BR-09 |
| **Dữ liệu** | E-04, E-11 |
| **Ưu tiên** | High |

#### UC-A13: Hỏi đáp dữ liệu tài chính cá nhân

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Actor phụ** | Hệ thống AI |
| **Mô tả** | User hỏi về số liệu của chính mình bằng ngôn ngữ tự nhiên: "Tháng này tôi tiêu bao nhiêu cho ăn uống?", "So với tháng trước thì sao?", "Ví nào còn nhiều tiền nhất?". |
| **Trigger** | User gửi câu hỏi (gõ hoặc giọng nói qua UC-B03). |
| **PRE** | PRE-1: Đã đăng nhập. PRE-2: Nhóm dữ liệu liên quan được phép trong UC-D05. |
| **POST** | POST-1: Câu hỏi và câu trả lời lưu vào hội thoại (intent = `QUERY_PERSONAL_DATA`). |
| **Normal Flow** | 1. Intent Router xác định intent = `QUERY_PERSONAL_DATA`. 2. AI chuyển câu hỏi thành **truy vấn có cấu trúc** (JSON) theo schema cố định: `{metric: sum/count/avg/list/balance, entity: transaction/wallet/budget/debt/goal, filters: {type, category, wallet, date_from, date_to}, group_by, compare_with}`. **Không** cho LLM sinh SQL trực tiếp. 3. Backend kiểm tra JSON hợp lệ → dựng truy vấn với điều kiện bắt buộc `user_id = <user hiện tại>` → chạy. 4. Kết quả số (JSON) được đưa cho LLM diễn đạt thành câu trả lời, có thể kèm bảng nhỏ / biểu đồ mini. |
| **AF** | AF-1 Câu hỏi mơ hồ về thời gian ("gần đây") → mặc định 30 ngày gần nhất và nói rõ giả định. AF-2 Câu hỏi so sánh → chạy 2 truy vấn và trả chênh lệch tuyệt đối + %. |
| **EX** | EX-1: Nhóm dữ liệu bị tắt quyền → "Bạn đang tắt quyền cho AI xem dữ liệu [X]. Bật trong Cài đặt → Quyền dữ liệu AI". EX-2: Không có dữ liệu → trả lời 0 / chưa có giao dịch, không bịa. EX-3: JSON không hợp lệ → hỏi user diễn đạt lại. |
| **BR** | BR-05, BR-06 |
| **Dữ liệu** | E-04, E-02, E-05, E-06, E-07, E-17 |
| **Ưu tiên** | High |

---

#### UC-A14: Quét hóa đơn bằng ảnh

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Actor phụ** | Hệ thống AI (model đọc được ảnh) |
| **Mô tả** | User chụp hoặc chọn ảnh hóa đơn trong khung chat; AI đọc các dòng hàng thành thẻ giao dịch CHI nháp để xác nhận, như UC-A11. Tham khảo tính năng "Scan receipts" của Rolly. |
| **Trigger** | Bấm nút máy ảnh cạnh micro trong ô chat. |
| **PRE** | PRE-1: Đã đăng nhập. PRE-2: Có ít nhất một ví. PRE-3: Ảnh JPG / PNG / WEBP, tối đa 8 MB (trình duyệt thu nhỏ cạnh dài còn 1600px trước khi gửi). |
| **POST** | POST-1: Tin nhắn user lưu nội dung "Ảnh hóa đơn" với `input_mode = IMAGE`; **ảnh không được lưu** (như BR-09 với âm thanh). POST-2: Khi user xác nhận, giao dịch lưu với `source = RECEIPT`. |
| **Normal Flow** | 1. User chọn ảnh. 2. AI đọc ảnh, trả JSON: tên cửa hàng, ngày, tổng thanh toán, giảm giá, hình thức thanh toán, các dòng hàng (số tiền in trên dòng, danh mục, tên món). 3. Hệ thống **chuẩn hóa bằng code** như UC-A11: số tiền theo BR-08, danh mục / ví khớp dữ liệu của user (không khớp → "Khác" / ví mặc định), ngày không ở tương lai. 4. **Đối chiếu tiền (BR-06)**: số tiền đã trả = tổng thanh toán đọc được; nếu thấp hơn tổng các món (có giảm giá) → chia phần giảm theo tỷ lệ vào từng món, tổng thanh toán chẵn nghìn thì làm tròn mỗi món tới nghìn đồng, phần chênh dồn vào món lớn nhất để tổng các thẻ đúng bằng số đã trả. 5. Hiển thị thẻ nháp có thể sửa từng trường (BR-04). 6. User bấm "Lưu tất cả" → lưu như UC-A11 bước 6. |
| **AF** | AF-1 Hóa đơn hơn 8 dòng → gộp các món cùng danh mục thành một thẻ (ghi chú liệt kê 3 món đầu). AF-2 Tổng các món thấp hơn tổng thanh toán → vẫn tạo thẻ và cảnh báo "có thể còn dòng chưa đọc được". |
| **EX** | EX-1: Sai định dạng / quá 8 MB → `IMAGE_INVALID`. EX-2: Ảnh không phải hóa đơn hoặc không đọc được → "Bạn chụp lại thẳng, đủ sáng và rõ các dòng giá tiền". EX-3: Model chính chậm / lỗi → gửi yêu cầu dự phòng tới model khác (§9.1). |
| **BR** | BR-04, BR-05, BR-06, BR-08, BR-09 |
| **Dữ liệu** | E-04, E-11 |
| **Ưu tiên** | Medium |

---

### NHÓM B — TƯ VẤN TÀI CHÍNH

#### UC-B01: Chat tư vấn kiến thức tài chính (RAG)

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Actor phụ** | Hệ thống AI |
| **Mô tả** | User hỏi kiến thức tài chính chung (vd "Quỹ khẩn cấp nên bao nhiêu?", "Vay tiêu dùng lãi suất phẳng khác dư nợ giảm dần thế nào?", "Thẻ tín dụng tính lãi ra sao?"). Hệ thống truy xuất đoạn tài liệu liên quan từ kho tri thức (UC-C01), sinh câu trả lời **có trích dẫn nguồn**. |
| **Trigger** | User gửi câu hỏi kiến thức. |
| **PRE** | PRE-1: Đã đăng nhập. PRE-2: Kho tri thức có ít nhất 1 tài liệu `APPROVED`. |
| **POST** | POST-1: Message lưu kèm `citations` = danh sách {document_id, chunk_id, title, url}. |
| **Normal Flow** | 1. Intent Router xác định intent = `KNOWLEDGE_QA`. 2. (Tùy chọn) Viết lại câu hỏi thành câu độc lập dựa trên lịch sử hội thoại. 3. Embed câu hỏi (bge-m3). 4. Tìm **top-k = 8** chunk trong vector DB, chỉ trong tài liệu `APPROVED`. 5. Rerank → giữ **top-n = 4**. 6. Nếu điểm chunk cao nhất < `score_threshold` (UC-C04) → AF-1. 7. Đưa câu hỏi + 4 chunk vào prompt (system prompt yêu cầu chỉ dùng thông tin trong ngữ cảnh, đánh số trích dẫn [1], [2]…). 8. LLM sinh câu trả lời. 9. Hệ thống hiển thị câu trả lời + danh sách nguồn (tiêu đề, link gốc) + **disclaimer**. |
| **AF** | AF-1 Không đủ nguồn liên quan → "Mình chưa tìm thấy thông tin này trong nguồn tham khảo của hệ thống" + gợi ý diễn đạt lại; ghi log câu hỏi cho UC-C03. AF-2 User bấm vào trích dẫn → mở đoạn tài liệu gốc / link nguồn. |
| **EX** | EX-1: Câu hỏi thuộc chủ đề bị chặn (vd "nên mua mã cổ phiếu nào?") → từ chối lịch sự, giải thích BR-01, gợi ý kiến thức chung liên quan. EX-2: LLM / vector DB lỗi → "Hệ thống đang bận, thử lại sau". |
| **BR** | BR-01, BR-02, BR-03 |
| **Dữ liệu** | E-11, E-12, E-13, E-16 |
| **Ưu tiên** | High |

#### UC-B02: Tư vấn tài chính cá nhân hóa

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Actor phụ** | Hệ thống AI |
| **Mô tả** | User hỏi lời khuyên gắn với tình hình của mình: "Tôi nên tiết kiệm thế nào?", "Chi tiêu của tôi có hợp lý không?", "Nên trả khoản nợ nào trước?". Câu trả lời kết hợp **số liệu cá nhân** (tính bằng code) + **kiến thức từ RAG** (có trích dẫn). |
| **Trigger** | User gửi câu hỏi tư vấn cá nhân. |
| **PRE** | PRE-1: Đã đăng nhập. PRE-2: Có dữ liệu ≥ 1 tháng hoặc đã điền hồ sơ (UC-B04); nếu không → AF-1. |
| **POST** | POST-1: Message lưu kèm citations và danh sách chỉ số đã dùng. |
| **Normal Flow** | 1. Intent Router xác định intent = `PERSONAL_ADVICE`. 2. Hệ thống **tính bằng code** bản tóm tắt tài chính (chỉ các nhóm được phép ở UC-D05): thu nhập TB/tháng, chi TB/tháng, tỷ lệ tiết kiệm = (thu − chi)/thu, top danh mục chi, tỷ lệ Thiết yếu / Mong muốn / Tiết kiệm (so với quy tắc 50/30/20), tổng nợ còn lại, số tháng chi tiêu mà quỹ hiện có chi trả được (= tổng số dư / chi TB tháng), tiến độ mục tiêu, hồ sơ (UC-B04). 3. Chạy truy xuất RAG như UC-B01 với câu hỏi. 4. Prompt gồm: tóm tắt số liệu (dạng JSON) + chunk tài liệu + câu hỏi. 5. LLM sinh lời khuyên: nhận xét dựa trên số liệu → 2–4 hành động cụ thể → trích dẫn → disclaimer. |
| **AF** | AF-1 Chưa đủ dữ liệu → trả lời ở mức kiến thức chung (UC-B01) + gợi ý điền hồ sơ / ghi chép thêm. AF-2 User bấm "Tạo ngân sách theo gợi ý" → chuyển sang UC-A04 với giá trị điền sẵn (user vẫn phải bấm Lưu). |
| **EX** | Như EX của UC-B01 và UC-A13. |
| **BR** | BR-01, BR-02, BR-05, BR-06 |
| **Dữ liệu** | E-04, E-05, E-06, E-07, E-09, E-13, E-17 |
| **Ưu tiên** | High |

#### UC-B03: Đặt câu hỏi bằng giọng nói

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Actor phụ** | Hệ thống AI |
| **Mô tả** | Thành phần **Voice-to-Text dùng chung** cho toàn bộ khung chat: user nói thay vì gõ; transcript được gửi như tin nhắn văn bản và đi qua Intent Router (có thể dẫn tới A11, A13, B01, B02, B07). |
| **Trigger** | User bấm nút 🎤 trong khung chat. |
| **PRE** | PRE-1: Đã đăng nhập. PRE-2: Có quyền micro. |
| **POST** | POST-1: Message lưu với `input_mode = VOICE`. Không lưu audio (BR-09). |
| **Normal Flow** | 1. User bấm 🎤 → nói → dừng. 2. STT chuyển thành văn bản (§9.3). 3. Transcript hiện trong ô nhập, user sửa nếu cần. 4. User bấm "Gửi" (hoặc bật tùy chọn "Tự gửi sau khi nói xong"). 5. Tin nhắn đi qua Intent Router như tin nhắn gõ tay. |
| **AF** | AF-1 Đổi ngôn ngữ nhận dạng Việt / Anh trong cài đặt. |
| **EX** | Như EX-1 → EX-3 của UC-A12. |
| **BR** | BR-09 |
| **Dữ liệu** | E-11 |
| **Ưu tiên** | High |

#### UC-B04: Thiết lập hồ sơ tài chính

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | User điền thông tin nền giúp tư vấn chính xác hơn: thu nhập hàng tháng, số người phụ thuộc, mục tiêu tài chính, mức chấp nhận rủi ro. |
| **Trigger** | Lần đầu dùng chatbot (gợi ý) hoặc User mở "Hồ sơ tài chính". |
| **PRE** | PRE-1: Đã đăng nhập. |
| **POST** | POST-1: Hồ sơ được lưu, dùng trong UC-B02. |
| **Normal Flow** | 1. Hệ thống hiển thị form: thu nhập/tháng, số người phụ thuộc, mục tiêu (chọn nhiều: quỹ khẩn cấp, trả nợ, mua nhà, nghỉ hưu, học tập, khác). 2. Bảng hỏi khẩu vị rủi ro **5 câu** trắc nghiệm, mỗi câu 1–3 điểm. 3. Hệ thống tính tổng điểm: 5–8 = `LOW`, 9–12 = `MEDIUM`, 13–15 = `HIGH`. 4. Hiển thị kết quả + giải thích ý nghĩa. 5. Lưu. |
| **AF** | AF-1 Bỏ qua → có thể điền sau. AF-2 Làm lại bảng hỏi. |
| **BR** | BR-01 (mức rủi ro chỉ dùng để điều chỉnh mức độ thận trọng của lời khuyên chung, không dẫn tới khuyến nghị sản phẩm cụ thể) |
| **Dữ liệu** | E-09 |
| **Ưu tiên** | Medium |

#### UC-B05: Công cụ mô phỏng tài chính

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | Bộ máy tính tài chính, kết quả tính **bằng công thức trong code** (BR-06), có thể mở từ menu hoặc khi chatbot gợi ý. |
| **Trigger** | User mở "Công cụ" hoặc bấm nút công cụ trong câu trả lời chatbot. |
| **PRE** | Không. |
| **POST** | Không lưu (trừ khi user bấm "Tạo mục tiêu từ kết quả" → UC-A05). |
| **Normal Flow** | 1. User chọn công cụ. 2. Nhập tham số. 3. Hệ thống tính và hiển thị kết quả + bảng chi tiết theo kỳ + biểu đồ. |
| **Công thức** | **(1) Lãi kép có góp định kỳ:** FV = P·(1+i)ⁿ + PMT·[((1+i)ⁿ − 1)/i], với i = lãi suất năm / số kỳ trong năm, n = số kỳ. **(2) Trả góp dư nợ giảm dần, trả đều (annuity):** PMT = P·i / (1 − (1+i)⁻ⁿ). **(3) Trả góp gốc đều:** gốc mỗi kỳ = P/n; lãi kỳ k = dư nợ đầu kỳ k × i. **(4) Lãi phẳng:** lãi mỗi kỳ = P × i (tính trên gốc ban đầu); kèm so sánh tổng lãi với (2). **(5) Tiết kiệm đạt mục tiêu:** PMT cần góp = FV·i / ((1+i)ⁿ − 1); nếu i = 0 thì PMT = FV/n. |
| **AF** | AF-1 Bấm "So sánh" để xem 2 phương án cạnh nhau (vd lãi phẳng vs dư nợ giảm dần). |
| **EX** | EX-1: Tham số âm / lãi suất > 100%/năm → báo lỗi. |
| **BR** | BR-06 |
| **Ưu tiên** | Medium |

#### UC-B06: Nhận cảnh báo ngân sách

| Mục | Nội dung |
|---|---|
| **Actor chính** | Hệ thống AI (tự động) → User nhận |
| **Mô tả** | Khi tổng **đã chi thực tế** của một danh mục trong tháng chạm ngưỡng ngân sách, hệ thống gửi cảnh báo trong app + gợi ý ngắn. *(Dự báo chi tiêu và phát hiện bất thường để ở Backlog §11.)* |
| **Trigger** | Mỗi khi giao dịch Chi được thêm / sửa (UC-A02, A11, A12, D04). |
| **PRE** | PRE-1: Danh mục có ngân sách tháng hiện tại. |
| **POST** | POST-1: Tạo `Notification` (`BUDGET_WARN` hoặc `BUDGET_OVER`). |
| **Normal Flow** | 1. Hệ thống tính % = đã chi / hạn mức. 2. Nếu % vượt **ngưỡng cảnh báo** (mặc định 80%) lần đầu trong tháng → `BUDGET_WARN`: "Ăn uống đã dùng 82% ngân sách, còn 810.000đ cho 9 ngày (~90.000đ/ngày)". 3. Nếu % ≥ 100% lần đầu → `BUDGET_OVER`. 4. Thông báo hiện ở chuông thông báo và trong khung chat. |
| **AF** | AF-1 User bấm vào thông báo → mở chi tiết ngân sách (UC-A04). AF-2 User bấm "Hỏi AI cách điều chỉnh" → mở UC-B02 với câu hỏi điền sẵn. |
| **EX** | EX-1: Mỗi mức chỉ cảnh báo **1 lần / danh mục / tháng** để tránh làm phiền. |
| **BR** | BR-06, BR-11 |
| **Dữ liệu** | E-05, E-15 |
| **Ưu tiên** | Medium |

#### UC-B07: Tra cứu tỷ giá, giá vàng, lãi suất

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Actor phụ** | Hệ thống AI, Nguồn dữ liệu mở |
| **Mô tả** | User hỏi "Tỷ giá USD hôm nay?", "Giá vàng SJC?" → hệ thống lấy số từ nguồn mở (SRC-10 → SRC-13), LLM chỉ diễn đạt. |
| **Trigger** | User hỏi trong chat (intent = `MARKET_DATA`) hoặc mở widget "Thị trường". |
| **PRE** | Không. |
| **POST** | POST-1: Dữ liệu được lưu cache vào `MarketRate`. |
| **Normal Flow** | 1. Intent Router xác định `MARKET_DATA` + trích xuất `kind` và `symbol`. 2. Hệ thống đọc cache; nếu `fetched_at` cũ hơn **60 phút** → gọi API nguồn → lưu cache. 3. LLM trả lời kèm **nguồn và thời điểm cập nhật** (BR-12). |
| **AF** | AF-1 Câu hỏi quy đổi ("100 đô bằng bao nhiêu tiền Việt?") → code tính, LLM diễn đạt. |
| **EX** | EX-1: API nguồn lỗi → trả dữ liệu cache gần nhất + ghi rõ thời điểm; không có cache → báo không lấy được dữ liệu. |
| **BR** | BR-06, BR-12 |
| **Dữ liệu** | E-18 |
| **Ưu tiên** | Low |

#### UC-B08: Quản lý lịch sử hội thoại

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | Xem danh sách hội thoại, mở lại, đổi tên, xóa, tạo hội thoại mới. |
| **Trigger** | User mở thanh lịch sử trong màn hình Chat. |
| **PRE** | PRE-1: Đã đăng nhập. |
| **POST** | POST-1: Hội thoại được đổi tên / xóa / tạo mới. |
| **Normal Flow** | 1. Hệ thống hiển thị danh sách hội thoại, mới nhất ở trên; tiêu đề tự sinh từ câu hỏi đầu tiên. 2. User chọn → hiển thị toàn bộ tin nhắn (kèm trích dẫn, thẻ giao dịch đã lưu). 3. User có thể đổi tên / xóa / bấm "Hội thoại mới". |
| **AF** | AF-1 Tìm kiếm hội thoại theo từ khóa. |
| **EX** | EX-1: Xóa hội thoại → hỏi xác nhận; giao dịch đã lưu từ hội thoại đó **không bị xóa**. |
| **Dữ liệu** | E-10, E-11 |
| **Ưu tiên** | Medium |

#### UC-B09: Đánh giá / báo cáo câu trả lời AI

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | User bấm 👍 / 👎 cho mỗi câu trả lời; với 👎 có thể chọn lý do và gửi báo cáo cho Admin. |
| **Trigger** | User bấm 👍 / 👎 dưới câu trả lời. |
| **PRE** | PRE-1: Câu trả lời thuộc hội thoại của user. |
| **POST** | POST-1: Lưu `Feedback`. POST-2: Với 👎 có lý do → tạo mục `OPEN` trong hàng chờ UC-C02. |
| **Normal Flow** | 1. User bấm 👎. 2. Hệ thống hiển thị lý do: Sai thông tin / Không có nguồn / Không liên quan / Số liệu sai / Khác + ô ghi chú. 3. User bấm "Gửi". 4. Hệ thống cảm ơn và xác nhận đã ghi nhận. |
| **AF** | AF-1 Bấm 👍 → chỉ lưu đánh giá, không cần lý do. AF-2 Đổi đánh giá → ghi đè. |
| **BR** | Admin chỉ xem được **tin nhắn bị báo cáo và câu hỏi ngay trước nó**, không xem toàn bộ hội thoại / dữ liệu tài chính của user. |
| **Dữ liệu** | E-14 |
| **Ưu tiên** | Medium |

---

### NHÓM C — QUẢN TRỊ & KHO TRI THỨC (ADMIN)

#### UC-C01: Quản lý kho tri thức RAG

| Mục | Nội dung |
|---|---|
| **Actor chính** | Admin |
| **Actor phụ** | Hệ thống AI |
| **Mô tả** | Admin (kiêm chuyên viên tài chính) thu thập tài liệu từ **nguồn mở** (§4.1), upload, duyệt; hệ thống trích xuất văn bản → chia chunk → embed → index. |
| **Trigger** | Admin mở "Kho tri thức". |
| **PRE** | PRE-1: Đăng nhập với role `ADMIN`. |
| **POST** | POST-1: Tài liệu `APPROVED` có chunk được index và tham gia truy xuất ở UC-B01/B02. |
| **Normal Flow** | 1. Hệ thống hiển thị danh sách tài liệu: tiêu đề, nguồn, ngôn ngữ, trạng thái, số chunk, ngày cập nhật. 2. Admin bấm "Thêm tài liệu" → upload file (PDF / DOCX / TXT / MD) **hoặc** dán URL trang công khai. 3. Admin nhập metadata **bắt buộc**: tiêu đề, `source_id` (SRC-xx), URL gốc, giấy phép, ngôn ngữ. 4. Hệ thống trích xuất văn bản (PyMuPDF / trafilatura), làm sạch (bỏ header/footer lặp, mục lục). 5. Chia chunk theo cấu trúc đề mục, **kích thước ~500 token, chồng lấn ~50 token**, giữ metadata (đề mục, số trang). 6. Hiển thị **xem trước chunk** → Admin rà soát. 7. Admin bấm "Duyệt" → trạng thái `APPROVED` → hệ thống embed (bge-m3) và lưu vào vector DB. |
| **AF** | AF-1 Tắt tài liệu (`DISABLED`) → không còn được truy xuất nhưng không mất dữ liệu. AF-2 Cập nhật phiên bản mới → xóa chunk cũ, index lại. AF-3 Thử truy vấn: Admin nhập câu hỏi → xem top-k chunk + điểm, để kiểm tra chất lượng index. AF-4 Xóa tài liệu → xóa luôn chunk và vector. |
| **EX** | EX-1: File lỗi / PDF dạng ảnh không trích được chữ → báo lỗi, gợi ý dùng bản văn bản. EX-2: Thiếu giấy phép / nguồn → không cho duyệt (BR-03). EX-3: Trùng nội dung với tài liệu đã có (hash) → cảnh báo. |
| **BR** | BR-03 |
| **Dữ liệu** | E-12, E-13 |
| **Ưu tiên** | High |

#### UC-C02: Kiểm duyệt câu trả lời bị báo cáo

| Mục | Nội dung |
|---|---|
| **Actor chính** | Admin |
| **Mô tả** | Xử lý hàng chờ báo cáo từ UC-B09 để tìm lỗi của chatbot và cải thiện kho tri thức / cấu hình. |
| **Trigger** | Admin mở "Báo cáo câu trả lời". |
| **PRE** | PRE-1: Role `ADMIN`. |
| **POST** | POST-1: Báo cáo chuyển trạng thái `RESOLVED` hoặc `REJECTED`, có ghi chú. |
| **Normal Flow** | 1. Hệ thống hiển thị danh sách báo cáo `OPEN`: lý do, thời gian. 2. Admin mở một báo cáo → xem câu hỏi, câu trả lời, **các chunk đã được truy xuất kèm điểm**, cấu hình lúc đó. 3. Admin phân loại nguyên nhân: Thiếu tài liệu / Truy xuất sai / LLM diễn đạt sai / Số liệu sai / Báo cáo không đúng. 4. Admin xử lý: bổ sung tài liệu (UC-C01), chỉnh cấu hình (UC-C04), hoặc ghi chú. 5. Đóng báo cáo. |
| **AF** | AF-1 Lọc theo lý do / trạng thái / thời gian. |
| **Dữ liệu** | E-14, E-11, E-13 |
| **Ưu tiên** | Medium |

#### UC-C03: Xem dashboard chất lượng chatbot

| Mục | Nội dung |
|---|---|
| **Actor chính** | Admin |
| **Mô tả** | Theo dõi chất lượng và mức sử dụng chatbot bằng số liệu **ẩn danh / tổng hợp**. |
| **Trigger** | Admin mở "Dashboard". |
| **PRE** | PRE-1: Role `ADMIN`. |
| **POST** | Không thay đổi dữ liệu. |
| **Normal Flow** | Hệ thống hiển thị theo khoảng thời gian chọn: **(a)** số user hoạt động, số hội thoại, số tin nhắn; **(b)** phân bố intent (A11 / A13 / B01 / B02 / B07); **(c)** tỷ lệ nhập bằng giọng nói vs gõ; **(d)** tỷ lệ 👍/👎; **(e)** **tỷ lệ "không đủ nguồn"** (AF-1 của UC-B01) + danh sách câu hỏi không có nguồn (để bổ sung tài liệu); **(f)** thời gian phản hồi trung bình; **(g)** kết quả lần chạy đánh giá gần nhất. |
| **AF** | AF-1 **Chạy bộ đánh giá:** chạy bộ câu hỏi SRC-22 (và SRC-20/21) qua pipeline → tính **Hit@k** (chunk đúng nằm trong top-k), **tỷ lệ câu trả lời có trích dẫn**, **tỷ lệ đúng** (so với đáp án mẫu) → lưu lịch sử để so sánh giữa các cấu hình. |
| **Dữ liệu** | E-11, E-14 |
| **Ưu tiên** | Medium |

#### UC-C04: Cấu hình AI & guardrail

| Mục | Nội dung |
|---|---|
| **Actor chính** | Admin |
| **Mô tả** | Điều chỉnh tham số mô hình và quy tắc an toàn mà không cần sửa code. |
| **Trigger** | Admin mở "Cấu hình AI". |
| **PRE** | PRE-1: Role `ADMIN`. |
| **POST** | POST-1: Cấu hình mới có hiệu lực cho các tin nhắn tiếp theo; lưu lịch sử thay đổi. |
| **Normal Flow** | 1. Hệ thống hiển thị các tham số: model LLM, temperature, `top_k` (mặc định 8), `top_n` sau rerank (mặc định 4), `score_threshold`, system prompt, câu disclaimer, danh sách **chủ đề bị chặn** (vd khuyến nghị mã cổ phiếu cụ thể, vay nặng lãi, cờ bạc), ngôn ngữ STT. 2. Admin sửa → bấm "Lưu". |
| **AF** | AF-1 Khôi phục cấu hình mặc định. AF-2 Xem lịch sử thay đổi. |
| **EX** | EX-1: Giá trị ngoài phạm vi (temperature ∉ [0, 1], top_k ∉ [1, 20]) → báo lỗi. |
| **BR** | BR-01, BR-02 |
| **Dữ liệu** | E-16 |
| **Ưu tiên** | Medium |

#### UC-C05: Quản lý tài khoản người dùng

| Mục | Nội dung |
|---|---|
| **Actor chính** | Admin |
| **Mô tả** | Xem danh sách tài khoản, khóa / mở khóa, đặt lại mật khẩu. Admin **không** xem được dữ liệu tài chính của user. |
| **Trigger** | Admin mở "Người dùng". |
| **PRE** | PRE-1: Role `ADMIN`. |
| **POST** | POST-1: Trạng thái tài khoản được cập nhật. |
| **Normal Flow** | 1. Hệ thống hiển thị: email, tên, ngày tạo, lần đăng nhập cuối, trạng thái. 2. Admin tìm kiếm / lọc. 3. Admin chọn thao tác: Khóa / Mở khóa / Gửi link đặt lại mật khẩu. |
| **EX** | EX-1: Admin không thể tự khóa tài khoản của chính mình. |
| **Dữ liệu** | E-01 |
| **Ưu tiên** | Medium |

#### UC-C06: Quản lý danh mục mặc định

| Mục | Nội dung |
|---|---|
| **Actor chính** | Admin |
| **Mô tả** | Quản lý bộ danh mục thu/chi mặc định được cấp cho mọi user (vd Chi: Ăn uống, Di chuyển, Nhà ở, Hóa đơn, Mua sắm, Giải trí, Sức khỏe, Giáo dục, Gia đình, Khác; Thu: Lương, Thưởng, Kinh doanh, Được tặng, Khác). |
| **Trigger** | Admin mở "Danh mục mặc định". |
| **PRE** | PRE-1: Role `ADMIN`. |
| **POST** | POST-1: Danh mục mặc định được cập nhật cho mọi user. |
| **Normal Flow** | 1. Hệ thống hiển thị danh mục mặc định theo loại. 2. Admin thêm / sửa tên, biểu tượng / ẩn. 3. Lưu. |
| **EX** | EX-1: Không xóa cứng danh mục đã được dùng (BR-10). |
| **BR** | BR-10 |
| **Dữ liệu** | E-03 (user_id = null) |
| **Ưu tiên** | Low |

---

### NHÓM D — CHUNG / TÀI KHOẢN / TỰ ĐỘNG

#### UC-D01: Đăng ký

| Mục | Nội dung |
|---|---|
| **Actor chính** | User (chưa có tài khoản) |
| **Mô tả** | Tạo tài khoản bằng email + mật khẩu. |
| **Trigger** | Bấm "Đăng ký" ở trang đăng nhập. |
| **PRE** | Không. |
| **POST** | POST-1: Tài khoản `USER` được tạo. POST-2: Hệ thống tạo sẵn **1 ví "Tiền mặt"** (số dư 0) và gán bộ danh mục mặc định. POST-3: Chuyển tới màn hình chào mừng (gợi ý nhập số dư ví, điền hồ sơ UC-B04, cài quyền AI UC-D05). |
| **Normal Flow** | 1. Nhập họ tên, email, mật khẩu, xác nhận mật khẩu. 2. Đồng ý điều khoản + disclaimer tư vấn. 3. Bấm "Đăng ký" → hệ thống kiểm tra → tạo tài khoản. |
| **EX** | EX-1: Email đã tồn tại. EX-2: Mật khẩu < 8 ký tự hoặc không có cả chữ và số. EX-3: Xác nhận mật khẩu không khớp. |
| **Dữ liệu** | E-01, E-02, E-17 |
| **Ưu tiên** | High |

#### UC-D02: Đăng nhập / Đăng xuất

| Mục | Nội dung |
|---|---|
| **Actor chính** | User, Admin |
| **Mô tả** | Đăng nhập bằng email + mật khẩu; hệ thống điều hướng theo role. |
| **Trigger** | Mở ứng dụng khi chưa đăng nhập / bấm "Đăng xuất". |
| **PRE** | PRE-1: Có tài khoản `ACTIVE`. |
| **POST** | POST-1: Phiên đăng nhập được tạo (JWT / session). POST-2: `USER` → trang chủ quản lý tài chính; `ADMIN` → trang quản trị. |
| **Normal Flow** | 1. Nhập email, mật khẩu. 2. Hệ thống xác thực. 3. Điều hướng theo role. |
| **AF** | AF-1 Quên mật khẩu → gửi link đặt lại qua email. AF-2 Đăng xuất → hủy phiên. |
| **EX** | EX-1: Sai thông tin → thông báo chung "Email hoặc mật khẩu không đúng". EX-2: Sai 5 lần liên tiếp → tạm khóa đăng nhập 15 phút. EX-3: Tài khoản bị khóa → thông báo liên hệ quản trị. |
| **Dữ liệu** | E-01 |
| **Ưu tiên** | High |

#### UC-D03: Quản lý hồ sơ cá nhân

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | Sửa tên, đổi mật khẩu, xuất toàn bộ dữ liệu, xóa tài khoản. |
| **Trigger** | Mở "Tài khoản". |
| **PRE** | PRE-1: Đã đăng nhập. |
| **POST** | POST-1: Thông tin cập nhật; hoặc tài khoản và **toàn bộ dữ liệu bị xóa**. |
| **Normal Flow** | 1. Sửa tên → Lưu. 2. Đổi mật khẩu: nhập mật khẩu cũ + mới → Lưu. |
| **AF** | AF-1 Xuất toàn bộ dữ liệu (ví, giao dịch, ngân sách, hội thoại) dạng ZIP chứa CSV/JSON. AF-2 Xóa tài khoản: nhập lại mật khẩu + gõ xác nhận → xóa vĩnh viễn toàn bộ dữ liệu của user. |
| **EX** | EX-1: Mật khẩu cũ sai. |
| **Dữ liệu** | E-01 và mọi thực thể có `user_id` |
| **Ưu tiên** | Medium |

#### UC-D04: Nhắc nhở tự động

| Mục | Nội dung |
|---|---|
| **Actor chính** | Hệ thống AI (tác vụ nền theo lịch) |
| **Mô tả** | Tác vụ chạy **mỗi ngày lúc 07:00** để xử lý giao dịch định kỳ và nhắc khoản nợ đến hạn. |
| **Trigger** | Bộ lập lịch (cron / APScheduler). |
| **PRE** | Có `RecurringRule` hoặc `Debt` thỏa điều kiện. |
| **POST** | POST-1: Giao dịch định kỳ được tạo hoặc thông báo được gửi. POST-2: `next_run_date` được cập nhật. |
| **Normal Flow** | 1. Lấy các `RecurringRule` đang bật có `next_run_date ≤ hôm nay`. 2. Với `auto_create = true` → tạo giao dịch `source = RECURRING` + thông báo "Đã tự ghi [nội dung]"; với `auto_create = false` → thông báo `RECURRING` có nút "Xác nhận ghi" / "Bỏ qua". 3. Tính `next_run_date` mới. 4. Lấy các `Debt` chưa `PAID` có `due_date` trong **3 ngày tới** hoặc **hôm nay** → thông báo `DEBT_DUE`. |
| **EX** | EX-1: Ví của quy tắc đã bị lưu trữ → không tạo giao dịch, gửi thông báo yêu cầu cập nhật quy tắc. |
| **Dữ liệu** | E-07, E-08, E-15, E-04 |
| **Ưu tiên** | Medium |

#### UC-D05: Cài đặt quyền dữ liệu cho AI

| Mục | Nội dung |
|---|---|
| **Actor chính** | User |
| **Mô tả** | User bật/tắt từng nhóm dữ liệu mà AI được dùng khi trả lời (UC-A13, UC-B02): Giao dịch, Ngân sách, Khoản nợ, Mục tiêu tiết kiệm, Hồ sơ tài chính. |
| **Trigger** | Mở "Cài đặt → Quyền dữ liệu AI", hoặc từ thông báo EX-1 của UC-A13. |
| **PRE** | PRE-1: Đã đăng nhập. |
| **POST** | POST-1: Quyền được lưu và áp dụng ngay cho tin nhắn tiếp theo. |
| **Normal Flow** | 1. Hệ thống hiển thị danh sách nhóm dữ liệu với công tắc (mặc định **bật tất cả**) + giải thích AI dùng dữ liệu đó để làm gì. 2. User bật/tắt → Lưu. |
| **AF** | AF-1 Tắt tất cả → chatbot chỉ còn trả lời kiến thức chung (UC-B01), ghi giao dịch (UC-A11, A12) và tra cứu thị trường (UC-B07). |
| **BR** | BR-05 |
| **Dữ liệu** | E-17 |
| **Ưu tiên** | Medium |

---

## 9. Kiến trúc luồng AI

### 9.1 Intent Router — điểm vào duy nhất của khung chat

Mọi tin nhắn (gõ hoặc từ giọng nói) đi qua bộ phân loại ý định trước:

| Intent | Ví dụ | Chuyển tới |
|---|---|---|
| `RECORD_TRANSACTION` | "ăn phở 50k", "nhận lương 15 triệu" | UC-A11 |
| `QUERY_PERSONAL_DATA` | "tháng này tiêu bao nhiêu cho cafe?" | UC-A13 |
| `KNOWLEDGE_QA` | "lãi suất phẳng là gì?" | UC-B01 |
| `PERSONAL_ADVICE` | "tôi nên tiết kiệm thế nào?" | UC-B02 |
| `MARKET_DATA` | "tỷ giá USD hôm nay?" | UC-B07 |
| `SMALL_TALK` | "chào bạn", "cảm ơn" | Trả lời ngắn, gợi ý tính năng |
| `OUT_OF_SCOPE` | câu hỏi không liên quan tài chính, chủ đề bị chặn | Từ chối lịch sự (BR-01) |

Cách làm: LLM phân loại với output JSON `{intent, confidence}`. Nếu `confidence` thấp → hỏi lại user muốn **ghi giao dịch** hay **hỏi thông tin**.

### 9.2 Pipeline RAG (UC-B01, UC-B02)

```
[Indexing — UC-C01, chạy khi Admin duyệt tài liệu]
Tài liệu nguồn mở → Trích xuất văn bản → Làm sạch → Chunk (~500 token, overlap 50)
→ Embedding (bge-m3) → Vector DB (+ metadata: document_id, source_id, url, heading, page)

[Querying — mỗi câu hỏi]
Câu hỏi → (Viết lại theo ngữ cảnh hội thoại) → Embedding → Vector search top_k=8 (chỉ APPROVED)
→ Rerank → top_n=4 → Kiểm tra score_threshold
   ├─ Không đạt → "Không đủ thông tin trong nguồn tham khảo" (BR-02)
   └─ Đạt → Prompt (system + [số liệu cá nhân nếu B02] + chunks + câu hỏi)
            → LLM → Câu trả lời + trích dẫn [1][2] + disclaimer
```

### 9.3 Voice-to-Text (UC-A12, UC-B03)

```
🎤 Ghi âm trên trình duyệt (MediaRecorder / Web Speech API)
→ STT: Phương án 1: Web Speech API (trên trình duyệt, lang = vi-VN)
       Phương án 2: gửi audio lên server → PhoWhisper / Whisper → text; xóa audio ngay (BR-09)
→ Transcript hiện trong ô nhập (user sửa được)
→ Gửi → Intent Router (§9.1)
```

### 9.4 Nguyên tắc "số liệu do code tính" (BR-06)

```
Câu hỏi → LLM chỉ sinh JSON truy vấn / tham số
→ Backend validate + chạy truy vấn / công thức / gọi API
→ Kết quả số (JSON) → LLM chỉ diễn đạt thành câu
```

---

## 10. Yêu cầu phi chức năng (NFR)

| ID | Loại | Yêu cầu |
|---|---|---|
| NFR-01 | Hiệu năng | Thao tác CRUD (ví, giao dịch…) phản hồi < 1 giây. |
| NFR-02 | Hiệu năng | Chatbot bắt đầu hiển thị câu trả lời (streaming) < 5 giây; voice-to-text trả transcript < 3 giây với câu nói ≤ 15 giây. |
| NFR-03 | Bảo mật | Mật khẩu băm bằng bcrypt/argon2; HTTPS; phân quyền theo role ở mọi API. |
| NFR-04 | Bảo mật | Mọi truy vấn dữ liệu tài chính có điều kiện `user_id` ở tầng backend, không tin dữ liệu từ LLM (BR-05). |
| NFR-05 | Riêng tư | Không lưu audio (BR-09); log hệ thống không chứa số tiền / ghi chú giao dịch. |
| NFR-06 | Khả dụng | Giao diện responsive (máy tính và điện thoại), tiếng Việt là ngôn ngữ chính. |
| NFR-07 | Chất lượng AI | Trên bộ SRC-22: Hit@4 ≥ 80%, ≥ 95% câu trả lời kiến thức có trích dẫn, độ chính xác trích xuất giao dịch (A11) ≥ 90% với bộ 50 câu mẫu. |
| NFR-08 | Bảo trì | Tham số AI cấu hình được qua UC-C04, không hard-code. |
| NFR-09 | Pháp lý | Mọi tài liệu trong kho tri thức có nguồn + giấy phép được ghi nhận (BR-03). |

---

## 11. Backlog (chưa làm trong phiên bản này)

| Mục | Ghi chú |
|---|---|
| Dự báo chi tiêu / số dư cuối tháng | Đã có phương án: Run-rate → TB theo thứ trong tuần → ETS (Holt-Winters, chu kỳ 7) → Croston/SBA cho danh mục chi lác đác; đánh giá bằng backtest rolling-origin (MAE, WAPE) so với naive baseline. Thư viện: `statsmodels`, `statsforecast`. Khi làm sẽ mở rộng UC-B06. |
| Phát hiện chi tiêu bất thường | Robust Z-score: z = \|x − median\| / (1,4826 × MAD), bất thường khi z > 3,5. |
| Quét hóa đơn (OCR) | PaddleOCR / VietOCR (mã nguồn mở). |
| Khóa ứng dụng bằng PIN / sinh trắc học | Thiên về NFR bảo mật. |
| Đa tiền tệ | Ví theo ngoại tệ, quy đổi bằng dữ liệu UC-B07. |
| Liên kết ngân hàng | **Ngoài phạm vi** — cần API ngân hàng, không phải nguồn mở. |

---

## 12. Tham khảo ứng dụng tương tự

| Ứng dụng | Điểm tham khảo đã áp dụng |
|---|---|
| Money Lover | Ví, ngân sách, nợ, định kỳ, mục tiêu tiết kiệm, xuất CSV (UC-A01 → A10) |
| MISA MoneyKeeper | Nhập bằng giọng nói (UC-A12); trợ lý AI chỉ phân tích nhóm dữ liệu user cho phép (UC-D05) |
| Cleo | Chat là giao diện chính; cảnh báo khi gần chạm ngân sách (UC-B06) |
| Monarch Money | Hỏi đáp về giao dịch / danh mục; tài sản ròng (UC-A13, UC-A09) |
| **Điểm khác biệt của đề tài** | Tư vấn kiến thức tài chính bằng **RAG trên nguồn mở, có trích dẫn**, kho tri thức do chuyên viên duyệt (UC-B01, UC-C01) |
