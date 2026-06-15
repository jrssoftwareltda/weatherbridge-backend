# WeatherBridge Backend Bootstrap

Initial project structure for the WeatherBridge backend using:

- Java 25
- Spring Boot 4.0.5
- Maven
- Spring MVC
- Spring Cache
- Redis
- Actuator
- Docker
- Docker Compose

## Included files

```text
.
├── .dockerignore
├── .env.example
├── .gitignore
├── Dockerfile
├── compose.yaml
├── pom.xml
└── src
    └── main
        ├── java
        │   └── br/com/jrs/weatherbridge
        │       └── WeatherBridgeApplication.java
        └── resources
            └── application.yml
```

## Requirements

For local execution:

- JDK 25
- Maven 3.9+
- Docker with Docker Compose
- OpenWeather API key

The Docker build does not require Maven installed on the host.

## Configure environment

Create the local environment file:

```bash
cp .env .env
```

Edit `.env` and set:

```dotenv
OPENWEATHER_API_KEY=your-real-api-key
```

Do not commit `.env`.

## Build locally

```bash
mvn clean install
```

Expected artifact:

```text
target/weatherbridge-backend-0.0.1-SNAPSHOT.jar
```

## Run Redis and the API with Docker

```bash
docker compose down --remove-orphans
docker compose build --no-cache
docker compose up
```

Detached mode:

```bash
docker compose up --build -d
```

View API logs:

```bash
docker compose logs -f api
```

## Verify services

```bash
docker compose ps
```

Redis:

```bash
docker compose exec redis redis-cli ping
```

Expected:

```text
PONG
```

Actuator:

```bash
curl http://localhost:8080/actuator/health
```

## Important notes

- The Dockerfile uses the official Maven image during the build stage.
- It does not require `.mvn`, `mvnw`, or Maven Wrapper.
- The final image runs with Java 25 and a numeric non-root user.
- Docker Compose refuses to start the API when `OPENWEATHER_API_KEY` is missing.
- JPA, Flyway, Clerk, R2, multipart, and ObraView-specific configuration were removed.
- The cache-specific TTLs in `weather.cache` still need to be applied by the future Redis cache configuration class.
- The actual weather endpoints and hexagonal architecture packages are the next implementation step.
