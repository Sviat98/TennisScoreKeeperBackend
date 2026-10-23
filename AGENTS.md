# AGENTS.md

Instructions for AI coding agents (ZCode, Claude Code, etc.) working with code in this repository.

The frontend is a separate repository, `TennisScoreKeeper` (Kotlin Multiplatform: Android / Desktop / Wasm).

## Build & Run Commands

| Command | Description |
|---------|-------------|
| `./gradlew run` | Start the server (port 8080) |
| `./gradlew build` | Build the project |
| `./gradlew test` | Run all tests |
| `./gradlew buildFatJar` | Build executable JAR with all dependencies |

JVM toolchain: **JDK 21**. Framework: **Ktor 3.5.0** with Kotlin 2.3.21, Exposed 1.3.0, Koin 4.1.1.

API docs (Swagger UI) are served at `/swagger` when the server is running.

## Environment Variables

The app requires these env vars at runtime (configured in `application.yaml`):
- `DB_USERNAME`, `DB_PASSWORD`, `DB_BASE_URL`, `DB_NAME` — PostgreSQL connection
- `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_REALM` — JWT authentication

## Architecture

**Ktor + Koin + Exposed ORM + PostgreSQL**, running on Netty.

Base package: `com.bashkevich.tennisscorekeeperbackend` (source in `src/main/kotlin`).

### Request Flow

Routes (`feature/*/XxxRoutes.kt`) → Service classes (`feature/*/XxxService.kt`) → Repository classes (`feature/*/XxxRepository.kt`) → Exposed DAO entities/tables (`model/`)

### Key Patterns

- **Feature modules**: Each domain concept lives in `feature/` as a self-contained package (auth, match, player, tournament, participant, counter, set_template, match_log, theme). Each feature typically has Route, Service, and Repository files.
- **Singles vs Doubles**: Both the `match` and `participant` features split into `singles/` and `doubles/` subdirectories with parallel Service/Repository pairs. `MatchServiceRouter` dispatches to the correct one based on tournament type.
- **Koin DI**: Each feature has a corresponding Koin module in `di/XxxModule.kt`. All are registered in `plugins/Frameworks.kt`.
- **Database**: PostgreSQL with Exposed DAO. Tables are auto-created on startup in `plugins/Databases.kt` via `SchemaUtils.create()`. All DB queries go through the `dbQuery` helper (wraps `newSuspendedTransaction`). Schema name: `tennis_score_keeper`. Sequences `match_seq` and `participant_seq` (constants `MATCH_SEQUENCE` / `PARTICIPANT_SEQUENCE`) generate cross-table IDs.
- **WebSocket**: Real-time match updates via `/matches/{id}` WebSocket (`feature/match/websocket/`). `MatchConnectionManager` tracks connections; `MatchObserver` (SharedFlow) broadcasts match state changes. Services call `MatchObserver.notifyChange()` after mutations. A heartbeat request is answered by re-sending the current match snapshot (see `isHeartbeatRequest`).
- **Auth**: JWT-based. Access tokens expire in 5 min; refresh tokens expire daily at 3:00 AM. Device-specific tokens with UUID tracking. Auth-protected routes use `authenticate(JWT_AUTH)`.
- **Undo/Redo**: Match scoring uses a `pointShift` field on the match entity to implement undo/redo without deleting log rows. Negative shift = undone points; restoring them increments back toward 0.
- **Theme AI**: The `theme` feature generates app color themes. AI endpoints (`/themes/ai`, legacy `/themes/ai/old`, `/themes/ai/describeMatch`) use Koog for LLM calls and `PaletteExtractor` (ColorThief MMCQ quantization) for extracting palettes from images. Do NOT "upgrade" the `color-thief` dependency to the `io.github.sundenjaeger` republish — it is Java 25 bytecode and won't load on the JDK 21 toolchain (see the comment in `gradle/libs.versions.toml`).

### Plugin Configuration (`plugins/`)

Each Ktor plugin is configured in its own file and wired in `Application.module()`. Key files:
- `Databases.kt` — DB connection + schema init + `dbQuery` helper
- `Authentication.kt` — JWT validation
- `Routing.kt` — Top-level route registration
- `Frameworks.kt` — Koin module setup
- `ErrorHandling.kt` — Custom error responses
- `Sockets.kt` — WebSocket plugin config
- `Scheduler.kt` — Scheduled tasks (e.g., token cleanup)
- `Cors.kt` — CORS allowlist. Allowed origins include `http://127.0.0.1:8080` / `:8081`, `https://tennisscorekeeper.onrender.com`, `https://tennisscorekeeper.tech`; `http://localhost:8080` is NOT allowed and gets 403 on WebSocket upgrades — the frontend must use `127.0.0.1`
- `Serialization.kt` — JSON content negotiation config
- `Validation.kt` — Request validation
- `Monitoring.kt` — Metrics
- `Swagger.kt` — Swagger UI at `/swagger`

### Model Layer (`model/`)

Contains Exposed `IdTable` definitions and `IntEntity`/`IntEntityClass` DAOs. DTOs and serialization mappings (`toXxxDto()` extension functions) also live here, typically as top-level functions in the model package.

### API Structure

- `/tournaments/{id}/matches` — Match CRUD per tournament
- `/matches/{id}` — Match operations (score updates, status changes, undo/redo, video link)
- `/matches/{id}` WebSocket — Real-time match state
- `/auth/*` — Login, register, token refresh
- `/players`, `/tournaments`, `/participants`, `/set-templates`, `/counter` — Resource CRUD
- `/themes`, `/themes/ai` (+ `/ai/old`, `/ai/describeMatch`) — Theme palettes and AI theme generation

## Repo Root Notes

One-off SQL data-migration scripts live at the repo root (`Перенос *.sql`, `пример переноса между базами release-debug.sql`) and in `scripts/` (`delete_tournament.sql`, `excel-draws/`). They are not part of the application runtime — do not wire them into the app. The `*.xlsx` files at the root are data sources for those scripts.

## Code Style Rules

- Do not introduce named constants for one-off values (error message texts included) and do not use `companion object` to hold them — write literals and single-use values inline at the use site.
- Always declare data classes in their own file under the appropriate `model/` package, non-private — never as private/local classes inside service or route files.
