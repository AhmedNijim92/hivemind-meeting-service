# Meeting Service — Code-Level Reference

## MeetingServiceApplication

**Package:** `com.hivemind.meeting`

**Annotations:**
- `@SpringBootApplication` — Enables auto-configuration, component scanning, and configuration properties
- `@EnableDiscoveryClient` — Registers with Eureka service registry
- `@EnableKafka` — Enables Kafka producer annotations

**Design Pattern:** Application Entry Point (Spring Boot convention)

### Methods

#### `main(String[] args)`
- **Signature:** `public static void main(String[] args)`
- **Logic:** `SpringApplication.run(MeetingServiceApplication.class, args)`
- **Returns:** void

---

## CassandraConfig

**Package:** `com.hivemind.meeting.config`

**Extends:** `AbstractCassandraConfiguration`

**Annotations:**
- `@Configuration`

**Design Pattern:** Template Method — overrides hook methods from abstract parent

### Overridden Methods

#### `getKeyspaceName()`
- **Returns:** `"meeting_keyspace"`

#### `getContactPoints()`
- **Returns:** Cassandra contact points (from configuration or default `"localhost"`)

#### `getPort()`
- **Returns:** Cassandra port (default `9042`)

#### `getLocalDataCenter()`
- **Returns:** `"datacenter1"`

#### `getSchemaAction()`
- **Returns:** `SchemaAction.CREATE_IF_NOT_EXISTS`

#### `getEntityBasePackages()`
- **Returns:** `new String[] { "com.hivemind.meeting.entity" }`

#### `getKeyspaceCreations()`
- **Logic:** Creates keyspace with SimpleStrategy, replication factor = 1, DURABLE_WRITES = true
- **Returns:** `List<CreateKeyspaceSpecification>`

---

## KafkaProducerConfig

**Package:** `com.hivemind.meeting.config`

**Annotations:**
- `@Configuration`

**Design Pattern:** Factory Method — creates configured Kafka producer components

### Beans

#### `producerFactory()`
- **Signature:** `@Bean public ProducerFactory<String, MeetingStartedEvent> producerFactory()`
- **Logic:** Configures producer with:
  - `bootstrap.servers` from application properties
  - Key serializer: `StringSerializer`
  - Value serializer: `JsonSerializer` (for MeetingStartedEvent)
- **Returns:** `DefaultKafkaProducerFactory<String, MeetingStartedEvent>`

#### `kafkaTemplate()`
- **Signature:** `@Bean public KafkaTemplate<String, MeetingStartedEvent> kafkaTemplate()`
- **Logic:** Wraps the `producerFactory()` in a `KafkaTemplate`
- **Returns:** `KafkaTemplate<String, MeetingStartedEvent>`

---

## RedisConfig

**Package:** `com.hivemind.meeting.config`

**Annotations:**
- `@Configuration`

**Design Pattern:** Factory Method — creates configured Redis template for participant tracking

### Beans

#### `redisTemplate(RedisConnectionFactory connectionFactory)`
- **Signature:** `@Bean public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory)`
- **Logic:**
  1. Creates new `RedisTemplate<String, String>`
  2. Sets `StringRedisSerializer` for key serializer
  3. Sets `StringRedisSerializer` for value serializer
  4. Sets connection factory
- **Returns:** `RedisTemplate<String, String>`
- **Purpose:** Used for Redis SET operations to track active meeting participants in real-time

---

## MeetingController

**Package:** `com.hivemind.meeting.controller`

**Annotations:**
- `@RestController`
- `@RequestMapping("/api/v1/meetings")`

**Design Pattern:** Façade — exposes simplified REST API over service layer

### Fields (Constructor Injection)

| Field | Type |
|-------|------|
| meetingService | IMeetingService |

### Endpoints

#### `POST /`
- **Signature:** `public ResponseEntity<MeetingDto> createMeeting(@RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody CreateMeetingRequest request)`
- **Logic:** Delegates to `meetingService.createMeeting(userId, request)`
- **Returns:** `201 Created` with `MeetingDto`

#### `GET /group/{groupId}`
- **Signature:** `public ResponseEntity<List<MeetingDto>> getMeetingsByGroup(@PathVariable UUID groupId)`
- **Logic:** Delegates to `meetingService.getMeetingsByGroup(groupId)`
- **Returns:** `List<MeetingDto>` — all meetings in the group

