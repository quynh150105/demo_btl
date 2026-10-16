# Uni Care — Phiên bản Android Java + Firebase (bản học tập)

> Đây là bản **viết lại luồng nghiệp vụ cốt lõi** dựa trên dự án BTL_Android cũ.
> Không chứa source cũ có khóa API nhúng; KHÔNG tự nhận là phần mềm bệnh viện dùng thật.
> Chưa được build/chạy thử end-to-end trong môi trường có Android SDK/Firebase của bạn.

## 1. Các chức năng đã code

- Android Studio + Java + XML, giao diện Material Components.
- Firebase Authentication (Email/Password) cho bệnh nhân và bác sĩ.
- Bệnh nhân **tự đăng ký**, được cấp role `patient` duy nhất.
- Bác sĩ đăng nhập bằng tài khoản quản trị viên tạo trong Firebase Console; **không thể tự nhận vai trò bác sĩ từ app**.
- Hồ sơ cá nhân: tên, số điện thoại, ngày sinh, giới tính, địa chỉ, tiền sử bệnh giả lập.
- Danh sách bác sĩ lấy từ Firestore (không cố định BS. Nguyễn Thị A).
- Đặt lịch khám theo bác sĩ, ngày, giờ, lý do; lưu `patientId` là Firebase UID thật.
- Đặt lịch dùng **Firestore transaction + slots** ngăn 2 client app tranh cùng khung giờ.
- Bệnh nhân theo dõi, hủy lịch; bác sĩ xem lịch được phân công, xác nhận/từ chối.
- Khi hủy, batch xóa `slots` và cập nhật lịch sang `CANCELLED`, có thể đặt lại khung giờ.
- Bác sĩ lập/sửa hồ sơ bệnh án + phác đồ; `appointmentId` luôn gắn đúng với lịch khám.
- Bệnh nhân chỉ xem bệnh án/phác đồ của chính mình. Bác sĩ chỉ sửa hồ sơ được phân công.
- Mục thông báo: **trạng thái lịch khám theo thời gian thực trong app**, không phải thông báo đẩy khi app đã đóng.
- Bảo mật `firestore.rules` theo tài khoản/vai trò.
- Xử lý `WindowInsets` để nội dung không bị camera/nút điều hướng che ở máy Android mới.

## 2. Phải cài Firebase trên tài khoản của bạn (bắt buộc)

1. Mở https://console.firebase.google.com/ → **Add project** → đặt tên `UniCare-demo`.
2. Trong Project Overview → **Add app → Android** → nhập **Package name**:
   `com.example.btl_android` (chính xác từng ký tự).
3. Tải file **google-services.json** → copy vào thư mục `UniCare_Firebase/app/google-services.json`.
   Không đổi tên file, không đặt trong thư mục `src`.
4. Trong **Build → Authentication → Sign-in method**, bật **Email/Password**.
5. Trong **Build → Firestore Database**, tạo database (chọn vùng phù hợp, dùng dữ liệu giả lập).
6. Mở **Firestore Database → Rules**, copy toàn bộ `firestore.rules` và bấm **Publish**.
   KHÔNG chọn `allow read, write: if true` để chạy cho nhanh.
7. Trong Android Studio → File → Open → chọn **thư mục `UniCare_Firebase`** → Sync Project with Gradle Files.
8. Chạy trên máy ảo Android có Internet, tạo tài khoản **bệnh nhân** ngay trong app.

### Gradle Wrapper

Đã kèm **gradle/wrapper/gradle-wrapper.jar**, `gradlew`, `gradlew.bat`
và cấu hình Gradle 8.13 từ bản mã nguồn gốc của bạn. Android Studio có thể
sử dụng Gradle Wrapper để tải Gradle lần đầu khi máy có kết nối Internet.
Nếu Gradle Sync thất bại, xem **Build Output** và cấu hình Android SDK/JDK trước.

## 3. Tạo tài khoản bác sĩ (chỉ trong Firebase Console)

Ứng dụng **không cho đăng ký bác sĩ từ điện thoại** để tránh bệnh nhân tự giả làm bác sĩ.

1. Firebase Console → **Authentication → Users → Add user**.
   Ví dụ: email `doctor.demo@example.com`, đặt mật khẩu mạnh (chỉ dùng bản demo).
2. Sao chép **User UID** của tài khoản bác sĩ vừa tạo; ví dụ `<DOCTOR_UID>`.
3. Firestore → **users** → Add document, document ID = `<DOCTOR_UID>`, thêm fields:

   | Key | Type | Value ví dụ |
   |---|---|---|
   | `role` | string | `doctor` |
   | `name` | string | `BS. Nguyễn Thị An` |
   | `email` | string | `doctor.demo@example.com` |
   | `phone` | string | `0900000000` |

4. Firestore → **doctors** → Add document, document ID phải là đúng `<DOCTOR_UID>`, thêm fields:

   | Key | Type | Value ví dụ |
   |---|---|---|
   | `name` | string | `BS. Nguyễn Thị An` |
   | `specialty` | string | `Nội tổng quát` |
   | `phone` | string | `0900000000` |

