FROM openjdk:17-jdk-slim
MAINTAINER Egor Babcinetchi
ADD docker /app/
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
# build the project
RUN chmod +x ./mvnw
RUN ./mvnw install -DskipTests
#ENTRYPOINT ["/bin/sh"]
COPY target/distributed-db-0.0.1-SNAPSHOT.jar distributed-db-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/distributed-db-0.0.1-SNAPSHOT.jar"]
