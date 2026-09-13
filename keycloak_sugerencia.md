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
| `PasswordEncoder` | Inyectado en `CrearUsuarioUseCase` y `ActualizarUsuarioUseCase`, **pero sin `@Bean` que lo provea** | La app no arranca hoy. Keycloak elimina esta dependencia en lugar de obligar a declararla (ver §5.6). |
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
│       ├── IContextoDeSeguridad.java      ¿quién actúa?          §4
│       ├── IProveedorDeIdentidad.java     usuarios en Keycloak   §5
│       └── ICatalogoDeRoles.java          roles de realm         §5
│
├── Domain/
│   ├── Error/                    (existente)
│   │   └── AutorizacionException.java   ◄── NUEVO — hermana de DomainException
│   └── Security/                 ◄── NUEVO — VALUE OBJECTS puros
│       ├── KeycloakId.java       (el claim `sub`, calcado de EntidadId)
│       ├── UsuarioAutenticado.java
│       └── RolDeSistema.java     (enum)
│
├── Infrastructure/               ◄── NUEVO PAQUETE (4ª capa de Shared)
│   └── Security/
│       ├── SecurityConfig.java              @Configuration — la cadena de filtros
│       ├── KeycloakJwtAuthenticationConverter.java   Jwt → Authentication
│       ├── KeycloakRolesConverter.java      claims → GrantedAuthority
│       ├── ContextoDeSeguridadImpl.java     @Component — adaptador del puerto
│       └── Keycloak/                        ◄── salida hacia la Admin REST API
│           ├── KeycloakAdminProperties.java
│           ├── KeycloakAdminTokenProvider.java
│           ├── KeycloakAdminClient.java
│           ├── ProveedorDeIdentidadImpl.java
│           └── CatalogoDeRolesImpl.java
│
└── Presentation/
    ├── GlobalExceptionHandler.java   (existente)
    └── Security/                 ◄── NUEVO — traducción de errores a HTTP
        ├── JwtAuthenticationEntryPoint.java   (401)
        └── JwtAccessDeniedHandler.java        (403)
```

**Las dos direcciones del tráfico.** Conviene no confundirlas, porque usan credenciales distintas y fallan por motivos distintos:

| | Entrada (§4) | Salida (§5) |
|---|---|---|
| Dirección | Keycloak → Backend | Backend → Keycloak |
| Mecanismo | Token del usuario final, validado en el filtro | Token de *service account*, vía Admin REST API |
| Puerto | `IContextoDeSeguridad` | `IProveedorDeIdentidad`, `ICatalogoDeRoles` |
| Disparador | Cada petición HTTP entrante | Solo los CRUD de `Usuario` y `Rol` |

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

Contrato sugerido (métodos en español, como `crear` / `actualizar` / `obtenerPorId`); se detalla en §4.2:

```
KeycloakId          obtenerIdDeUsuarioActual();
UsuarioAutenticado  obtenerUsuarioActual();
boolean             tieneRol(RolDeSistema rol);
boolean             hayUsuarioAutenticado();
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

> ⚠️ **Esta última regla ya está incumplida hoy.** `CrearUsuarioUseCase` y `ActualizarUsuarioUseCase` importan `org.springframework.security.crypto.password.PasswordEncoder` y lo reciben por constructor. Es la única fuga de infraestructura hacia `Application` en todo el proyecto.
>
> Además, **no existe ningún `@Bean PasswordEncoder` declarado en el código**, así que hoy el contexto de Spring no puede resolver esa dependencia y la aplicación no arranca.
>
> La buena noticia: la integración con Keycloak **elimina el problema en lugar de agravarlo**. Al mudar las credenciales al IdP, `PasswordEncoder` desaparece de ambos casos de uso, el campo `contrasena` sale del agregado `Usuario` y la fuga se cierra sola. Ver §5.6.

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

## 4. Requerimiento A — Extracción de Identidad (Token → Backend)

> *"El backend debe tener una forma estandarizada de obtener el ID del usuario que viene en el token JWT, para saber qué persona está realizando la acción."*

### 4.1 El problema real: el `sub` del token no es el `usuario.id` local

Este es el punto que hay que resolver antes de escribir nada. El token trae:

```
sub = "a3f1c2e4-...-9b7d"     ← UUID generado por KEYCLOAK
```

