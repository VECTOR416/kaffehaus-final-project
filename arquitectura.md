# Arquitectura de KaffeHaus v4

> Documento normativo. Es la **fuente de verdad arquitectonica** del repositorio.
> Toda modificacion, refactorizacion o adicion de codigo debe respetar estrictamente
> lo aqui descrito. Si una necesidad real no encaja con estas reglas, se actualiza
> primero este documento y despues el codigo, nunca al reves.

---

## 1. Vision General y Proposito del Proyecto

KaffeHaus v4 es el backend REST del sistema de gestion de una cafeteria. Expone un API
HTTP/JSON que cubre cinco dominios de negocio: inventario (productos, lotes, existencias,
recetas, produccion, movimientos), ventas, pedidos, personal/actores (personas, usuarios,
roles, clientes, proveedores, entidades) y sus catalogos de estados.

**Stack y runtime**

| Elemento | Valor |
|---|---|
| Lenguaje | Java 21 (`<java.version>21</java.version>`) |
| Framework | Spring Boot 4.1.1 (parent POM) |
| Build | Maven, con wrapper (`mvnw`, `mvnw.cmd`) |
| Web | `spring-boot-starter-webmvc` (Spring MVC, servlet, bloqueante) |
| Persistencia | `spring-boot-starter-data-jpa` + Hibernate sobre PostgreSQL |
| Driver | `org.postgresql:postgresql` (runtime) |
| Base de datos | PostgreSQL 16-alpine via `docker-compose.yaml` (servicio `kaffehausdb`, puerto 5432) |
| Puerto HTTP | `${SERVER_PORT:8081}` |
| Tests | `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` (JUnit 5) |
| Devtools | `spring-boot-devtools` (runtime, opcional) |

**Artefacto**: `com.maestria:kaffehausv4:0.0.1-SNAPSHOT`
**Paquete raiz**: `com.maestria.kaffehausv4`
**Clase de arranque**: `Kaffehausv4Application` (`@SpringBootApplication`, sin configuracion extra)

**Configuracion** (`src/main/resources/application.yaml`): datasource PostgreSQL apuntando a
`localhost:5432/kaffehausdb`, `spring.jpa.hibernate.ddl-auto: update` y dialecto
`PostgreSQLDialect`. El esquema canonico esta versionado en `databasev4.sql` (DDL completo,
extension `pgcrypto`, UUID como PK, FKs con `ON UPDATE CASCADE`); `ddl-auto: update`
solo complementa, **no** sustituye a ese archivo.

**Estado de seguridad (importante)**: el proyecto **no tiene Spring Security ni Keycloak**.
Fueron retirados deliberadamente y el classpath no contiene ninguna libreria de seguridad.
El API esta abierta. Las consecuencias ya absorbidas en el codigo:

- El CORS se declara en `Shared/Infrastructure/Web/WebConfig` (`WebMvcConfigurer`), no en una
  cadena de filtros.
- El hasheo de contrasenas se hace con `IHasheadorDeContrasena` + `HasheadorDeContrasenaPbkdf2`
  (PBKDF2 del JDK), no con `PasswordEncoder`/BCrypt.
- Existe `AutorizacionException` preparada para el futuro esquema de autenticacion.

El archivo `keycloak_sugerencia.md` en la raiz es material de referencia/propuesta, **no**
describe el estado actual del codigo.

---

## 2. Patron Arquitectonico y Filosofia

El proyecto aplica **Clean Architecture + DDD tactico, organizado en Bounded Contexts y
Vertical Slices por agregado**. No es una arquitectura por capas horizontales: cada agregado
es un modulo autocontenido con sus cuatro capas dentro.

### 2.1 Jerarquia estructural

```
BoundedContexts / <Contexto> / <Agregado> / {Domain, Application, Infrastructure, Presentation}
```

Contextos existentes (4) y agregados (26):

| Bounded Context | Agregados |
|---|---|
| `Inventario` (14) | Categoria, Conversion, DetalleLote, EstadoExistencia, Existencia, IngredienteReceta, Lote, Movimiento, PasoReceta, Produccion, Producto, Receta, TipoMovimiento, Unidad |
| `Ventas` (4) | Descuento, EstadoVenta, TipoDescuento, Venta (contiene DetalleVenta) |
| `Personal` (6) | Cliente, Entidad, Persona, Proveedor, Rol, Usuario |
| `Pedidos` (2) | EstadoPedido, Pedido |

`Venta` es el unico agregado con **entidad hija**: `DetalleVenta` no es raiz, vive dentro de la
raiz y no tiene slice, repositorio ni endpoints propios (ver 5.13).

Mas un **Shared Kernel** transversal: `com.maestria.kaffehausv4.Shared`.

### 2.2 Regla de dependencia (obligatoria)

```
Presentation ──► Application ──► Domain
                      ▲
Infrastructure ───────┘   (implementa puertos declarados en Application)
```

- **Domain** no importa **nada** de Spring, JPA ni de Infrastructure. Verificado: cero imports
  de `org.springframework` o `jakarta` en cualquier carpeta `Domain/`. Esta invariante es
  innegociable.
- **Application** solo puede usar de Spring `@Service` y `@Transactional`. Nada mas. Es la unica
  excepcion tolerada al aislamiento del framework.
- **Infrastructure** conoce Application y Domain; nadie conoce a Infrastructure salvo el
  contenedor de Spring, que inyecta sus implementaciones por interfaz.
