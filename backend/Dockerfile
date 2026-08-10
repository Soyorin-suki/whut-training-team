FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /app

# 先复制 pom.xml，利用 BuildKit 缓存 maven 依赖，避免每次构建重复下载
# (需要 Docker 23+ / docker compose v2，默认已启用 BuildKit)
COPY pom.xml .
# 预下载依赖（失败忽略：仅作为预热，真正的依赖缓存由下方 cache mount 保证）
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline || true

# 复制源码并编译
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

# Stage 2: 运行环境
# 使用 jammy (Ubuntu) 系列而不是 Alpine，因为 SQLite JDBC 的 native 驱动通常更适配 glibc 而非 musl libc
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 修正时区 (项目依赖 Asia/Shanghai 定时任务)
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 从编译阶段复制打包好的 jar
COPY --from=builder /app/target/*.jar app.jar

# 暴露后端端口
EXPOSE 8080

# 启动 Spring Boot 后端
ENTRYPOINT ["java", "-jar", "app.jar"]
