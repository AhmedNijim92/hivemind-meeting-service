# Meeting Service

> HiveMind Meeting Scheduling & Live Participant Microservice

## Overview

The meeting-service handles scheduling, starting, joining, leaving, and ending meetings within groups. It uses Redis sets to track live participants in real-time and publishes events to Kafka when meetings start.

## Service Info

| Property | Value |
|----------|-------|
| Port | 8085 |
| Service Name | `meeting-service` |
| Database | Apache Cassandra + Redis |
| Keyspace | `meeting_keyspace` |
| Spring Boot | 3.3.5 |
| Spring Cloud | 2023.0.3 |
| Java | 17 |

## Architecture

```
Client (via Gateway)
  │
  ▼
MeetingController
  │
  ├── IMeetingService (create, getMeetingById, getMeetingsByGroup, start, join, leave, end, getParticipants)
  │       ├── MeetingRepository (Cassandra)
  │       └── Redis (live participant sets)
  │
  └── Kafka Producer → meeting-started-topic
```

## API Endpoints

Base path: `/api/v1/meetings`
All endpoints require JWT (X-User-Id header injected by gateway).

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create/schedule a meeting |
| GET | `/{groupId}/{meetingId}` | Get meeting details |
| GET | `/group/{groupId}` | Get all meetings in a group |
| POST | `/{groupId}/{meetingId}/start` | Start a meeting |
| POST | `/{groupId}/{meetingId}/join` | Join a meeting |
| POST | `/{meetingId}/leave` | Leave a meeting |
| POST | `/{groupId}/{meetingId}/end` | End a meeting |
| GET | `/{meetingId}/participants` | Get live participants |

### Request/Response Examples

#### POST /api/v1/meetings
```json
// Request
{
  "groupId": "uuid",
  "title": "Sprint Planning",
  "description": "Weekly sprint planning meeting",
  "privacy": "PRIVATE",
  "scheduledAt": "2025-06-05T14:00:00"
}

// Response (201)
{
  "meetingId": "uuid",
  "groupId": "uuid",
  "hostId": "uuid",
  "title": "Sprint Planning",
  "description": "Weekly sprint planning meeting",
  "status": "SCHEDULED",
  "privacy": "PRIVATE",
  "scheduledAt": "2025-06-05T14:00:00",
  "createdAt": "2025-06-04T10:00:00"
}
```

#### POST /api/v1/meetings/{groupId}/{meetingId}/start
```json
// Response (200) — meeting with status changed to ACTIVE
{
  "meetingId": "uuid",
  "status": "ACTIVE",
  "startedAt": "2025-06-05T14:00:00"
}
```

#### GET /api/v1/meetings/{meetingId}/participants
```json
// Response (200)
["user-id-1", "user-id-2", "user-id-3"]
```

## Data Model

### Meeting (Cassandra table: `meetings`)

| Column | Type | Key Type | Description |
|--------|------|----------|-------------|
| group_id | UUID | PARTITION | Group this meeting belongs to |
| meeting_id | UUID | CLUSTERED (DESC) | Meeting identifier |
| host_id | UUID | — | Meeting creator/host |
| title | String | — | Meeting title |
| description | String | — | Meeting description |
| status | String | — | SCHEDULED, ACTIVE, ENDED |
| privacy | String | — | PUBLIC, PRIVATE |
| scheduled_at | LocalDateTime | — | Scheduled start time |
| started_at | LocalDateTime | — | Actual start time |
| ended_at | LocalDateTime | — | End time |
| created_at | LocalDateTime | — | Creation timestamp |

### Redis: Live Participants

- Key pattern: `meeting:{meetingId}:participants`
- Type: Redis SET
- Values: User ID strings
- Lifecycle: Populated on join, removed on leave, cleared on end

## Kafka Events

### Produces: `meeting-started-topic`

Published when a meeting transitions to ACTIVE:

```json
{
  "meetingId": "uuid",
  "groupId": "uuid",
  "hostId": "uuid",
  "title": "Sprint Planning",
  "timestamp": "2025-06-05T14:00:00"
}
```

**Consumers:**
- `notification-service` — notifies group members that a meeting has started

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| CASSANDRA_HOST | localhost | Cassandra contact point |
| CASSANDRA_PORT | 9042 | Cassandra port |
| CASSANDRA_DATACENTER | datacenter1 | Cassandra datacenter |
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092 | Kafka brokers |
| REDIS_HOST | localhost | Redis host |
| REDIS_PORT | 6379 | Redis port |
| EUREKA_SERVER | http://localhost:8761/eureka | Eureka URL |

## Dependencies

- spring-boot-starter-web
- spring-boot-starter-websocket
- spring-boot-starter-data-cassandra
- spring-boot-starter-data-redis
- spring-cloud-starter-netflix-eureka-client
- spring-cloud-starter-config
- spring-kafka
- hivemind-common (1.0.0)
- lombok

## Running Locally

```bash
# Prerequisites: Cassandra, Kafka, Redis running
cd microservices/meeting-service
mvn spring-boot:run
```

Auto-creates `meeting_keyspace` and `meetings` table on startup.

## Business Rules

- Only the host can start or end a meeting
- Meeting status flow: SCHEDULED → ACTIVE → ENDED
- Participants are tracked in Redis only while meeting is ACTIVE
- When a meeting ends, the Redis participant set is cleared
