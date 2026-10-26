# Arquitectura de KaffeHaus v4

> Documento normativo y **plantilla de la arquitectura** del backend. Describe cada pieza del
> sistema, que puede y que no puede hacer, y como se combinan; su fin es servir para disenar
> modulos nuevos y ampliar los existentes sin romper la estructura.
>
> **No contiene reglas de negocio.** Que hace el sistema (stock, ventas, caja, permisos por
> operacion...) lo decide `.rules/logica.md`, que es la fuente de la verdad de la logica. Aqui los
> casos reales aparecen solo como ejemplo de una pieza o de un patron.
>
> Todo cambio de codigo respeta este documento. Si una necesidad real no encaja, se propone el
> cambio, se aprueba y se actualiza primero este archivo y despues el codigo, nunca al reves.

---

## 1. Vision general y stack

KaffeHaus v4 es el backend REST (HTTP/JSON) del sistema de gestion de una cafeteria. Se organiza
en cuatro bounded contexts (`Inventario`, `Ventas`, `Personal`, `Pedidos`) y un kernel compartido
(`Shared`).

| Elemento | Valor |
|---|---|
| Lenguaje | Java 21 (`<java.version>21</java.version>`) |
| Framework | Spring Boot 4.1.1 (parent POM) |
| Build | Maven, con wrapper (`mvnw`, `mvnw.cmd`) |
| Web | `spring-boot-starter-webmvc` (Spring MVC, servlet, bloqueante) |
| Seguridad | `spring-boot-starter-security` (Spring Security 7, cadena stateless) + JWT propios con jjwt |
| Persistencia | `spring-boot-starter-data-jpa` + Hibernate sobre PostgreSQL |
| Driver | `org.postgresql:postgresql` (runtime) |
| Base de datos | PostgreSQL 16-alpine via `docker-compose.yaml` (servicio `kaffehausdb`, puerto 5432) |
| Puerto HTTP | `${SERVER_PORT:8081}` |
| Tests | `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` (JUnit 5, Mockito), H2 con `scope` test |
| Devtools | `spring-boot-devtools` (runtime, opcional) |

- **Artefacto**: `com.maestria:kaffehausv4:0.0.1-SNAPSHOT`
- **Paquete raiz**: `com.maestria.kaffehausv4`
- **Clase de arranque**: `Kaffehausv4Application` (`@SpringBootApplication`, sin configuracion extra)
- **Configuracion**: `src/main/resources/application.yaml`, unico archivo, sobreescribible por
  variables de entorno (`JWT_SECRET`, `JWT_REFRESH_SECRET`, `JWT_ISSUER`, `JWT_ACCESS_MINUTES`,
  `JWT_REFRESH_DAYS`, `GOOGLE_CLIENT_ID`, `SEED_HABILITADO`, `SEED_ADMIN_CONTRASENA`,
  `CORS_ORIGENES`, `DB_LOCK_TIMEOUT`, `SERVER_PORT`). Datasource `localhost:5432/kaffehausdb`,
  `spring.jpa.hibernate.ddl-auto: update`, dialecto `PostgreSQLDialect`. Cada conexion del pool
  ejecuta `SET lock_timeout` (`spring.datasource.hikari.connection-init-sql`, por defecto 5 s):
  ninguna operacion espera un bloqueo de fila indefinidamente, y al vencer la espera
  `GlobalExceptionHandler` responde 409 "reintenta" (4.16, 5.7). El esquema y sus limites se
  describen en 4.20.

---

## 2. Patron arquitectonico

**Clean Architecture + DDD tactico, organizado en Bounded Contexts y Vertical Slices por
agregado.** No es una arquitectura por capas horizontales: cada agregado es un modulo
autocontenido con sus cuatro capas dentro.

### 2.1 Jerarquia estructural

```
BoundedContexts / <Contexto> / <Agregado> / {Domain, Application, Infrastructure, Presentation}
```

| Bounded Context | Agregados |
|---|---|
| `Inventario` (12) | Categoria, Conversion, EstadoExistencia, Existencia, Lote, Movimiento, Produccion, Producto, Receta, Tipo, TipoMovimiento, Unidad |
| `Ventas` (6) | Descuento, EstadoVenta, SesionCaja, TipoDescuento, TipoPago, Venta |
| `Personal` (6) | Cliente, Entidad, Persona, Proveedor, Rol, Usuario |
| `Pedidos` (2) | EstadoPedido, Pedido |

Cuatro agregados tienen **entidades hijas** (4.15), que viven dentro de su raiz y no tienen slice,
repositorio ni endpoints propios:

| Raiz | Entidades hijas |
|---|---|
| `Venta` | `DetalleVenta`, `PagoVenta` |
| `Lote` | `DetalleLote` |
| `Receta` | `IngredienteReceta`, `PasoReceta` |
| `SesionCaja` | `MovimientoCaja` |

Mas un **Shared Kernel** transversal: `com.maestria.kaffehausv4.Shared` (3.3).

### 2.2 Regla de dependencia (obligatoria)

```
Presentation ──► Application ──► Domain
                      ▲
Infrastructure ───────┘   (implementa puertos declarados en Application)
```

- **Domain** no importa **nada** de Spring, Jakarta ni de Infrastructure. Es la invariante mas
  dura del proyecto. La unica dependencia externa admitida es `Shared/Domain` (las excepciones).
- **Application** solo puede usar de Spring `@Service` y `@Transactional`.
- **Infrastructure** conoce Application y Domain; nadie conoce a Infrastructure salvo el
  contenedor de Spring, que inyecta sus implementaciones por interfaz.
- **Presentation** solo habla con casos de uso y DTOs. Nunca con repositorios, datasources,
  modelos JPA ni agregados.

### 2.3 Doble indireccion Repository → Datasource

Patron distintivo del proyecto. Cada agregado tiene **dos puertos con la misma firma**:

- `Application/Repositories/I<Agregado>Repository` — el puerto que consume el caso de uso.
- `Application/Datasources/I<Agregado>Datasource` — el puerto de la fuente de datos concreta.

Y dos implementaciones:

- `Infrastructure/Repositories/<Agregado>RepositoryImpl` — **delegacion pura** al datasource.
- `Infrastructure/Datasources/<Agregado>DatasourceImpl` — acceso real a JPA, mapeo y paginacion.

La indireccion permite cambiar el origen de datos (JPA, HTTP, memoria) sin tocar los casos de uso.
**Se mantiene aunque parezca redundante**: es una decision estructural.

### 2.4 Principios

1. **Agregados con constructor privado y mutacion controlada.** Sin setters; se muta solo por
   metodos de negocio.
2. **Modelo de persistencia separado del agregado.** El agregado nunca lleva anotaciones JPA.
3. **Mapeo explicito.** Mappers estaticos escritos a mano; sin MapStruct, Lombok ni reflexion.
4. **Validacion de entrada en el borde de Application** (`createFromRequest` + `Validador`); las
   invariantes, en el dominio. Sin Bean Validation.
5. **Composicion entre agregados en el caso de uso**, nunca en persistencia: ningun
   `DatasourceImpl` importa el `JpaRepository` o `JpaModel` de otro agregado.
6. **Sin eventos de dominio, CQRS, mensajeria ni cache.** El flujo es sincrono y transaccional.
   El unico estado es la base de datos: no hay `@Cacheable`, broker, scheduler, WebSocket ni
   sesion HTTP.
7. **El agregado es la frontera transaccional.** Una raiz se guarda entera, en una llamada a su
   repositorio y dentro de la transaccion del caso de uso.

---

## 3. Mapa del repositorio

```
kaffehausv4/
├── CLAUDE.md                    # Guia de trabajo para la IA
├── .rules/
│   ├── arquitectura.md          # Este documento: plantilla de la arquitectura
│   └── logica.md                # Fuente de la verdad de la logica de negocio
├── pom.xml                      # Dependencias y Java 21
├── docker-compose.yaml          # PostgreSQL 16 local (kaffehausdb:5432)
├── mvnw / mvnw.cmd / .mvn/      # Maven wrapper
└── src/
    ├── main/
    │   ├── java/com/maestria/kaffehausv4/
    │   │   ├── Kaffehausv4Application.java      # Unico @SpringBootApplication
    │   │   ├── Shared/                          # Shared Kernel (3.3)
    │   │   └── BoundedContexts/
    │   │       ├── Inventario/<Agregado>/
    │   │       ├── Ventas/<Agregado>/
    │   │       ├── Personal/<Agregado>/
    │   │       └── Pedidos/<Agregado>/
    │   └── resources/
    │       ├── application.yaml                 # Unico archivo de configuracion
    │       └── schema.sql                       # Esquema fuera de las entidades (4.20)
    └── test/
        ├── java/com/maestria/kaffehausv4/       # Espeja el paquete de produccion (4.21)
        │   └── Soporte/                         # DatosDePrueba, PruebaConBaseDeDatos, EscenarioDeCafeteria
        └── resources/
            ├── application-test.yaml            # Perfil 'test' (H2 en modo PostgreSQL)
            └── h2/esquema-postgresql.sql        # Lo que el esquema define fuera de las entidades
```

### 3.1 Anatomia de un agregado (plantilla obligatoria)

Tomada de `Inventario/Tipo` (19 archivos), el **slice de referencia**: se copia literalmente al
crear un agregado nuevo (6.1). `Inventario/Categoria` es el mismo molde mas un campo `esBorrable`.

```
<Agregado>/
├── Domain/
│   ├── Aggregates/
│   │   └── <Agregado>.java                 # Raiz de agregado (4.13). POJO puro
│   └── ValueObjects/
│       └── <Agregado>Id.java               # VO de identidad (4.14)
│
├── Application/
│   ├── Datasources/
│   │   └── I<Agregado>Datasource.java      # Puerto de fuente de datos (4.7)
│   ├── Repositories/
│   │   └── I<Agregado>Repository.java      # Puerto que consume el caso de uso (4.7)
│   ├── Dtos/
│   │   ├── Crear<Agregado>Request.java     # Entrada (4.2)
│   │   ├── Actualizar<Agregado>Request.java
│   │   ├── Obtener<Agregados>Request.java  # extends PaginationDTO (4.17)
│   │   └── <Agregado>Response.java         # Salida (4.4)
│   └── UseCases/                           # Un archivo = una operacion (4.5)
│       ├── Crear<Agregado>UseCase.java
│       ├── Actualizar<Agregado>UseCase.java
│       ├── Obtener<Agregado>PorIdUseCase.java
│       ├── Obtener<Agregados>UseCase.java
│       └── Eliminar<Agregado>UseCase.java
│
├── Infrastructure/
│   ├── Datasources/
│   │   └── <Agregado>DatasourceImpl.java   # JPA real + paginacion + mapeo (4.9)
│   ├── Repositories/
│   │   └── <Agregado>RepositoryImpl.java   # Delegacion pura (4.8)
│   └── Persistence/Jpa/
│       ├── <Agregado>JpaModel.java         # @Entity. Unico lugar con anotaciones JPA (4.10)
│       ├── <Agregado>JpaMapper.java        # toPersistence / copiarCambios / toDomain (4.11)
│       └── <Agregado>JpaRepository.java    # extends JpaRepository<Model, UUID> (4.12)
│
└── Presentation/
    └── <Agregado>ControllerV1.java         # @RestController, api/v1/<kebab-case> (4.1)
```

