# Многостадийная сборка десктоп-клиента (JavaFX, Java 25, Maven).
#
# Сборка образа:
#   docker build -t desktop-client .
#
# Запуск GUI (нужен X-сервер, иначе окно не отобразится):
#   Linux:
#     xhost +local:docker
#     docker run --rm -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix:ro desktop-client
#   macOS (XQuartz: разрешить подключения, затем):
#     xhost +localhost
#     docker run --rm -e DISPLAY=host.docker.internal:0 desktop-client
# Без X-сервера процесс стартует, но окно JavaFX не покажет (headless-контейнер).

# ---------- stage 1: сборка jar ----------
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app

# Сначала только дескрипторы — слой с зависимостями кешируется,
# пока не меняется pom.xml.
COPY pom.xml ./
COPY .mvn/ .mvn/
RUN mvn -B dependency:go-offline -DskipTests

# Затем исходники и сборка без тестов
# (verify-плагины spotless/checkstyle не входят в фазу package).
COPY src/ src/
COPY checkstyle.xml ./
RUN mvn -B package -DskipTests \
  && mvn -B dependency:copy-dependencies -DoutputDirectory=target/libs -DincludeScope=runtime

# ---------- stage 2: запуск ----------
FROM eclipse-temurin:25-jre-jammy
LABEL org.opencontainers.image.description="JavaFX desktop-клиент"

# Нативные зависимости JavaFX (GTK/GL/X11/шрифты) для Ubuntu Jammy.
RUN apt-get update \
  && apt-get install -y --no-install-recommends \
    libgtk-3-0 \
    libgl1 \
    libxtst6 \
    libxi6 \
    libxrender1 \
    libxrandr2 \
    libxinerama1 \
    libxcursor1 \
    libxcomposite1 \
    libxdamage1 \
    libasound2 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libpango-1.0-0 \
    libcairo2 \
    fontconfig \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/*.jar ./app.jar
COPY --from=build /app/target/libs/ ./libs/

# Дефолты для запуска без бэкенда можно переопределить через -e:
#   docker run --rm -e MOCK_DATA=true ... desktop-client
ENV INITIAL_PAGE_PATH=/fxml/LoginForm.fxml \
    MOCK_DATA=false \
    JAVA_OPTS="-Dprism.order=sw -Djava.awt.headless=false --enable-native-access=ALL-UNNAMED"

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -cp '/app/app.jar:/app/libs/*' com.DesktopApplicationClientJava.Main"]
