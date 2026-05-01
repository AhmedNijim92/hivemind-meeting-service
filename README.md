# Meeting Service

Meeting management service for the HiveMind platform. Handles meeting creation, scheduling, participant management, and RSVP tracking.

## Details

| Property | Value |
|----------|-------|
| **Port** | `8085` |
| **Database** | Cassandra |
| **Cache** | Redis |
| **Messaging** | Kafka |
| **Role** | Meetings + Participants |

## Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/*.jar

# Docker
docker build -t hivemind/meeting-service .
```

## Links

- [Main Repository](https://github.com/AhmedNijim92/hivemind-backend)
