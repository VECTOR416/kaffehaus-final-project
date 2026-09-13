# Propuesta Técnica: Integración de Keycloak en KaffeHaus v4

> **Documento de diseño.** No contiene código listo para pegar: describe *dónde* va cada pieza y *por qué*, alineado estrictamente con la arquitectura observada en
> `src/main/java/com/maestria/kaffehausv4/BoundedContexts/Personal/Entidad`.
>
> **Principio rector:** Keycloak es un detalle de infraestructura. La capa `Domain` no debe ganar ni un solo `import` nuevo, y la capa `Application` solo puede depender de *puertos* propios, nunca de Spring Security.

---

## 0. Punto de partida (estado real del proyecto)

| Elemento | Estado actual | Implicación |
|---|---|---|
| `spring-boot-starter-security` | Ya está en `pom.xml` | Al arrancar, Spring Security **ya protege todo** con el login por defecto. No es un "añadido", es algo que hay que *configurar*. |
| Starter de OAuth2 Resource Server | **Ausente** | Hay que añadirlo (ver §1.4). |
| Keycloak | Definido en `docker-compose.yaml`, `start-dev`, puerto `8080` | Colisiona con el puerto por defecto de Spring Boot (ver §1.5). |
| `application.yaml` | Solo contiene `spring.application.name` | Falta toda la configuración: datasource y ahora también el issuer de Keycloak. |
| Tabla `usuario` | Tiene columna `contrasena VARCHAR(255)` y FK `id_rol → rol(id)` | Las credenciales se mudan a Keycloak; esa columna queda obsoleta (ver §3.4). |
| `GlobalExceptionHandler` | `@RestControllerAdvice` con record `ErrorResponse(codigo, tipo, mensaje, timestamp)` | Los errores 401/403 **no pasan por él** por defecto (ver §2.4). Es el punto más fácil de romper la consistencia del contrato HTTP. |

---

## 1. Ubicación Arquitectónica

### 1.1 Regla de oro

La seguridad es **transversal**: aplica igual a `Entidad`, `Cliente`, `Proveedor`, `Usuario` y a todo Bounded Context futuro. Por tanto **no debe vivir dentro de ningún Bounded Context**, sino en `Shared/`.

Dentro de `Shared/`, la seguridad es **Infraestructura**: depende de Spring Security, de JWT y de un servidor externo. Hoy `Shared/` solo tiene tres capas (`Application`, `Domain`, `Presentation`); esta propuesta **añade la cuarta**, respetando el mismo esquema de 4 capas que ya usan los Bounded Contexts.

### 1.2 Estructura de paquetes propuesta

```
com.maestria.kaffehausv4.Shared
├── Application/
│   ├── Dtos/Pagination/          (existente)
│   ├── Validation/               (existente)
│   └── Security/                 ◄── NUEVO — PUERTOS (interfaces)
│       └── IContextoDeSeguridad.java
│
├── Domain/
│   ├── Error/                    (existente)
│   │   └── AutorizacionException.java   ◄── NUEVO — hermana de DomainException
│   └── Security/                 ◄── NUEVO — VALUE OBJECTS puros
│       ├── UsuarioAutenticado.java
│       └── RolDeSistema.java     (enum)
│
├── Infrastructure/               ◄── NUEVO PAQUETE (4ª capa de Shared)
│   └── Security/
│       ├── SecurityConfig.java              @Configuration — la cadena de filtros
│       ├── KeycloakJwtAuthenticationConverter.java   Jwt → Authentication
│       ├── KeycloakRolesConverter.java      claims → GrantedAuthority
│       └── ContextoDeSeguridadImpl.java     @Component — adaptador del puerto
│
└── Presentation/
    ├── GlobalExceptionHandler.java   (existente)
    └── Security/                 ◄── NUEVO — traducción de errores a HTTP
        ├── JwtAuthenticationEntryPoint.java   (401)
        └── JwtAccessDeniedHandler.java        (403)
```

### 1.3 Justificación pieza por pieza

**`Shared/Infrastructure/Security/SecurityConfig.java`**
Es el único lugar donde aparecen `SecurityFilterChain`, `HttpSecurity`, `JwtDecoder` y `oauth2ResourceServer`. Es el equivalente conceptual de `EntidadRepositoryImpl`: un adaptador que conecta el mundo exterior con la aplicación, anotado con estereotipo de Spring (`@Configuration`, igual que `@Repository`/`@Service` en el resto del proyecto).

