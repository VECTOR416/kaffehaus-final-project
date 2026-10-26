# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

KaffeHaus v4 is the REST backend for a café management system (inventory, sales/cash register, orders, staff). Java 21, Spring Boot 4.1.1 (Spring MVC, Spring Security 7 + own JWTs, Data JPA/Hibernate), PostgreSQL 16.

**`arquitectura.md` is the architectural source of truth** (in Spanish). It is a template of the architecture with no business rules: section 4 describes each backend piece, section 5 the reusable patterns, section 6 the procedures to add or extend modules, section 7 ("Reglas para la IA") the hard rules, 7.4 the known deviations that must not be replicated or "fixed" unless asked, and 7.5 the known architectural gaps (e.g. schema objects outside the entities are not versioned). Read the relevant section before adding or changing a slice.

todos los cambios arquitectonicos que propongas y que no esten definidos en el archivo arquitectura.md deben ser explicados al usuario y deben tener su la aprobacion de este usuario para poder alterar el archivo `arquitectura.md` de esta forma no alteraras la fuente de la verdad de la arquitectura.

**`.rules/logica.md` es la fuente de la verdad de la logica de negocio** (decisiones de diseno numeradas y agrupadas por agregado). Reglas obligatorias:

- Antes de cualquier cambio de codigo, lee las decisiones de los agregados afectados y verifica que el cambio no contradiga ninguna.
- Las decisiones de `logica.md` no pueden contradecirse entre si. Si un cambio pedido, el codigo existente o `arquitectura.md` entran en conflicto con una decision, **detente y consulta al usuario** como resolverlo; no elijas tu una solucion. Solo despues de su respuesta actualiza `logica.md` (y el codigo).
- Toda regla de negocio nueva o modificada que se acuerde con el usuario se registra en `logica.md` en la seccion de su agregado, con el siguiente numero libre. Los identificadores (`<AGREGADO>-NN`) son estables: no se renumeran ni se reutilizan; una decision que se retira se marca como derogada indicando la que la sustituye.
- Los conflictos ya detectados y aun sin resolver estan en la seccion "Pendientes de decision" de `logica.md`; no los resuelvas por tu cuenta.

## Commands

On Windows use `mvnw.cmd` (or `./mvnw` from Git Bash).

```bash
cp .env.template .env                # once; fill in DB credentials and JWT secrets (.env is git-ignored)
docker compose up -d                 # PostgreSQL on ${DB_PORT}, credentials from .env
./mvnw spring-boot:run               # API on port ${SERVER_PORT}
./mvnw test                          # all tests
./mvnw test -Dtest=GuardiaDeRolesTest                 # one class
./mvnw test -Dtest=GuardiaDeRolesTest#nombreDelMetodo  # one method
./mvnw package -DskipTests
```

First run against an empty DB: start the app with `SEED_HABILITADO=true` and `SEED_ADMIN_CONTRASENA=<password>` (`ddl-auto: update` creates tables, then `src/main/resources/schema.sql` adds the sequence, partial unique indexes, CHECKs and FKs) and call `POST /api/v1/seed` once (idempotent; public only while `SEED_HABILITADO=true`, which defaults to `false`; 409 if the admin does not exist yet and no password is configured). It seeds roles `Administrador`/`Operador`, the admin `admin@gmail.com` with that password, catalogs resolved by name, units/conversions, and the 'Mostrador' client. Restart without `SEED_HABILITADO` afterwards.

