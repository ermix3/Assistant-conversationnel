# Escape Health Club assistant

Java 21 / Spring Boot 4.1 / Spring AI 2.0, Ollama and PostgreSQL + pgvector. The assistant answers club questions in French, English and Arabic/Darija, uses database tools for prices/contact/hours, and retrieves descriptive Markdown with RAG.

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

`GET /api/health` reports liveness; `/actuator/health` includes configured dependency indicators. Validation failures return `INVALID_REQUEST`, dependency/model failures `SERVICE_UNAVAILABLE`, excessive requests HTTP 429 with `Retry-After`, oversized bodies HTTP 413. Error bodies never echo submitted values or internal diagnostics.

## Official data

Flyway owns all tables, vector schema and indexes. `V2` seeds 22 price entries from the [official WordPress menu](https://www.escapehealthclub.com/wp-json/wp/v2/pages/18), reviewed 2026-11-10. Contact details are seeded in `club_facts`; opening hours are deliberately absent because no official hours were verified. Staff must review prices before public launch and maintain authoritative values through new migrations or an authorized database workflow. Unknown prices/hours produce a handoff, never an estimate. No prices, phone numbers or email addresses are embedded in the prompt, Java or knowledge files.

Knowledge Markdown is manually curated from the official site, with provenance in front matter. It excludes medical claims and monetary/contact facts. `ClubTools` exposes a service catalog, exact price lookup, contact and hours. Every chat request executes the official-information tool before generation and supplies its results to the model, so required data retrieval does not depend on optional model tool calls. The system prompt is in `src/main/resources/prompts/system.txt`. No model output should be treated as medical advice or an appointment confirmation.

## Verification

```bash
./mvnw test
./mvnw verify
# Widget interaction tests (Node.js 18+):
node --test src/test/js/widget.test.cjs
# Optional behavioral evaluation against installed local models; never a hosted LLM:
./mvnw -Descape.eval=true -Dtest=LocalModelEvaluationTests test
```

Default tests use JUnit/Mockito and a real `pgvector/pgvector:pg16` Testcontainer, with mocked embedding responses and no LLM requests. They cover validation, domain database tools, API limits, origin restrictions, cookie isolation, ingestion replacement and rollback. Docker is required; database integration tests are not silently skipped. The opt-in evaluation consumes `src/test/resources/eval/questions.csv` and checks tool calls, language/behavior markers, refusal, medical handoff, unknown facts, hours and prompt injection without asserting exact model wording. Changes to model, prompt or knowledge should pass this evaluation before release.

## Website deployment and privacy

A centered Escape Health Club welcome introduces the club and provides a direct invitation to open the assistant. The home page is a full-height off-white canvas with a subtle violet contour pattern matching the chatbot button and a floating chatbot button in the bottom-right corner. Click it to open or minimize the conversation; the header button and Escape key also close it. Drafts and the current conversation remain available when reopening the widget. The panel adapts to mobile screens and supports French, English and Arabic.

Serve the application through the club's HTTPS origin (reverse proxy `/api`, `/widget.js`, `/widget.css`, or embed the page in a same-origin iframe). Same-origin cookies are required for memory. Same-origin browser requests are accepted, including local pages at `http://localhost:8080` and `http://127.0.0.1:8080`. Cross-origin requests are restricted to the club's www and apex HTTPS origins by default; customize `CLUB_ALLOWED_ORIGINS` only when needed. Set `COOKIE_SECURE=true` in production. The in-process per-IP limiter permits 20 API writes per minute and caps JSON bodies at 16 KiB, including chunked bodies. It uses the socket IP and deliberately ignores client-controlled forwarding headers. Configure an additional trusted reverse-proxy limiter for multiple replicas and proxied client IPs.

**Personal data / Morocco law 09-08:** chat text is transient but still sent to the configured model; local Ollama is the default. Cloud inference is explicit opt-in via the `cloud` profile and `OLLAMA_CLOUD_MODEL`, subject to review of any personal-data transfer. Do not enable request-body, prompt, tool-argument or SQL bind-value logging in production. Do not put real customer data in tests.

## Dependencies

Lombok generates dependency constructors and entity getters/no-argument constructors. Maven annotation processing is configured for main and test sources; enable annotation processing in your IDE. Constructors with Spring `@Value` parameters are explicit. Keep DTOs as Java records and constructors with validation or transformation explicit.

Flyway starter and PostgreSQL module were added to manage relational and vector schema migrations reliably. Existing Spring AI, JPA, Validation and MVC dependencies provide the rest of the implementation. See [AGENTS.md](AGENTS.md) for repository rules.