**`Shared/Domain/Security/UsuarioAutenticado.java`**
Un POJO **inmutable y sin anotaciones**, exactamente como `EntidadId`:

- campos `private final` (`subject`, `correo`, `roles`),
- constructor + getters,
- cero imports de Spring, cero imports de `org.springframework.security.oauth2.jwt.Jwt`.

Este objeto es el que circula por la capa `Application` cuando un caso de uso necesita saber *quién* está actuando. Que el token sea JWT, opaco o SAML es irrelevante para el dominio — exactamente el mismo razonamiento por el que `Entidad` no sabe que existe `EntidadesJpaModel`.

**`Shared/Application/Security/IContextoDeSeguridad.java`**
Puerto con prefijo `I`, ubicado en `Application/` — **no en `Domain/`**. Esto no es arbitrario: replica literalmente la decisión ya tomada en el proyecto, donde `IEntidadRepository` e `IEntidadDatasource` viven en `Application/Repositories` y `Application/Datasources`, y no en `Domain`.

Contrato sugerido (métodos en español, como `crear` / `actualizar` / `obtenerPorId`):

```
UsuarioAutenticado obtenerUsuarioActual();
boolean tieneRol(RolDeSistema rol);
```

**`Shared/Infrastructure/Security/ContextoDeSeguridadImpl.java`**
Adaptador `@Component` que lee `SecurityContextHolder`, extrae el `Jwt` y lo convierte en `UsuarioAutenticado`. Es el espejo exacto de `EntidadRepositoryImpl`: implementa el puerto y encapsula la tecnología.

> **Por qué importa esta indirección.** Cuando llegue el requisito de auditoría (`fec_c`, `fec_u`, y un futuro `usuario_creacion`), el caso de uso va a necesitar el usuario actual. Sin este puerto, la tentación será importar `SecurityContextHolder` dentro de `CrearEntidadUseCase` — y eso ata la capa `Application` a Spring Security de forma irreversible. Con el puerto, `CrearEntidadUseCase` recibe `IContextoDeSeguridad` por constructor, igual que hoy recibe `IEntidadRepository`, y sigue siendo testeable con un doble de prueba.

**Lo que NO se debe hacer** (anti-patrones que romperían esta arquitectura):

- ❌ Poner `SecurityConfig` en `com.maestria.kaffehausv4.Config` o en la raíz del proyecto: rompe el esquema de 4 capas.
- ❌ Crear un Bounded Context `Keycloak` o `Auth` bajo `BoundedContexts/`: Keycloak no es un concepto del negocio de la cafetería.
- ❌ Anotar el agregado `Entidad` o el VO `EntidadId` con algo de Spring Security.
- ❌ Importar `org.springframework.security.*` en cualquier clase bajo `Application/UseCases/`.
- ❌ Usar el adaptador `keycloak-spring-boot-starter` (adaptador legacy de Keycloak, descontinuado). Lo correcto es el soporte nativo de **Spring Security OAuth2 Resource Server**.

### 1.4 Dependencia a añadir en `pom.xml`

⚠️ **Atención al nombre del artefacto.** El proyecto usa Spring Boot **4.1.1**, que renombró los starters (por eso el `pom.xml` ya dice `spring-boot-starter-webmvc` y no `spring-boot-starter-web`). El artefacto correcto **no** es el clásico `spring-boot-starter-oauth2-resource-server`, sino:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

Verificado contra los artefactos de la línea Boot 4 presentes en el repositorio local `~/.m2`. La mayoría de tutoriales en internet usan el nombre antiguo de Boot 3 y fallarán al resolver la dependencia.

### 1.5 Configuración en `application.yaml`

La URL del issuer va en configuración externa, **nunca hardcodeada** en una clase Java:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/kaffehaus}
```

Con `issuer-uri`, Spring descubre automáticamente el JWKS y valida firma, `exp`, `iss` y `nbf`. No hay que escribir ni una línea de criptografía.

> **Conflicto de puertos a resolver.** El `docker-compose.yaml` publica Keycloak en `8080:8080`, que es también el puerto por defecto de Spring Boot. Hay que decidir uno de los dos: mover la app a `server.port: 8081` en `application.yaml`, o publicar Keycloak como `8081:8080`. Recomiendo mover la app, para que la URI del issuer sea idéntica dentro y fuera de Docker.

---

## 2. Estrategia de Implementación (acoplamiento sin modificar los Controladores)

### 2.1 El principio: la seguridad ocurre *antes* del controlador

El `EntidadControllerV1` actual **no cambia ni una línea**. Esto es posible porque Spring Security actúa como un `Filter` de Servlet que se ejecuta antes del `DispatcherServlet`:

```
HTTP Request  ( Authorization: Bearer eyJhbGc... )
     ↓
