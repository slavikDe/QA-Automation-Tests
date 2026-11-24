FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-21

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget ca-certificates \
    && wget -q -O /tmp/chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt-get install -y --no-install-recommends /tmp/chrome.deb \
    && rm -rf /tmp/chrome.deb /var/lib/apt/lists/*

WORKDIR /app

# Download dependencies in a separate layer so code changes don't re-download them.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q test-compile

# Headless Chrome; --no-sandbox and --disable-dev-shm-usage are required inside a container.
ENTRYPOINT ["mvn", "-B", \
    "-Dselenide.headless=true", \
    "-Dchromeoptions.args=--no-sandbox,--disable-dev-shm-usage"]
CMD ["test"]
