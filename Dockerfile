# ---- Sử dụng OpenJDK 21 (Render hỗ trợ tốt)
FROM openjdk:21-jdk-slim

# Thư mục làm việc trong container
WORKDIR /app

# Copy file jar build ra từ Maven
COPY target/AdminWeb-0.0.1-SNAPSHOT.jar app.jar

# Render sẽ cấp PORT qua biến môi trường
ENV PORT=8080

# Mở cổng container
EXPOSE 8080

# Lệnh khởi động (Render sẽ set PORT tự động)
CMD ["sh", "-c", "java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT} -jar app.jar"]