#### `GET /{groupId}/{meetingId}`
- **Signature:** `public ResponseEntity<MeetingDto> getMeetingById(@PathVariable UUID groupId, @PathVariable UUID meetingId)`
- **Logic:** Delegates to `meetingService.getMeetingById(groupId, meetingId)`
- **Returns:** `MeetingDto`

#### `POST /{groupId}/{meetingId}/start`
- **Signature:** `public ResponseEntity<MeetingDto> startMeeting(@PathVariable UUID groupId, @PathVariable UUID meetingId, @RequestHeader("X-User-Id") UUID userId)`
- **Logic:** Delegates to `meetingService.startMeeting(groupId, meetingId, userId)`
- **Returns:** `MeetingDto` with status = ACTIVE

#### `POST /{groupId}/{meetingId}/join`
- **Signature:** `public ResponseEntity<ApiResponse> joinMeeting(@PathVariable UUID groupId, @PathVariable UUID meetingId, @RequestHeader("X-User-Id") UUID userId)`
- **Logic:** Delegates to `meetingService.joinMeeting(groupId, meetingId, userId)`
- **Returns:** `ApiResponse` with success message

#### `POST /{groupId}/{meetingId}/leave`
- **Signature:** `public ResponseEntity<ApiResponse> leaveMeeting(@PathVariable UUID groupId, @PathVariable UUID meetingId, @RequestHeader("X-User-Id") UUID userId)`
- **Logic:** Delegates to `meetingService.leaveMeeting(groupId, meetingId, userId)`
- **Returns:** `ApiResponse` with success message

#### `POST /{groupId}/{meetingId}/end`
- **Signature:** `public ResponseEntity<MeetingDto> endMeeting(@PathVariable UUID groupId, @PathVariable UUID meetingId, @RequestHeader("X-User-Id") UUID userId)`
- **Logic:** Delegates to `meetingService.endMeeting(groupId, meetingId, userId)`
- **Returns:** `MeetingDto` with status = ENDED

#### `GET /{groupId}/{meetingId}/participants`
- **Signature:** `public ResponseEntity<Set<String>> getParticipants(@PathVariable UUID groupId, @PathVariable UUID meetingId)`
- **Logic:** Delegates to `meetingService.getParticipants(groupId, meetingId)`
- **Returns:** `Set<String>` — set of participant user IDs currently in the meeting

---

## Meeting (Entity)

**Package:** `com.hivemind.meeting.entity`

**Annotations:**
- `@Table("meetings")` — Maps to Cassandra `meetings` table

**Design Pattern:** Composite Key — partitioned by group for efficient group-level queries

### Fields

| Field | Type | Key Type | Description |
|-------|------|----------|-------------|
| groupId | UUID | `PARTITIONED` | Group this meeting belongs to |
| meetingId | UUID | `CLUSTERED` (DESC) | Unique meeting identifier |
| hostId | UUID | | User who created/hosts the meeting |
| title | String | | Meeting title |
| description | String | | Meeting description |
| status | String | | `"SCHEDULED"`, `"ACTIVE"`, or `"ENDED"` |
| privacy | String | | `"PUBLIC"` or `"PRIVATE"` |
| scheduledAt | LocalDateTime | | Planned start time |
| startedAt | LocalDateTime | | Actual start time (null until started) |
| endedAt | LocalDateTime | | End time (null until ended) |
| createdAt | LocalDateTime | | Meeting creation timestamp |

**State Machine:**
```
SCHEDULED → ACTIVE → ENDED
```

---

## MeetingRepository

**Package:** `com.hivemind.meeting.repository`

**Extends:** `CassandraRepository<Meeting, Object>`

**Design Pattern:** Repository pattern

### Methods

#### `findByGroupId(UUID groupId)`
- **Signature:** `@Query List<Meeting> findByGroupId(UUID groupId)`
- **Logic:** Fetches all meetings in a group partition (returned in clustered order — newest first)
- **Returns:** `List<Meeting>`

#### `findByGroupIdAndMeetingId(UUID groupId, UUID meetingId)`
- **Signature:** `@Query Optional<Meeting> findByGroupIdAndMeetingId(UUID groupId, UUID meetingId)`
- **Logic:** Looks up a specific meeting by composite key
- **Returns:** `Optional<Meeting>`

