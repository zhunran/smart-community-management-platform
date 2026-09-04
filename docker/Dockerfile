FROM maven:3.9-eclipse-temurin-25-alpine AS builder

WORKDIR /build

# Maven 阿里云镜像配置（加速依赖下载）
COPY docker/maven-settings.xml /build/maven-settings.xml

# pom 层缓存
COPY pom.xml .
COPY property-common/pom.xml property-common/
COPY property-framework/pom.xml property-framework/
COPY property-module-bill/pom.xml property-module-bill/
COPY property-module-owner/pom.xml property-module-owner/
COPY property-module-payment/pom.xml property-module-payment/
COPY property-module-parking/pom.xml property-module-parking/
COPY property-module-notification/pom.xml property-module-notification/
COPY property-module-statistic/pom.xml property-module-statistic/
COPY property-module-ai/pom.xml property-module-ai/
COPY property-module-housing/pom.xml property-module-housing/
COPY property-admin-api/pom.xml property-admin-api/
COPY property-owner-api/pom.xml property-owner-api/
COPY property-task/pom.xml property-task/

RUN mvn -s /build/maven-settings.xml dependency:go-offline -B -q

# 源码
COPY property-common/src property-common/src/
COPY property-framework/src property-framework/src/
COPY property-module-bill/src property-module-bill/src/
COPY property-module-owner/src property-module-owner/src/
COPY property-module-payment/src property-module-payment/src/
COPY property-module-parking/src property-module-parking/src/
COPY property-module-notification/src property-module-notification/src/
COPY property-module-statistic/src property-module-statistic/src/
COPY property-module-ai/src property-module-ai/src/
COPY property-module-housing/src property-module-housing/src/
COPY property-admin-api/src property-admin-api/src/
COPY property-owner-api/src property-owner-api/src/
COPY property-task/src property-task/src/

ARG MODULE
RUN mvn -s /build/maven-settings.xml clean package -pl ${MODULE} -am -Dmaven.test.skip=true -q

FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
ARG MODULE
COPY --from=builder /build/${MODULE}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Duser.timezone=Asia/Shanghai -Xms256m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/ -jar /app/app.jar"]