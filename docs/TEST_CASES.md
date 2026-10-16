# Bảng test case cần chạy trên máy ảo + Firebase trước khi nộp

| Mã | Vai trò | Thao tác | Mong đợi |
|---|---|---|---|
| TC01 | Bệnh nhân mới | Đăng ký email hợp lệ | Auth user + `users/{uid}` role=patient |
| TC02 | Bệnh nhân | Đăng nhập sai mật khẩu | Hiển thị lỗi; không vào dashboard |
| TC03 | Bác sĩ được cấp | Đăng nhập | Vào dashboard bác sĩ, không có nút đặt lịch |
| TC04 | Bệnh nhân | Chọn BS và đặt giờ tương lai | `appointments` PENDING + `slots` được tạo |
| TC05 | 2 bệnh nhân | Đặt trùng bác sĩ và giờ | Một thành công, một bị từ chối |
| TC06 | Bác sĩ được phân công | Xác nhận lịch | Trạng thái CONFIRMED hiển thị hai thiết bị |
| TC07 | Bệnh nhân sở hữu | Hủy lịch PENDING/CONFIRMED | `CANCELLED`, xóa slot, có thể đặt lại |
| TC08 | Bác sĩ | Nhập chẩn đoán + thuốc hợp lệ | `medical_records` + `treatment_plans` + COMPLETED |
| TC09 | Bác sĩ | Nhập số lần uống = chữ hoặc >12 | Chặn trên ứng dụng và rules |
| TC10 | Bệnh nhân | Xem bệnh án | Chỉ bệnh án có `patientId == uid` |
| TC11 | Bác sĩ khác | Đọc/sửa lịch hoặc bệnh án không được phân công | PERMISSION_DENIED |
| TC12 | Bệnh nhân | Cố thay field role thành doctor bằng client | PERMISSION_DENIED |
| TC13 | Chưa đăng nhập | Truy cập trực tiếp Firestore | PERMISSION_DENIED |
| TC14 | Bệnh nhân | Đổi tên/số điện thoại trong hồ sơ | Cập nhật, không sửa được role |
| TC15 | Bệnh nhân/Bác sĩ | Đăng xuất/đăng nhập lại | Lưu phiên đúng, không lọt dữ liệu tài khoản khác |
| TC16 | Điện thoại Android có camera khoét lỗ | Mở tất cả màn hình | Nội dung không bị status/navigation bar che |

Đánh dấu PASS/FAIL, bổ sung screenshot thực tế và ngày kiểm thử. Đừng ghi PASS trước khi tự chạy.
