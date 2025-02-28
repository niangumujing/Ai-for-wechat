FROM eclipse-temurin:17-jre-jammy

# 创建配置文件目录
RUN mkdir -p /app/config

# 设置工作目录和配置文件路径
WORKDIR /app
ENV SPRING_CONFIG_LOCATION=classpath:/,file:/app/config/

# 复制 jar 包（注意这里不需要复制配置文件）
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 80

# 启动命令（自动加载外部配置）
ENTRYPOINT ["java", "-jar", "app.jar"]