La estructura es **cerrada**: no se abren carpetas nuevas (`Services`, `Mappers`, `Helpers`...).
Un agregado con entidad hija anade archivos en estas mismas carpetas (4.15). Un agregado con
operaciones extra anade casos de uso y DTOs en `UseCases/` y `Dtos/`.

### 3.2 Nombres

| Elemento | Convencion | Ejemplo real |
|---|---|---|
| Paquetes de contexto/agregado/capa | **PascalCase** (deliberadamente no estandar en Java) | `BoundedContexts.Inventario.Categoria.Application.UseCases` |
| Clases | PascalCase | `CrearCategoriaUseCase` |
| Interfaces (puertos) | Prefijo `I` + PascalCase | `ICategoriaRepository`, `IHasheadorDeContrasena` |
| Metodos y variables | camelCase, **en espanol sin tildes** | `obtenerPorId`, `contrasenaEnClaro`, `motivoDiferencia` |
| Constantes | `UPPER_SNAKE_CASE` | `LONGITUD_SAL_BYTES`, `CAMPOS_DERIVADOS` |
| Caso de uso | `<Verbo><Agregado>UseCase` | `CrearTipoUseCase`, `AbrirDetalleLoteUseCase` |
| Controller | `<Agregado>ControllerV1` | `TipoControllerV1` |
| Ruta HTTP | `api/v1/<agregado-en-kebab-case>` | `api/v1/tipo-movimiento`, `api/v1/sesion-caja` |
| Tablas y columnas | `snake_case` | `detalle_lote`, `id_estado_venta`, `fec_c` |
| Campos JSON | camelCase | `idEstadoVenta`, `stockMinimo`, `fechaVenta` |

**Pluralizacion en espanol.** Los listados usan el plural real, no una `s` mecanica:
`ObtenerProveedoresUseCase`, `ObtenerConversionesUseCase`, `ObtenerSesionesCajaUseCase`. El
modificador no se pluraliza: `ObtenerTiposDescuentoUseCase`, no `ObtenerTipoDescuentosUseCase`.

### 3.3 Shared Kernel (`Shared/`)

Lo unico compartido entre contextos. Espeja las mismas capas.

| Ruta | Pieza |
|---|---|
| `Shared/Domain/Error/BaseException` | Base abstracta: `codigo` (HTTP), `mensaje`, `tipo`. Extiende `RuntimeException` (4.16) |
| `Shared/Domain/Error/DomainException` | Errores de negocio y validacion. Tipo `"Domain Exception"` |
| `Shared/Domain/Error/SystemException` | Fallos tecnicos (500). Tipo `"System Exception"` |
| `Shared/Domain/Error/AutorizacionException` | 401/403. La usan el filtro, el guardia de roles y los casos de uso que comprueban permisos |
| `Shared/Application/Validation/Validador` | Validador estatico de `Map<String,Object>`. Unico punto de validacion de entrada (4.3) |
| `Shared/Application/Dtos/Pagination/PaginationDTO` | Base de los requests de listado (4.17) |
| `Shared/Application/Dtos/Pagination/PaginatedResponse<T>` | Envoltorio de salida de listados (4.17) |
| `Shared/Application/Crypto/IHasheadorDeContrasena` | Puerto de hasheo de credenciales |
| `Shared/Infrastructure/Crypto/HasheadorDeContrasenaPbkdf2` | `@Component`. PBKDF2WithHmacSHA256, 210 000 iteraciones, sal de 16 bytes, formato `pbkdf2$iter$sal$hash`, comparacion en tiempo constante |
| `Shared/Infrastructure/Persistence/OffsetPageRequest` | `Pageable` por offset absoluto (4.17) |
| `Shared/Presentation/GlobalExceptionHandler` | `@RestControllerAdvice`. Traduce excepciones a `ErrorResponse` (4.16) |
| `Shared/Application/Inicializacion/SembrarSistemaUseCase` | Siembra idempotente de primer arranque (4.19) |
| `Shared/Presentation/Inicializacion/InicializacionControllerV1` | `POST api/v1/seed` (4.19) |
| `Shared/Application/Salud/` | `ConsultarSaludUseCase`, `SaludResponse` y el puerto `IVerificadorDeBaseDeDatos` (4.22) |
| `Shared/Infrastructure/Salud/VerificadorDeBaseDeDatosJdbc` | `@Component`. Valida una conexion del pool con `Connection.isValid` (4.22) |
| `Shared/Presentation/Salud/SaludControllerV1` | `GET api/v1/salud`, health check publico (4.22) |
| `Shared/Presentation/Security/` | Toda la seguridad (4.18) |

### 3.4 Donde colocar cada cosa

| Necesidad | Ubicacion exacta |
|---|---|
| Invariante de una entidad | `Domain/Aggregates/<Agregado>.java` |
| Regla pura sobre varios agregados del mismo tipo ya cargados | Metodo `static` en el agregado (5.10) |
| Identidad tipada | `Domain/ValueObjects/<Agregado>Id.java` |
| Orquestacion de una operacion | `Application/UseCases/<Verbo><Agregado>UseCase.java` |
| Operacion que reutilizan otros casos de uso en su transaccion | Caso de uso interno (4.6) |
| Contrato de entrada / salida HTTP | `Application/Dtos/` |
| Validacion y coercion de tipos de entrada | `Validador` (reutilizar; ampliar solo si falta un tipo) |
| Consulta a base de datos | `Infrastructure/Persistence/Jpa/<Agregado>JpaRepository.java` |
| Traduccion agregado ↔ fila | `Infrastructure/Persistence/Jpa/<Agregado>JpaMapper.java` |
| Endpoint HTTP | `Presentation/<Agregado>ControllerV1.java` |
| Proteger un controller nuevo | `@RequiereRol({"Operador", "Administrador"})` sobre la clase (obligatorio) |
| Restringir un endpoint a menos roles | `@RequiereRol("Administrador")` sobre el metodo |
| Saber quien firma la peticion | `@UsuarioEnSesion String idUsuario` en el controller → `createFromRequest(body, idUsuario)` |
| Utilidad usada por 2+ contextos | `Shared/`, en la capa que le corresponda |
| Regla de negocio nueva | `.rules/logica.md`, en la seccion de su agregado, y despues el codigo |
| Tests | `src/test/java/...`, espejando el paquete de produccion (4.21) |

---

## 4. Las piezas del backend

Cada pieza se describe con su contrato y un ejemplo del slice `Tipo`. Las piezas de un slice se
encadenan siempre igual:

```
Controller → Request.createFromRequest → UseCase.execute → I*Repository → *RepositoryImpl
          → I*Datasource → *DatasourceImpl → *JpaMapper / *JpaRepository → base de datos
          ← *JpaMapper.toDomain ← agregado ← Response.createFromAggregate ← ResponseEntity
```

### 4.1 Controller (`Presentation/<Agregado>ControllerV1`)

Traduce HTTP a casos de uso. No tiene logica.

- `@RestController` + `@RequestMapping("api/v1/<kebab>")` (sin barra inicial) y
  **`@RequiereRol({"Operador", "Administrador"})` sobre la clase, siempre**.
- Inyecta por constructor solo casos de uso.
- Recibe `@RequestBody Map<String, Object> body`: **nunca** un DTO tipado como parametro de
  Spring, porque la validacion vive en `createFromRequest`.
- Sin `@Valid`, sin `try/catch`, sin `ResponseEntity` de error a mano.
- Codigos fijos: `201` en `crear`, `200` en lecturas y `actualizar`, `204` en `eliminar`. Una
  operacion extra que crea algo responde `201`; una que consulta o modifica, `200`.
- Los cinco metodos estandar se llaman `crear`, `obtenerPorId`, `obtenerTodos`, `actualizar` y
  `eliminar`.
- Si la operacion necesita al usuario de la sesion, recibe `@UsuarioEnSesion String idUsuario` y
  lo pasa a `createFromRequest(body, idUsuario)`.

```java
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/tipo")
public class TipoControllerV1 {
    // constructor con los cinco casos de uso

    @PostMapping
    public ResponseEntity<TipoResponse> crear(@RequestBody Map<String, Object> body) {
        CrearTipoRequest request = CrearTipoRequest.createFromRequest(body);
        TipoResponse response = crearTipoUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<TipoResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);
        return ResponseEntity.ok(obtenerTiposUseCase.execute(ObtenerTiposRequest.createFromRequest(params)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarTipoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 4.2 Request (`Application/Dtos/<Verbo><Agregado>Request`)

Valida y tipa la entrada. Es la frontera entre el JSON crudo y el caso de uso.

- Constructor privado + factoria `createFromRequest(Map<String, Object> request)` (o
  `createFromRequest(request, idUsuarioEnSesion)` si necesita al usuario). Solo getters.
- Valida campo a campo con `Validador`. `Crear*Request` usa las variantes `*Requerido` para lo
  obligatorio; `Actualizar*Request` usa siempre `*Opcional` y deja `null` lo ausente (semantica
  PATCH, 4.13).
- **Campos que el cliente no puede enviar** (derivados o inmutables): se declaran en una
  constante (`CAMPOS_DERIVADOS`, `CAMPOS_INMUTABLES`) y se rechazan con 400 si llegan (5.5).
- Una coleccion hija viaja como lista de objetos (`Validador.listaDeObjetosRequerida`) y cada
  elemento se convierte con el request de la linea (`DetalleVentaRequest.createFromRequest`).
- Los `BigDecimal` se construyen desde `Validador.numero*` con `BigDecimal.valueOf(double)`.
- Los requests de operaciones internas (4.6) no viajan por HTTP: se crean con factorias con
  nombre (`crear(...)`, `deVenta(...)`, `deProduccion(...)`) y no con `createFromRequest`.

```java
public class CrearTipoRequest {
    private String nombre;

    private CrearTipoRequest(String nombre) { this.nombre = nombre; }

