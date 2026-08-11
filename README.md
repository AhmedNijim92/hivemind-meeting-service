# HiveMind Meeting Service

> Manages meetings, real-time chat, participant tracking, and WebSocket support for collaboration.

## Overview

The Meeting Service handles the full meeting lifecycle — create, start, join, leave, and end. It includes a real-time chat API where messages are stored in Redis (accessed via REST). Participants are tracked in Redis sets for fast membership checks. Meetings support PUBLIC (anyone can join) and PRIVATE (members only) privacy modes. WebSocket STOMP support is available as a future enhancement for real-time push.

## Features

- Meeting CRUD: create, start, join, leave, end
- Real-time chat stored in Redis (REST-based)
- Participant tracking via Redis sets
- PUBLIC / PRIVATE meeting privacy
- Kafka integration for meeting events
- WebSocket STOMP support (future enhancement)

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/meetings` | JWT | Create a meeting |
| PUT | `/api/v1/meetings/{id}/start` | JWT | Start a meeting |
| POST | `/api/v1/meetings/{id}/join` | JWT | Join a meeting |
| POST | `/api/v1/meetings/{id}/leave` | JWT | Leave a meeting |
| PUT | `/api/v1/meetings/{id}/end` | JWT | End a meeting |
| GET | `/api/v1/meetings/{id}` | JWT | Get meeting details |
| POST | `/api/v1/chat/{conversationId}` | JWT | Send a chat message |
| GET | `/api/v1/chat/{conversationId}` | JWT | Get chat messages |
| GET | `/api/v1/chat/{conversationId}/count` | JWT | Get message count |

## Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | Service port | `8085` |
| `spring.cassandra.contact-points` | Cassandra hosts | `localhost` |
| `spring.data.redis.host` | Redis host (chat + participants) | `localhost` |
| `spring.kafka.bootstrap-servers` | Kafka brokers | `localhost:9092` |
| `eureka.client.serviceUrl.defaultZone` | Eureka registry URL | `http://localhost:8761/eureka` |

## Tech Stack

- Java 17
- Spring Boot 3.x
- Apache Cassandra
- Redis (chat storage + participant sets)
- Apache Kafka
- WebSocket / STOMP
- Eureka Client
- Maven

## Docker

```
Port: 8085
Base image: eclipse-temurin:17-jre-alpine
JVM flags: -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC
User: non-root (spring)
```

## CI/CD

- **Build**: Maven `clean package` with JDK 17 (Temurin)
- **Test**: Unit tests run during build phase
- **Docker**: Build and push to Docker Hub on `main` branch merge
- **Security**: Trivy vulnerability scan (CRITICAL, HIGH) on built image