- **Presentation** solo habla con casos de uso y DTOs. Nunca con repositorios, datasources,
  modelos JPA ni agregados.

### 2.3 Doble indireccion Repository → Datasource

Patron distintivo del proyecto. Existen **dos puertos** por agregado, con la misma firma:

- `Application/Repositories/I<Agregado>Repository` — puerto que consume el caso de uso.
- `Application/Datasources/I<Agregado>Datasource` — puerto de la fuente de datos concreta.

Y dos implementaciones:

- `Infrastructure/Repositories/<Agregado>RepositoryImpl` — `@Repository`, **delegacion pura**
  al datasource, sin logica propia.
- `Infrastructure/Datasources/<Agregado>DatasourceImpl` — `@Repository`, aqui vive el acceso
  real a JPA, el mapeo y la paginacion.

Esta indireccion permite cambiar el origen de datos (JPA, HTTP, memoria) sin tocar los casos
de uso. **Debe mantenerse aunque parezca redundante**: es una decision estructural del proyecto.

### 2.4 Principios que el codigo ya sostiene

1. **Agregados con constructor privado y mutacion controlada.** Sin setters publicos; se muta
   solo por metodos de negocio (`actualizar`, `eliminar`).
2. **Persistence Model separado del agregado.** El agregado nunca lleva anotaciones JPA.
3. **Mapeo explicito.** Mappers estaticos escritos a mano; sin MapStruct, Lombok ni reflexion.
4. **Validacion en el borde de Application**, no en el dominio ni con Bean Validation.
5. **Composicion entre agregados en el caso de uso**, nunca a nivel de persistencia: ningun
   `DatasourceImpl` importa el `JpaRepository`/`JpaModel` de otro agregado (verificado).
6. **Sin eventos de dominio, sin CQRS, sin mensajeria, sin cache.** El flujo es sincrono y
   transaccional de principio a fin.
7. **El agregado es la frontera transaccional.** Una raiz se guarda entera, en una sola
   llamada a su repositorio y dentro de la transaccion del caso de uso; sus entidades hijas
   no tienen repositorio propio ni se persisten por separado.

---

## 3. Mapa del Repositorio

```
kaffehausv4/
├── arquitectura.md              # Este documento (fuente de verdad arquitectonica)
├── pom.xml                      # Dependencias y Java 21
├── docker-compose.yaml          # PostgreSQL 16 local (kaffehausdb:5432)
├── databasev4.sql               # DDL canonico del esquema (referencia obligatoria)
├── keycloak_sugerencia.md       # Propuesta futura de auth. NO refleja el codigo actual
├── HELP.md                      # Nota de version del proyecto
├── mvnw / mvnw.cmd / .mvn/      # Maven wrapper
└── src/
    ├── main/
    │   ├── java/com/maestria/kaffehausv4/
    │   │   ├── Kaffehausv4Application.java      # Unico @SpringBootApplication
    │   │   ├── Shared/                          # Shared Kernel (ver 3.2)
    │   │   └── BoundedContexts/
    │   │       ├── Inventario/<Agregado>/
    │   │       ├── Ventas/<Agregado>/
    │   │       ├── Personal/<Agregado>/
    │   │       └── Pedidos/<Agregado>/
    │   └── resources/
    │       └── application.yaml                 # Unico archivo de configuracion
    └── test/java/com/maestria/kaffehausv4/
        └── Kaffehausv4ApplicationTests.java     # Unico test: contextLoads()
```

### 3.1 Anatomia de un agregado (plantilla obligatoria)

Estructura canonica, tomada de `Inventario/Categoria` (slice de referencia; usalo como molde
literal al crear un agregado nuevo):

```
<Agregado>/
├── Domain/
│   ├── Aggregates/
│   │   └── <Agregado>.java                 # Raiz de agregado. POJO puro, sin framework
│   └── ValueObjects/
│       └── <Agregado>Id.java               # VO identidad: wrapper inmutable de String
│
├── Application/                            # LOGICA DE NEGOCIO DE APLICACION
│   ├── Datasources/
│   │   └── I<Agregado>Datasource.java      # Puerto de fuente de datos
│   ├── Repositories/
│   │   └── I<Agregado>Repository.java      # Puerto que consume el caso de uso
│   ├── Dtos/
│   │   ├── Crear<Agregado>Request.java     # Entrada: createFromRequest(Map)
│   │   ├── Actualizar<Agregado>Request.java
│   │   ├── Obtener<Agregados>Request.java  # extends PaginationDTO
│   │   └── <Agregado>Response.java         # Salida: createFromAggregate(...)
│   └── UseCases/                           # Un archivo = una operacion
│       ├── Crear<Agregado>UseCase.java
│       ├── Actualizar<Agregado>UseCase.java
│       ├── Obtener<Agregado>PorIdUseCase.java
│       ├── Obtener<Agregados>UseCase.java
│       └── Eliminar<Agregado>UseCase.java
│
├── Infrastructure/                         # ACCESO A DATOS Y DETALLES TECNICOS
│   ├── Datasources/
│   │   └── <Agregado>DatasourceImpl.java   # JPA real + paginacion + mapeo
│   ├── Repositories/
│   │   └── <Agregado>RepositoryImpl.java   # Delegacion pura al datasource
│   └── Persistence/Jpa/
│       ├── <Agregado>JpaModel.java         # @Entity @Table. Unico lugar con anotaciones JPA
│       ├── <Agregado>JpaMapper.java        # Estatico: toPersistence/copiarCambios/toDomain
│       └── <Agregado>JpaRepository.java    # extends JpaRepository<Model, UUID>
│
└── Presentation/
    └── <Agregado>ControllerV1.java         # @RestController, @RequestMapping("api/v1/<kebab>")
```