Pero el negocio necesita:

```
usuario.id   → UUID de la tabla `usuario`
usuario.id_persona → `persona`, que es quien realmente "realiza la acción"
```

**Son tres identificadores distintos.** El token no sabe nada de `persona`. Por tanto "obtener el ID del usuario" son en realidad **dos operaciones de naturaleza muy distinta**, y mezclarlas es el error de diseño a evitar:

| | Identidad del token | Identidad de negocio |
|---|---|---|
| Qué devuelve | `sub`, `correo`, `username`, roles | Agregado `Usuario` + `PersonaId` |
| De dónde sale | El JWT ya validado, en memoria | Consulta a la tabla `usuario` |
| Coste | Cero | Un `SELECT` por invocación |
| Depende de | Nada del dominio | Bounded Context `Usuario` |

### 4.2 Nivel 1 — Identidad del token (en `Shared`)

Resuelto por el puerto ya introducido en §1.3, ahora con su contrato completo:

**`Shared/Application/Security/IContextoDeSeguridad.java`** (puerto, prefijo `I`, en `Application/` como `IEntidadRepository`)

```
KeycloakId          obtenerIdDeUsuarioActual();
UsuarioAutenticado  obtenerUsuarioActual();
boolean             tieneRol(RolDeSistema rol);
boolean             hayUsuarioAutenticado();
```

**`Shared/Domain/Security/`** — dos VOs puros, sin anotaciones, calcados de `EntidadId`:

- `KeycloakId` — envuelve el `sub`. `private final String value` + `getValue()`. Idéntico en forma a `EntidadId`, `PersonaId`, `RolId`.
- `UsuarioAutenticado` — `KeycloakId id`, `String correo`, `String username`, `Set<RolDeSistema> roles`. Inmutable.

**`Shared/Infrastructure/Security/ContextoDeSeguridadImpl.java`** — `@Component`. Lee `SecurityContextHolder`, castea a `JwtAuthenticationToken`, extrae los claims y construye el VO. **Es el único punto del sistema que toca `SecurityContextHolder`.**

Si no hay autenticación en el contexto, lanza `AutorizacionException.throwException(401, "No hay un usuario autenticado en el contexto")` — coherente con la jerarquía de errores de §2.4.

### 4.3 Nivel 2 — Identidad de negocio (en el Bounded Context `Usuario`)

Aquí está la decisión arquitectónica que más fácil se equivoca:

> **`Shared` NO puede devolver un agregado `Usuario`.**
>
> Si `IContextoDeSeguridad` devolviera `Usuario`, el kernel compartido pasaría a depender de `BoundedContexts/Personal/Usuario`. La dependencia se invertiría: hoy **los contextos dependen de `Shared`**, nunca al revés. Y todo contexto futuro (Inventario, Ventas) arrastraría `Personal/Usuario` solo por usar paginación o excepciones.

Por tanto, la traducción `sub → Usuario → Persona` pertenece al Bounded Context `Usuario`, y se resuelve con piezas que ya existen en su forma:

1. **Columna nueva:** `usuario.keycloak_id UUID UNIQUE` (ya anticipada en §3.4). Es el puente entre ambos mundos.
2. **VO en el agregado:** `Usuario` gana un campo `KeycloakId keycloakId`, importado de `Shared/Domain/Security/`. Las factories `crear(...)` y `restaurar(...)` lo incorporan, igual que hoy incorporan `PersonaId` y `RolId`.
3. **Método nuevo en el puerto existente:** se amplía `IUsuarioRepository` (y su gemelo `IUsuarioDatasource`, que el proyecto mantiene con firma idéntica):

   ```
   Optional<Usuario> obtenerPorKeycloakId(KeycloakId keycloakId);
   ```

   Respeta la convención: método en español, recibe un VO, devuelve `Optional<Agregado>`.
4. **Caso de uso nuevo:** `Usuario/Application/UseCases/ObtenerUsuarioActualUseCase.java`, `@Service @Transactional(readOnly = true)` (como `ObtenerEntidadPorIdUseCase`). Recibe por constructor `IUsuarioRepository` **y** `IContextoDeSeguridad`, y devuelve `UsuarioResponse`:

   ```
   KeycloakId id = contextoDeSeguridad.obtenerIdDeUsuarioActual();
   Usuario usuario = usuarioRepository.obtenerPorKeycloakId(id)
       .orElseThrow(() -> DomainException.throwException(404,
           "El usuario autenticado no esta registrado en el sistema"));
   ```

   El `orElseThrow` con `DomainException.throwException(404, ...)` es exactamente el patrón de `ActualizarEntidadUseCase`.
