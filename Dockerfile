FROM openjdk:13-alpine
COPY ./* /app/
WORKDIR /app/
RUN javac -d ./output ./opg.java
WORKDIR /app/output