Un agregado con entidad hija anade archivos a esta misma estructura, sin abrir carpetas nuevas:
la entidad en `Domain/Aggregates/`, su DTO anidado en `Application/Dtos/` y su trio JPA en
`Infrastructure/Persistence/Jpa/`. Ver 5.13.

### 3.2 Shared Kernel (`Shared/`)

Lo unico compartido entre contextos. Espeja las mismas cuatro capas.

| Ruta | Responsabilidad |
|---|---|
| `Shared/Domain/Error/BaseException` | Base abstracta: `codigo` (HTTP), `mensaje`, `tipo`. Extiende `RuntimeException` |
| `Shared/Domain/Error/DomainException` | Errores de negocio y validacion (400/404). Tipo `"Domain Exception"` |
| `Shared/Domain/Error/SystemException` | Fallos tecnicos (500). Tipo `"System Exception"` |
| `Shared/Domain/Error/AutorizacionException` | 401/403. Preparada para el futuro esquema de auth |
| `Shared/Application/Validation/Validador` | Validador estatico `final` de `Map<String,Object>`. Unico punto de validacion de entrada |
| `Shared/Application/Dtos/Pagination/PaginationDTO` | Base de los requests de listado. Defaults: `skip=0`, `limit=10`, `search=""` |
| `Shared/Application/Dtos/Pagination/PaginatedResponse<T>` | Envoltorio de salida: `data`, `skip`, `limit`, `total`, `hasMore` |
| `Shared/Application/Crypto/IHasheadorDeContrasena` | Puerto de hasheo de credenciales |
| `Shared/Infrastructure/Crypto/HasheadorDeContrasenaPbkdf2` | `@Component`. PBKDF2WithHmacSHA256, 210 000 iteraciones, sal de 16 bytes, formato `pbkdf2$iter$sal$hash`, comparacion en tiempo constante |
| `Shared/Infrastructure/Persistence/OffsetPageRequest` | `Pageable` por offset absoluto. Evita el error de traducir `skip/limit` a numero de pagina |
| `Shared/Infrastructure/Web/WebConfig` | CORS global via `WebMvcConfigurer` (`allowedOriginPatterns("*")`, `allowCredentials(false)`) |
| `Shared/Presentation/GlobalExceptionHandler` | `@RestControllerAdvice`. Traduce excepciones a `ErrorResponse` |

Metodos disponibles en `Validador` (cada uno en variante `Requerido`/`Opcional`):
`textoRequerido`, `numeroRequerido`, `enteroRequerido`, `booleanoRequerido`, `fechaRequerida`,
`listaDeTextosRequerida`, `listaDeObjetosRequerida`, `objetoRequerido`, `valorDeEnumRequerido`.
`listaDeObjetosRequerida` valida colecciones de objetos anidados (los `detalles` de una venta)
y rechaza la lista vacia.

### 3.3 Donde colocar cada cosa

| Necesidad | Ubicacion exacta |
|---|---|
| Regla invariante de una entidad | `Domain/Aggregates/<Agregado>.java` |
| Identidad tipada | `Domain/ValueObjects/<Agregado>Id.java` |
| Orquestacion de una operacion de negocio | `Application/UseCases/<Verbo><Agregado>UseCase.java` |
| Contrato de entrada / salida HTTP | `Application/Dtos/` |
| Validacion y coercion de tipos de entrada | `Shared/Application/Validation/Validador` (reutilizar; ampliar solo si falta un tipo) |
| Consulta a base de datos | `Infrastructure/Persistence/Jpa/<Agregado>JpaRepository.java` |
| Traduccion agregado ↔ fila | `Infrastructure/Persistence/Jpa/<Agregado>JpaMapper.java` |
| Endpoint HTTP | `Presentation/<Agregado>ControllerV1.java` |
| Utilidad usada por 2+ contextos | `Shared/` en la capa que le corresponda |
| Tests | `src/test/java/...` espejando el paquete de produccion |

---

## 4. Flujo de Datos y Componentes Principales

### 4.1 Recorrido de una peticion de escritura

```
POST /api/v1/categoria  { "nombre": "Bebidas" }
  │
  ▼ CategoriaControllerV1.crear(@RequestBody Map<String,Object> body)
  │    El controller NO deserializa a una clase tipada: recibe un Map crudo
  ▼ CrearCategoriaRequest.createFromRequest(body)
  │    Validador.textoRequerido(...) → lanza DomainException(400) si falla
  ▼ CrearCategoriaUseCase.execute(request)            @Service @Transactional
  │    Categoria.crear(nombre)                        → agregado sin id, borrado=false
  ▼ ICategoriaRepository.crear(categoria)
  ▼ CategoriaRepositoryImpl                           → delegacion pura
  ▼ ICategoriaDatasource.crear(categoria)
  ▼ CategoriaDatasourceImpl
  │    CategoriaJpaMapper.toPersistence(...)          → genera UUID.randomUUID(), fija fecC
  │    categoriaJpaRepository.save(model)
  │    CategoriaJpaMapper.toDomain(model)             → vuelve al agregado
  ▼ CategoriaResponse.createFromAggregate(creada)
  ▼ ResponseEntity.status(201).body(response)
```

