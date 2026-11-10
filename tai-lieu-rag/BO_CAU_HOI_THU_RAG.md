# Bộ 30 câu hỏi thử RAG

Dùng để thử pipeline RAG với tài liệu nhóm A, B, C. Đáp án kỳ vọng đã đối chiếu với nội dung PDF (mã file và số trang ở cột Nguồn), trừ các câu kiến thức chung không ghi số trang: các câu đó ghi theo tóm tắt, chưa đối chiếu từng chữ.

**Tài liệu đang tạm dừng, chưa có trong kho:** A1c (NĐ 253), A2 (Luật BHXH), A7 (Luật TCTD). Bộ câu hỏi này không phụ thuộc vào ba file đó.

**Cách chấm mỗi câu:** (1) đoạn đúng có nằm trong top 4 không, (2) câu trả lời có đúng đáp án không, (3) trích dẫn có trỏ đúng tài liệu không. Với nhóm Ngoài phạm vi: đạt nếu không trả lời bừa và không gắn trích dẫn.


## Thuế TNCN

| # | Câu hỏi | Đáp án kỳ vọng | Nguồn |
|---|---|---|---|
| 1 | Mức giảm trừ gia cảnh cho bản thân năm 2026 là bao nhiêu? | 15,5 triệu đồng/tháng | A1b, B4b |
| 2 | Tôi nuôi 2 con thì được giảm trừ thêm bao nhiêu mỗi tháng? | 6,2 triệu đồng/người, tức 12,4 triệu đồng (cần LLM tự nhân) | A1b, B4b |
| 3 | Bố mẹ tôi có thu nhập bao nhiêu thì vẫn được tính là người phụ thuộc? | Thu nhập bình quân tháng không quá 3 triệu đồng | A1d tr.2 |
| 4 | Biểu thuế TNCN mới có bao nhiêu bậc? | 5 bậc, giảm từ 7 bậc | B4a tr.8 |

## Bảo hiểm tiền gửi

| # | Câu hỏi | Đáp án kỳ vọng | Nguồn |
|---|---|---|---|
| 5 | Ngân hàng phá sản thì tiền gửi của tôi được trả tối đa bao nhiêu? | 350 triệu đồng tại một tổ chức tham gia BHTG | A4b tr.1, B2a |
| 6 | Nghe nói bảo hiểm tiền gửi chỉ trả 125 triệu, đúng không? | Không. Mức 125 triệu đã được thay, từ 13/7/2026 là 350 triệu (câu bẫy số cũ) | A4b, B2a |
| 7 | Khi ngân hàng đổ vỡ, tôi có phải tự làm hồ sơ xin nhận tiền bảo hiểm không? | Không phải lập hồ sơ đề nghị; tổ chức tham gia BHTG gửi hồ sơ trong 5 ngày làm việc | B2b tr.1 |

## BHXH, BHYT

| # | Câu hỏi | Đáp án kỳ vọng | Nguồn |
|---|---|---|---|
| 8 | Đóng BHXH tối thiểu bao nhiêu năm thì có lương hưu? | 15 năm | B3 tr.2 |
| 9 | Trường hợp nào được rút BHXH một lần? | Theo danh sách trường hợp ở mục BHXH một lần, ví dụ đủ tuổi hưởng lương hưu mà chưa đủ năm đóng | B3 tr.3 |
| 10 | Khi nào BHYT thanh toán 100% chi phí khám chữa bệnh? | Theo đối tượng; chi phí một lần khám thấp hơn mức Chính phủ quy định; khám ở cấp ban đầu | A3 tr.23, B5 tr.1 |

## Thẻ ngân hàng, người tiêu dùng

| # | Câu hỏi | Đáp án kỳ vọng | Nguồn |
|---|---|---|---|
| 11 | Thẻ tín dụng được rút tiền mặt tối đa bao nhiêu? | Tổng hạn mức rút tiền mặt theo BIN tối đa 100 triệu đồng | A5b tr.4 |
| 12 | Ngân hàng đổi biểu phí thẻ thì phải báo trước bao lâu? | Tối thiểu 07 ngày | A5a tr.6 |
| 13 | Mua hàng online có được tính là giao dịch từ xa không? | Có: giao dịch trên không gian mạng, phương tiện điện tử | A6 tr.2 |

## Kiến thức chung (hỏi tiếng Việt, tài liệu tiếng Anh)

| # | Câu hỏi | Đáp án kỳ vọng | Nguồn |
|---|---|---|---|
| 14 | Quỹ khẩn cấp nên có bao nhiêu tiền? | Đủ cho tình huống như mất việc; một số người để tới 6 tháng thu nhập | C2a tr.2 |
| 15 | Lãi kép là gì? | Lãi tính trên cả tiền gốc và tiền lãi đã có | C2a |
| 16 | Có nên trả hết nợ thẻ tín dụng trước khi đầu tư không? | Có: gần như không khoản đầu tư nào lãi hơn việc xóa nợ lãi cao | C2a tr.3 |
| 17 | Đa dạng hóa đầu tư nghĩa là gì? | Không bỏ tất cả trứng vào một giỏ | C2a, C2c |
| 18 | Khẩu vị rủi ro là gì và ảnh hưởng thế nào tới việc đầu tư? | Mức chấp nhận rủi ro, đổi lấy lợi nhuận tiềm năng | C2a |
| 19 | Mục tiêu SMART là gì? | Cụ thể, đo được, khả thi, phù hợp, có thời hạn | C1 |
| 20 | Khác nhau giữa nhu cầu và mong muốn khi chi tiêu là gì? | Nhu cầu là thứ phải có để sống: nhà ở, điện nước, ăn, thuốc, quần áo, đi lại | C1 tr.29 |
| 21 | Làm sao lập ngân sách theo dòng tiền từng tuần trong tháng? | Dự kiến tiền vào, tiền ra theo từng tuần để thấy lúc thiếu hụt | C1 |
| 22 | Tỷ lệ nợ trên thu nhập là gì? | Tỷ lệ giữa khoản trả nợ hằng tháng và thu nhập | C1 |
| 23 | Vay nóng ngắn hạn kiểu payday loan có rủi ro gì? | Khoản vay ngắn hạn, chi phí cao, dễ phải vay lại nhiều lần | C1 tr.66 |
| 24 | Mô hình Ponzi hoạt động thế nào? | Lấy tiền của người vào sau trả cho người vào trước | C2b |
| 25 | Dấu hiệu nào cho thấy một lời mời đầu tư là lừa đảo? | Hứa lợi nhuận cao được đảm bảo, "quá tốt để là thật" | C2b tr.3 |
| 26 | Tái cân bằng danh mục đầu tư là gì? | Đưa danh mục về tỷ lệ phân bổ ban đầu; cân nhắc phí và thuế | C2c tr.5 |

## Ngoài phạm vi

| # | Câu hỏi | Đáp án kỳ vọng | Nguồn |
|---|---|---|---|
| 27 | Công thức nấu phở bò ngon là gì? | Từ chối hoặc báo không có nguồn | — |
| 28 | Thủ đô của nước Pháp là thành phố nào? | Không trả lời từ kho tri thức, không có trích dẫn | — |
| 29 | Sửa lỗi NullPointerException trong Java thế nào? | Từ chối hoặc báo không có nguồn | — |
| 30 | Tháng này nên mua cổ phiếu nào để lời nhanh nhất? | Không khuyến nghị mã cổ phiếu cụ thể | — |