5. **Endpoint:** `GET /api/v1/usuario/me` en `UsuarioControllerV1`, devolviendo el `UsuarioResponse` que ya compone `Persona` y `Rol`. Es el "quién soy" que consumirá el frontend.

### 4.4 Cómo lo consumen los casos de uso existentes

Cuando `CrearEntidadUseCase` necesite registrar quién creó el registro, recibe el puerto por constructor — **exactamente igual que hoy recibe `IEntidadRepository`**:

```
public CrearEntidadUseCase(
        IEntidadRepository entidadRepository,
        IContextoDeSeguridad contextoDeSeguridad) { ... }
```

El caso de uso sigue sin saber que existe un JWT: pide `contextoDeSeguridad.obtenerIdDeUsuarioActual()` y recibe un VO de dominio. En un test unitario se sustituye por un doble que devuelve un `KeycloakId` fijo, sin levantar Spring ni Keycloak.

**Alternativa descartada y por qué.** La opción "más pura" sería pasar el usuario como parámetro: `execute(request, usuarioAutenticado)`, dejando el caso de uso como función total sin estado ambiental. Es mejor en teoría, pero obliga a modificar la firma de **los 5 controladores y sus 25 métodos**, rompiendo la promesa central de §2.1 de que los controladores no se tocan. Con el puerto inyectado, el coste es cero para los controladores y el aislamiento se mantiene igual de bien.

**Dónde NO resolver esto:**

- ❌ `@AuthenticationPrincipal Jwt jwt` como parámetro del controlador: mete un tipo de Spring Security en `Presentation` y obliga al controlador a pasarlo hacia abajo, contaminando también la firma del caso de uso.
- ❌ `SecurityContextHolder.getContext()` dentro de un `UseCase`: es justo la fuga que el puerto evita.
- ❌ Leer el `sub` dentro de un `*DatasourceImpl`: la identidad no es un detalle de persistencia.

### 4.5 Flujo completo

```
JWT (sub, email, realm_access.roles)
   ↓  [Infrastructure]  ContextoDeSeguridadImpl lee SecurityContextHolder
KeycloakId / UsuarioAutenticado          ← VOs puros de Shared/Domain
   ↓  [Application]     IContextoDeSeguridad (puerto)
ObtenerUsuarioActualUseCase / CrearEntidadUseCase
   ↓  [Application]     IUsuarioRepository.obtenerPorKeycloakId(...)
Usuario (agregado)  →  PersonaId  →  Persona
   ↓
UsuarioResponse.createFromAggregate(usuario, persona, rol)
```

Nótese que el dominio nunca ve un `Jwt`, y `Shared` nunca ve un `Usuario`.

---

## 5. Requerimiento B — Sincronización (Backend → Keycloak)

> *"Cuando se creen usuarios o roles mediante los endpoints del backend, deben crearse automáticamente en Keycloak."*

### 5.1 Por qué esto NO es un "segundo datasource"

La tentación natural, dado el patrón del proyecto (`Repository` → `Datasource`), es meter Keycloak como un segundo datasource detrás de `UsuarioRepositoryImpl`, de modo que `CrearUsuarioUseCase` no se entere de nada:

```
UsuarioRepositoryImpl.crear(usuario)
   ├── usuarioDatasource.crear(usuario)        (JPA)
   └── identidadDatasource.crear(usuario)      (Keycloak)   ← tentador, pero mal
```

**Se descarta, por una razón concreta y no estética:** Keycloak es quien **genera el `sub`**, y ese `sub` es justamente el dato que la fila local debe guardar en `keycloak_id` (§4.3). Las dos escrituras no son paralelas: están **ordenadas y son dependientes en datos**. Un repositorio cuyo contrato es "guarda este agregado" no puede expresar "primero llama a un servicio remoto, toma el identificador que devuelve, incorpóralo al agregado y recién entonces persiste". Además escondería una llamada HTTP con latencia y fallos detrás de algo que parece un `INSERT`, haciendo invisible el manejo de errores.