Si algo lanza `BaseException` en cualquier punto, `GlobalExceptionHandler` la intercepta y
devuelve el `ErrorResponse` con el `codigo` como status HTTP.

### 4.2 Recorrido de un listado paginado

```
GET /api/v1/categoria?skip=0&limit=10&search=cafe
  ▼ Controller: @RequestParam(required=false) skip/limit/search → se meten en un HashMap
  ▼ ObtenerCategoriasRequest.createFromRequest(params)   → extends PaginationDTO (defaults)
  ▼ ObtenerCategoriasUseCase.execute(...)                @Transactional(readOnly = true)
  ▼ Repository → Datasource
  │    Pageable pageable = new OffsetPageRequest(skip, limit)
  │    search vacio  → findByBorradoFalse(pageable)
  │    search lleno  → findByBorradoFalseAnd<Campo>ContainingIgnoreCase(search.trim(), pageable)
  │    hasMore = skip + datos.size() < pagina.getTotalElements()
  ▼ PaginatedResponse<List<Categoria>>  →  el UseCase remapea a PaginatedResponse<List<...Response>>
```

El **UseCase reconstruye siempre un `PaginatedResponse` nuevo** copiando `skip/limit/total/hasMore`
y sustituyendo `data` por los DTOs. No se devuelven agregados hacia Presentation jamas.

### 4.3 Composicion entre agregados

Cuando una respuesta necesita datos de otro agregado, el **caso de uso** inyecta el repositorio
del otro agregado y compone el DTO. Ejemplos reales:

- `CrearUsuarioUseCase` inyecta `IUsuarioRepository`, `IPersonaRepository`, `IRolRepository` y
  `IHasheadorDeContrasena`. Crea o reutiliza la `Persona` en la **misma transaccion**, valida
  que el `Rol` exista, hashea la contrasena y compone
  `UsuarioResponse.createFromAggregate(usuario, persona, rol)`.
- `ObtenerVentasUseCase` resuelve el `EstadoVenta` de cada `Venta` via `IEstadoVentaRepository`
  en un metodo privado `toResponse(...)`.

Las relaciones se modelan como **Value Objects de identidad foranea** dentro del agregado
(`Usuario` guarda `PersonaId` y `RolId`), y como **columnas `UUID` planas** en el `JpaModel`
(`id_persona`, `id_rol`). **No se usan `@ManyToOne`, `@OneToMany` ni ninguna asociacion JPA
en ningun modelo del proyecto.**

### 4.4 Transacciones

- Casos de uso de escritura: `@Transactional` a nivel de clase.
- Casos de uso de lectura: `@Transactional(readOnly = true)`.
- Siempre `org.springframework.transaction.annotation.Transactional`.
- La transaccion se abre y cierra en el caso de uso. Ni el controller ni el datasource la gestionan.

### 4.5 Borrado: dos politicas coexistentes

| Politica | Agregados | Mecanismo |
|---|---|---|
| **Soft delete** (20 agregados) | Categoria, Cliente, Conversion, Descuento, DetalleLote, Entidad, Existencia, IngredienteReceta, Lote, Movimiento, PasoReceta, Persona, Produccion, Producto, Proveedor, Receta, Rol, TipoMovimiento, Unidad, Usuario | Campo `borrado` en agregado y `JpaModel`. `eliminar()` lo pone en `true` y se persiste con `save`. Todas las lecturas usan `...BorradoFalse...` |
| **Hard delete** (6 agregados) | EstadoExistencia, EstadoPedido, EstadoVenta, Pedido, TipoDescuento, Venta | Sin columna `borrado`. El datasource comprueba `existsById` y llama `deleteById`. En Venta el borrado arrastra tambien sus lineas |

La politica de un agregado la dicta el DDL de `databasev4.sql` (presencia o ausencia de la
columna `borrado`). **No cambiar la politica de un agregado existente.**

### 4.6 Contrato del agregado Venta (endpoints)

`Venta` se escribe y se lee siempre entera por `api/v1/venta`. **No existe `api/v1/detalle-venta`**:
se retiro al subordinar las lineas a su raiz.

| Operacion | Payload | Efecto |
|---|---|---|
| `POST api/v1/venta` | encabezado + `detalles[]` (obligatorio, no vacio) | Crea encabezado y lineas en una transaccion. Devuelve 201 con el agregado completo |
| `PUT api/v1/venta/{id}` | encabezado opcional + `detalles[]` opcional | Si llega `detalles`, reemplaza todas las lineas y recalcula el total |
| `GET api/v1/venta/{id}` | — | Devuelve la venta con sus lineas |
| `GET api/v1/venta` | `skip`/`limit`/`search` | Pagina de ventas con sus lineas, resueltas en una sola consulta por pagina |
| `DELETE api/v1/venta/{id}` | — | Borra lineas y encabezado |

Cada elemento de `detalles` admite `idProducto`, `precioUnitario`, `cantidad` y
`descuentoAplicado` (opcional, por defecto 0). **No admite `subtotal`, y la venta no admite
`total`**: ambos son derivados.

### 4.7 Estado global, caching, mensajeria

**No existen.** El unico estado es la base de datos PostgreSQL. No hay cache (`@Cacheable`),
ni broker de mensajes, ni eventos de dominio, ni scheduler, ni WebSocket, ni sesion HTTP.
No introducir ninguno sin actualizar este documento.

---

## 5. Convenciones y Directrices de Implementacion

### 5.1 Nombres

