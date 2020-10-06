FROM openjdk:13
WORKDIR /app/
COPY ./* /folder/
RUN javac WordAnalyze.java