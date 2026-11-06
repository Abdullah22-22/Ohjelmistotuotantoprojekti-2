# ---------- Stage 1: build the jar on Linux ----------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: run it ----------
FROM eclipse-temurin:17-jdk

RUN apt-get update && apt-get install -y \
    libgtk-3-0 libgl1 libxtst6 libxrender1 libxi6 \
    libasound2t64 fontconfig \
    fonts-noto-core fonts-noto-cjk fonts-noto-cjk-extra \
    fonts-dejavu-core fonts-kacst fonts-liberation \
    && rm -rf /var/lib/apt/lists/*

# Make Noto Sans CJK the default sans-serif so Japanese renders
# instead of empty boxes. Arabic and Latin fall through to Noto too.
RUN printf '%s\n' \
  '<?xml version="1.0"?>' \
  '<!DOCTYPE fontconfig SYSTEM "fonts.dtd">' \
  '<fontconfig>' \
  '  <alias><family>sans-serif</family><prefer>' \
  '    <family>Noto Sans CJK JP</family>' \
  '    <family>Noto Sans</family>' \
  '    <family>DejaVu Sans</family>' \
  '  </prefer></alias>' \
  '  <alias><family>serif</family><prefer>' \
  '    <family>Noto Serif CJK JP</family>' \
  '  </prefer></alias>' \
  '  <alias><family>system-ui</family><prefer>' \
  '    <family>Noto Sans CJK JP</family>' \
  '  </prefer></alias>' \
  '</fontconfig>' > /etc/fonts/local.conf \
  && fc-cache -fv

WORKDIR /app

COPY --from=build /build/target/week3-junit-1.0-SNAPSHOT.jar app.jar

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

ENV DISPLAY=host.docker.internal:0.0
ENV DB_URL=jdbc:mariadb://host.docker.internal:3307/tempdb?useUnicode=true&characterEncoding=utf8
ENV DB_USER=root
ENV DB_PASS=root

CMD ["java", "-Dfile.encoding=UTF-8", "-Dprism.lcdtext=false", "-jar", "app.jar"]