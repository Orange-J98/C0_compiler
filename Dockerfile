FROM openjdk:13-alpine
COPY ./* /app/
WORKDIR /app/
RUN javac -d ./output ./WordAnalyze.java
WORKDIR /app/output