    public static CrearTipoRequest createFromRequest(Map<String, Object> request) {
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");
        return new CrearTipoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
```

### 4.3 Validador (`Shared/Application/Validation/Validador`)

Clase `final` con metodos estaticos que validan y convierten un valor del `Map`. Cada metodo
existe en variante `Requerido` (lanza 400 si falta o no es del tipo) y `Opcional` (devuelve
`null` si falta, 400 si es de otro tipo):

`texto`, `numero` (`Double`), `entero`, `booleano`, `fecha` (`LocalDateTime`), `listaDeTextos`,
`listaDeObjetos` (rechaza la lista vacia en la variante requerida), `objeto` y `valorDeEnum`.

Si falta un tipo de validacion, **se anade un metodo aqui**; no se valida ad hoc en el DTO.

### 4.4 Response (`Application/Dtos/<Agregado>Response`)

Forma de salida. Nunca se devuelve un agregado ni un `JpaModel` hacia Presentation.

- Constructor privado + factoria `createFromAggregate(<Agregado> ...)`. Solo getters.
- Si la respuesta incluye datos de otros agregados, la factoria **los recibe como parametros**
  (`UsuarioResponse.createFromAggregate(usuario, persona, rol)`); nunca los busca por su cuenta.
- Las lineas de una entidad hija salen anidadas en la respuesta de la raiz
  (`DetalleVentaResponse` dentro de `VentaResponse`).
- Una consulta que no corresponde a un agregado entero tiene su propio response con nombre de lo
  que devuelve (`ArqueoCajaResponse`, `TicketVentaResponse`, `InventarioProductoResponse`).

### 4.5 Caso de uso (`Application/UseCases/<Verbo><Agregado>UseCase`)

Orquesta una operacion. Es el unico lugar donde se abre la transaccion y donde se combinan
agregados.

- `@Service` + `@Transactional` a nivel de clase; las lecturas, `@Transactional(readOnly = true)`.
  Siempre `org.springframework.transaction.annotation.Transactional`.
- **Un unico metodo publico: `execute(...)`.** Para descomponer, metodos `private`.
- Dependencias `private final`, inyeccion por constructor exclusivamente.
- Solo depende de `I*Repository` (de su agregado o de otros), de `IHasheadorDeContrasena` y de
  casos de uso internos (4.6). Nunca de `I*Datasource` ni de Infrastructure.
- Entrada: un request, o `String` crudo para los que van por id (`execute(String id)`,
  `execute(String id, Actualizar...Request request)`).
- Salida: un response, `PaginatedResponse<List<...Response>>` o `void`.
- La existencia de un agregado referenciado se valida aqui:
  `.orElseThrow(() -> DomainException.throwException(404, "..."))`.
- Las reglas de negocio no se escriben aqui si pueden vivir en el agregado: el caso de uso carga,
  llama a metodos del dominio y guarda.

```java
@Service
@Transactional
public class ActualizarTipoUseCase {
    private final ITipoRepository tipoRepository;

    public ActualizarTipoUseCase(ITipoRepository tipoRepository) {
        this.tipoRepository = tipoRepository;
    }

    public TipoResponse execute(String tipoId, ActualizarTipoRequest request) {
        TipoId id = new TipoId(tipoId);
        Tipo tipo = tipoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo con id '" + tipoId + "' no existe"));
        tipo.actualizar(request.getNombre());
        return TipoResponse.createFromAggregate(tipoRepository.actualizar(id, tipo));
    }
}
```

**El listado reconstruye siempre un `PaginatedResponse` nuevo**: copia `skip/limit/total/hasMore`
y sustituye `data` por los responses.

### 4.6 Caso de uso interno

Un caso de uso **sin controller** que otros casos de uso inyectan y ejecutan dentro de su propia
transaccion, para que una operacion compartida viva en un solo lugar. Es la unica excepcion a que
un caso de uso dependa solo de repositorios.

- Mismo contrato que 4.5 (`@Service`, `@Transactional`, `execute`). Como lo invoca otro caso de
  uso, se une a la transaccion del llamador: si falla, revierte todo.
- Su request se arma con una factoria con nombre, no con `createFromRequest` (4.2).
- Puede invocar a otro caso de uso interno.
- **No se crean nuevos sin registrarlos en esta tabla.**

| Operacion interna | Slice | La invocan |
|---|---|---|
| `RegistrarSalidaDeInventarioUseCase` | Movimiento | `CrearProduccionUseCase`, `CrearVentaUseCase` |
| `RegistrarAperturaUseCase` | Existencia | `AbrirDetalleLoteUseCase` |
| `RecalcularStockUseCase` | Producto | Todos los que cambian existencias o unidades de lote: compra, correccion y baja de lotes, apertura, salida, produccion, reintegro y pesaje |
| `RegistrarReintegroDeInventarioUseCase` | Movimiento | `RegistrarAnulacionDeVentaUseCase` |
| `RegistrarAnulacionDeVentaUseCase` | Venta | `AnularVentaUseCase`, `ActualizarPedidoUseCase` (5.12) |

### 4.7 Puertos (`I<Agregado>Repository` e `I<Agregado>Datasource`)

Interfaces en `Application/`. **Las dos tienen firmas identicas.** El CRUD estandar:

```java
public interface ITipoRepository {
    Tipo crear(Tipo tipo);
    Tipo actualizar(TipoId tipoId, Tipo tipo);
    Optional<Tipo> obtenerPorId(TipoId tipoId);
    PaginatedResponse<List<Tipo>> obtenerTodos(ObtenerTiposRequest obtenerTiposRequest);
    void eliminar(TipoId tipoId);
}
```

- Reciben y devuelven agregados y VOs de identidad, nunca `JpaModel` ni `UUID`.
- `Optional<T>` solo como retorno de lecturas de un elemento.
- Una consulta extra se anade a **los dos** puertos con un nombre de negocio
  (`obtenerPorNombre`, `obtenerVigentes`, `obtenerPorClaveIdempotencia`). Si bloquea, lleva el
  sufijo de 5.7 (`...ParaActualizar`, `...ParaConsumo`, `...ParaVenta`).
- Una consulta que alcanza una entidad hija se declara en el puerto de **la raiz**
  (`ILoteRepository.obtenerPorDetalleId`), nunca en un repositorio de la hija (4.15).

### 4.8 RepositoryImpl (`Infrastructure/Repositories`)

`@Repository` que implementa `I<Agregado>Repository` delegando cada metodo al datasource. **Sin
logica, sin excepciones.**

```java
@Override
public Tipo crear(Tipo tipo) {
    return this.tipoDatasource.crear(tipo);
}
```

### 4.9 DatasourceImpl (`Infrastructure/Datasources`)

`@Repository` que implementa `I<Agregado>Datasource`. Aqui viven JPA, el mapeo y la paginacion.

- Inyecta los `JpaRepository` de **su** agregado (la raiz y sus hijas), nunca los de otro.
- Convierte ids con el helper privado obligatorio:

```java
private static UUID aUuid(String valor) {
    try {
        return UUID.fromString(valor);
    } catch (IllegalArgumentException | NullPointerException e) {
        throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
    }
}
```

- `actualizar` y `eliminar` recargan primero el modelo con `findByIdAndBorradoFalse` y lanzan 404
  si no existe.
- `eliminar` es una baja logica: recarga, pasa a dominio, llama `eliminar()` del agregado (que
  decide si la baja es posible), copia los cambios y guarda (5.4).
- Paginacion con `new OffsetPageRequest(skip, limit)`, **nunca `PageRequest.of`**. `search`
  vacio usa `findByBorradoFalse`; con texto, `findByBorradoFalseAnd<Campo>ContainingIgnoreCase`.
  `hasMore = skip + datos.size() < total`.
- Un agregado sin campo de texto buscable ignora `search` y deja la linea
  `//String search = ...` comentada (precedentes: `Venta`, `Lote`).
- En un agregado con hijas, escribe todas sus tablas y concilia las lineas por id (5.8). Las
  lineas de una pagina se resuelven con una consulta por coleccion, no una por raiz.

### 4.10 JpaModel (`Infrastructure/Persistence/Jpa/<Agregado>JpaModel`)

La fila de la tabla. Unico lugar con anotaciones JPA.

- `@Entity`, `@Table(name = "<tabla_snake_case>")`, `@Id` **sin `@GeneratedValue`** y
  `@Column(name = "...")` explicito en cada campo. Constructor publico vacio, getters y setters.
- Auditoria en **todos** los modelos: `fec_c` (`OffsetDateTime`, `nullable = false`), `fec_u`
  (`OffsetDateTime`) y `borrado` (`Boolean`, `nullable = false`).
- Tipos: `UUID` para claves e ids foraneos, `BigDecimal` para dinero y cantidades,
  `OffsetDateTime` para auditoria, `LocalDateTime`/`LocalDate` para fechas de negocio.
- **Sin asociaciones JPA** (`@ManyToOne`, `@OneToMany`, `@JoinColumn`...): las relaciones son
  columnas `UUID` planas (`id_persona`, `id_rol`).

### 4.11 JpaMapper (`Infrastructure/Persistence/Jpa/<Agregado>JpaMapper`)

Clase con constructor privado y tres metodos `static`:

- `toPersistence(agregado)` → modelo nuevo; genera `UUID.randomUUID()` si el id es `null` y fija
  `fecC = OffsetDateTime.now()`.
- `copiarCambios(agregado, model)` → muta un modelo existente y fija `fecU = OffsetDateTime.now()`.
- `toDomain(model)` → `<Agregado>.restaurar(...)`, normalizando nulos
  (`model.getBorrado() != null && model.getBorrado()`).

El mapper de una entidad hija recibe ademas el id de la raiz para completar la columna FK (4.15).

### 4.12 JpaRepository (`Infrastructure/Persistence/Jpa/<Agregado>JpaRepository`)

`interface <Agregado>JpaRepository extends JpaRepository<<Agregado>JpaModel, UUID>` con **solo
query methods derivados**: sin `@Query`, JPQL, SQL nativo ni Criteria. Los del molde:

```java
Optional<TipoJpaModel> findByIdAndBorradoFalse(UUID id);
Page<TipoJpaModel> findByBorradoFalse(Pageable pageable);
Page<TipoJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
Optional<TipoJpaModel> findFirstByNombreIgnoreCaseAndBorradoFalse(String nombre);
```

Un orden fijo se expresa en el nombre (`findByBorradoFalseOrderByFechaAperturaDesc`). Los
metodos con bloqueo siguen la nomenclatura de 5.7. No existe ningun `deleteBy...` (5.4).

### 4.13 Agregado (`Domain/Aggregates/<Agregado>`)

Raiz de agregado: estado + invariantes. POJO puro.

```java
public class Tipo {
    private TipoId id;
    private String nombre;
    private boolean borrado;

    private Tipo(TipoId id, String nombre, boolean borrado) { ... }   // SIEMPRE privado

    public static Tipo restaurar(String id, String nombre, boolean borrado)   // desde persistencia
    public static Tipo crear(String nombre)                                   // nuevo: id null, borrado false
    public void actualizar(String nombre)                                     // patch parcial
    public void eliminar()                                                    // baja logica

    public TipoId getId() {return id;}                                        // getters, sin setters
}
```

- `actualizar(...)` aplica `this.campo = valor != null ? valor : this.campo;`: lo que no llega,
  no cambia.
- `restaurar` recibe primitivos (`String id`) y construye los VOs dentro.
- Las invariantes se validan aqui lanzando `DomainException` (es `Shared/Domain`, no rompe el
  aislamiento). Una operacion que el negocio prohibe en ese agregado (por ejemplo, una baja) la
  rechaza el propio metodo con 409 (5.11).
- Los valores derivados los calcula el agregado y no se reciben (5.5).
- Getters de `boolean` con prefijo `is`. Sin `equals`/`hashCode`/`toString`, sin Lombok.
- Un agregado que reconoce la semantica de una fila de catalogo lo hace por nombre con un metodo
  propio (`TipoPago.esEfectivo()`), nunca con constantes de id (5.6).

### 4.14 Value Object de identidad (`Domain/ValueObjects/<Agregado>Id`)

Wrapper inmutable minimo sobre `String`, con `getValue()`. Hay uno por agregado y por entidad
hija referenciada desde fuera (`DetalleLoteId`), y es el unico tipo de VO del proyecto. Un
agregado guarda las referencias a otros como VOs (`Usuario` guarda `PersonaId` y `RolId`). La
conversion a `UUID` ocurre exclusivamente en Infrastructure.

### 4.15 Entidad hija

Una entidad que solo existe dentro de su raiz (2.1). Todas siguen la misma forma, que es la
plantilla si aparece otra:

- **Dominio.** Se declara en `Domain/Aggregates/`, en el mismo paquete que su raiz. Sus factorias
  de creacion son **package-private** (`static DetalleVenta crear(...)`): solo la raiz la
  instancia. `restaurar(...)` es publica porque la invoca el mapper. **No guarda el id de su
  padre**: dentro del agregado la relacion es la contencion.
- **Raiz.** Expone la coleccion como vista de solo lectura (`Collections.unmodifiableList`) y
  concentra las mutaciones en metodos de negocio (`agregarDetalle`, `removerDetalle(id)`). Tiene
  dos vistas: `getDetalles()` con todas, incluidas las dadas de baja (lo que la persistencia
  escribe), y `getDetallesActivos()` con las vivas (lo que suma, valida y sale en la respuesta).
  `validarRegistrable()` comprueba las invariantes de la coleccion antes de persistir.
  `eliminar()` de la raiz arrastra a todas sus hijas.
- **Aplicacion.** El request de la linea viaja anidado en el de la raiz y el response de la
  linea, anidado en el de la raiz. La capa de aplicacion nunca muta la coleccion directamente.
- **Persistencia.** La hija tiene su `JpaModel`, `JpaMapper` y `JpaRepository` **dentro del
  slice de la raiz**. El datasource de la raiz inyecta todos los `JpaRepository` y escribe todas
  las tablas; la atomicidad la da la transaccion del caso de uso. La columna FK
  (`detalle_venta.id_venta`) la completa el mapper a partir de la raiz.
- **Acceso desde otro agregado.** Si otro agregado referencia una hija (`Existencia` guarda un
  `DetalleLoteId`), el puerto de la raiz expone una consulta que la alcanza y devuelve la raiz
  duena (`ILoteRepository.obtenerPorDetalleId`). El VO de la hija vive en `Domain/ValueObjects/`
  del slice de la raiz y es publico para esto.
- **Sin** repositorio, datasource, caso de uso ni controller propios.

### 4.16 Errores

Jerarquia unica: `BaseException` (abstracta) → `DomainException` | `SystemException` |
`AutorizacionException`.

- Se crean **siempre** con la factoria estatica `DomainException.throwException(codigo, "...")`,
  que **devuelve** la excepcion: `throw DomainException.throwException(...)` o
  `.orElseThrow(() -> DomainException.throwException(...))`.
- `codigo` es el status HTTP. Reparto: `400` validacion, campo no admitido o UUID malformado;
  `401`/`403` autenticacion y permisos (`AutorizacionException`); `404` recurso o fila de
  catalogo inexistente; `409` conflicto con el estado actual (la regla de negocio que lo provoca
  esta en `logica.md`); `500` fallo tecnico (`SystemException`).
- `GlobalExceptionHandler` responde
  `record ErrorResponse(int codigo, String tipo, String error, String mensaje, Object detalle, OffsetDateTime timestamp)`.
  Si `HttpStatus.resolve(codigo)` es nulo, cae a 500. Tiene handlers para `DomainException`,
  `SystemException`, `BaseException` (cubre `AutorizacionException`), `RuntimeException` y dos
  excepciones de Spring que surgen al confirmar la transaccion, fuera del alcance de un caso de
  uso:
  - `PessimisticLockingFailureException` (interbloqueo o espera abortada) → 409 pidiendo
    reintentar.
  - `DataIntegrityViolationException` de un indice unico que respalda una regla
    (`uq_venta_clave_idempotencia`, `uq_sesion_caja_abierta`) → 409. Cualquier otra violacion de
    integridad sigue siendo 500.
- Las excepciones crudas de JPA/JDBC no se propagan: se traducen a `DomainException` o
  `SystemException`.
- **Error estructurado** (para errores que el cliente debe poder resolver solo, sin interpretar el
  texto del mensaje). `DomainException` lleva dos datos opcionales, nulos en todos los demas
  errores:
  - `error`: codigo de negocio estable en `MAYUSCULAS_CON_GUION_BAJO`. Cada codigo lo define la
    regla de `logica.md` que lo provoca; el `codigo` HTTP sigue siendo el reparto de arriba.
  - `detalle`: `Map<String, Object>` de datos simples (`String`, `BigDecimal`, `LocalDate`, numeros
    y otros mapas o listas), sin agregados, `JpaModel` ni DTOs. Admite valores nulos, asi que se
    arma con `LinkedHashMap` (conserva el orden de las claves en el JSON) y no con `Map.of`;
    `DomainException` guarda una copia inmodificable.
  Se crea con la sobrecarga `DomainException.throwException(codigo, error, mensaje, detalle)`, que
  tambien devuelve la excepcion. No se crean subclases de `DomainException` por cada error: la
  jerarquia sigue siendo la de arriba. `GlobalExceptionHandler` y `EscritorDeErrorHttp` copian
  ambos campos al `ErrorResponse`, que los **omite del JSON cuando son nulos**
  (`@JsonInclude(NON_NULL)` sobre el record), de modo que el contrato de los errores existentes no
  cambia. El caso de uso que llama a otro no captura ni traduce el error: lo deja pasar tal cual.
- **Errores estructurados vigentes:**

  | `error` | HTTP | Regla | Lo lanza | `detalle` |
  |---|---|---|---|---|
  | `APERTURA_REQUERIDA` | 409 | VENTA-30 | `RegistrarSalidaDeInventarioUseCase` | `{ faltantes: [ { idProducto, producto, requerido, disponibleAbierto, unidad, bolsasNecesarias, bolsaSugerida: { idDetalleLote, idProductoPresentacion, presentacion, fechaVencimiento, contenido } } ] }` |
  | `STOCK_INSUFICIENTE` | 409 | VENTA-30, LOTE-17 | `RegistrarSalidaDeInventarioUseCase` | `{ faltantes: [ { idProducto, producto, requerido, disponible, unidad } ] }` |

  Las cantidades son `BigDecimal` a 4 decimales en la unidad actual del insumo (GENERAL-10);
  `fechaVencimiento` es `yyyy-MM-dd` o nulo; `faltantes` trae una entrada por producto del plan de
  consumo que no alcanza, en orden de id de producto. Ejemplo:

  ```json
  {
    "codigo": 409,
    "tipo": "Domain Exception",
    "error": "APERTURA_REQUERIDA",
    "mensaje": "Stock abierto insuficiente de 'Leche': abre una presentacion para continuar",
    "detalle": {
      "faltantes": [
        {
          "idProducto": "<leche>",
          "producto": "Leche",
          "requerido": 0.4000,
          "disponibleAbierto": 0.1500,
          "unidad": "Litro",
          "bolsasNecesarias": 1,
          "bolsaSugerida": {
            "idDetalleLote": "<detalle>",
            "idProductoPresentacion": "<leche 1 l>",
            "presentacion": "Leche 1 l",
            "fechaVencimiento": "2026-10-30",
            "contenido": 1.0000
          }
        }
      ]
    },
    "timestamp": "2026-10-23T10:15:00-04:00"
  }
  ```

  Un error estructurado nuevo se agrega a esta tabla junto con su regla en `logica.md`.
- **Mensajes en espanol, sin tildes**, citando el campo o id entre comillas simples:
  `"La categoria con id '...' no existe"`, `"El campo 'nombre' es obligatorio..."`.
- No hay logging: ni `Logger`, ni `System.out`, ni `printStackTrace`. La trazabilidad se apoya en
  el `ErrorResponse`. Si algun dia se necesita, se disena centralizado y se documenta aqui primero.

### 4.17 Paginacion

- **Entrada**: `Obtener<Agregados>Request extends PaginationDTO`, con `createFromRequest` que lee
  `skip`, `limit` y `search` del `Map` por casting directo. `PaginationDTO` aplica los defaults
  `skip = 0`, `limit = 10`, `search = ""`.
- **Consulta**: `OffsetPageRequest(skip, limit)`, un `Pageable` por offset absoluto.
  `PageRequest.of` esta prohibido porque traduce mal el offset cuando `skip` no es multiplo de
  `limit`.
- **Salida**: `PaginatedResponse<T>` con `data`, `skip`, `limit`, `total` y `hasMore`. En las capas
  internas `PaginatedResponse<List<Tipo>>`; hacia fuera `PaginatedResponse<List<TipoResponse>>`.
- `PaginationDTO` y `PaginatedResponse` son las unicas clases de negocio con constructor publico.

### 4.18 Seguridad (`Shared/Presentation/Security`)

La seguridad **no es un bounded context**: no tiene dominio, agregado ni tablas propias, vive
entera en este paquete y es **la capa mas externa**. Es el unico paquete que importa
`org.springframework.security` o `io.jsonwebtoken`, y es plano a proposito.

| Pieza | Responsabilidad |
|---|---|
| `SecurityConfig` | `@EnableWebSecurity`. Cadena stateless, sin CSRF; `/api/v1/auth/**` y `GET /api/v1/salud` (4.22) abiertos y el resto autenticado (`anyRequest().authenticated()`); CORS (`corsConfigurationSource`, dentro de la cadena porque el preflight se resuelve antes de Spring MVC) con los origenes enumerados en `seguridad.cors.origenes-permitidos` (`CORS_ORIGENES`, por defecto `localhost` en cualquier puerto): admite patrones acotados y rechaza al arrancar el comodin `*` a secas; registro del interceptor y del resolvedor |
| `JwtAuthenticationFilter` | `OncePerRequestFilter`. **Unico sitio donde se valida un token entrante**. Deja al usuario en el `SecurityContext` |
| `JwtService` | `@Component`. Unico punto que conoce jjwt: firma, claims y vigencia |
| `RequiereRol` | Anotacion de clase o metodo con nombres de rol tal como estan en la tabla `rol`. La del metodo manda sobre la de la clase |
| `GuardiaDeRoles` | `HandlerInterceptor` que aplica `@RequiereRol`: 401 sin token, 403 si el rol no figura |
| `UsuarioEnSesion` | Anotacion de parametro: el id del usuario del token. Evita que los controllers importen Spring Security |
| `ResolvedorDeUsuarioEnSesion` | `HandlerMethodArgumentResolver` de `@UsuarioEnSesion`; 401 si no hay sesion |
| `UsuarioAutenticado` | `record` con los datos que viajan en el token y que devuelve el login |
| `SecurityUser` | Adaptador `UserDetails` sobre `UsuarioAutenticado`. Concede `ROLE_<NOMBRE_DEL_ROL>` |
| `AutenticacionController` | `/api/v1/auth`: `login`, `google`, `refresh` |
| `AutenticacionService` | Los tres flujos. Reutiliza `IUsuarioRepository`, `IPersonaRepository` e `IRolRepository` |
| `VerificadorDeTokenGoogle` | Comprueba el `id_token` de Google y devuelve el correo verificado |
| `EscritorDeErrorHttp` | Escribe `ErrorResponse` desde el filtro, donde `GlobalExceptionHandler` no llega |
| `Dtos/` | `record`s de entrada y salida de los tres endpoints |

Como se usa desde un agregado:

- **Todo controller lleva `@RequiereRol({"Operador", "Administrador"})` sobre la clase**; un
  metodo se restringe mas con su propia anotacion (`@RequiereRol("Administrador")`). Que
  operaciones exigen que rol lo decide `logica.md`.
- **El cliente nunca envia `idUsuario`**: el controller lo recibe con `@UsuarioEnSesion` y lo
  pasa a `createFromRequest(body, idUsuario)`; si llega en el cuerpo, se ignora.
- **Los roles son filas de la tabla `rol`**, no enums ni constantes. El nombre viaja firmado en
  el token y es contra el que compara `@RequiereRol`. Crear un rol es insertar una fila.
- Un permiso que depende de los datos (por ejemplo, "solo el titular o un Administrador") lo
  decide el agregado; el caso de uso resuelve el rol por nombre y lanza `AutorizacionException`.

Decisiones deliberadas: la autenticacion **no sigue la plantilla de agregado**
(`AutenticacionController` + `AutenticacionService`, sin casos de uso, puertos ni datasource
propio); los agregados **no** implementan `UserDetails` ni conocen `GrantedAuthority`; las
contrasenas usan `IHasheadorDeContrasena` (PBKDF2), **no** `PasswordEncoder`/BCrypt; no se usa
`AuthenticationManager` ni `DaoAuthenticationProvider`. Keycloak esta descartado.

### 4.19 Siembra (`Shared/.../Inicializacion`)

`SembrarSistemaUseCase` (`POST api/v1/seed`, `InicializacionControllerV1`) deja una base vacia
lista para usar. Es **idempotente**: busca cada fila por nombre (`obtenerPorNombre`) y crea solo
lo que falta; la respuesta lista lo `creados` y lo `existentes`. Que se siembra lo fija
`logica.md` (SIEMBRA-01 y las decisiones de catalogo de cada agregado).

- Depende de repositorios de todos los contextos, como la autenticacion. No es un agregado ni
  tiene datasource propio.
- El controller no lleva `@RequiereRol`: la ruta es publica mientras `seguridad.seed-habilitado`
  (`SEED_HABILITADO`, por defecto `false`) sea verdadero; en `false`, solo la alcanza un
  `Administrador`. Se activa solo para la primera siembra de una base nueva.
- La contrasena del administrador inicial sale de `seguridad.seed-admin-contrasena`
  (`SEED_ADMIN_CONTRASENA`, sin valor por defecto). La lee el controller con `@Value` y la pasa a
  `SembrarSistemaUseCase.execute(contrasena)`, porque la capa de aplicacion no lee configuracion
  (2.2). Que pasa si falta lo fija `logica.md` (SIEMBRA-02).
- Una fila que un flujo reconoce por nombre (5.6) debe estar en la siembra.

Primer arranque: `docker compose up -d`, levantar la aplicacion con `SEED_HABILITADO=true` y
`SEED_ADMIN_CONTRASENA` definidos (`ddl-auto: update` crea las tablas y despues el script de 4.20
crea el resto del esquema), llamar una vez a `POST api/v1/seed` y volver a arrancar sin
`SEED_HABILITADO`.

### 4.20 Esquema de base de datos

- Las tablas las crea y amplia Hibernate desde los `JpaModel` (`ddl-auto: update`). `update` no
  borra columnas: retirar un campo de un modelo exige un `ALTER TABLE ... DROP COLUMN` manual en
  las bases existentes.
- Todas las tablas tienen `borrado`, `fec_c` y `fec_u` (4.10). PK `UUID` generada por el mapper.
- Hay objetos de esquema **que no se derivan de las entidades** y de los que dependen piezas del
  codigo. Estan versionados en `src/main/resources/schema.sql`:
  - `seq_numero_ticket` y el valor por defecto de `venta.numero_ticket` (el modelo lo mapea con
    `@Generated` e `insertable/updatable = false`, y `VentaDatasourceImpl.crear` usa
    `saveAndFlush` para releerlo).
  - Indices unicos parciales `uq_sesion_caja_abierta` y `uq_receta_paso` (`... WHERE borrado =
    FALSE`), e indice unico parcial `uq_venta_clave_idempotencia`.
  - En `movimientos`: `id_existencia` sin `NOT NULL` (Hibernate no lo quita en una base
    existente) y las restricciones `chk_movimiento_sujeto` (exactamente una de `id_existencia` e
    `id_detalle_lote`), `chk_movimiento_documento` (no `id_produccion` e `id_venta` a la vez) y
    `chk_movimiento_cantidad` (`cantidad >= 0`).
  - Las claves foraneas de todas las columnas UUID que apuntan a otra tabla (las relaciones son
    columnas planas sin asociaciones JPA, 2.4, asi que Hibernate no las crea). Se declaran
    `DEFERRABLE INITIALLY DEFERRED NOT VALID`: se comprueban al confirmar la transaccion, porque sin
    asociaciones Hibernate no ordena los `INSERT` por dependencia, y no revisan las filas
    historicas, para que una base con huerfanas de antes siga arrancando. Sin `ON DELETE`: el
    borrado es siempre logico. Usan los nombres del script antiguo para reemplazar las que existan.
- **El script** lo ejecuta la inicializacion SQL de Spring Boot (`spring.sql.init.mode: always`)
  en cada arranque, despues de Hibernate (`spring.jpa.defer-datasource-initialization: true`).
  Por eso cada sentencia es idempotente (`IF NOT EXISTS`, y los `CHECK` con `DROP CONSTRAINT IF
  EXISTS` seguido de `ADD CONSTRAINT`, igual que las FK). No admite bloques `DO $$ ... $$`, porque Spring separa las
  sentencias por `;`. Si una sentencia falla, la aplicacion no arranca. No siembra filas: los
  datos iniciales son de la siembra (4.19).
- Un objeto de esquema nuevo que no salga de las entidades se agrega a ese script y, si las
  pruebas lo necesitan y H2 lo admite, tambien a `src/test/resources/h2/esquema-postgresql.sql`,
  que el perfil `test` ejecuta en su lugar. Ese script crea la secuencia del ticket y las
  restricciones de `movimientos`; los indices parciales no se pueden reproducir en H2.
- Sin versionar siguen las demas restricciones del script antiguo (`chk_lote_origen`,
  `chk_distintas_unidades` y otras `CHECK` de importes y saldos) (7.5). Las FK no se reproducen en
  H2.
- Un indice unico parcial se usa cuando hay unicidad bajo borrado logico: una fila dada de baja
  conserva sus valores y una `UNIQUE CONSTRAINT` normal impediria reutilizarlos.

### 4.21 Tests

Bajo `src/test/java/com/maestria/kaffehausv4/`, espejando el paquete de produccion.

- **Unitarios** (JUnit 5 + Mockito, sin Spring ni base): dominio y casos de uso con repositorios
  simulados. Datos comunes en `Soporte/DatosDePrueba`.
- **De persistencia** (`*IntegracionTest`): extienden `Soporte/PruebaConBaseDeDatos`, que levanta
  el contexto completo con el perfil `test` contra H2 en memoria en modo PostgreSQL
  (`application-test.yaml`). Hibernate crea el esquema y `h2/esquema-postgresql.sql` agrega lo que
  vive fuera de las entidades. Cada prueba corre en una transaccion que se revierte: no limpian a
  mano y no pueden comprobar el rollback de un caso de uso fallido, solo su error.
  `Soporte/EscenarioDeCafeteria` arma escenarios (siembra, productos, compras, caja) con los
  mismos casos de uso que la API.
- **Seguridad**: `GuardiaDeRolesTest` prueba el guardia. Ningun test verifica todavia que cada
  controller declare `@RequiereRol` (pendiente, 7.5).
- **Limites de H2**: no reproduce los indices unicos parciales ni los bloqueos `FOR UPDATE` /
  `FOR SHARE`; lo que dependa de ellos (unicidad entre transacciones, concurrencia) no se prueba.
- Un agregado nuevo trae, como minimo, test de su agregado y de sus casos de uso; si tiene
  consultas o mapeo propios, un `*IntegracionTest` de su datasource.

### 4.22 Salud (`Shared/.../Salud`)

`GET api/v1/salud` (`SaludControllerV1`) informa si el sistema y su base de datos estan
disponibles, para un balanceador, Docker o un monitor externo. No se usa Spring Boot Actuator
porque seria una dependencia nueva (7.2, regla 9).

- **Publica**: `SecurityConfig` abre solo el `GET` de `RUTA_DE_SALUD`, y el controller no lleva
  `@RequiereRol`. Por eso el cuerpo solo dice `"UP"` o `"DOWN"`, sin versiones, latencias ni
  mensajes de error: `{"estado": "UP", "baseDeDatos": "UP"}`.
- **Codigo HTTP**: 200 si `estado` es `"UP"`, 503 si es `"DOWN"`, con el mismo cuerpo. El sistema
  esta disponible solo si la base lo esta.
- **Capas**: `ConsultarSaludUseCase` depende del puerto `IVerificadorDeBaseDeDatos`
  (`Shared/Application/Salud`); el adaptador `VerificadorDeBaseDeDatosJdbc`
  (`Shared/Infrastructure/Salud`) pide una conexion al pool y la valida con
  `Connection.isValid`, sin escribir SQL (regla 18). Cualquier fallo se traduce a `false` y no se
  propaga (regla 22).
- **Sin `@Transactional`**: abrir la transaccion pediria una conexion antes de entrar al caso de
  uso, y con la base caida responderia un error en vez de `"DOWN"`.
- Con la base caida, la respuesta tarda hasta el `connection-timeout` del pool de Hikari (30 s
  por defecto), que es lo que espera `getConnection`.

---

## 5. Patrones reutilizables

Soluciones que ya existen y que se reutilizan al ampliar el sistema. Los ejemplos citan donde
estan hoy; la regla de negocio que cada caso implementa esta en `logica.md`.

### 5.1 Recorridos de referencia

**Escritura**

```
POST /api/v1/tipo  { "nombre": "Bebidas" }
  ▼ TipoControllerV1.crear(@RequestBody Map<String,Object> body)
  ▼ CrearTipoRequest.createFromRequest(body)    Validador → DomainException(400) si falla
  ▼ CrearTipoUseCase.execute(request)           @Transactional
  │    Tipo.crear(nombre)                       agregado sin id, borrado = false
  ▼ ITipoRepository.crear → TipoRepositoryImpl → ITipoDatasource.crear → TipoDatasourceImpl
  │    TipoJpaMapper.toPersistence(...)         genera UUID y fecC
  │    tipoJpaRepository.save(model)
  │    TipoJpaMapper.toDomain(model)
  ▼ TipoResponse.createFromAggregate(creado)
  ▼ ResponseEntity.status(201).body(response)
```

**Listado**

```
GET /api/v1/tipo?skip=0&limit=10&search=cafe
  ▼ Controller: skip/limit/search → HashMap → ObtenerTiposRequest.createFromRequest
  ▼ ObtenerTiposUseCase.execute                 @Transactional(readOnly = true)
  ▼ Datasource: OffsetPageRequest → findByBorradoFalse | findByBorradoFalseAndNombreContainingIgnoreCase
  ▼ PaginatedResponse<List<Tipo>> → el caso de uso lo remapea a PaginatedResponse<List<TipoResponse>>
```

Si algo lanza `BaseException`, `GlobalExceptionHandler` devuelve el `ErrorResponse` con el
`codigo` como status.

### 5.2 Composicion entre agregados

Cuando una operacion o una respuesta necesita otro agregado, **el caso de uso inyecta el
`I*Repository` del otro agregado** y compone. Ejemplos: `CrearUsuarioUseCase` crea o reutiliza
la `Persona` en la misma transaccion, valida el `Rol`, hashea la contrasena y compone
`UsuarioResponse.createFromAggregate(usuario, persona, rol)`; `ObtenerVentasUseCase` resuelve el
`EstadoVenta` de cada venta en un `toResponse(...)` privado.

`Persona` es un agregado sin endpoint ni casos de uso propios: la crean y actualizan los casos de
uso de `Usuario`, `Cliente` y `Proveedor`, y la leen otros que necesitan un nombre (estado de caja,
ticket).

### 5.3 Transacciones

La transaccion se abre y cierra en el caso de uso (4.5); ni el controller ni el datasource la
gestionan. Una operacion que toca varios agregados (o invoca un caso de uso interno) ocurre en
una sola transaccion: si una parte falla, revierte todo.

### 5.4 Borrado logico

**Ninguna fila se elimina jamas.** `DELETE /api/v1/<recurso>/{id}` responde 204 y persiste un
UPDATE de `borrado = true`.

- Toda lectura filtra lo borrado: `findByIdAndBorradoFalse`, `findByBorradoFalse`,
  `findByBorradoFalseAnd<Campo>ContainingIgnoreCase`.
- La decision de la baja es del dominio (`eliminar()` del agregado), no del SQL (4.9).
- **No existe `deleteById`, `deleteAll` ni un `deleteBy...` derivado en `src/main`.** Si aparece
  uno, es un error. Tampoco se regeneran los ids de una entidad hija (5.8).
- Un agregado cuyo negocio prohibe la baja conserva su `EliminarUseCase` y su ruta `DELETE`, y su
  `eliminar()` responde 409 (5.11).

### 5.5 Valores derivados y campos no admitidos

Lo que el sistema calcula (totales, subtotales, secuencias, estados iniciales...) **lo calcula la
raiz** y se recalcula en cada mutacion; nunca se acepta del cliente.

- El request declara los campos que no admite en una constante y responde 400 si llegan:

```java
private static final List<String> CAMPOS_DERIVADOS = List.of("total", "idEstadoVenta", "idSesionCaja", "numeroTicket");
```

- Igual para un `PUT` que solo admite algunos campos (`CAMPOS_INMUTABLES` en
  `ActualizarProduccionRequest`, `ActualizarExistenciaRequest`).
- Las escalas y el redondeo los fija `logica.md` (GENERAL-10); se aplican en la raiz con
  `BigDecimal.setScale(..., RoundingMode.HALF_UP)`.

### 5.6 Catalogo con semantica, resuelto por nombre

Cuando un flujo necesita una fila concreta de un catalogo (un estado, un tipo de movimiento, un
rol, un tipo de pago), **la busca por nombre**, nunca por una constante de id.

- El puerto del catalogo expone `obtenerPorNombre(String nombre)`, sin distinguir mayusculas,
  respaldado por `findFirstByNombreIgnoreCaseAndBorradoFalse`.
- Si la fila no existe, el flujo responde 404 nombrandola.
- La siembra crea esas filas (4.19).
- Si el agregado del catalogo necesita reconocer su propia semantica, lo hace con un metodo
  (`TipoPago.esEfectivo()`, `TipoDescuento.esPorcentaje()` / `esMontoFijo()`).
- Excepcion documentada: el cliente generico de las ventas tiene un id fijo (`Cliente.ID_MOSTRADOR`)
  porque es una fila de datos, no de catalogo.

Para proteger una fila base de la que dependen flujos, el agregado tiene un campo `esBorrable` y
su `eliminar()` rechaza la baja (precedentes: `Categoria`, `Unidad`, `Conversion`).

### 5.7 Bloqueos

Siguen siendo query methods derivados; el bloqueo se declara con `@Lock` y se distingue por el
prefijo del metodo y el sufijo del puerto.

| Bloqueo | `@Lock` | Prefijo del derivado | Sufijo en el puerto | Uso |
|---|---|---|---|---|
| Exclusivo (`FOR UPDATE`) | `PESSIMISTIC_WRITE` | `read...By` (gemelo de su `find...By`) | `...ParaActualizar`, `...ParaConsumo` | Lectura que precede a una modificacion de inventario o de caja |
| Compartido (`FOR SHARE`) | `PESSIMISTIC_READ` | `get...By` | `...ParaVenta` | Lectura de la sesion de caja que hace una venta |

- Donde se bloquea y en que orden lo fija `logica.md` (GENERAL-15, EXISTENCIA-14,
  SESIONCAJA-09). Para respetar el orden, un caso de uso que bloquea filas de varios productos
  procesa sus lineas en orden de id de producto (el plan de consumo de
  `RegistrarSalidaDeInventarioUseCase`, un `TreeMap` por id, y la devolucion de
  `RegistrarReintegroDeInventarioUseCase`).
- Con Hibernate, bloquear una entidad ya cargada no refresca su estado: un valor que se proyecta
  (como `producto.stock`) se bloquea **y despues** se recalcula desde sus fuentes, nunca sumando o
  restando sobre el valor leido (`RecalcularStockUseCase`).
- **La lectura con bloqueo debe ser la primera carga de la fila en la transaccion.** Si un caso
  de uso necesita leer algo de una fila antes de bloquearla (para ordenar los bloqueos o para
  planificar), lo lee con una **proyeccion de interfaz cerrada**: un query method derivado cuyo
  tipo de retorno es una interfaz con solo los getters que necesita
  (`Optional<ExistenciaDetalleLoteProyeccion> findDetalleLoteByIdAndBorradoFalse(UUID id)`). Spring Data
  lee esas columnas sin cargar la entidad en el contexto de persistencia, asi que el `read...By`
  posterior trae el estado vigente. Sigue siendo un metodo derivado: sin `@Query` ni SQL.
  - La interfaz vive junto a su `JpaModel` en `Infrastructure/Persistence/Jpa/` y se llama
    `<Modelo><Lo que trae>Proyeccion`. El datasource la mapea a un VO o, si trae todas las
    columnas, a un agregado (`LoteJpaMapper.toDomain(LoteLecturaProyeccion, ...)`).
  - En el puerto el metodo dice que no carga: `obtenerProductoDeDetalle`, `obtenerLoteDeDetalle`,
    `obtenerDetalleLoteDe`, `obtenerPorProductosSinCargar`.
  - Precedentes: el reintegro averigua el producto de cada linea o existencia antes de bloquearla
    (`RegistrarReintegroDeInventarioUseCase`) y la salida planifica con los lotes sin cargar
    (`RegistrarSalidaDeInventarioUseCase`).
- **Un agregado con hijas se bloquea por su raiz** (logica.md LOTE-27). Las hijas no tienen
  bloqueo propio: el datasource bloquea la fila de la raiz con su `read...By` y lee las hijas
  despues, de modo que el agregado entero se carga con el bloqueo tomado. Varias raices se
  bloquean en una sola lectura ordenada por id (`readByIdInAndBorradoFalseOrderByIdAsc`), el
  orden de GENERAL-15. En el puerto: `obtenerPorIdParaActualizar`, `obtenerPorIdsParaActualizar`
  y `obtenerPorDetalleIdParaActualizar`, que averigua la raiz con una proyeccion y delega.
  Precedente: `Lote`; lo usan la salida, el reintegro, la apertura, el PUT y la baja de lotes.
- **Un datasource no reescribe lo que la operacion no cambio.** Al guardar una raiz con
  colecciones, la conciliacion (5.8) solo copia a su fila las hijas que difieren de ella
  (`DetalleLoteJpaMapper.difiere`): reescribir una linea que la operacion no toco pisaria, con un
  valor ya leido, lo que otra transaccion haya hecho con ella.

### 5.8 Conciliacion por id de una coleccion hija

Un `PUT` con la coleccion hija **no la reemplaza**: la linea con `id` se corrige, la que no trae
`id` se agrega y la ausente se da de baja logica.

- En el dominio: `removerDetalle(id)` marca la linea y **la conserva en la coleccion** para que la
  baja llegue a la base como UPDATE.
- En el datasource: un metodo `conciliar(...)` por linea; si ya tenia identidad, recupera su fila
  y copia los cambios; si no, crea una nueva.
- Motivo: sin borrado fisico, reemplazar dejaria una fila muerta por linea modificada, y otras
  tablas pueden referenciar a la linea (`existencias.id_detalle_lote`, `ON DELETE RESTRICT`).
- Si la raiz gobierna **varias colecciones** (`Receta`), cada una se concilia por separado: la
  coleccion que no viene en el payload queda intacta. `validarRegistrable()` comprueba todas.

### 5.9 Coleccion ordenada

Si una coleccion tiene orden (los pasos de una receta), el numero de secuencia es un valor
derivado mas: no se recibe, el orden del array es la secuencia y la raiz asigna 1..N sin huecos
en cada mutacion (`moverPasoAlFinal(id)` aplicado en el orden del array; `reordenarPasos(ids)`
exigiendo exactamente el conjunto vivo).

Con unicidad del numero bajo borrado logico hace falta un **indice unico parcial** (4.20), y como
una renumeracion colisiona consigo misma a medio camino, el datasource escribe **en dos fases**:
aparca los numeros en negativos, hace `flush()` y escribe los definitivos (`RecetaDatasourceImpl`).

### 5.10 Funciones de dominio sobre colecciones

Una regla pura que opera sobre varios agregados del mismo tipo ya cargados por el caso de uso es
un metodo `static` del agregado: `Conversion.factorEntre`, `Lote.aperturablesEnOrden`,
`Lote.conUnidadesEnOrden`, `Lote.dueno`. El caso de uso carga y el
dominio decide.

### 5.11 Operacion estandar que el negocio rechaza

Si el negocio prohibe una de las cinco operaciones en un agregado (crear a mano, modificar o dar
de baja), **no se retira la pieza**: el caso de uso y la ruta se conservan y la rechaza el
agregado (o el caso de uso, si no hay agregado que consultar) con 409. Asi el contrato HTTP es
uniforme y la regla vive en un solo sitio. Precedentes: `POST api/v1/existencia`,
`POST|PUT|DELETE api/v1/movimiento`, `DELETE` de `venta`, `produccion` y `pedido`.

### 5.12 Invocacion entre contextos

Un contexto solo usa otro a traves de sus `I*Repository`, salvo un caso: `ActualizarPedidoUseCase`
(Pedidos) invoca el caso de uso interno `RegistrarAnulacionDeVentaUseCase` (Ventas) para que la
anulacion viva en un solo sitio. Es el unico precedente; uno nuevo se registra en 4.6.

### 5.13 Rutas fuera de los cinco verbos

La plantilla expone `POST`, `GET {id}`, `GET`, `PUT {id}` y `DELETE {id}`. Una operacion que no es
ninguno de ellos (abrir, anular, cerrar, consultar algo derivado) se publica como subruta del
recurso, en kebab-case, con su propio caso de uso, **y se registra aqui**:

| Ruta | Caso de uso | Rol (ademas del de la clase) | Por que no es uno de los cinco |
|---|---|---|---|
| `GET api/v1/lote/detalles-aperturables?idProducto=` | `ObtenerDetallesAperturablesUseCase` | — | Consulta derivada sobre las lineas de varios lotes |
| `POST api/v1/lote/detalles/{idDetalleLote}/apertura` | `AbrirDetalleLoteUseCase` | — | Opera sobre una linea a traves de su raiz; crea una existencia (201) |
| `GET api/v1/producto/{id}/inventario` | `ObtenerInventarioProductoUseCase` | — | Consulta derivada del inventario de un producto |
| `POST api/v1/producto/{id}/pesaje` | `RegistrarPesajeUseCase` | `Administrador` | Ajusta las existencias de un producto, no el producto |
| `POST api/v1/venta/{id}/anulacion` | `AnularVentaUseCase` | `Administrador` | Deshacer un cobro no es una baja logica |
| `GET api/v1/venta/ticket/{numeroTicket}` | `ObtenerVentaPorTicketUseCase` | — | Busqueda por la clave que usa el personal |
| `POST api/v1/sesion-caja/apertura` | `AbrirSesionCajaUseCase` | — | El turno se abre, no se crea con datos libres |
| `POST api/v1/sesion-caja/movimientos` | `RegistrarMovimientoCajaUseCase` | — | Agrega una entidad hija a la sesion abierta |
| `POST api/v1/sesion-caja/cierre` | `CerrarSesionCajaUseCase` | — (el titular lo decide el agregado) | Arqueo y cierre |
| `GET api/v1/sesion-caja/estado` | `ObtenerEstadoCajaUseCase` | — | Consulta del turno abierto |
| `POST api/v1/seed` | `SembrarSistemaUseCase` | Sin `@RequiereRol` (4.19) | Inicializacion, no es un agregado |
| `POST api/v1/auth/{login,google,refresh}` | `AutenticacionService` | Publica | Autenticacion (4.18) |
| `GET api/v1/salud` | `ConsultarSaludUseCase` | Publica, sin `@RequiereRol` (4.22) | Health check, no es un agregado |

Endpoints estandar restringidos a `Administrador` con `@RequiereRol` en el metodo: `crear`,
`actualizar` y `eliminar` de `usuario`, `rol` y `producto`, y `actualizar` de `lote`. `SesionCaja`
no expone `POST`, `PUT` ni `DELETE` genericos (7.4).

### 5.14 Comentarios y citas

Prosa tecnica en espanol. El Javadoc se reserva para decisiones no obvias y explica **por que**
(ver `OffsetPageRequest`, `HasheadorDeContrasenaPbkdf2`, `IHasheadorDeContrasena`). El codigo
CRUD repetitivo va sin comentarios. Un comentario que justifica una regla de negocio cita su
identificador de `logica.md` (`PRODUCTO-16`); uno que justifica una decision estructural cita la
seccion de este archivo.

---

## 6. Procedimientos

Antes de cualquiera: leer en `logica.md` las decisiones de los agregados afectados. Si el cambio
necesita una regla nueva, se acuerda y se registra alli primero.

### 6.1 Anadir un agregado nuevo

1. Copiar literalmente `Inventario/Tipo` (19 archivos) en `BoundedContexts/<Contexto>/<Agregado>`
   y renombrar clases, paquetes, tabla y ruta.
2. Ajustar campos en: agregado, `JpaModel` (con `borrado`, `fec_c`, `fec_u`), `JpaMapper`, DTOs y
   llamadas a `Validador`.
3. Declarar en el `JpaRepository` `findByIdAndBorradoFalse`, `findByBorradoFalse` y, si hay
   campo de texto buscable, `findByBorradoFalseAnd<Campo>ContainingIgnoreCase`; si no lo hay, el
   datasource ignora `search` (4.9).
4. Registrar el controller bajo `api/v1/<kebab-case>` con `@RequiereRol({"Operador",
   "Administrador"})` en la clase y los cinco metodos estandar; restringir metodos si
   `logica.md` lo pide.
5. Si la tabla necesita objetos que no se derivan de la entidad (indice parcial, secuencia,
   `CHECK`), documentarlos en 4.20 y en el script de pruebas de H2 cuando aplique.
6. Si el agregado es un catalogo con semantica, anadir `obtenerPorNombre` (5.6) y sus filas a la
   siembra (4.19).
7. Escribir los tests (4.21).
8. No hace falta configuracion: el component scan desde `com.maestria.kaffehausv4` recoge
   `@Service`, `@Repository`, `@Component` y `@RestController`.

### 6.2 Anadir una entidad hija a un agregado

1. Crear la entidad en `Domain/Aggregates/` de la raiz con factorias package-private y sin id del
   padre (4.15).
2. Anadir a la raiz la coleccion, sus dos vistas, los metodos de mutacion y la validacion en
   `validarRegistrable()`; `eliminar()` arrastra a la hija.
3. Anadir `<Hija>Request` y `<Hija>Response` en `Application/Dtos/`, anidados en los de la raiz.
4. Anadir el trio `JpaModel`/`JpaMapper`/`JpaRepository` en `Infrastructure/Persistence/Jpa/` de
   la raiz y escribir la tabla desde el datasource de la raiz, con `conciliar(...)` (5.8).
5. Si otro agregado la referencia, crear su VO de identidad y la consulta en el puerto de la raiz.
6. Si la coleccion es ordenada, aplicar 5.9.

### 6.3 Ampliar un agregado existente

- **Campo nuevo**: agregado (`restaurar`, `crear`, `actualizar`), `JpaModel`, `JpaMapper`
  (los tres metodos), requests, response y tests. En bases existentes, `ddl-auto` crea la columna;
  si es `NOT NULL`, necesita valor para las filas que ya existen.
- **Campo retirado**: quitarlo de todas esas piezas y dejar indicado el `ALTER TABLE ... DROP
  COLUMN` (4.20).
- **Consulta nueva**: derivado en el `JpaRepository`, metodo con el mismo nombre de negocio en
  los dos puertos, delegacion en el `RepositoryImpl` e implementacion en el `DatasourceImpl`.
  Registrarla en 7.4 si sale del CRUD.
- **Operacion nueva**: un caso de uso nuevo (nunca un segundo metodo publico en uno existente),
  su request y su ruta; si no es uno de los cinco verbos, registrarla en 5.13.
- **Cambio de formato de respuesta**: solo en el `*Response` y su factoria; si incluye datos de
  otro agregado, el caso de uso los obtiene y los pasa a `createFromAggregate`.

### 6.4 Anadir una operacion interna

1. Crear el caso de uso en el slice del agregado al que pertenece la operacion, con su request y
   una factoria con nombre (4.6).
2. Inyectarlo en los casos de uso que lo necesitan.
3. Registrarlo en la tabla de 4.6. Si lo invoca otro contexto, registrarlo tambien en 5.12.

---

## 7. Reglas para la IA

### 7.1 Prohibiciones estructurales

1. **NO** importar `org.springframework.*`, `jakarta.*` ni nada de `Infrastructure/` dentro de
   `Domain/`.
2. **NO** usar en `Application/` ninguna anotacion de Spring distinta de `@Service` y
   `@Transactional`.
3. **NO** inyectar `I<Agregado>Datasource` en un caso de uso. Los casos de uso solo ven
   `I<Agregado>Repository` (y los casos de uso internos de 4.6).
4. **NO** saltarse el `RepositoryImpl` ni meterle logica: es delegacion pura.
5. **NO** exponer agregados ni `JpaModel` desde un controller. Solo DTOs de `Application/Dtos`.
6. **NO** inyectar repositorios ni casos de uso de un agregado dentro del `DatasourceImpl` de
   otro. Un `DatasourceImpl` si maneja varios `JpaRepository` cuando las tablas son **del mismo**
   agregado.
7. **NO** crear carpetas nuevas dentro de un agregado. La estructura de 3.1 es cerrada; una
   entidad hija convive con su raiz en `Domain/Aggregates/`.
8. **NO** colocar codigo suelto en el paquete raiz: solo existe `Kaffehausv4Application`.

### 7.2 Prohibiciones de implementacion

9. **NO** anadir dependencias al `pom.xml` (Lombok, MapStruct, ModelMapper, Bean Validation,
   Guava, Apache Commons, springdoc/Swagger, un logger). Sin Lombok, la inyeccion por constructor
   se escribe a mano. Unica excepcion: `com.h2database:h2` con `scope` test.
10. **NO** sacar la seguridad de `Shared/Presentation/Security`: `org.springframework.security` e
    `io.jsonwebtoken` no aparecen en ningun agregado. **NO** crear un bounded context de
    seguridad, **NO** validar tokens fuera del filtro, **NO** implementar `UserDetails` en un
    agregado y **NO** darle a la autenticacion datasource, repositorio ni tabla propios. Para
    hashear, `IHasheadorDeContrasena`; para 401/403, `AutorizacionException`.
11. **NO** escribir un catalogo de roles en el codigo (enums, constantes, listas). Los roles son
    filas de la tabla `rol`.
12. **NO** usar `@Valid` ni anotaciones de Bean Validation. Si falta un tipo de validacion, se
    anade un metodo a `Validador`.
13. **NO** escribir constructores publicos en agregados ni en DTOs de negocio (salvo
    `PaginatedResponse` y `PaginationDTO`).
14. **NO** anadir setters a los agregados.
15. **NO** usar asociaciones JPA (`@ManyToOne`, `@OneToMany`, `@JoinColumn`, `fetch`, cascadas).
16. **NO** usar `@GeneratedValue`: el UUID lo genera `<Agregado>JpaMapper.toPersistence`.
17. **NO** usar `PageRequest.of(...)`. Siempre `OffsetPageRequest`.
18. **NO** escribir `@Query`, JPQL, SQL nativo ni Criteria API. Solo query methods derivados.
19. **NO** usar `double`/`float` para dinero o cantidades: `BigDecimal`.
20. **NO** usar inyeccion por campo (`@Autowired` sobre atributos) ni por setter.
21. **NO** capturar excepciones en controllers ni devolver `ResponseEntity` de error a mano.
22. **NO** propagar excepciones crudas de JPA/JDBC: se traducen a `DomainException` o
    `SystemException`.
23. **NO** introducir tablas, modelos ni agregados sin `borrado` (5.4).
24. **NO** introducir cache, eventos de dominio, mensajeria, `@Scheduled`, `@Async`, WebFlux ni
    estado en memoria compartido.
25. **NO** anadir endpoints fuera de `api/v1/`, ni rutas que no sigan el kebab-case del agregado,
    ni operaciones fuera de los cinco verbos sin registrarlas en 5.13.
26. **NO** escribir logs (`System.out`, `printStackTrace`, loggers).
27. **NO** dar repositorio, datasource, caso de uso ni controller propios a una entidad hija.
28. **NO** aceptar del cliente un valor que el agregado deriva: se rechaza con 400 (5.5).
29. **NO** mutar la coleccion de hijas desde la capa de aplicacion: siempre por la raiz.
30. **NO** borrar fisicamente nada: ni `deleteById`, ni `deleteAll`, ni un `deleteBy...` derivado
    en `src/main`. Tampoco se regeneran los ids de una entidad hija.
31. **NO** exponer un repositorio de la hija a un agregado externo: la consulta va en el puerto
    de la raiz.
36. **NO** escribir reglas de negocio en este archivo: van en `logica.md`, y aqui solo se cita su
    identificador cuando una pieza las implementa.
37. **NO** crear un caso de uso interno ni una invocacion entre contextos sin registrarlos en 4.6
    y 5.12.

### 7.3 Nomenclatura obligatoria

32. Paquetes en **PascalCase** dentro de `BoundedContexts` y `Shared`.
33. Identificadores **en espanol y sin tildes**, sin mezclar ingles y espanol en una clase.
34. Interfaces de puerto con prefijo `I`.
35. Controllers terminan en `ControllerV1`; una nueva version de API crearia `...ControllerV2`
    bajo `api/v2/`, sin modificar el V1.

(Los numeros de regla son estables: las reglas 36 y 37 se agregaron despues y no se renumera.)

### 7.4 Desviaciones conocidas (no replicar, no "arreglar" sin pedirlo)

- `Personal/Entidad` usa `Infrastructure/Datasource/` **en singular**; en agregados nuevos,
  siempre el plural.
- `Personal/Persona` no tiene `Presentation/` ni `UseCases/` y su unico DTO es `PersonaResponse`:
  se gestiona desde los casos de uso de `Usuario`, `Cliente` y `Proveedor` (5.2).
- `Ventas/SesionCaja` no expone los cinco casos de uso: se abre, se le registran movimientos, se
  cierra y se consulta (`ObtenerEstadoCajaUseCase`, `ObtenerSesionCajaPorIdUseCase`,
  `ObtenerSesionesCajaUseCase`). `SesionCaja.eliminar()` responde 409.
- `MovimientoCajaJpaModel` expone `getFecC()`: la tabla no tiene otra fecha y es el instante del
  movimiento que muestra el arqueo.
- `VentaJpaModel.numeroTicket` usa `@Generated` de Hibernate: es el unico valor que asigna la base
  y no el mapper (4.20).
- `Ventas/Venta` e `Inventario/Lote` tienen dos trios JPA; `Inventario/Receta`, tres; `Venta`
  tiene ademas el de `PagoVenta` y `SesionCaja` el de `MovimientoCaja`.
- `pasos_receta` es la unica tabla con indice unico parcial por una coleccion ordenada, y
  `RecetaDatasourceImpl` el unico datasource que escribe en dos fases (5.9).
- Slices con casos de uso ademas de los cinco: `Lote` (`ObtenerDetallesAperturablesUseCase`,
  `AbrirDetalleLoteUseCase`, `DarDeBajaVencidoDetalleLoteUseCase`, `ObtenerVencimientosUseCase`),
  `Producto` (`ObtenerInventarioProductoUseCase`, `RegistrarPesajeUseCase`,
  `RecalcularStockUseCase`), `Existencia` (`RegistrarAperturaUseCase`,
  `DarDeBajaVencidaExistenciaUseCase`),
  `Movimiento` (`RegistrarSalidaDeInventarioUseCase`, `RegistrarReintegroDeInventarioUseCase`),
  `Venta` (`AnularVentaUseCase`, `RegistrarAnulacionDeVentaUseCase`, `ObtenerVentaPorTicketUseCase`).
- Puertos con metodos fuera del CRUD, cada uno con la misma firma en su `I*Datasource`:
  - `ILoteRepository`: `obtenerPorDetalleId`, `obtenerPorDetalleIdParaActualizar`,
    `obtenerPorIdParaActualizar`, `obtenerPorIdsParaActualizar` (bloqueo por la raiz, 5.7),
    `obtenerPorProductos`, `obtenerPorProductosSinCargar`, `obtenerProductoDeDetalle`,
    `obtenerLoteDeDetalle` (estos tres ultimos con proyecciones, 5.7),
    `obtenerConUnidadesQueVencenHasta`.
  - `IExistenciaRepository`: `obtenerDisponiblesPorDetallesLote`, `obtenerDisponiblesParaConsumo`,
    `obtenerVivasParaPesaje`, `obtenerPorIdParaActualizar`, `obtenerDetalleLoteDe` (con proyeccion,
    5.7), `existenPorDetallesLote`, `obtenerConContenidoQueVencenHasta`.
- Proyecciones de interfaz (5.7): `DetalleLoteLecturaProyeccion`, `LoteLecturaProyeccion` y
  `ExistenciaDetalleLoteProyeccion`. Son las unicas interfaces en
  `Infrastructure/Persistence/Jpa/` ademas de los `JpaRepository`.
  - `IProductoRepository`: `obtenerPresentaciones`, `obtenerPorIdParaActualizar`, `actualizarStock`,
    `existeConTipo`, `existeConCategoria`, `existeConUnidad`.
  - `IRecetaRepository`: `obtenerPorProductoObtenido`, `obtenerVivasPorProductoObtenido`,
    `obtenerPorIngrediente`. `IConversionRepository.obtenerVivas`.
  - `IMovimientoRepository`: `obtenerPorExistencias`, `obtenerPorVenta`.
  - `ISesionCajaRepository`: `obtenerAbierta`, `obtenerAbiertaParaActualizar`,
    `obtenerAbiertaParaVenta`.
  - `IVentaRepository`: `obtenerPorClaveIdempotencia`, `obtenerPorNumeroTicket`,
    `obtenerPorSesionCaja`. `IDescuentoRepository`: `obtenerVigentes`, `obtenerPorProducto`,
    `existeConTipoDescuento`. `IUsuarioRepository.contarPorRol`.
    `IPedidoRepository.obtenerPorVentaId`. `ITipoPagoRepository.obtenerVivos`.
  - `obtenerPorNombre` en `ITipoMovimientoRepository`, `IEstadoExistenciaRepository`,
    `IEstadoVentaRepository`, `IEstadoPedidoRepository`, `ITipoPagoRepository`, `IRolRepository`,
    `ITipoRepository`, `ICategoriaRepository`, `IUnidadRepository` e `ITipoDescuentoRepository`.
- `Shared/Application/Inicializacion` y `Shared/Presentation/Security` dependen de repositorios de
  varios contextos sin ser agregados (4.18, 4.19).
- `ErrorResponse` es el unico sitio fuera de seguridad que usa una anotacion de Jackson
  (`@JsonInclude`, 4.16).
- `record` solo se usa en `ErrorResponse` y en el paquete de seguridad (`UsuarioAutenticado` y
  `Dtos/`); los DTOs de los agregados son clases con factorias.

### 7.5 Pendientes arquitectonicos

Desajustes conocidos entre este documento y el repositorio. No se resuelven sin consultar.

- **Esquema fuera de las entidades versionado casi entero** (4.20): la secuencia del ticket, los
  indices unicos y parciales, las restricciones de `movimientos` y las claves foraneas estan en
  `schema.sql`; las demas `CHECK` del script antiguo (importes, saldos, origen del lote) aun no.
  Una base creada desde cero no las tiene. Las FK son `NOT VALID`: las filas huerfanas anteriores
  no se detectan hasta que alguien valide la restriccion (`ALTER TABLE ... VALIDATE CONSTRAINT`).
- **Proteccion de controllers sin test** (4.21): ningun test comprueba que cada controller declare
  `@RequiereRol`.
- **Citas a secciones antiguas de este archivo** en comentarios del codigo y de los tests. Se
  actualizan al modificar cada archivo, como las citas de `logica.md` (GENERAL-12). Equivalencia:
  4.2 → 5.1; 4.3 → 5.2; 4.7 → 5.13 y 5.8; 4.11 → `logica.md` (VENTA-xx, SESIONCAJA-xx) y 5.7;
  5.13 → 4.15, 5.8 y 5.9; 6.5 → 7.4; reglas 1 a 35 → mismos numeros en 7.1 a 7.3.
