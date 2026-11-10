# Tài liệu nhóm C — Kiến thức tài chính nền (tiếng Anh, nạp vào Kho tri thức RAG)

Tải ngày 30/10/2026. Nhóm này để chatbot trả lời các câu **kiến thức chung** mà nhóm A (luật) và B (bài giải thích chính sách) không có: lập ngân sách, quỹ khẩn cấp, trả nợ lãi cao, lãi kép, rủi ro, đa dạng hóa, nhận diện lừa đảo đầu tư.

Nội dung theo **bối cảnh Mỹ**. Đã bỏ các trang chỉ áp dụng ở Mỹ (tài khoản hưu trí, quy định SEC, điểm tín dụng). Khi trả lời, nếu đoạn trích nhắc tới luật hay sản phẩm của Mỹ, chatbot chỉ nên dùng **nguyên tắc chung**, không dùng con số hay thủ tục của Mỹ.

| File | Nội dung | Nguồn | Ghi chú |
|---|---|---|---|
| `C2a_InvestorGov_Save-and-Invest.pdf` | 12 trang Investor.gov gộp lại: tính toán tài chính, đặt mục tiêu, quỹ khẩn cấp, trả nợ thẻ tín dụng, lãi kép, rủi ro, khẩu vị rủi ro, đa dạng hóa, các loại tài sản | [Investor.gov — Save and Invest](https://www.investor.gov/introduction-investing/investing-basics/save-and-invest) | Mỗi mục là một `h2` kèm URL gốc. 8 trang PDF |
| `C2b_InvestorGov_Avoid-Investment-Fraud.pdf` | 12 trang: cách tránh lừa đảo, dấu hiệu cảnh báo, bảo vệ tài khoản mạng xã hội, Ponzi, pump-and-dump, lừa đảo phí ứng trước, mạo danh, lợi nhuận cao bất thường, quyền chọn nhị phân | [Investor.gov — How to Avoid Fraud](https://www.investor.gov/protect-your-investments/fraud/how-avoid-fraud) | Bù cho phần cảnh báo lừa đảo của NHNN chưa tải được. 16 trang PDF |
| `C2c_InvestorGov_Beginners-Guide-Asset-Allocation.pdf` | Hướng dẫn phân bổ tài sản, đa dạng hóa, tái cân bằng | [PDF gốc của Investor.gov](https://www.investor.gov/sites/investorgov/files/2019-02/Beginners-Guide-to-Asset-Allocation.pdf) | Bản PDF nguyên gốc, không chỉnh sửa. 6 trang |
| `C1_CFPB_Your-Money-Your-Goals_chon-loc.pdf` | CFPB "Your Money, Your Goals" (bản 6/2020): đặt mục tiêu SMART, tiết kiệm, theo dõi chi tiêu, ưu tiên hóa đơn, ngân sách dòng tiền, quản lý nợ, dấu hiệu lừa đảo | [Trang toolkit CFPB](https://www.consumerfinance.gov/consumer-tools/educator-tools/your-money-your-goals/toolkit/) | Cắt 91/244 trang từ [PDF gốc](https://files.consumerfinance.gov/f/documents/cfpb_your-money-your-goals_financial-empowerment_toolkit.pdf), giữ nguyên nội dung. Xem bên dưới |

## C1 — CFPB: phần giữ và phần bỏ

Số trang tính theo PDF gốc 244 trang. Module đánh số theo đúng tài liệu.

**Giữ:**
- Trang 1 (bìa) và 243 (tuyên bố miễn trừ của CFPB)
- Module 1 Setting Goals: 25–38
- Module 2 Saving: 39–47, 51–53
- Module 4 Paying Bills: 78–97
- Module 5 Getting through the Month: 98–110
- Module 6 Dealing with Debt: 111–115, 118–133
- Module 9 Protecting your Money: 230–232, 234–237, 240–241

**Bỏ:**
- Phần giới thiệu: 2–24
- Module 3 (phúc lợi xã hội Mỹ), 7 (báo cáo và điểm tín dụng), 8 (tài khoản ngân hàng Mỹ)
- Giới hạn tài sản của các chương trình trợ cấp: 48–50
- Tiết kiệm mùa hoàn thuế: 54–55
- Vay sinh viên: 116–117, 137–143
- So sánh vay mua ô tô: 134–136
- Chủ nợ và thư mẫu: 144–150; nợ y tế: 151–152
- Cơ quan bảo vệ người tiêu dùng Mỹ: 233; xử lý trộm danh tính: 238–239; khiếu nại lên CFPB: 242

**Còn sót:** phần tổng quan của Module 6 vẫn nhắc tới vay sinh viên và vay payday của Mỹ, và file có cả các trang biểu mẫu để điền. Đoạn từ các trang này có thể lọt vào kết quả tìm kiếm; nếu gây nhiễu, Tắt hoặc cắt thêm. Tài liệu từ năm 2020, nên con số và quy định của Mỹ trong đó không còn đảm bảo; chỉ dùng phần nguyên tắc.

## Giấy phép

- **Investor.gov (SEC):** tài liệu do cơ quan liên bang Mỹ làm ra nên nhìn chung không có bản quyền (17 U.S.C. §105). Vẫn ghi nguồn "U.S. SEC — Investor.gov" khi trích dẫn.
- **CFPB:** cũng là cơ quan liên bang Mỹ, áp dụng như trên. Tuyên bố của CFPB (trang cuối file) nói tài liệu chỉ để giáo dục, không thay thế tư vấn chuyên môn. Trong file không có câu nào nói rõ về bản quyền, nên ghi nguồn đầy đủ khi dùng.

## Khi nạp vào hệ thống

- **Tiêu đề nên ghi rõ bối cảnh**, ví dụ `Investor.gov (SEC, Mỹ) — Save and Invest`, để người đọc trích dẫn biết đây là nguồn quốc tế.
- **Ngôn ngữ:** tài liệu tiếng Anh, câu hỏi tiếng Việt. `text-embedding-3-large` hiểu đa ngôn ngữ, nhưng điểm tương đồng sẽ thấp hơn. Nạp xong, hãy chạy bộ ~15 câu kiến thức chung, kiểm tra xem có đoạn nào vượt ngưỡng 0.45 không. Nếu trượt nhiều, hạ ngưỡng (ví dụ 0.40) hoặc dịch sang tiếng Việt các đoạn hay dùng.
- Ưu tiên nhóm A khi nội dung trùng: luật và con số Việt Nam luôn thắng nguồn nước ngoài.