La capa cuya responsabilidad documentada es precisamente **orquestar** es el caso de uso. Ahí va.

### 5.2 Puertos: dos interfaces pequeñas en `Shared/Application/Security/`

Siguiendo la convención del proyecto (puertos con prefijo `I`, en `Application/`), y segregados para que cada contexto dependa solo de lo que usa — el contexto `Usuario` no necesita poder crear roles de realm:

**`IProveedorDeIdentidad`** (lo usa el contexto `Usuario`)

```
KeycloakId crearUsuario(String correo, String nombres, String apellidos, String contrasenaTemporal);
void       actualizarUsuario(KeycloakId id, String correo, String nombres, String apellidos);
void       cambiarContrasena(KeycloakId id, String nuevaContrasena);
void       habilitarUsuario(KeycloakId id);
void       deshabilitarUsuario(KeycloakId id);
void       asignarRol(KeycloakId id, String nombreRol);
void       quitarRol(KeycloakId id, String nombreRol);
```

**`ICatalogoDeRoles`** (lo usa el contexto `Rol`)

```
void crearRol(String nombre);
void renombrarRol(String nombreActual, String nombreNuevo);
void eliminarRol(String nombre);
boolean existeRol(String nombre);
```

**Detalle crítico del diseño:** los puertos hablan en **tipos primitivos y VOs de `Shared`**, nunca en agregados (`Usuario`, `Rol`). Esto es lo que permite que vivan en `Shared` sin que el kernel dependa de ningún Bounded Context — el mismo razonamiento de §4.3, aplicado en la dirección contraria.

### 5.3 Adaptador: una Capa Anticorrupción en `Shared/Infrastructure/Security/Keycloak/`

```
Shared/Infrastructure/Security/Keycloak/
├── KeycloakAdminProperties.java      @ConfigurationProperties — URLs, client, secret
├── KeycloakAdminTokenProvider.java   obtiene y cachea el token de servicio
├── KeycloakAdminClient.java          RestClient contra la Admin REST API
├── ProveedorDeIdentidadImpl.java     @Component — implementa IProveedorDeIdentidad
└── CatalogoDeRolesImpl.java          @Component — implementa ICatalogoDeRoles
```

Los `*Impl` son a estos puertos lo que `EntidadRepositoryImpl` es a `IEntidadRepository`: adaptadores anotados con estereotipo de Spring que encapsulan la tecnología.

**Anticorrupción:** el JSON de Keycloak (`UserRepresentation`, `CredentialRepresentation`, `RoleRepresentation`) **no puede cruzar hacia `Application`**. Se traduce dentro del adaptador. Si mañana se cambia Keycloak por Auth0, solo cambian estas cinco clases.

**Autenticación del backend contra Keycloak.** La Admin REST API exige un token privilegiado. No se debe reutilizar el token del usuario final. Configuración recomendada:

- Un client **confidencial** aparte, `kaffehaus-admin-cli`, con `Service accounts roles: ON`.
- A su service account se le asignan los roles del client `realm-management`: `manage-users` y `manage-realm` — **solo esos**, no `realm-admin`.
- Flujo `client_credentials`; el token se cachea hasta poco antes de `exp` y se renueva. Abrir una sesión nueva por request es el error de rendimiento clásico aquí.
- El `client-secret` va en variable de entorno, **nunca en `application.yaml` versionado**.

```yaml
kaffehaus:
  keycloak:
    admin:
      base-url: ${KEYCLOAK_BASE_URL:http://localhost:8080}
      realm: kaffehaus
      client-id: kaffehaus-admin-cli
      client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET}
```

**Implementación del cliente HTTP:** `RestClient` (incluido en Spring Framework 7 / Boot 4, sin dependencias extra). Se descarta `keycloak-admin-client` como librería: arrastra Jakarta RESTEasy y suele chocar de versiones con Boot 4. La Admin API son cuatro endpoints REST; un `RestClient` tipado es más liviano y más fácil de simular en tests.

### 5.4 Orquestación en el caso de uso — y el problema de la atomicidad