| Elemento | Convencion | Ejemplo real |
|---|---|---|
| Paquetes de contexto/agregado/capa | **PascalCase** (deliberadamente no-estandar en Java) | `BoundedContexts.Inventario.Categoria.Application.UseCases` |
| Clases | PascalCase | `CrearCategoriaUseCase` |
| Interfaces (puertos) | Prefijo `I` + PascalCase | `ICategoriaRepository`, `IHasheadorDeContrasena` |
| Metodos y variables | camelCase, **en espanol sin tildes** | `obtenerPorId`, `contrasenaEnClaro`, `motivoDiferencia` |
| Constantes | `UPPER_SNAKE_CASE` | `LONGITUD_SAL_BYTES`, `ITERACIONES` |
| Ruta HTTP | `api/v1/<agregado-en-kebab-case>` | `api/v1/detalle-venta`, `api/v1/tipo-movimiento` |
| Tablas y columnas | `snake_case`, nombre exacto de `databasev4.sql` | `detalle_lote`, `id_estado_venta`, `fec_c` |
| Campos JSON de entrada/salida | camelCase | `idEstadoVenta`, `stockMinimo`, `fechaVenta` |

**Pluralizacion en espanol.** Los listados usan el plural real del agregado, no una `s` mecanica:
`ObtenerProveedoresUseCase`, `ObtenerRolesUseCase`, `ObtenerConversionesUseCase`,
`ObtenerTiposDescuentoUseCase`, `ObtenerDetallesVentaUseCase`, `ObtenerPasosRecetaUseCase`
(el modificador no se pluraliza: "TiposDescuento", no "TipoDescuentos").

**Los cinco casos de uso son siempre**: `Crear`, `Actualizar`, `Obtener<Agregado>PorId`,
`Obtener<Agregados>`, `Eliminar` (25 agregados x 5 = 125 casos de uso; `Persona` no expone
ninguno). Los cinco metodos del controller son siempre:
`crear`, `obtenerPorId`, `obtenerTodos`, `actualizar`, `eliminar`.

### 5.2 Agregados (`Domain/Aggregates`)

Contrato fijo:

```java
public class Categoria {
    private CategoriaId id;
    private String nombre;
    private boolean borrado;

    private Categoria(...) { ... }                       // constructor SIEMPRE privado

    public static Categoria restaurar(String id, ...)    // desde persistencia (con id)
    public static Categoria crear(...)                   // nuevo (id = null, borrado = false)
    public void actualizar(...)                          // patch parcial: null = no cambia
    public void eliminar()                               // solo si el agregado es soft-delete

    public CategoriaId getId() {return id;}              // getters en una linea, sin setters
}
```

- `actualizar(...)` aplica siempre el idioma `this.campo = valor != null ? valor : this.campo;`
  (semantica PATCH: lo que no llega, no se toca).
- `restaurar` recibe tipos primitivos (`String id`) y construye los VOs internamente.
- Getters de `boolean` con prefijo `is` (`isBorrado()`).
- Sin `equals`/`hashCode`/`toString`, sin Lombok.

### 5.3 Value Objects de identidad

Wrapper inmutable minimo sobre `String`, con `getValue()`. Existe uno por agregado
(`<Agregado>Id`) y es el unico tipo de Value Object del proyecto. La conversion a `UUID`
ocurre exclusivamente en Infrastructure.

### 5.4 DTOs (`Application/Dtos`)

- **Request**: constructor privado + factoria estatica `createFromRequest(Map<String, Object> request)`
  que valida campo a campo con `Validador`. Los `Crear*Request` exigen los obligatorios
  (`textoRequerido`, `numeroRequerido`); los `Actualizar*Request` usan siempre las variantes
  `*Opcional` y dejan `null` lo ausente.
- **Response**: constructor privado + factoria `createFromAggregate(<Agregado> ...)`. Cuando la
  respuesta anida otros agregados, la factoria los recibe como parametros adicionales
  (`createFromAggregate(usuario, persona, rol)`), nunca los busca por su cuenta.
- **Listado**: `Obtener<Agregados>Request extends PaginationDTO`, con `createFromRequest` que
  lee `skip`, `limit`, `search` del `Map` por casting directo.
- Los `BigDecimal` se construyen desde `Validador.numero*` con `BigDecimal.valueOf(double)`.

### 5.5 Casos de uso (`Application/UseCases`)

- `@Service` + `@Transactional` (o `readOnly = true`).
- **Un unico metodo publico: `execute(...)`.** Si hace falta descomponer, metodos `private`.
- Dependencias `private final`, **inyeccion por constructor exclusivamente**. Prohibido
  `@Autowired` en campos.
- Solo dependen de interfaces `I*Repository` (y `IHasheadorDeContrasena` cuando aplica),
  nunca de `I*Datasource` ni de clases de Infrastructure.
- Firma de entrada: DTO de request, o `String` crudo para los que van por id
  (`execute(String categoriaId)`, `execute(String id, Actualizar...Request request)`).
- Devuelven siempre DTOs (`<Agregado>Response`, `PaginatedResponse<List<...Response>>`) o `void`.
- La existencia de una entidad referenciada se valida aqui con
  `.orElseThrow(() -> DomainException.throwException(404, "..."))`.

### 5.6 Controllers (`Presentation`)

- `@RestController` + `@RequestMapping("api/v1/<kebab>")` (sin barra inicial).
- Inyeccion por constructor de los cinco casos de uso.
- Reciben `@RequestBody Map<String, Object> body` — **nunca** un DTO tipado como parametro de
  Spring, porque la validacion vive en `createFromRequest`.
