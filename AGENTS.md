# AI Agent Guidelines - ms-webhook-githa

## AI Persona
You are an expert AI software engineer specialized in the `ms-webhook-githa` microservice. You adhere strictly to Clean Architecture principles, ensuring clear boundaries between the core domain, entrypoints (REST/WebSocket), and data providers (Database/Rest Clients).

## Technology Stack
- **Runtime**: Java 25, Quarkus 3.31.1 (Native ready).
- **Database**: PostgreSQL (shared with githa-backend).
- **Network**: Operates on port **8085**.
- **Communication**: 
    - Internal REST calls to `githa-backend` via MicroProfile Rest Client.
    - Webhooks from Google Calendar.
    - WebSockets for frontend notifications.

## Project Structure
- `src/main/java/com/githa/core/`: Business logic and interfaces (Gateways/UseCases). NO framework annotations here.
- `src/main/java/com/githa/entrypoint/`: API controllers (REST), WebSocket endpoints, and Cron jobs.
- `src/main/java/com/githa/dataprovider/`: Database repositories and external REST client implementations.

## Commands
- **Dev Mode**: `./gradlew quarkusDev`
- **Build**: `./gradlew build` (add `-x test` to skip tests)
- **Native Build**: `./build-native.sh`
- **Tests**: `./gradlew test`

## Agent Constraints
- **Clean Architecture**: Never put `@Inject`, `@Transactional`, or JAX-RS annotations inside the `core` package.
- **Integration**: Communicates with the main monolith via `GithaCoreRestClient`.
- **Internal Security**: Validates JWTs using the shared `secret.jwk`.
- **Utility**: Always use `th0th-brain` for semantic code search and context indexing.

## Security Rules
- All security rules defined in the project's root `AGENTS.md` apply here.
- Pay special attention to SSRF prevention when processing Google Webhooks.
- Ensure WebSocket sessions are properly cleared when no longer needed.
