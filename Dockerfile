FROM gradle:latest AS build
WORKDIR /app

#Копируем исходный код и файлы Gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle ./gradle
COPY src ./src

# Собираем fat JAR
RUN gradle buildFatJar --no-daemon

# Используем легковесный образ для запуска приложения
FROM amazoncorretto:22
WORKDIR /app

# Копируем собранный JAR-файл из стадии сборки
COPY --from=build /app/build/libs/*.jar ./tennisscorekeeperbackend.jar

# Открываем порт, на котором работает Ktor
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "tennisscorekeeperbackend.jar"]