---

## IMeetingService (Interface)

**Package:** `com.hivemind.meeting.service`

### Method Signatures

| Method | Parameters | Returns |
|--------|-----------|---------|
| `createMeeting` | `UUID hostId, CreateMeetingRequest request` | `MeetingDto` |
| `getMeetingById` | `UUID groupId, UUID meetingId` | `MeetingDto` |
| `getMeetingsByGroup` | `UUID groupId` | `List<MeetingDto>` |
| `startMeeting` | `UUID groupId, UUID meetingId, UUID userId` | `MeetingDto` |
| `joinMeeting` | `UUID groupId, UUID meetingId, UUID userId` | `void` |
| `leaveMeeting` | `UUID groupId, UUID meetingId, UUID userId` | `void` |
| `endMeeting` | `UUID groupId, UUID meetingId, UUID userId` | `MeetingDto` |
| `getParticipants` | `UUID groupId, UUID meetingId` | `Set<String>` |

---

## MeetingServiceImpl

**Package:** `com.hivemind.meeting.service.impl`

**Annotations:**
- `@Service`

**Implements:** `IMeetingService`

**Design Patterns:**
- Service Layer — encapsulates meeting lifecycle business logic
- State Pattern — manages meeting state transitions (SCHEDULED → ACTIVE → ENDED)
- CQRS-lite — uses Redis for real-time participant state, Cassandra for meeting metadata

### Fields (Constructor Injection)

| Field | Type |
|-------|------|
| meetingRepository | MeetingRepository |
| kafkaTemplate | KafkaTemplate<String, MeetingStartedEvent> |
| redisTemplate | RedisTemplate<String, String> |

### Redis Key Pattern

```
meeting:participants:{meetingId}
```
- **Type:** Redis SET
- **Content:** User ID strings of active participants
- **Lifecycle:** Created on `startMeeting`, deleted on `endMeeting`

### Methods

#### `createMeeting(UUID hostId, CreateMeetingRequest request)`
- **Signature:** `@Override public MeetingDto createMeeting(UUID hostId, CreateMeetingRequest request)`
- **Logic:**
  1. Builds `Meeting` entity:
     - `groupId` = request.getGroupId()
     - `meetingId` = UUID.randomUUID()
     - `hostId` = hostId
     - `title` = request.getTitle()
     - `description` = request.getDescription()
     - `status` = "SCHEDULED"
     - `privacy` = request.getPrivacy() (defaults to "PUBLIC")
     - `scheduledAt` = request.getScheduledAt()
     - `createdAt` = LocalDateTime.now()
  2. Saves meeting via `meetingRepository.save(meeting)`
  3. Maps to MeetingDto via `toDto(meeting)`
- **Returns:** `MeetingDto`

#### `getMeetingById(UUID groupId, UUID meetingId)`
- **Signature:** `@Override public MeetingDto getMeetingById(UUID groupId, UUID meetingId)`
- **Logic:**
  1. Calls `meetingRepository.findByGroupIdAndMeetingId(groupId, meetingId)`
  2. If not found → throws RuntimeException ("Meeting not found")
  3. Maps to MeetingDto via `toDto(meeting)`
- **Returns:** `MeetingDto` (includes participantCount from Redis)
- **Exceptions:** RuntimeException if meeting not found

#### `getMeetingsByGroup(UUID groupId)`
- **Signature:** `@Override public List<MeetingDto> getMeetingsByGroup(UUID groupId)`
- **Logic:**
  1. Calls `meetingRepository.findByGroupId(groupId)`
  2. Maps each Meeting to MeetingDto via `toDto()`
- **Returns:** `List<MeetingDto>`

#### `startMeeting(UUID groupId, UUID meetingId, UUID userId)`
- **Signature:** `@Override public MeetingDto startMeeting(UUID groupId, UUID meetingId, UUID userId)`
- **Logic:**
  1. Loads meeting by composite key
  2. Validates that `userId` equals `meeting.getHostId()` — only host can start
  3. Sets `status` = "ACTIVE"
  4. Sets `startedAt` = LocalDateTime.now()
  5. Saves updated meeting
  6. Adds host to Redis participant set: `redisTemplate.opsForSet().add("meeting:participants:" + meetingId, userId.toString())`
  7. Publishes `MeetingStartedEvent` to Kafka topic `"meeting-started-topic"` (contains meetingId, groupId, hostId, title)
  8. Maps to MeetingDto
