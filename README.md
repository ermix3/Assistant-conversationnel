# Agent Fitness

Escape Health Club support chatbot. See [AGENTS.md](AGENTS.md) for setup and development commands.

## Health checks

```bash
curl http://localhost:8080/api/health
```

Returns HTTP 200 with `{"status":"UP"}` when the application can serve requests.
This liveness endpoint does not check PostgreSQL or Ollama. Use the existing
`/actuator/health` endpoint for the health indicators configured by Spring Boot.