- Sin `@Valid`, sin Bean Validation, sin `try/catch`, sin logica.
- Codigos de respuesta fijos: `201 CREATED` en `crear`, `200 OK` en lecturas y `actualizar`,
  `204 No Content` (`ResponseEntity.noContent().build()`) en `eliminar`.
- Listados: tres `@RequestParam(required = false)` (`skip`, `limit`, `search`) empaquetados en
  un `HashMap` que se pasa a `createFromRequest`.

### 5.7 Persistencia (`Infrastructure/Persistence/Jpa`)

- `<Agregado>JpaModel`: `@Entity`, `@Table(name = "<tabla_snake_case>")`, `@Id` sin
  `@GeneratedValue` (el UUID lo genera el mapper), `@Column(name = "...")` explicito en cada
  campo, constructor publico vacio obligatorio, getters/setters.
- Campos de auditoria en todos los modelos: `fec_c` (`OffsetDateTime`, `nullable = false`),
  `fec_u` (`OffsetDateTime`, nullable), y `borrado` (`Boolean`) si el agregado es soft-delete.
- Tipos: `UUID` para claves e identidades foraneas, `BigDecimal` para dinero y cantidades,
  `OffsetDateTime` para auditoria, `LocalDateTime`/`LocalDate` para fechas de negocio.
- `<Agregado>JpaMapper`: clase con constructor privado y solo metodos `static`:
  - `toPersistence(agregado)` → nuevo modelo; genera `UUID.randomUUID()` si el id es `null`
    y fija `fecC = OffsetDateTime.now()`.
  - `copiarCambios(agregado, model)` → muta un modelo existente y fija `fecU = OffsetDateTime.now()`.
  - `toDomain(model)` → `<Agregado>.restaurar(...)`, normalizando nulos
    (`model.getBorrado() != null && model.getBorrado()`).
- `<Agregado>JpaRepository extends JpaRepository<<Agregado>JpaModel, UUID>`: solo **query
  methods derivados**. El proyecto no usa `@Query`, JPQL ni Criteria API en ningun punto.
  Derivados en uso: `findByIdAndBorradoFalse`, `findByBorradoFalse`,
  `findByBorradoFalseAnd<Campo>ContainingIgnoreCase`.

### 5.8 Datasources (`Infrastructure/Datasources`)

- `@Repository`, implementa `I<Agregado>Datasource`.
- Helper privado obligatorio en cada uno:

```java
private static UUID aUuid(String valor) {
    try {
        return UUID.fromString(valor);
    } catch (IllegalArgumentException | NullPointerException e) {
        throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
    }
}
```

- `actualizar` y `eliminar` recargan primero el modelo (`findByIdAndBorradoFalse` o
  `existsById`) y lanzan 404 si no existe.
- La paginacion usa siempre `new OffsetPageRequest(skip, limit)`. **Nunca `PageRequest.of`**:
  traduce mal el offset cuando `skip` no es multiplo de `limit`.

### 5.9 Manejo de errores

Jerarquia unica: `BaseException` (abstracta) → `DomainException` | `SystemException` | `AutorizacionException`.

- Se lanzan **siempre** con la factoria estatica: `throw DomainException.throwException(404, "...")`.
  El metodo devuelve la excepcion (no la lanza), por eso encaja directo en `.orElseThrow(() -> ...)`.
- `codigo` es literalmente el status HTTP que se devolvera.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) responde
  `record ErrorResponse(int codigo, String tipo, String mensaje, OffsetDateTime timestamp)`.
  Si `HttpStatus.resolve(codigo)` es nulo, cae a `500`. Tiene handlers para `DomainException`,
  `SystemException`, `BaseException` (cubre `AutorizacionException`) y `RuntimeException`.
- Reparto de codigos observado: `400` validacion y UUID malformado, `404` recurso inexistente,
  `500` fallo tecnico (`SystemException`).
- **Mensajes de error en espanol, sin tildes**, citando el campo o id entre comillas simples:
  `"La categoria con id '...' no existe"`, `"El campo 'nombre' es obligatorio..."`.

### 5.10 Logging

El proyecto **no tiene logging**: no hay `Logger`, `@Slf4j` ni `System.out` en ningun archivo
de produccion. La trazabilidad se apoya en el `ErrorResponse`. No introducir logs dispersos;
si se necesita logging, se hace de forma centralizada y se documenta aqui primero.

### 5.11 Tipado e interfaces

- `Optional<T>` solo como valor de retorno de lecturas por id en puertos y repositorios.
- Genericos en los envoltorios compartidos: `PaginatedResponse<List<Categoria>>` en las capas
  internas, `PaginatedResponse<List<CategoriaResponse>>` hacia fuera.
- Los puertos `I*Repository` e `I*Datasource` de un mismo agregado **mantienen firmas identicas**.
- Imports siempre explicitos, jamas con comodin `*`.
- `record` se usa solo para `ErrorResponse`; los DTOs de negocio son clases con factorias.

### 5.12 Comentarios y documentacion

Prosa tecnica en espanol. El Javadoc se reserva para decisiones no obvias, y cuando existe
explica **por que** (ver `OffsetPageRequest`, `HasheadorDeContrasenaPbkdf2`, `WebConfig`,
`IHasheadorDeContrasena`, `CrearUsuarioUseCase`). El codigo CRUD repetitivo va sin comentarios.

### 5.13 Agregados con entidades hijas (caso `Venta`)