5. Dùng email/mật khẩu bác sĩ đó đăng nhập ứng dụng trên **máy ảo khác**.

**Không tạo tài khoản bác sĩ bằng cách đổi field `role` trong app**. Security Rules chặn việc đó.

## 4. DEMO đúng quy trình

1. Máy A đăng ký tài khoản bệnh nhân, mở "Đặt lịch khám", chọn bác sĩ, ngày mai và 08:30.
2. Máy B đăng nhập bác sĩ, mở "Quản lý lịch khám", nhận lịch từ máy A.
3. Máy B bấm "Xác nhận", máy A sẽ thấy trạng thái mới sau khi đồng bộ.
4. Máy B bấm "Nhập bệnh án", điền chẩn đoán, đơn thuốc và phác đồ rồi lưu.
5. Máy A mở "Hồ sơ bệnh án / Phác đồ" để xem thông tin bác sĩ vừa ghi.
6. Thử hai bệnh nhân đặt **cùng một bác sĩ, cùng giờ**: một người sẽ nhận báo trùng lịch.
7. Thử đăng nhập bằng tài khoản bệnh nhân rồi tìm cách sửa bệnh án: Firestore Rules phải từ chối.

## 5. Sơ đồ Firestore

```
users/{firebaseUid}         role: patient | doctor, name, phone, email, ...
doctors/{firebaseUid}       name, specialty, phone  (quản trị viên cấp)
appointments/{autoId}      patientId, doctorId, startAt, reason, slotId, status, ...
slots/{doctorUid_yyyyMMdd_HHmm}   appointmentId, doctorId, createdAt
medical_records/{appointmentId} patientId, doctorId, diagnosis, prescription, notes, ...
treatment_plans/{appointmentId} patientId, doctorId, medicine, timesPerDay, guide, ...
```

`status`: `PENDING` → `CONFIRMED` → `COMPLETED`; hoặc `PENDING/CONFIRMED` → `CANCELLED`.

## 6. Cải tiến so với mã nguồn SQLite của nhóm trước

| Điểm cũ | Cách xử lý trong bản này |
|---|---|
| `getPatientProfile(1)` | Bệnh nhân = `FirebaseAuth.getCurrentUser().getUid()` |
| Bác sĩ được hardcode | Chọn từ Firestore `doctors` |
| Bệnh án không luôn gắn đúng lịch | `medical_records/{appointmentId}` liên kết cố định |
| Trạng thái lịch chỉ có ở một thiết bị | Firestore snapshot listeners đồng bộ hai máy |
| Người dùng tự chọn vào giao diện bác sĩ | Firebase Auth + role trên Firestore Rules |
| Xóa bảng khi nâng cấp SQLite | Không dùng SQLite, không xóa bảng người dùng |
| `GroqAIService` chứa khóa API trong Java | Không tích hợp AI trong bản mới; không có khóa API nhúng |

## 7. Chưa làm / giới hạn của bản demo

- **Chưa tự động chuyển dữ liệu SQLite cũ sang Firestore**. Cần ánh xạ ID số thành Firebase UID và xác minh từng bản ghi. Đừng đưa bệnh án thật lên Firebase demo.
- **Chưa gửi FCM Push Notifications** khi ứng dụng tắt; mục Thông báo là danh sách trạng thái lịch khám trong ứng dụng.
- **Chưa có lịch nhắc uống thuốc tự động**, ảnh đại diện trên Cloud Storage, bản tin, thanh toán hoặc AI.
- Chưa có lịch khám theo ca/giới hạn sức chứa bác sĩ; mỗi bác sĩ có tối đa 1 bệnh nhân cho mỗi mốc giờ được chọn.
- Trường `doctorName` hiển thị theo lúc bệnh nhân đặt lịch; nếu bác sĩ đổi tên sau đó, lịch cũ giữ tên lịch sử.
- Chưa có Cloud Functions để kiểm tra logic độc lập với client và chống người dùng sửa mã ứng dụng.
- Quy tắc và tính năng mới được **kiểm tra tĩnh** trong môi trường này, CHƯA build/chạy end-to-end hoặc chạy test Security Rules trên Firebase Emulator; cần chạy bộ kiểm thử trong `docs/TEST_CASES.md` trước khi nộp bài.
- Firebase project chưa được kết nối vì mình không có quyền tạo tài khoản hay tải `google-services.json` thay bạn.

## 8. Cấu trúc code

```
app/src/main/java/com/example/btl_android/
├── App.java                 Tiện ích Firebase, insets, session
├── AuthActivity.java        Đăng ký & đăng nhập
├── DashboardActivity.java   Điều hướng theo vai trò
├── BookingActivity.java     Đặt lịch, transaction chống trùng
├── AppointmentsActivity.java  Xem/duyệt/hủy/nhập hồ sơ
├── ProfileActivity.java     Xem, cập nhật thông tin
├── RecordEditorActivity.java  Bác sĩ ghi bệnh án & phác đồ
└── RecordsActivity.java     Tra cứu lịch sử bệnh án
```

Liên hệ nguồn tham khảo và nêu rõ nhóm tự xây dựng lại phần Firebase, phân quyền, dữ liệu khi báo cáo.
"# demo_btl" 
