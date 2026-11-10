# Escape Health Club assistant

Java 21 / Spring Boot 4.1 / Spring AI 2.0, Ollama and PostgreSQL + pgvector. The assistant answers club questions in French, English and Arabic/Darija, uses database tools for prices/contact/hours, and retrieves descriptive Markdown with RAG. The browser page includes chat and a separate consent-based callback form.

## Run locally

Requires Java 21+, Docker and Ollama. Set a local database password in your shell (do not commit it), then:

```bash
export POSTGRES_PASSWORD='your-local-password'
ollama pull qwen3.5:latest
ollama pull bge-m3
docker compose up -d
KNOWLEDGE_INGEST=true ./mvnw spring-boot:run
```

Open http://localhost:8080. Startup with `KNOWLEDGE_INGEST=true` replaces the club corpus transactionally, including obsolete chunks; failure rolls back to the previous corpus. The default is false so normal startup does not depend on an embedding call. Run once after installing or changing knowledge. Chunks have stable IDs, heading boundaries, a source and a language. Embeddings remain `bge-m3`, 1024 dimensions. A change requires a new schema migration and a full re-embedding; never mix embedding models.

For an isolated temporary database without configuring a password:

```bash
./mvnw spring-boot:test-run -Dspring-boot.run.arguments="--club.knowledge.ingest=true --spring.docker.compose.enabled=false"
```

## API

```bash
curl -c /tmp/escape-cookies -b /tmp/escape-cookies \
  -H 'Content-Type: application/json' \
  -d '{"message":"Combien coûte l abonnement mensuel ?"}' \
  http://localhost:8080/api/chat
```

`POST /api/chat` accepts `message` (1–2000 characters) and returns `sessionId` and `message`. Continue with the same server cookie. The optional legacy `sessionId` input is accepted but never grants access to history. The server keeps at most 20 messages per conversation, 1000 conversations, expiring after 30 minutes. No chat history is stored in PostgreSQL. The controller serializes requests in each session.

`POST /api/callbacks` accepts `name` (1–100 characters), `phone` (Moroccan mobile/landline, `0[5-7]` + 8 digits or `+212[5-7]` + 8 digits), `language` (`fr`, `en`, `ar`) and `consent:true`. Returns HTTP 201 with a request ID and `CALLBACK_REQUESTED`. This records a callback only, never a booking. The model cannot submit or fabricate consent; the visitor submits the separate form.

`GET /api/health` reports liveness; `/actuator/health` includes configured dependency indicators. Validation failures return `INVALID_REQUEST`, dependency/model failures `SERVICE_UNAVAILABLE`, excessive requests HTTP 429 with `Retry-After`, oversized bodies HTTP 413. Error bodies never echo submitted values or internal diagnostics.

## Official data

Flyway owns all tables, vector schema and indexes. `V2` seeds 22 price entries from the [official WordPress menu](https://www.escapehealthclub.com/wp-json/wp/v2/pages/18), reviewed 2026-11-10. Contact details are seeded in `club_facts`; opening hours are deliberately absent because no official hours were verified. Staff must review prices before public launch and maintain authoritative values through new migrations or an authorized database workflow. Unknown prices/hours produce a handoff, never an estimate. No prices, phone numbers or email addresses are embedded in the prompt, Java or knowledge files.

Knowledge Markdown is manually curated from the official site, with provenance in front matter. It excludes medical claims and monetary/contact facts. `ClubTools` exposes a service catalog, exact price lookup, contact and hours. Every chat request executes the official-information tool before generation and supplies its results to the model, so required data retrieval does not depend on optional model tool calls. The system prompt is in `src/main/resources/prompts/system.txt`. No model output should be treated as medical advice or an appointment confirmation.

## Staff notifications

For local development, the Compose Mailpit service captures outgoing emails. Start the app with the `dev` profile:

```bash
# POSTGRES_PASSWORD must already be set, as in the local setup above.
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Open http://localhost:8025 to inspect emails. The app sends SMTP to `localhost:1025` without authentication or TLS. The dev profile enables callback notifications, checks pending requests every 5 seconds, and uses synthetic sender/recipient addresses. Submit a callback through the widget to trigger a generic notification; customer names and phone numbers remain in PostgreSQL. The Mailpit ports bind to localhost, and captured messages persist in the `mail_data` volume. If the application is later containerized, set `SPRING_MAIL_HOST=mailpit` instead of localhost. Use the dev profile only for local development.


Enable `MAIL_NOTIFICATIONS_ENABLED=true` and configure `STAFF_NOTIFICATION_TO`, `STAFF_NOTIFICATION_FROM`, `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` and SMTP TLS settings as appropriate. A scheduled poll locks pending callbacks and sends a generic email; names and phone numbers stay exclusively in the database. Staff retrieve pending callbacks via their existing restricted database access. Successful sends set `notified_at`; failed transactions leave requests pending for retry. Delivery is at least once, so a crash after sending may produce a duplicate generic notification. No callback listing endpoint is public.

## Verification

```bash
./mvnw test
./mvnw verify
# Optional behavioral evaluation against installed local models; never a hosted LLM:
./mvnw -Descape.eval=true -Dtest=LocalModelEvaluationTests test
```

Default tests use JUnit/Mockito and a real `pgvector/pgvector:pg16` Testcontainer, with mocked embedding responses and no LLM requests. They cover validation, domain database tools, consent, API limits, origin restrictions, cookie isolation, ingestion replacement and rollback. Docker is required; database integration tests are not silently skipped. The opt-in evaluation consumes `src/test/resources/eval/questions.csv` and checks tool calls, language/behavior markers, refusal, medical handoff, unknown facts, hours and prompt injection without asserting exact model wording. Changes to model, prompt or knowledge should pass this evaluation before release.

## Website deployment and privacy

Serve the application through the club's HTTPS origin (reverse proxy `/api`, `/widget.js`, `/widget.css`, or embed the page in a same-origin iframe). Same-origin cookies are required for memory. Default permitted origins are the club's www and apex HTTPS origins; customize `CLUB_ALLOWED_ORIGINS` only when needed. Set `COOKIE_SECURE=true` in production. Local same-origin browsing needs no CORS override. The in-process per-IP limiter permits 20 API writes per minute and caps JSON bodies at 16 KiB, including chunked bodies. It uses the socket IP and deliberately ignores client-controlled forwarding headers. Configure an additional trusted reverse-proxy limiter for multiple replicas and proxied client IPs.

**Personal data / Morocco law 09-08:** this implementation introduces a callback table containing names, phone numbers, preferred language and consent timestamps. Before public launch the club must establish its applicable data-protection process, consent/privacy notice, access controls and retention/deletion schedule. Do not collect medical history. Chat text is transient but still sent to the configured model; local Ollama is the default. Cloud inference is explicit opt-in via the `cloud` profile and `OLLAMA_CLOUD_MODEL`, subject to review of any personal-data transfer. Do not enable request-body, prompt, tool-argument or SQL bind-value logging in production. Do not put real customer data in tests.

## Dependencies

Lombok generates dependency constructors and entity getters/no-argument constructors. Maven annotation processing is configured for main and test sources; enable annotation processing in your IDE. Constructors with Spring `@Value` parameters are explicit. Keep DTOs as Java records and constructors with validation or transformation explicit.

Flyway starter and PostgreSQL module were added to manage relational and vector schema migrations reliably. Existing Spring AI, JPA, Mail, Validation and MVC dependencies provide the rest of the implementation. See [AGENTS.md](AGENTS.md) for repository rules.
