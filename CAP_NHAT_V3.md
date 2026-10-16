# UniCare Firebase – cập nhật thử nghiệm giao diện và chức năng

## Đã chỉnh sửa
- Thu nhỏ header trang chủ 240dp thành 188dp, giữ trạng thái đăng nhập và dữ liệu Firebase.
- Liên kết nhấn toàn bộ hàng với các nút chức năng bác sĩ: Quản lý lịch, thống kê, lịch làm việc, bệnh án.
- Bổ sung 2 công cụ bác sĩ trên trang chủ: **Nhập chỉ số sức khỏe cho bệnh nhân**, **Kê lịch uống thuốc cho bệnh nhân**.
- Chọn bệnh nhân từ **appointments** đã CONFIRMED hoặc COMPLETED của chính bác sĩ, chuyển patientId và appointmentId đến màn hình nhập.
- Chỉ số sức khỏe và thuốc lưu theo UID bệnh nhân; bệnh nhân xem từ tài khoản của mình, bác sĩ chỉ xem theo lịch đã chọn. Các bản ghi mới có `appointmentId`, `createdBy`.
- Bổ sung điều kiện Firestore Rules để bác sĩ chỉ ghi cho bệnh nhân đã có lịch với chính bác sĩ đó và trạng thái CONFIRMED/COMPLETED.
- Hiển thị thông báo khi Firestore trả lỗi ở trang lịch làm việc và thống kê.

## BẮT BUỘC để đồng bộ Firebase
1. Sao lưu project Android Studio hiện tại. Giải nén project mới ra một thư mục khác, không chép đè ngay.
2. Copy **google-services.json cũ của bạn** vào `app/google-services.json` (bản zip không chứa file này).
3. Firebase Console → Firestore Database → Rules → thay nội dung bằng file `firestore.rules` mới → Publish. **Đọc và rà soát quyền trước khi Publish**. Bản này dành cho dữ liệu demo, chưa qua đánh giá bảo mật cho bệnh án thật.
4. Android Studio → File → Open → chọn đúng thư mục mới → Gradle Sync → Run.
5. Demo: bác sĩ mở trang chủ → chọn công cụ chỉ số/thuốc → chọn bệnh nhân có lịch CONFIRMED/COMPLETED → thêm dữ liệu. Đăng nhập bệnh nhân trên máy khác để xem.

## Hạn chế còn lại
- **Chưa chạy build hoặc kiểm thử máy ảo thực tế** trong môi trường tạo bản cập nhật. Các thao tác Gradle, Firestore và UI cần kiểm tra tại Android Studio của bạn.
- Màn hình đánh giá: bệnh nhân chỉ được đánh giá lịch đã hoàn thành; mục đánh giá bác sĩ phụ thuộc vào các tài liệu `ratings` thực tế. Cần ảnh lỗi/Logcat cụ thể nếu vẫn không chạy.
- `DoctorScheduleActivity` quản lý khung giờ tuần, nhưng cơ chế đặt lịch trong `BookingActivity` chưa được đồng bộ đầy đủ với khung giờ bác sĩ đã cấu hình; đây là mục cần phát triển tiếp trước khi xem là hệ thống hoàn chỉnh.
- Thuốc ở `treatment_plans` và thuốc mới ở `medications` đang là hai nguồn. Bản cập nhật hiển thị cả hai, nhưng **chưa tự gửi thông báo nhắc thuốc khi app đóng**.
- Bệnh án và phác đồ chỉ dùng dữ liệu giả lập. Không đưa dữ liệu sức khỏe thật lên khi chưa được đánh giá bảo mật và phân quyền.
