# 多阶段构建 - AI API Platform Backend

# 第一阶段：构建
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 先复制 pom.xml 以利用 Docker 缓存
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 第二阶段：运行
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 安装必要工具
RUN apk add --no-cache curl

# 创建日志和数据目录
RUN mkdir -p /app/logs /app/data

# 复制 jar 文件
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM 配置
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
