# ---- Build stage: build jar bằng Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# copy file cấu hình trước để tận dụng cache
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw

# copy source và build
COPY src ./src
RUN ./mvnw -DskipTests package || mvn -DskipTests package

# ---- Run stage: chạy jar nhẹ ----
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV PORT=8080
EXPOSE 8080

# copy jar từ stage build (dùng wildcard để khỏi lo đúng tên)
COPY --from=build /app/target/*.jar app.jar

CMD ["sh","-c","java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT} -jar app.jar"]