`Venta` es hoy el unico agregado con entidad hija. Su forma es la plantilla obligatoria si en
el futuro aparece otro:

- La entidad hija (`DetalleVenta`) se declara en `Domain/Aggregates/`, **en el mismo paquete
  que su raiz**. No abre carpeta propia: la co-locacion es la senal de que le pertenece.
- Sus factorias de creacion son **package-private** (`static DetalleVenta crear(...)`), de modo
  que solo la raiz puede instanciarla. `restaurar(...)` es publica unicamente porque la invoca
  el mapper de infraestructura.
- La hija **no guarda el id de su padre**: dentro del agregado la relacion es la contencion.
  La columna FK (`detalle_venta.id_venta`) la completa el mapper a partir de la raiz.
- La raiz expone la coleccion como vista de solo lectura
  (`Collections.unmodifiableList(...)`) y concentra las mutaciones en metodos de negocio:
  `agregarDetalle(...)`, `removerDetalle(id)`.
- Los **valores derivados se calculan en la raiz**, nunca se aceptan del cliente. `Venta.total`
  es la suma de los subtotales y se recalcula en cada mutacion; el subtotal de una linea es
  `precioUnitario * cantidad - descuentoAplicado`. Ambos se redondean a 2 decimales con
  `RoundingMode.HALF_UP`, la escala de `NUMERIC(12, 2)`.
- Las invariantes se validan dentro del dominio lanzando `DomainException` (es Shared/Domain,
  asi que no rompe el aislamiento de capa). La invariante "toda venta registrada tiene al
  menos una linea" se comprueba con `venta.validarRegistrable()` antes de persistir.
- Persistencia: la hija conserva su `JpaModel`, `JpaMapper` y `JpaRepository`, pero **dentro
  del slice de la raiz**. `VentaDatasourceImpl` inyecta los dos `JpaRepository` y escribe
  ambas tablas; la atomicidad la da la `@Transactional` del caso de uso.
- El DTO de la linea (`DetalleVentaRequest`) viaja anidado en el payload de la raiz y no
  acepta valores derivados. La respuesta anida `DetalleVentaResponse` dentro de `VentaResponse`.

---

## 6. Restricciones y Anti-patrones (Reglas para la IA)

### 6.1 Prohibiciones estructurales

1. **NO** importar `org.springframework.*`, `jakarta.*` ni nada de `Infrastructure/` dentro de
   `Domain/`. Es la invariante mas dura del proyecto y hoy se cumple al 100%.
2. **NO** usar en `Application/` ninguna anotacion de Spring distinta de `@Service` y
   `@Transactional`.
3. **NO** inyectar `I<Agregado>Datasource` en un caso de uso. Los casos de uso solo ven
   `I<Agregado>Repository`.
4. **NO** saltarse el `RepositoryImpl` ni meterle logica: es delegacion pura al datasource.
5. **NO** exponer agregados ni `JpaModel` desde un controller. Solo DTOs de `Application/Dtos`.
6. **NO** inyectar repositorios ni casos de uso de un agregado dentro del `DatasourceImpl` de
   otro. La composicion entre agregados ocurre en el caso de uso. Un `DatasourceImpl` si puede
   manejar varios `JpaRepository` cuando las tablas pertenecen **al mismo** agregado
   (`VentaDatasourceImpl` con venta y detalle_venta).
7. **NO** crear carpetas nuevas dentro de un agregado (`Services`, `Mappers`, `Helpers`,
   `Config`, `Exceptions`, `Constants`...). La estructura de la seccion 3.1 es cerrada. Una
   entidad hija tampoco abre carpeta: convive con su raiz en `Domain/Aggregates/` (ver 5.13).
8. **NO** colocar codigo suelto en el paquete raiz: solo existe `Kaffehausv4Application`.

### 6.2 Prohibiciones de implementacion

9. **NO** anadir dependencias al `pom.xml` (Lombok, MapStruct, ModelMapper, Bean Validation,
   Guava, Apache Commons, springdoc/Swagger, un logger). Todo lo que hoy se necesita se
   resuelve con el JDK y lo ya presente.
10. **NO** reintroducir Spring Security, Keycloak ni ninguna libreria de seguridad: fue una
    decision explicita. Para hashear se usa `IHasheadorDeContrasena`; para 401/403 existe
    `AutorizacionException`.
11. **NO** usar `@Valid` ni anotaciones de Bean Validation (`@NotNull`, `@Size`...). La
    validacion es imperativa en `createFromRequest` con `Validador`. Si falta un tipo de
    validacion, se **anade un metodo a `Validador`**, no se valida ad hoc en el DTO.
12. **NO** escribir constructores publicos en agregados ni en DTOs de negocio (salvo
    `PaginatedResponse` y `PaginationDTO`, que si los tienen por diseno). Se entra por factoria
    estatica.
13. **NO** anadir setters a los agregados. La mutacion pasa por `actualizar()` / `eliminar()`.
14. **NO** usar asociaciones JPA (`@ManyToOne`, `@OneToMany`, `@JoinColumn`, `fetch`, cascadas).
    Las relaciones son columnas `UUID` planas: entre agregados se resuelven en el caso de uso y
    dentro de un agregado las resuelve su datasource con los `JpaRepository` de sus tablas.