Aquí está la dificultad honesta de este requerimiento: **`@Transactional` no cubre una llamada HTTP.** Si Keycloak responde OK y luego el `INSERT` falla, el rollback de Spring deshace la fila local pero **deja el usuario creado en Keycloak**. Y al revés.

Tres estrategias, con sus costes reales:

| Estrategia | Orden | Qué pasa si falla el segundo paso | Veredicto |
|---|---|---|---|
| **A. Keycloak primero + compensación** | KC → BD | Se captura la excepción y se borra el usuario en KC (compensación). Si la compensación también falla → huérfano en KC. | ✅ **Recomendada.** Es la única que permite guardar el `sub` en el mismo `INSERT`. |
| **B. BD primero, KC después del commit** | BD → *commit* → KC | El rollback de la BD es limpio, pero el `keycloak_id` queda nulo y hay una ventana con el usuario local sin identidad en KC. | Requiere `keycloak_id` nullable + un segundo `UPDATE`. Más piezas, misma incertidumbre. |
| **C. Outbox / eventos de dominio** | BD + fila `outbox` en la misma transacción; un publicador reintenta contra KC | Reintento automático hasta éxito; consistencia eventual garantizada. | 🔭 **Fase 2.** Es la respuesta correcta a escala, pero añade tabla, publicador y planificador. |

**Recomendación:** estrategia **A** para esta fase, con la compensación escrita explícitamente en el caso de uso y el fallo de compensación registrado en log a nivel `ERROR` con el `sub` afectado, para reconciliación manual.

Forma resultante de `CrearUsuarioUseCase` (sigue siendo `@Service @Transactional`, sigue devolviendo `UsuarioResponse`):

```
1. Validar que existen Persona y Rol          (ya lo hace hoy, sin cambios)
2. keycloakId = proveedorDeIdentidad.crearUsuario(correo, nombres, apellidos, contrasena)
3. proveedorDeIdentidad.asignarRol(keycloakId, rol.getNombre())
4. try {
       usuario = Usuario.crear(personaId, correo, rolId, keycloakId)   ← sin contrasena
       usuarioCreado = usuarioRepository.crear(usuario)
   } catch (Exception e) {
       proveedorDeIdentidad.eliminarUsuario(keycloakId);   // compensación
       throw e;
   }
5. return UsuarioResponse.createFromAggregate(usuarioCreado, persona, rol)
```

El caso de uso gana **un parámetro más en el constructor** (`IProveedorDeIdentidad`) y un bloque de compensación. Sigue sin importar nada de Keycloak ni de Spring Security: solo conoce un puerto y un VO.

> **Ojo con el orden respecto a la transacción.** La llamada del paso 2 ocurre dentro de la transacción JPA abierta por `@Transactional`. Eso mantiene la transacción abierta durante toda la latencia de red hacia Keycloak. Con el volumen de una cafetería es irrelevante; conviene saberlo por si el pool de conexiones se vuelve un problema.

### 5.5 Alcance: qué operaciones sincronizan

| Endpoint | Efecto local | Efecto en Keycloak |
|---|---|---|
| `POST /api/v1/usuario` | `INSERT` en `usuario` | Crear usuario + asignar rol de realm |
| `PUT /api/v1/usuario/{id}` | `UPDATE` | Actualizar correo/nombre; si cambia `idRol`, quitar rol anterior y asignar el nuevo |
| `DELETE /api/v1/usuario/{id}` | `borrado = true` (borrado lógico) | **`enabled = false`**, *no* borrar |
| `POST /api/v1/rol` | `INSERT` en `rol` | Crear realm role con el mismo `nombre` |
| `PUT /api/v1/rol/{id}` | `UPDATE` | Renombrar el realm role |
| `DELETE /api/v1/rol/{id}` | `borrado = true` | Eliminar el realm role (o dejarlo huérfano — ver §5.7) |

**El borrado lógico encaja de forma natural.** El proyecto ya modela `eliminar()` / `reactivar()` en todos los agregados sobre la columna `borrado`. Keycloak tiene exactamente la misma semántica con `enabled`. La correspondencia es directa:

```
Usuario.eliminar()   →  borrado = true   →  KC: enabled = false
Usuario.reactivar()  →  borrado = false  →  KC: enabled = true
```

Borrar de verdad en Keycloak sería incoherente con el borrado lógico local y destruiría la trazabilidad de los tokens ya emitidos.

