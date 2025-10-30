# **Dự án X-Mate - CỬA HÀNG KINH DOANH QUẦN ÁO**

## **Giới thiệu**
Mô hình kinh doanh quần áo truyền thống hiện nay thường gặp phải các hạn chế lớn như: quản lý sản phẩm, tồn kho và đơn hàng dựa trên sổ sách hoặc các nền tảng mạng xã hội không chuyên biệt. Điều này dẫn đến tình trạng thiếu hệ thống, dễ gây ra sai sót, khó khăn trong việc tra cứu, tính toán và quản lý doanh thu. Đề tài được lựa chọn nhằm giải quyết triệt để những vấn đề này, cung cấp một giải pháp quản lý toàn diện, hiệu quả và chuyên nghiệp. 
## **Mục lục**

1.  [Tính năng chính](#tinh-nang-chinh)
2.  [Công nghệ sử dụng](#cong-nghe-su-dung)
3.  [Kiến trúc hệ thống](#kien-truc-he-thong)
4.  [Yêu cầu môi trường](#yeu-cau-moi-truong)
5.  [Hướng dẫn cài đặt và khởi chạy](#huong-dan-cai-dat-va-khoi-chay)
6.  [Thông tin tài khoản mặc định](#thong-tin-tai-khoan-mac-dinh)
7.  [Cấu trúc dự án](#cau-truc-du-an)
8.  [Tác giả](#tac-gia)

## **Tính năng chính**

Hệ thống hỗ trợ 4 vai trò người dùng khác nhau, mỗi vai trò có một bộ chức năng riêng biệt:

**1. Khách hàng vãng lai (Guest):**
*   Xem và duyệt sản phẩm.
*   Tìm kiếm sản phẩm theo tên.
*   Hỏi thông tin sản phẩm thông qua Chatbot.
*   Lọc sản phẩm theo danh mục, kích thước, màu sắc, khoảng giá.
*   Xem chi tiết sản phẩm.
*   Đăng ký tài khoản mới (phải xác thực reCapcha).

**2. Khách hàng (Khách hàng):**
*   Bao gồm tất cả các quyền của Guest.
*   Đăng nhập, đăng xuất và quản lý thông tin cá nhân.
*   Quản lý giỏ hàng (thêm, xóa, cập nhật số lượng).
*   Thực hiện quy trình thanh toán và đặt hàng.
*   Xem lịch sử và chi tiết các đơn hàng đã đặt.
*   Viết và gửi đánh giá cho sản phẩm.
*   Sử dụng tính năng chat thời gian thực với phía quản lý cửa hàng.

**3. Nhân viên (Staff):**
*   Quản lý sản phẩm của cửa hàng (thêm, sửa, xóa).
*   Quản lý các biến thể sản phẩm (màu sắc, kích thước).
*   Xử lý và cập nhật trạng thái các đơn hàng thuộc cửa hàng mình.
*   Xem báo cáo doanh thu và phân tích bán hàng.
*   Tạo và quản lý các mã giảm giá (discount).


**4. Quản trị viên (Admin):**
*   Toàn quyền quản lý hệ thống và các chức năng của nhân viên.
*   Quản lý tài khoản của tất cả người dùng (khóa, mở khóa, phân quyền).
*   Quản lý toàn bộ đơn hàng.
*   Quản lý các danh mục, sản phẩm, kho, mã giảm giá.
*   Xem dashboard tổng quan về hoạt động của toàn hệ thống.

## **Công nghệ sử dụng**

*   **Ngôn ngữ:** Java 21
*   **Framework:** Spring Boot 3.5.6
*   **Quản lý phụ thuộc:** Apache Maven
*   **Cơ sở dữ liệu:** My SQL
*   **Truy vấn CSDL:** Spring Data JPA (Hibernate)
*   **Bảo mật:** Spring Security (Xác thực session-based, phân quyền theo vai trò), JWT
*   **View Engine:** Thymeleaf
*   **Giao tiếp Real-time:** Spring WebSocket (cho tính năng Chat, thanh toán)
*   **Front-end:** HTML, CSS, JavaScript, Bootstrap

## **Kiến trúc hệ thống**

Dự án được xây dựng theo kiến trúc phân lớp (Layered Architecture), một biến thể của mô hình MVC, giúp mã nguồn được tổ chức một cách rõ ràng, dễ bảo trì và mở rộng.

*   **Controller Layer:** Tiếp nhận các HTTP request từ người dùng, gọi các phương thức xử lý logic ở tầng Service và trả về View (Thymeleaf) hoặc dữ liệu (JSON) cho client.
*   **DTO (Data Transfer Object) Layer:** Đóng vai trò là các đối tượng trung gian để truyền dữ liệu giữa các lớp, đặc biệt là giữa Controller và Service, giúp giảm sự phụ thuộc và che giấu cấu trúc của Entity.
*   **Service Layer:** Chứa toàn bộ logic nghiệp vụ của ứng dụng (ví dụ: xử lý đặt hàng, tính toán doanh thu). Lớp này giao tiếp với Repository Layer để thao tác với dữ liệu.
*   **Repository Layer:** Giao tiếp trực tiếp với cơ sở dữ liệu thông qua Spring Data JPA. Cung cấp các phương thức để truy vấn, thêm, sửa, xóa dữ liệu mà không cần viết mã SQL thủ công.
*   **Entity Layer:** Định nghĩa các thực thể ứng với các bảng trong cơ sở dữ liệu.

## **Yêu cầu môi trường**

*   **JDK:** Phiên bản 21 hoặc mới hơn.
*   **Maven:** Phiên bản 3.8 hoặc mới hơn.
*   **Cơ sở dữ liệu:** SQL Server 20.

## **Hướng dẫn cài đặt và khởi chạy**

Vui lòng thực hiện các bước sau để thiết lập và chạy dự án trên máy cục bộ.

**Bước 1: Tải mã nguồn**
```bash
https://github.com/Web-x-mate/x-mate.git
cd x-mate
```

**Bước 2: Thiết lập cơ sở dữ liệu trên mysql**
1. Mở trình quản lý CSDL.
2. Tạo một schema mới với tên `xmate`.
   ```sql
   CREATE DATABASE xmate;
   ```
3. Chạy file script `database/ScriptXmate.sql` để tạo cấu trúc bảng.
4. Chạy file script `database/catalog.sql, database/systemQuery` để thêm dữ liệu mẫu (bao gồm các tài khoản mặc định).

**Bước 3: Cấu hình kết nối**
1. Mở file `src/main/resources/application.properties`.
2. Cập nhật các thông tin sau để khớp với cấu hình CSDL của bạn
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
**Bước 4: Khởi chạy ứng dụng**
1. Mở Terminal hoặc Command Prompt tại thư mục gốc của dự án.
2. Chạy lệnh Maven để build dự án:
   ```bash
   mvn clean install
   ```
3. Sau khi build thành công, chạy ứng dụng bằng lệnh:
   ```bash
   mvn spring-boot:run
   ```
   Hoặc bạn có thể mở dự án bằng một IDE (IntelliJ, Eclipse) và chạy file `AdminWebApplication.java`.

**Bước 5: Truy cập ứng dụng**
Mở trình duyệt và truy cập vào địa chỉ: 
`http://localhost:8080` ,
‘https://x-mate-v425.onrender.com’ (nếu deploy)

## **Thông tin tài khoản mặc định**

Bạn có thể sử dụng các tài khoản sau để kiểm tra các chức năng của hệ thống 

*   **Admin:**
    *   email: `admin@example.com`
    *   password: `123456`
*   **Customer:**
    *   Đăng nhập nhanh bằng google
    *   Đăng nhập bằng facebook
    *   Đăng nhập bằng tài khoản

## **Cấu trúc dự án**

```
.
├── src                          # Thư mục chứa toàn bộ mã nguồn của dự án
│   ├── main                    production/dev
│   │   ├── java                 # Chứa các file mã Java
│   │   │   └── xmate
│   │   │       └── com
│   │   │           ├── api        # Các REST API endpoint
│   │   │           ├── chatbot    # Module Chatbot( python)
│   │   │           ├── config     # Cấu hình Spring Boot 
│   │   │           ├── controller # Controller giao diện 
│   │   │           ├── dto        # Lớp trung gian giữa Entity và View
│   │   │           ├── entity     # Các class Entity ánh xạ với bảng trong cơ sở dữ liệu (JPA)
│   │   │           ├── repo       # Repository (DAO layer) – thao tác với database bằng JPA
│   │   │           ├── security   # Xử lý bảo mật (JWT, Authentication, Authorization, UserDetails,...)
│   │   │           ├── service    # Tầng nghiệp vụ (Business logic) của ứng dụng
│   │   │           ├── storage    # Xử lý upload/lưu file 
│   │   │           ├── util       # Các hàm tiện ích 
│   │   │           └── AdminWebApplication.java  # File main – điểm khởi động ứng dụng Spring Boot
│   │   └── resources
│   │       ├── static            # File tĩnh (CSS, JS, ảnh, icon, fonts,...) phục vụ cho giao diện
│   │       ├── templates         # Các file HTML (Thymeleaf template) – giao diện của AdminWeb
│   │       ├── application-dev.properties  # Cấu hình cho môi trường phát triển (dev)
│   │       └── application.properties      # Cấu hình mặc định / production
│   └── test                      # Chứa các file kiểm thử (JUnit, MockMvc, Integration test,...)
├── uploads                       # Thư mục chứa file người dùng upload (ảnh sản phẩm, avatar,...)
├── Dockerfile                    # Cấu hình Docker – build image chạy Spring Boot (multi-stage)

└── pom.xml                       # File cấu hình Maven (dependencies, plugins, build settings,...)

```
## **Nhóm Tác giả**
1.  Võ An Thái - 23110325
2.  Châu Kim Lương - 23110259
3.  Nguyễn Việt Hiếu - 23110215
4.  Đinh Văn Sáng - 23110302



