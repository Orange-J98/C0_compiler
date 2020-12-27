# gradle
FROM gradle:jdk14
WORKDIR /
COPY build.gradle gradle settings.gradle miniplc0-java.iml /
COPY src /src
RUN gradle fatjar --no-daemon
