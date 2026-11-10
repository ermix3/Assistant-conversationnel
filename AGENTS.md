# AGENTS.md

Guidance for AI coding agents working on this repository. Read this fully before changing code.

## Project overview

An AI customer-support chatbot for **Escape Health Club** (https://www.escapehealthclub.com), a health and wellness club offering physiotherapy, fitness and performance, cryotherapy, spa and well-being, memberships and an "Escape Pass". The bot answers visitors' questions on the website, looks up official prices, and captures callback requests for the staff.

- **Scope:** questions about the club only. Anything else gets a polite refusal.
- **Languages:** French, English and Arabic/Darija. Always reply in the language the customer writes in.
- **Currency:** Moroccan dirham (written `dh`).
- **Human handoff:** phone `05 22 52 78 31`, email `contact@escapehealthclub.com`.

## Stack

- Java 21, Spring Boot, Maven (use the wrapper: `./mvnw`)
- Spring AI (chat, RAG, tool calling, chat memory)
- Ollama for local development (chat model `qwen3.5:latest`, embeddings `bge-m3`, 1024 dimensions)
- PostgreSQL with the pgvector extension (vector store + relational data)
- Spring Data JPA + Flyway for the schema
- Spring Mail for staff notifications

## Commands

```bash
# one-time: pull local models
ollama pull qwen3.5:latest
ollama pull bge-m3

# start Postgres/pgvector (also started automatically by Docker Compose Support)
# Set POSTGRES_PASSWORD in your environment first.
docker compose up -d

./mvnw spring-boot:run          # run the app on :8080
./mvnw test                     # unit tests
./mvnw verify                   # full build + integration tests
```

Quick manual test:

```bash
curl -X POST localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"dev-1","message":"Combien coute l abonnement mensuel ?"}'
```

## Architecture

```
Website widget -> POST /api/chat -> ChatService (Spring AI ChatClient)
                                        |-- QuestionAnswerAdvisor  -> pgvector (descriptive content)
                                        |-- MessageChatMemoryAdvisor -> conversation history
                                        '-- Tools (ClubTools)      -> prices table, callback requests
```

Suggested layout (base package `com.escape.chatbot`):

```
chat/        ChatController, ChatRequest, ChatService
agent/       ClubTools (@Tool methods), system prompt
knowledge/   IngestionService, knowledge loader
pricing/     Price entity + repository
leads/       CallbackRequest entity + repository + notifier
config/      ChatClient, CORS, rate limiting
src/main/resources/
  knowledge/ Markdown files, one per service (physio, fitness, cryo, spa, pass, faq)
  db/migration/ Flyway scripts (V1__..., V2__...)
  prompts/   system prompt text
```

## Hard rules (domain)

1. **Prices, opening hours and contact details never come from the model's memory or from vector search.** They come from the database through a tool (`getPrice`, etc.). If a price is not in the database, the bot says it does not have it and offers the staff contact.
2. **No medical advice.** Physiotherapy and cryotherapy have contraindications (pregnancy, heart conditions, and so on). The bot gives general descriptions only and sends health-specific questions to the staff.
3. **No invented facts.** If the retrieved context does not contain the answer, the bot says so and offers a callback, the phone number or the email.
4. **Stay on topic.** Refuse unrelated requests (coding help, politics, other businesses) briefly and politely.
5. **No booking promises.** There is no booking system. The bot can only record a callback request; it must never claim that an appointment is confirmed.
6. **Reply in the customer's language**, and keep answers short and friendly.

## Code conventions

- Constructor injection only; no field `@Autowired`.
- Use Java records for request/response DTOs. Validate input with Bean Validation (`@NotBlank`, `@Size`).
- Keep the system prompt in `src/main/resources/prompts/`, not hard-coded in Java.
- Each `@Tool` method needs a precise `description` (the model uses it to decide when to call it) and returns plain, short strings or small records.
- Tools that write data (e.g. callback requests) must validate arguments and never trust model-supplied values blindly (phone format, length limits).
- Schema changes go through Flyway migrations. Never edit an applied migration; add a new one.
- Configuration lives in `application.yml`; secrets come from environment variables, never from committed files.
- Prefer small, focused classes. Add Javadoc only where behaviour is non-obvious.

## Knowledge base and ingestion

- Descriptive content (what a treatment is, what to expect, the club's philosophy) lives as Markdown in `src/main/resources/knowledge/`, one file per service, with the language noted in the front matter.
- Ingestion chunks by heading, embeds with the configured embedding model, and stores in pgvector. It must be idempotent: re-running it replaces existing chunks rather than duplicating them.
- If the embedding model or its dimensions change, update `spring.ai.vectorstore.pgvector.dimensions` and re-embed everything.
- The public website is built with Elementor and loads some content in tabs, so scraping the rendered HTML misses content. Prefer the WordPress REST API (`/wp-json/wp/v2/pages`) or maintain the Markdown files by hand.

## Testing

- Unit-test tools and services with plain JUnit 5 and Mockito.
- Use Testcontainers (`pgvector/pgvector:pg16`) for repository and ingestion tests.
- Keep a small evaluation set in `src/test/resources/eval/` (question, expected behaviour). Include cases for: a price question, an out-of-scope question, a medical question, an unknown-fact question, and each supported language. Do not assert exact wording from the model; assert on behaviour (tool called, refusal given, contact offered).
- Do not call a hosted LLM from automated tests.

## Security and privacy

- Callback requests contain names and phone numbers. Store them only in the database, never log them in full, and add a consent line in the widget.
- Rate-limit `/api/chat` per IP and cap message length.
- Treat user input as untrusted: the system prompt must resist instructions to ignore its rules, and tools must never expose internal data.
- Restrict CORS to the club's website origin.
- Moroccan data protection law (09-08) applies; flag anything that stores personal data in a new way.

## Definition of done

- `./mvnw verify` passes.
- New behaviour has tests; the evaluation set still passes.
- No prices or contact details hard-coded in prompts or Java code.
- Flyway migration added for any schema change.
- README or this file updated if commands, structure or rules changed.

## Do not

- Do not commit API keys, passwords or real customer data.
- Do not bypass the price tool by putting prices into the system prompt or knowledge files.
- Do not add medical claims, guarantees of results, or discounts that are not in the database.
- Do not change the embedding model or dimensions without a re-embedding plan.
- Do not add dependencies without a clear need; mention the reason in the PR description.