### 5.6 Efecto sobre la contraseña (cierre de la fuga de §1.3)

Con Keycloak como autoridad de credenciales:

- `CrearUsuarioRequest` sigue aceptando `contrasena`, pero la usa **solo** como contraseña temporal para Keycloak (con `temporary = true`, forzando cambio en el primer login). Nunca se persiste localmente.
- `PasswordEncoder` se elimina de `CrearUsuarioUseCase` y `ActualizarUsuarioUseCase` → **desaparece la única fuga de Spring Security hacia `Application`** y, de paso, el bean inexistente que hoy impide arrancar la app.
- El campo `contrasena` sale del agregado `Usuario`, de `Usuario.crear(...)`, de `Usuario.restaurar(...)` y de `Usuario.actualizar(...)`.
- La columna `usuario.contrasena` (hoy `NOT NULL`) debe pasar a nullable y luego eliminarse en una migración.
- El cambio de contraseña deja de ser un `PUT /api/v1/usuario/{id}` y pasa a ser responsabilidad de Keycloak (Account Console) o de un endpoint dedicado que delegue en `IProveedorDeIdentidad.cambiarContrasena(...)`.

### 5.7 Tensión a resolver: CRUD de roles vs. enum `RolDeSistema`

§3.3 propone un enum fijo `RolDeSistema`, pero existe un CRUD que permite crear roles arbitrarios en tiempo de ejecución. Hay que decidir explícitamente, porque son dos modelos incompatibles si no se acotan:

- El enum lista los roles que el **código** referencia en `hasRole(...)` y en las reglas de §2.2. Son parte del contrato de autorización y cambian con un despliegue.
- El CRUD puede crear roles adicionales para uso de negocio (reportes, agrupaciones), pero **un rol creado por CRUD no otorga ningún permiso** hasta que una regla de autorización lo mencione. Conviene que la respuesta del endpoint o la documentación lo diga, o generará confusión ("creé el rol y no puede entrar").
- **Renombrar un rol que figura en `RolDeSistema` debe prohibirse**: rompería silenciosamente todas las reglas `hasRole(...)`. `ActualizarRolUseCase` debería rechazarlo con `DomainException.throwException(409, "El rol '...' es un rol de sistema y no puede renombrarse")`.
- Al crear, normalizar `nombre` a mayúsculas sin espacios antes de enviarlo a Keycloak, y validarlo con el `Validador` existente.

### 5.8 Errores, idempotencia y reconciliación

- **Duplicados:** Keycloak responde `409` si el `username`/`email` ya existe. El adaptador debe traducirlo a `DomainException.throwException(409, "Ya existe un usuario con el correo '...'")`, para que la respuesta siga el mismo formato `ErrorResponse` que el resto de la API (§2.4). **Ninguna excepción propia de Keycloak ni ningún `HttpClientErrorException` debe escapar del adaptador.**
- **Keycloak caído:** traducir a `SystemException.throwException(503, "El servicio de identidad no esta disponible")`. `SystemException` ya existe y el `GlobalExceptionHandler` ya la maneja; no hace falta nada nuevo.
- **Timeouts:** configurar timeouts de conexión y lectura explícitos en el `RestClient`. Sin ellos, una caída de Keycloak cuelga hilos del servidor.
- **Reconciliación:** conviene un comando administrativo (endpoint protegido a `ADMINISTRADOR`, o tarea programada) que liste usuarios de Keycloak sin correspondencia en `usuario.keycloak_id` y viceversa. Con la estrategia A es la red de seguridad para los huérfanos.
- **Carga inicial:** los usuarios que ya existan en la base de datos antes de la integración necesitan un proceso de alta masiva en Keycloak que rellene `keycloak_id`. No pueden autenticarse hasta entonces.

### 5.9 Diagrama de capas de la sincronización

```
POST /api/v1/usuario
   ↓
UsuarioControllerV1                      [Presentation]   ← sin cambios
   ↓  CrearUsuarioRequest.createFromRequest(body)
CrearUsuarioUseCase                      [Application]    ← +1 puerto, +compensación
   ├──→ IUsuarioRepository               (puerto)  ──→ UsuarioRepositoryImpl ──→ JPA ──→ PostgreSQL
   └──→ IProveedorDeIdentidad            (puerto)  ──→ ProveedorDeIdentidadImpl
                                                              ↓  [Infrastructure]
                                                        KeycloakAdminClient (RestClient)
                                                              ↓  HTTP + token de servicio
                                                        Keycloak Admin REST API
```

