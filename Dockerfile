FROM gradle:7.0.2-jdk11-openj9
WORKDIR /app
COPY . /app
RUN gradle build -x test --no-daemon

CMD ["gradle", "run", "--no-daemon"]