- **Returns:** `MeetingDto`
- **Exceptions:** RuntimeException if user is not the host

#### `joinMeeting(UUID groupId, UUID meetingId, UUID userId)`
- **Signature:** `@Override public void joinMeeting(UUID groupId, UUID meetingId, UUID userId)`
- **Logic:**
  1. Loads meeting by composite key
  2. Validates meeting `status` is "ACTIVE" — throws if not
  3. Adds userId to Redis participant set: `redisTemplate.opsForSet().add("meeting:participants:" + meetingId, userId.toString())`
- **Returns:** void
- **Exceptions:** RuntimeException if meeting is not ACTIVE

#### `leaveMeeting(UUID groupId, UUID meetingId, UUID userId)`
- **Signature:** `@Override public void leaveMeeting(UUID groupId, UUID meetingId, UUID userId)`
- **Logic:**
  1. Removes userId from Redis participant set: `redisTemplate.opsForSet().remove("meeting:participants:" + meetingId, userId.toString())`
- **Returns:** void

#### `endMeeting(UUID groupId, UUID meetingId, UUID userId)`
- **Signature:** `@Override public MeetingDto endMeeting(UUID groupId, UUID meetingId, UUID userId)`
- **Logic:**
  1. Loads meeting by composite key
  2. Validates that `userId` equals `meeting.getHostId()` — only host can end
  3. Sets `status` = "ENDED"
  4. Sets `endedAt` = LocalDateTime.now()
  5. Saves updated meeting
  6. Deletes Redis key: `redisTemplate.delete("meeting:participants:" + meetingId)`
  7. Maps to MeetingDto
- **Returns:** `MeetingDto`
- **Exceptions:** RuntimeException if user is not the host

#### `getParticipants(UUID groupId, UUID meetingId)`
- **Signature:** `@Override public Set<String> getParticipants(UUID groupId, UUID meetingId)`
- **Logic:** Returns `redisTemplate.opsForSet().members("meeting:participants:" + meetingId)`
- **Returns:** `Set<String>` — set of userId strings currently in the meeting

#### `toDto(Meeting meeting)` (Private)
- **Signature:** `private MeetingDto toDto(Meeting meeting)`
- **Logic:**
  1. Maps all fields from entity to DTO
  2. Gets participantCount from Redis SET size: `redisTemplate.opsForSet().size("meeting:participants:" + meeting.getMeetingId())`
  3. If Redis returns null (meeting not active or key expired) → sets participantCount = 0
- **Returns:** `MeetingDto` (includes live participantCount)

---

## DTOs

**Package:** `com.hivemind.meeting.dto`

### CreateMeetingRequest

| Field | Type | Validation | Default | Description |
|-------|------|------------|---------|-------------|
| groupId | UUID | `@NotNull` | — | Target group |
| title | String | `@NotBlank` | — | Meeting title |
| description | String | Optional | — | Meeting description |
| privacy | String | Optional | `"PUBLIC"` | PUBLIC or PRIVATE |
| scheduledAt | LocalDateTime | Optional | — | Planned start time |

### MeetingDto

| Field | Type | Description |
|-------|------|-------------|
| meetingId | UUID | Unique meeting identifier |
| groupId | UUID | Group the meeting belongs to |
| hostId | UUID | Host's user ID |
| title | String | Meeting title |
| description | String | Meeting description |
| status | String | SCHEDULED, ACTIVE, or ENDED |
| privacy | String | PUBLIC or PRIVATE |
| scheduledAt | LocalDateTime | Planned start time |
| startedAt | LocalDateTime | Actual start time |
| endedAt | LocalDateTime | End time |
| createdAt | LocalDateTime | Creation timestamp |
| participantCount | int | Current number of participants (from Redis SET size) |

### MeetingStartedEvent (Kafka Event — produced)

| Field | Type | Description |
|-------|------|-------------|
| meetingId | UUID | Meeting ID |
| groupId | UUID | Group ID |
| hostId | UUID | Host's user ID |
| title | String | Meeting title |

### ApiResponse

| Field | Type | Description |
|-------|------|-------------|
| message | String | Success/error message |
| success | boolean | Operation result |