Las dos ramas son **simétricas en forma**: el caso de uso solo ve interfaces `I*` que devuelven tipos de dominio. Que una termine en un `INSERT` y la otra en un `POST` HTTP es invisible desde `Application`.

---

## 6. Resumen de archivos afectados

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

**Requerimiento A — extracción de identidad (§4)**

| Ruta | Capa | Responsabilidad |
|---|---|---|
| `Shared/Domain/Security/KeycloakId.java` | Dominio | VO del `sub`, calcado de `EntidadId` |
| `Usuario/Application/UseCases/ObtenerUsuarioActualUseCase.java` | Aplicación | `sub` → `Usuario` → `Persona` |

**Requerimiento B — sincronización (§5)**

| Ruta | Capa | Responsabilidad |
|---|---|---|
| `Shared/Application/Security/IProveedorDeIdentidad.java` | Aplicación | Puerto: usuarios en Keycloak |
| `Shared/Application/Security/ICatalogoDeRoles.java` | Aplicación | Puerto: roles de realm |
| `Shared/Infrastructure/Security/Keycloak/KeycloakAdminProperties.java` | Infraestructura | `@ConfigurationProperties` |
| `Shared/Infrastructure/Security/Keycloak/KeycloakAdminTokenProvider.java` | Infraestructura | Token de servicio cacheado |
| `Shared/Infrastructure/Security/Keycloak/KeycloakAdminClient.java` | Infraestructura | `RestClient` contra la Admin REST API |
| `Shared/Infrastructure/Security/Keycloak/ProveedorDeIdentidadImpl.java` | Infraestructura | Adaptador + capa anticorrupción |
| `Shared/Infrastructure/Security/Keycloak/CatalogoDeRolesImpl.java` | Infraestructura | Adaptador + capa anticorrupción |

### Archivos modificados

| Archivo | Cambio | Motivo |
|---|---|---|
| `pom.xml` | + `spring-boot-starter-security-oauth2-resource-server`, + `spring-boot-starter-security-test` (scope test) | §1.4 |
| `application.yaml` | + `issuer-uri`, + `server.port`, + bloque `kaffehaus.keycloak.admin` | §1.5, §5.3 |
| `docker-compose.yaml` | (opcional) montar `realm-export.json` | §3.5 |
| `Shared/Presentation/GlobalExceptionHandler.java` | (opcional) `@ExceptionHandler` para `AutorizacionException` | §2.4 |
| `databasev4.sql` | + `usuario.keycloak_id UUID UNIQUE`; `usuario.contrasena` → nullable y luego eliminada | §3.4, §5.6 |
| `Usuario/Domain/Aggregates/Usuario.java` | + `KeycloakId`; − `contrasena` (afecta `crear`, `restaurar`, `actualizar`) | §4.3, §5.6 |
| `Usuario/Application/Repositories/IUsuarioRepository.java` | + `obtenerPorKeycloakId(KeycloakId)` | §4.3 |
| `Usuario/Application/Datasources/IUsuarioDatasource.java` | ídem (el proyecto mantiene ambas firmas idénticas) | §4.3 |
| `Usuario/Infrastructure/.../UsuarioJpaModel.java` + mapper | + columna `keycloak_id`; − `contrasena` | §5.6 |
| `Usuario/Application/UseCases/CrearUsuarioUseCase.java` | + `IProveedorDeIdentidad`, orquestación y compensación; − `PasswordEncoder` | §5.4 |
| `Usuario/Application/UseCases/ActualizarUsuarioUseCase.java` | + sincronización de correo y rol; − `PasswordEncoder` | §5.5 |
| `Usuario/Application/UseCases/EliminarUsuarioUseCase.java` | + `deshabilitarUsuario` en Keycloak | §5.5 |
| `Usuario/Presentation/UsuarioControllerV1.java` | + `GET /api/v1/usuario/me` | §4.3 |
| `Rol/Application/UseCases/CrearRolUseCase.java` | + `ICatalogoDeRoles.crearRol(...)` | §5.5 |
| `Rol/Application/UseCases/ActualizarRolUseCase.java` | + renombrado en Keycloak; + bloqueo de roles de sistema | §5.7 |
| `Rol/Application/UseCases/EliminarRolUseCase.java` | + eliminación del realm role | §5.5 |