┌──────────────────────────────────────────────────┐
│  SecurityFilterChain   (Shared/Infrastructure)   │
│   1. BearerTokenAuthenticationFilter             │
│      → extrae el token de la cabecera            │
│   2. JwtDecoder                                  │
│      → valida firma contra el JWKS de Keycloak   │
│      → valida exp / iss / nbf                    │
│   3. KeycloakJwtAuthenticationConverter          │
│      → realm_access.roles → ROLE_*               │
│   4. AuthorizationFilter                         │
│      → ¿este rol puede hacer este método+ruta?   │
└──────────────────────────────────────────────────┘
     ↓  (si todo pasa)
EntidadControllerV1.actualizar(...)     ← INTACTO
     ↓
ActualizarEntidadUseCase.execute(...)   ← INTACTO
     ↓
EntidadRepositoryImpl → Datasource → JPA
```

Cuando la petición llega a `EntidadControllerV1`, el token ya fue validado. El controlador sigue haciendo exactamente lo que hace hoy: construir el DTO con `ActualizarEntidadRequest.createFromRequest(body)` y delegar al caso de uso.

### 2.2 Opción recomendada: reglas centralizadas por ruta

Dado que el proyecto ya tiene una convención de rutas **perfectamente regular** (`api/v1/entidad`, `api/v1/cliente`, `api/v1/proveedor`, todas en singular, todas con los mismos 5 verbos), las reglas se pueden expresar de forma declarativa en `SecurityConfig` sin tocar ningún controlador:

| Método | Patrón de ruta | Rol mínimo |
|---|---|---|
| `GET` | `/api/v1/**` | `EMPLEADO` |
| `POST`, `PUT` | `/api/v1/**` | `SUPERVISOR` |
| `DELETE` | `/api/v1/**` | `ADMINISTRADOR` |
| `GET` | `/actuator/health` | público |

Esquema de la configuración (stateless, sin sesión, sin CSRF — es una API REST consumida por token):

```
http
  .csrf(csrf -> csrf.disable())
  .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
  .authorizeHttpRequests(auth -> auth
      .requestMatchers(GET,    "/api/v1/**").hasRole("EMPLEADO")
      .requestMatchers(POST,   "/api/v1/**").hasRole("SUPERVISOR")
      .requestMatchers(PUT,    "/api/v1/**").hasRole("SUPERVISOR")
      .requestMatchers(DELETE, "/api/v1/**").hasRole("ADMINISTRADOR")
      .anyRequest().authenticated())
  .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
      jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)))
  .exceptionHandling(ex -> ex
      .authenticationEntryPoint(jwtAuthenticationEntryPoint)
      .accessDeniedHandler(jwtAccessDeniedHandler));
```

**Ventaja:** acoplamiento cero. Los 5 controladores existentes y todos los futuros quedan protegidos sin editarlos, y las reglas de autorización se leen en un solo archivo.

### 2.3 Opción complementaria: `@PreAuthorize` cuando la regla es específica

Si un endpoint concreto necesita una regla distinta a la general (por ejemplo, que `POST /api/v1/entidad` sea exclusivo de `ADMINISTRADOR` aunque el resto de POST sean de `SUPERVISOR`), se activa `@EnableMethodSecurity` en `SecurityConfig` y se anota **únicamente el método del controlador**:

```java
@PreAuthorize("hasRole('ADMINISTRADOR')")
@PostMapping
public ResponseEntity<EntidadResponse> crear(@RequestBody Map<String, Object> body) { ... }
```

Esto añade **una anotación declarativa** y no modifica el cuerpo del método: la lógica interna (`createFromRequest` → `execute` → `ResponseEntity.status(CREATED)`) permanece idéntica.

> **Criterio de decisión:** usar §2.2 como base para todo, y §2.3 solo como excepción puntual. Anotar los 25 métodos de los 5 controladores sería duplicación innecesaria.
>
> **Dónde NO poner `@PreAuthorize`:** nunca en los `UseCase`. Aunque técnicamente funcionaría sobre `@Service`, ataría la capa `Application` a Spring Security y contradiría la separación que el proyecto mantiene hoy de forma limpia.

### 2.4 Punto crítico: consistencia del formato de error

Este es el detalle que más fácilmente rompe el contrato de la API. El `GlobalExceptionHandler` existente responde siempre con:

```json
{ "codigo": 404, "tipo": "Domain Exception", "mensaje": "...", "timestamp": "..." }
```

Pero un 401 (token ausente o inválido) o un 403 (rol insuficiente) se producen **dentro de la cadena de filtros**, antes de que exista un controlador. Por tanto **`@RestControllerAdvice` nunca los ve**, y Spring Security devolvería una respuesta vacía con cabecera `WWW-Authenticate` — un formato completamente distinto al del resto de la API.

**Solución:** implementar dos clases en `Shared/Presentation/Security/` que serialicen exactamente el mismo record `ErrorResponse`:

- `JwtAuthenticationEntryPoint implements AuthenticationEntryPoint` → `401`, `tipo: "Authorization Exception"`, mensaje `"Token de acceso ausente o invalido"`.
- `JwtAccessDeniedHandler implements AccessDeniedHandler` → `403`, `tipo: "Authorization Exception"`, mensaje `"No cuenta con los permisos necesarios para esta operacion"`.

Ambas escriben el JSON directamente sobre el `HttpServletResponse` usando el `ObjectMapper`. Los mensajes van **en español y sin tildes**, respetando la convención observada en `Validador` y en los `DomainException.throwException(404, "La entidad con id '...' no existe")`.

**Complemento en la jerarquía de errores.** Para los casos de autorización que sí ocurren *dentro* de un caso de uso (ej.: "un usuario solo puede modificar entidades de su propia sucursal"), se añade una tercera hermana junto a `DomainException` y `SystemException`, siguiendo el patrón exacto que ya existe:

```
BaseException (abstracta)
├── DomainException        → tipo "Domain Exception"
├── SystemException        → tipo "System Exception"
└── AutorizacionException  → tipo "Authorization Exception"    ◄── NUEVA
        public static AutorizacionException throwException(int codigo, String mensaje)
```

Basta con registrar su `@ExceptionHandler` en `GlobalExceptionHandler` — o incluso dejar que caiga en el handler genérico de `BaseException`, que ya existe y funcionaría sin ningún cambio.

### 2.5 Impacto sobre los tests

`@WebMvcTest` empezará a devolver 401 en cuanto se active la cadena de filtros. La dependencia `spring-boot-starter-security-test` (nombre Boot 4) habilita `@WithMockUser` y `SecurityMockMvcRequestPostProcessors.jwt()` para simular tokens sin necesidad de levantar Keycloak.

---

## 3. Manejo de Roles

### 3.1 El problema: Spring no lee los roles de Keycloak por defecto

El `JwtGrantedAuthoritiesConverter` por defecto de Spring Security lee el claim **`scope`/`scp`** y genera authorities con prefijo `SCOPE_`. Pero Keycloak **no** publica los roles ahí: los publica anidados en `realm_access.roles`.

Token real emitido por Keycloak (fragmento):

```json
{
  "sub": "a3f1c2e4-0000-0000-0000-000000009b7d",
  "preferred_username": "fichazo",
  "email": "usuario@ejemplo.com",
  "realm_access": {
    "roles": ["ADMINISTRADOR", "EMPLEADO", "offline_access"]
  },
  "resource_access": {
    "kaffehaus-api": { "roles": ["GESTIONAR_ENTIDADES"] }
  }
}
```

Sin conversor personalizado, `hasRole('ADMINISTRADOR')` **siempre devuelve false** y todo responde 403. Este es el fallo número uno al integrar Keycloak con Spring Boot.

### 3.2 Solución: `KeycloakRolesConverter`

Una clase en `Shared/Infrastructure/Security/` que implementa `Converter<Jwt, Collection<GrantedAuthority>>` y:

1. Lee el claim `realm_access` como `Map<String, Object>`.
2. Extrae la lista `roles`.
3. Descarta los roles técnicos de Keycloak (`offline_access`, `uma_authorization`, `default-roles-*`), que no significan nada para el negocio.
4. Normaliza a mayúsculas y antepone el prefijo **`ROLE_`** → `ROLE_ADMINISTRADOR`.

> **El prefijo `ROLE_` no es opcional.** `hasRole('ADMINISTRADOR')` lo añade internamente antes de comparar. Si se omite, hay que usar `hasAuthority('ADMINISTRADOR')`. **Elegir una convención y no mezclarlas.** Recomiendo `ROLE_` + `hasRole()`, que es lo idiomático.

Luego, `KeycloakJwtAuthenticationConverter` envuelve a ese conversor y produce el `JwtAuthenticationToken` final, fijando `principalClaimName` en `preferred_username` (por defecto sería `sub`, un UUID ilegible en los logs).

**Roles de realm vs. roles de cliente.** Recomiendo usar **roles de realm** (`realm_access`) en esta fase: hay un solo backend, y los roles de cliente añaden un nivel de anidamiento (`resource_access.kaffehaus-api.roles`) que solo se justifica cuando varios servicios comparten realm con permisos distintos. El conversor debe quedar escrito de forma que añadir la segunda fuente después sea trivial.

### 3.3 Catálogo de roles como enum de dominio

Para evitar *magic strings* dispersos por `SecurityConfig` y `@PreAuthorize`, se define en `Shared/Domain/Security/`:

```
public enum RolDeSistema { ADMINISTRADOR, SUPERVISOR, EMPLEADO, CAJERO }
```

Esto encaja con una utilidad que el proyecto **ya tiene y todavía no usa**: `Validador.valorDeEnumRequerido(valor, campo, enumClass)`, que valida contra un enum y produce el mensaje `"El campo 'X' debe ser uno de: A, B, C"`. Es el mecanismo natural para validar un rol recibido en un `Request` DTO — por ejemplo al asignar rol a un usuario desde el CRUD de `Usuario`.

Los nombres de los roles en Keycloak deben coincidir **carácter a carácter** con las constantes del enum y con la columna `rol.nombre` de la base de datos.

### 3.4 Convivencia con las tablas `rol` y `usuario` (decisión clave)

El esquema `databasev4.sql` ya define `rol(id, nombre, ...)` y `usuario(id, id_persona, correo, contrasena, id_rol, ...)`. Con Keycloak aparecen **dos fuentes de verdad**, y hay que separar responsabilidades explícitamente:

| Responsabilidad | Fuente de verdad | Motivo |
|---|---|---|
| **Autenticación** (¿quién eres?) | Keycloak | Contraseñas, MFA, tokens, expiración. |
| **Autorización** (¿qué puedes hacer?) | Keycloak (claims del token) | Se resuelve en el filtro, sin tocar la BD en cada request. |
| **Relación de negocio** (¿qué persona es este usuario?) | Tabla local `usuario` → `persona` | El dominio necesita relacionar un usuario con pedidos, cajas, turnos. |

**Recomendaciones concretas:**

1. **`usuario.contrasena` queda obsoleta.** Ya no se debe escribir ni leer. Para no romper el `NOT NULL` existente, marcarla como deprecada en una migración posterior; **no** replicar hashes de contraseña fuera de Keycloak.
2. **Enlazar el usuario local con el `sub` de Keycloak.** Es necesaria una correspondencia estable. Lo más robusto es añadir una columna `keycloak_id UUID UNIQUE` a `usuario`. Enlazar por `correo` funciona, pero es frágil: si el usuario cambia su email en Keycloak, se rompe el vínculo.
3. **La tabla `rol` se mantiene** como catálogo de negocio y para la FK `usuario.id_rol` (útil en reportes y consultas SQL). Pero **la decisión de permitir o denegar una petición HTTP la toma siempre el token**, no la BD. El `rol` local es descriptivo; el claim es normativo.
4. **Sin sincronización bidireccional en esta fase.** Mantener los nombres alineados por convención documentada. Un sincronizador contra la Admin REST API de Keycloak es un problema aparte, y si llega, su cliente HTTP vive en `Shared/Infrastructure/Security/` (o en un `Datasource` del Bounded Context `Usuario`), nunca en un `UseCase`.

### 3.5 Configuración necesaria del lado de Keycloak

Pasos en la consola de administración (`http://localhost:8080`, credenciales `admin` / `adminpassword` según el `docker-compose.yaml`):

1. Crear el realm **`kaffehaus`** (el `issuer-uri` debe coincidir exactamente).
2. Crear el client **`kaffehaus-api`**: `Client authentication: OFF` (público, si el frontend usa PKCE), `Standard flow` habilitado, `Direct access grants` habilitado solo para pruebas con Postman.
3. En *Realm roles*, crear `ADMINISTRADOR`, `SUPERVISOR`, `EMPLEADO`, `CAJERO` — en mayúsculas, idénticos al enum `RolDeSistema`.
4. Asignar roles a los usuarios de prueba.
5. Verificar el token en [jwt.io](https://jwt.io) y comprobar que `realm_access.roles` contiene lo esperado **antes** de escribir el conversor. Esto ahorra horas de depuración.
6. **Persistencia del realm:** el `docker-compose.yaml` usa `start-dev` con `keycloakdb` sobre volumen, así que el realm sobrevive a reinicios. Aun así, conviene exportar el realm a `keycloak/realm-export.json` y montarlo, para que el entorno sea reproducible por cualquier integrante del equipo.

---

## 4. Resumen de archivos afectados

### Archivos nuevos

| Ruta | Capa | Responsabilidad |
|---|---|---|
| `Shared/Infrastructure/Security/SecurityConfig.java` | Infraestructura | Cadena de filtros y reglas por ruta |
| `Shared/Infrastructure/Security/KeycloakRolesConverter.java` | Infraestructura | `realm_access.roles` → `ROLE_*` |
| `Shared/Infrastructure/Security/KeycloakJwtAuthenticationConverter.java` | Infraestructura | `Jwt` → `AbstractAuthenticationToken` |
| `Shared/Infrastructure/Security/ContextoDeSeguridadImpl.java` | Infraestructura | Adaptador del puerto |
| `Shared/Application/Security/IContextoDeSeguridad.java` | Aplicación | Puerto (prefijo `I`) |
| `Shared/Domain/Security/UsuarioAutenticado.java` | Dominio | VO inmutable, sin anotaciones |
| `Shared/Domain/Security/RolDeSistema.java` | Dominio | Enum de roles |
| `Shared/Domain/Error/AutorizacionException.java` | Dominio | Hermana de `DomainException` |
| `Shared/Presentation/Security/JwtAuthenticationEntryPoint.java` | Presentación | 401 en formato `ErrorResponse` |
| `Shared/Presentation/Security/JwtAccessDeniedHandler.java` | Presentación | 403 en formato `ErrorResponse` |

### Archivos modificados

| Archivo | Cambio |
|---|---|
| `pom.xml` | + `spring-boot-starter-security-oauth2-resource-server`, + `spring-boot-starter-security-test` (scope test) |
| `application.yaml` | + `issuer-uri`, + `server.port` (resolver choque con Keycloak) |
| `docker-compose.yaml` | (opcional) montar `realm-export.json` |
| `Shared/Presentation/GlobalExceptionHandler.java` | (opcional) `@ExceptionHandler` para `AutorizacionException` |

### Archivos que NO se tocan

**Los 5 controladores `*ControllerV1`, los 25 `*UseCase`, todos los agregados, Value Objects, DTOs, repositorios, datasources y modelos JPA permanecen sin cambios.** Ese es precisamente el indicador de que la integración respeta la arquitectura: si hiciera falta editar `CrearEntidadUseCase` para añadir seguridad, la propuesta estaría mal planteada.

---

## 5. Orden de implementación sugerido

1. Levantar Keycloak, crear realm `kaffehaus`, client y roles. Obtener un token y verificarlo en jwt.io.
2. Resolver el conflicto de puertos (app a `8081`).
3. Añadir la dependencia y el `issuer-uri`. Verificar que la app arranca y que **todo** responde 401 (señal de que el resource server está activo).
4. Escribir `SecurityConfig` con `.anyRequest().authenticated()` únicamente. Comprobar que con un token válido `GET /api/v1/entidad` ya pasa el filtro.
5. Añadir `KeycloakRolesConverter` y verificar los authorities resultantes. **Aquí es donde falla la mayoría de integraciones** (§3.1).
6. Introducir las reglas por rol (§2.2).
7. Añadir `EntryPoint` y `AccessDeniedHandler` para unificar el formato de error (§2.4).
8. Solo entonces, si un caso de uso necesita saber quién actúa: puerto `IContextoDeSeguridad` + adaptador.

> **Nota final sobre el estado del proyecto.** Los `*DatasourceImpl` de `Entidad`, `Cliente` y `Proveedor` todavía lanzan `UnsupportedOperationException`, y `application.yaml` no tiene configurado el datasource de PostgreSQL. La integración de Keycloak es **ortogonal** a eso y puede desarrollarse en paralelo, pero conviene tenerlo presente: hasta que la persistencia funcione, una petición autenticada correctamente seguirá fallando con 500 al llegar al datasource. Conviene validar la seguridad contra un endpoint trivial (`/actuator/health` o un `GET` de prueba) antes de darla por buena sobre el CRUD real.