All configuration comes from environment variables; `src/main/resources/application.yaml` has no hardcoded values. `application.yaml` imports `.env` from the working directory (`spring.config.import: optional:file:.env[.properties]`, so `KEY=value` without quotes); OS environment variables take precedence. `docker-compose.yaml` and the VS Code launch config read the same `.env`. `.env.template` is the versioned list of variables. Required (startup fails without them): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_LOCK_TIMEOUT` (PostgreSQL `lock_timeout`), `SERVER_PORT`, `JWT_SECRET`, `JWT_REFRESH_SECRET` (>= 32 chars, distinct), `JWT_ISSUER`, `JWT_ACCESS_MINUTES`, `JWT_REFRESH_DAYS`, `CORS_ORIGENES` (comma-separated origin patterns; bare `*` is rejected at startup). Optional: `GOOGLE_CLIENT_ID` (empty disables Google login), `SEED_HABILITADO` (defaults to `false`, SIEMBRA-02), `SEED_ADMIN_CONTRASENA`. Tests do not read `.env`: `application-test.yaml` sets fixed test values.

Tests mirror the production package under `src/test/java/com/maestria/kaffehausv4/`. Unit tests (JUnit 5 + Mockito, no Spring context, no DB) use the shared fixtures in `Soporte/DatosDePrueba`. Persistence tests (`*IntegracionTest`) extend `Soporte/PruebaConBaseDeDatos`: full context, profile `test`, H2 in memory in PostgreSQL mode (`src/test/resources/application-test.yaml` plus `h2/esquema-postgresql.sql` for the ticket sequence and the `movimientos` CHECKs; FKs are not reproduced), each test rolled back; `Soporte/EscenarioDeCafeteria` builds scenarios through the real use cases. H2 does not reproduce PostgreSQL partial unique indexes or row locks, so those are not covered.

## Architecture

Clean Architecture + tactical DDD, organized as **Bounded Contexts → vertical slice per aggregate**, each slice containing its own four layers:

```
com.maestria.kaffehausv4.BoundedContexts.<Contexto>.<Agregado>.{Domain, Application, Infrastructure, Presentation}
```

Contexts: `Inventario`, `Ventas`, `Personal`, `Pedidos`. Plus a cross-cutting `Shared` kernel (errors, `Validador`, pagination, password hashing, seeding, and all security). Package names are PascalCase on purpose.

`Inventario/Tipo` is the reference slice: copy it literally when creating a new aggregate (procedure in arquitectura.md 6.1).

### Dependency rule and the Repository → Datasource double indirection

- `Domain` imports nothing from Spring/Jakarta/Infrastructure. `Application` may only use `@Service` and `@Transactional`.
- Each aggregate has two ports with identical signatures: `Application/Repositories/I<X>Repository` (what use cases inject) and `Application/Datasources/I<X>Datasource`. `Infrastructure/Repositories/<X>RepositoryImpl` is pure delegation; `Infrastructure/Datasources/<X>DatasourceImpl` does the real JPA access, mapping, and pagination. Keep this even though it looks redundant.
- Aggregates never carry JPA annotations. `<X>JpaModel` + static hand-written `<X>JpaMapper` (`toPersistence` generates the UUID and `fecC`, `copiarCambios` sets `fecU`, `toDomain` calls `<X>.restaurar`).
- Cross-aggregate composition happens only in use cases (inject the other aggregate's `I*Repository`). Relationships are flat `UUID` columns; no JPA associations anywhere.

### Request flow

Controller receives a raw `@RequestBody Map<String, Object>` → `Crear<X>Request.createFromRequest(map[, idUsuarioEnSesion])` validates with `Shared/Application/Validation/Validador` → `<Verbo><X>UseCase.execute(...)` (`@Transactional`, single public method) → repository → datasource → `<X>Response.createFromAggregate(...)`. Lists use `skip/limit/search` query params, `Obtener<Plural>Request extends PaginationDTO`, `OffsetPageRequest` (never `PageRequest.of`), and return `PaginatedResponse`.

Errors: throw `DomainException.throwException(codigo, "mensaje")` (returns the exception, so it fits `.orElseThrow(() -> ...)`); `codigo` is the HTTP status. `GlobalExceptionHandler` renders `ErrorResponse`. Messages in Spanish without accents.

### Security

Lives entirely in `Shared/Presentation/Security`; `org.springframework.security` and `io.jsonwebtoken` must not appear in any aggregate. Only `/api/v1/auth/**` (login, google, refresh) and `GET /api/v1/salud` (public health check: `UP`/`DOWN` for the system and the DB, 200/503; arquitectura.md 4.22) are open. Every aggregate controller must carry `@RequiereRol({"Operador", "Administrador"})` at class level; a method-level `@RequiereRol` overrides it. Roles are rows in the `rol` table, never enums/constants. The session user id is injected with `@UsuarioEnSesion String idUsuario` and passed into `createFromRequest`; the client never sends `idUsuario`. Passwords use `IHasheadorDeContrasena` (PBKDF2), not `PasswordEncoder`.

### Non-obvious domain mechanics

- **Soft delete only.** Every table/model/aggregate has `borrado`; reads filter `...BorradoFalse...`; `DELETE` endpoints persist an UPDATE. No `deleteById`/`deleteBy...` in `src/main`.
- **Aggregates with child entities** (`Venta`, `Lote`, `Receta`, `SesionCaja`): children live in the root's `Domain/Aggregates/` package, have no repository/use case/controller, and are mutated only through root methods. A `PUT` with child collections reconciles by id (update / add / soft-delete missing) rather than replacing. Derived values (totals, subtotals, step sequence) are computed by the root, never accepted from the client. `RecetaDatasourceImpl` writes steps in two phases because of a partial unique index. Details in arquitectura.md 4.15, 5.8 and 5.9.
- **Internal use cases** (no controller, invoked by other use cases inside their transaction): `RegistrarSalidaDeInventarioUseCase` (FEFO consumption), `RegistrarAperturaUseCase`, `RecalcularStockUseCase`, `RegistrarReintegroDeInventarioUseCase`, `RegistrarAnulacionDeVentaUseCase`. Don't add new ones without documenting them in arquitectura.md 4.6.
- **Locking:** reads that precede an inventory decrement use `@Lock(PESSIMISTIC_WRITE)` on derived methods prefixed `read...By`, exposed in ports as `...ParaActualizar` / `...ParaConsumo`; the cash-session read for a sale uses `PESSIMISTIC_READ` with `get...By` / `...ParaVenta`.

### Implementation constraints worth remembering

- No new `pom.xml` dependencies (no Lombok, MapStruct, Bean Validation, Swagger, loggers); the only exception is H2 with test scope. No logging at all.
- No `@Query`/JPQL/native SQL/Criteria: derived query methods only. No `@GeneratedValue`. `BigDecimal` for money and quantities.
- Constructor injection only; aggregates and business DTOs have private constructors + static factories, no setters.
- No new folders inside a slice; the structure in arquitectura.md 3.1 is closed.
- Identifiers and comments in Spanish without accents; ports prefixed with `I`; controllers end in `ControllerV1` under `api/v1/<kebab-case>`.