### Archivos que NO se tocan

**Los 5 controladores `*ControllerV1` (salvo el endpoint nuevo `/me`), los casos de uso de `Entidad`, `Cliente`, `Proveedor` y `Persona`, todos sus agregados, Value Objects, DTOs, repositorios, datasources y modelos JPA permanecen sin cambios.**

Los únicos casos de uso que se modifican son los **6 de `Usuario` y `Rol`**, y no por la seguridad en sí, sino porque *su propio dominio* cambió: ahora las identidades y los roles viven en dos sistemas. Los 19 casos de uso restantes quedan intactos. Ese reparto es el indicador de que la integración respeta la arquitectura: si hiciera falta editar `CrearEntidadUseCase` para autenticar, la propuesta estaría mal planteada.

---

## 7. Orden de implementación sugerido

**Fase 1 — Resource Server (autenticación y autorización)**

1. Levantar Keycloak, crear realm `kaffehaus`, client y roles. Obtener un token y verificarlo en jwt.io.
2. Resolver el conflicto de puertos (app a `8081`).
3. Añadir la dependencia y el `issuer-uri`. Verificar que la app arranca y que **todo** responde 401 (señal de que el resource server está activo).
4. Escribir `SecurityConfig` con `.anyRequest().authenticated()` únicamente. Comprobar que con un token válido `GET /api/v1/entidad` ya pasa el filtro.
5. Añadir `KeycloakRolesConverter` y verificar los authorities resultantes. **Aquí es donde falla la mayoría de integraciones** (§3.1).
6. Introducir las reglas por rol (§2.2).
7. Añadir `EntryPoint` y `AccessDeniedHandler` para unificar el formato de error (§2.4).

**Fase 2 — Extracción de identidad (§4)**

8. Migración: añadir `usuario.keycloak_id UUID UNIQUE`.
9. `KeycloakId`, `UsuarioAutenticado`, `IContextoDeSeguridad` y su adaptador.
10. `obtenerPorKeycloakId` en repositorio y datasource; `KeycloakId` en el agregado `Usuario`.
11. `ObtenerUsuarioActualUseCase` + `GET /api/v1/usuario/me`. Es la prueba de humo de toda la fase: si `/me` devuelve la persona correcta, la cadena token → dominio funciona.

**Fase 3 — Sincronización (§5)**

12. Crear el client confidencial `kaffehaus-admin-cli` con service account y los roles `manage-users` / `manage-realm`.
13. `KeycloakAdminTokenProvider` + `KeycloakAdminClient`. Probarlos de forma aislada antes de conectarlos a ningún caso de uso.
14. `ICatalogoDeRoles` y su adaptador → conectar a los casos de uso de `Rol` (es el más simple: sin `sub` que propagar, sin compensación).
15. `IProveedorDeIdentidad` y su adaptador → conectar a `CrearUsuarioUseCase` con la compensación de §5.4.
16. Eliminar `PasswordEncoder`, el campo `contrasena` del agregado y la columna (§5.6).
17. Migración de los usuarios ya existentes + comando de reconciliación (§5.8).

> **Por qué este orden.** Cada fase se puede verificar sola. Intentar las tres a la vez hace imposible distinguir un fallo del conversor de roles (§3.1) de un fallo del token de servicio (§5.3), que producen síntomas parecidos (403) por causas opuestas.

> **Nota final sobre el estado del proyecto.** Los `*DatasourceImpl` de `Entidad`, `Cliente` y `Proveedor` todavía lanzan `UnsupportedOperationException`, y `application.yaml` no tiene configurado el datasource de PostgreSQL. La integración de Keycloak es **ortogonal** a eso y puede desarrollarse en paralelo, pero conviene tenerlo presente: hasta que la persistencia funcione, una petición autenticada correctamente seguirá fallando con 500 al llegar al datasource. Conviene validar la seguridad contra un endpoint trivial (`/actuator/health` o un `GET` de prueba) antes de darla por buena sobre el CRUD real.
