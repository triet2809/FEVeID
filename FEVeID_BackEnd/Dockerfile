# ---- Giai đoạn 1: build (JDK + Maven wrapper, KHÔNG mang lên server) ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy riêng những file Maven cần để tải thư viện.
# Lớp này chỉ build lại khi pom.xml (hoặc wrapper) đổi
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Sửa code chỉ làm đổi lớp này trở xuống, thư viện ở lớp trên được dùng lại từ cache.
# -DskipTests: CI đã chạy test; Testcontainers cũng không chạy được bên trong docker build
COPY src src
RUN ./mvnw -B package -DskipTests

# ---- Giai đoạn 2: chạy (chỉ JRE + jar) ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# User thường, không có shell đăng nhập: app bị chiếm quyền cũng không thành root của container
RUN useradd --system --no-create-home --shell /usr/sbin/nologin feveid
USER feveid

# Lấy jar từ giai đoạn build. *.jar không khớp với file FEVeID-...jar.original nên chỉ ra đúng 1 file
COPY --from=build /app/target/*.jar app.jar

# Chỉ là ghi chú cho người đọc + docker compose; mở cổng là do -p / ports quyết định
EXPOSE 8080

# Dạng mảng (exec form): java là tiến trình số 1 nhận được tín hiệu tắt (docker stop) và tắt êm
ENTRYPOINT ["java", \
  "-Duser.timezone=Asia/Ho_Chi_Minh", \
  "-XX:MaxRAMPercentage=75", \
  "-jar", "app.jar"]