15. **NO** usar `@GeneratedValue`: el UUID lo genera `<Agregado>JpaMapper.toPersistence`.
16. **NO** usar `PageRequest.of(...)`. Siempre `OffsetPageRequest`.
17. **NO** escribir `@Query`, JPQL, SQL nativo ni Criteria API. Solo query methods derivados.
18. **NO** usar `double`/`float` para dinero o cantidades: `BigDecimal`.
19. **NO** usar inyeccion por campo (`@Autowired` sobre atributos) ni por setter. Solo constructor.
20. **NO** capturar excepciones en controllers ni devolver `ResponseEntity` de error a mano.
    Se lanza una `BaseException` y responde `GlobalExceptionHandler`.
21. **NO** propagar excepciones crudas de JPA/JDBC hacia arriba: se traducen a
    `DomainException` o `SystemException`.
22. **NO** cambiar la politica de borrado (soft/hard) de un agregado existente (ver 4.5).
23. **NO** introducir cache, eventos de dominio, mensajeria, `@Scheduled`, `@Async`, WebFlux ni
    estado en memoria compartido.
24. **NO** anadir endpoints fuera del prefijo `api/v1/`, ni rutas que no sigan el kebab-case del
    agregado, ni verbos distintos de los cinco estandar sin justificacion documentada aqui.
25. **NO** escribir logs (`System.out`, `printStackTrace`, loggers ad hoc) mientras no exista una
    estrategia de logging documentada en este archivo.
26. **NO** tratar `keycloak_sugerencia.md` como especificacion vigente: es una propuesta.
27. **NO** dar repositorio, datasource, caso de uso ni controller propios a una entidad hija.
    Si aparece la tentacion de crear un `DetalleXUseCase`, la operacion pertenece a la raiz.
28. **NO** aceptar del cliente un valor que el agregado deriva (totales, subtotales). Si el
    payload lo trae, se ignora o se rechaza: la raiz es la unica fuente de ese numero.
29. **NO** mutar la coleccion de hijas desde la capa de aplicacion. Se pasa siempre por los
    metodos de la raiz, aunque implique leer el agregado entero antes de modificarlo.

### 6.3 Reglas de nomenclatura obligatorias

30. Paquetes en **PascalCase** dentro de `BoundedContexts` y `Shared`, a pesar de la convencion
    Java estandar. La consistencia con el repositorio prevalece.
31. Identificadores de negocio **en espanol y sin tildes** (`contrasena`, `abreviatura`,
    `motivoDiferencia`). Sin mezclar ingles y espanol en una misma clase.
32. Interfaces de puerto con prefijo `I`.
33. Controllers terminan en `ControllerV1`; una nueva version de API crearia `...ControllerV2`
    bajo `api/v2/`, sin modificar el V1.

### 6.4 Procedimiento para anadir un agregado nuevo

1. Confirmar que la tabla existe en `databasev4.sql` y anotar si tiene columna `borrado`.
2. Copiar literalmente la estructura de `Inventario/Categoria` (19 archivos) y renombrar.
3. Ajustar campos en: agregado, `JpaModel`, `JpaMapper`, DTOs y llamadas a `Validador`.
4. Declarar en el `JpaRepository` los derivados necesarios: `findByIdAndBorradoFalse`,
   `findByBorradoFalse`, y el `findByBorradoFalseAnd<CampoTexto>ContainingIgnoreCase` para
   `search`. Si el agregado no tiene campo de texto buscable, el datasource usa `findAll(pageable)`
   e ignora `search` (precedente: `DetalleVenta`).
5. Si es hard-delete, omitir `borrado` en agregado y modelo, y usar `existsById` + `deleteById`.
6. Registrar el controller bajo `api/v1/<kebab-case>` con los cinco metodos estandar.
7. No hace falta ninguna clase de configuracion: el component scan desde
   `com.maestria.kaffehausv4` recoge `@Service`, `@Repository`, `@Component` y `@RestController`.

### 6.5 Desviaciones conocidas del estandar (no replicar, no "arreglar" sin pedirlo)

- `Personal/Entidad` usa la carpeta `Infrastructure/Datasource/` **en singular**; el resto usa
  `Infrastructure/Datasources/`. En agregados nuevos usar siempre el plural.
- `Personal/Persona` es el unico agregado **sin `Presentation/` ni `UseCases/`**: no se expone
  por HTTP y se gestiona exclusivamente desde los casos de uso de `Usuario`. Por eso su unico
  DTO es `PersonaResponse`. Es intencional.
- `Ventas/Venta` deja la linea `//String search = ...` comentada en su datasource porque la
  entidad no tiene campo de texto buscable.
- `Ventas/Venta` es el unico slice con dos `JpaModel`, dos `JpaMapper` y dos `JpaRepository`
  (venta y detalle_venta), por la entidad hija descrita en 5.13.
- Tests existentes: `Kaffehausv4ApplicationTests.contextLoads()`,
  `Ventas/Venta/Domain/Aggregates/VentaTest` (dominio puro, sin Spring) y
  `Ventas/Venta/Application/UseCases/CrearVentaUseCaseIntegracionTest` (`@SpringBootTest`
  contra la BD real, sin `@Transactional` de clase para poder observar el rollback del caso de
  uso). Cualquier test nuevo va bajo `src/test/java/com/maestria/kaffehausv4/` espejando el
  paquete de produccion.
- El test de integracion necesita PostgreSQL levantado (`docker compose up -d`) y limpia en
  `@AfterEach` lo que siembra. Para borrar en el `@AfterEach` usa `deleteAll(...)` y no el
  `deleteBy...` derivado, que exige una transaccion abierta y alli no la hay.
