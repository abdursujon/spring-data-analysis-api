FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/spring-data-analysis-api.jar spring-data-analysis-api.jar
EXPOSE 8080
CMD ["java", "-jar", "spring-data-analysis-api.jar"]

