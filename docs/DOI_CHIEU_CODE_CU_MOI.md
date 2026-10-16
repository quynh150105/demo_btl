# Đối chiếu nguồn Uni Care SQLite và bản Firebase

## Mã nguồn gốc (RAR người dùng cung cấp)

- `Database/DatabaseHelper.java`: 6 bảng SQLite và nhiều thao tác đồng bộ trên một máy.
- `LoginActivity.java`: chuyển trực tiếp sang `MainActivity1` hoặc `HomeActivity` theo nút, không xác thực tài khoản.
- `CreateAppointmentActivity.java`: dùng `getPatientProfile(1)` và bác sĩ hardcode.
- `MedicalRecordRepository.java`: thiếu liên kết `appointment_id` trong một số đường tạo bệnh án.
- `GroqAIService.java`: gọi AI có thông tin cấu hình nhúng. Không chép nguồn cũ vào ZIP mới để tránh phát tán khóa API.

## Phiên bản viết lại

| Mã cũ | Mã mới | Ghi chú |
|---|---|---|
| `DatabaseHelper` | Firestore (`App.db()` + gọi trực tiếp ở Activity) | Không còn bảng SQLite; collection cloud |
| `LoginActivity` | `AuthActivity` | Firebase Auth; role patient/doctor lấy từ `users/{uid}` |
| `MainActivity1`, `HomeActivity` | `DashboardActivity` | Điều hướng theo vai trò; server rules chặn hành vi trái quyền |
| `CreateAppointmentActivity` | `BookingActivity` | Chọn bác sĩ từ `doctors`, khung giờ transaction |
| `LichKhamActivity` | `AppointmentsActivity` | Snapshot real-time, nhận/hủy lịch theo quyền |
| `MedicalRecordRepository`, UI bệnh án | `RecordEditorActivity`, `RecordsActivity` | Thêm/sửa/xem theo appointment ID, Firestore batch |
| UI thông tin cá nhân | `ProfileActivity` | Chỉ chỉnh thông tin bản thân; không tự sửa role |
| `GroqAIService` | Không đưa sang bản mới | Cần backend riêng và bảo mật khi muốn tích hợp lại |
| nhắc uống thuốc | Chưa chuyển | Cần thiết kế lịch thuốc và kiểm thử chính xác thời gian |

### Vì sao không giữ nguyên toàn bộ màn hình cũ?

Nhóm trước có nhiều Activity đang gọi các phương thức SQLite đồng bộ trả về `long`, `boolean`, `Cursor` hoặc `List` ngay lập tức. Firestore làm việc **bất đồng bộ**. Giữ nguyên màn hình nhưng thay vài dòng SQLite bằng Firestore sẽ rất dễ sinh lỗi giao diện chưa tải xong dữ liệu, ID bị lệch, hoặc lỗi quyền. Bản mới viết lại các luồng chính với callback và snapshot listeners để dễ học, sửa và demo.

Các giao diện như tin tức, AI, nhắc uống thuốc, màn hình chuyên khoa chi tiết sẽ cần phát triển riêng nếu muốn giống hệt bản cũ; không nằm trong phạm vi project ZIP này. Bản này được thiết kế để demo nghiệp vụ chính theo góp ý giảng viên, **không phải bản sao từng pixel của giao diện trước**.
