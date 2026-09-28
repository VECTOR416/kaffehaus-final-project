# Plan: sesión de caja, pagos, ticket, RBAC y siembra

> Fase 1 del requerimiento "caja y cobro". Fecha: 2026-09-28, rama `feature-inventarios`.
> Punto de partida: 200 tests en verde (`./mvnw test`).

## Decisiones tomadas con el equipo

| Tema | Decisión |
|---|---|
| Convenciones | Se respeta `arquitectura.md`: casos de uso (`AbrirSesionCajaUseCase`…) en lugar de `CajaService`/`VentaService`, `*ControllerV1` y rutas singulares en kebab-case. |
| Swagger / OpenAPI | **No se agrega** (regla 9). Los endpoints nuevos se documentan en `arquitectura.md` (4.11 y 4.12), porque `ventas.md` se retiró del repositorio; las reglas nuevas llevan el prefijo `CAJ-xx`. |
| Base de datos | Se recrea `kaffehausdb` desde el nuevo `databasev4.sql`, sin migraciones incrementales. |

## 1. Hallazgos del análisis

- **No existe `VentaService`.** El alta la orquesta `CrearVentaUseCase`:
  1. comprueba la idempotencia (`clave_idempotencia`, VEN-66);
  2. valida cada línea;
  3. pone el precio y el mejor descuento;
  4. guarda la venta;
  5. descuenta el inventario línea por línea, en orden de id de producto
     (`RegistrarSalidaDeInventarioUseCase`: FEFO con `SELECT … FOR UPDATE`, backflush para
     MTO; INV-53);
  6. guarda el costo.

  Todo ocurre en una sola `@Transactional`. **Los pasos nuevos se insertan sin alterar ese
  orden de bloqueos.**
- La idempotencia se consulta **antes** que cualquier otra validación. Así, un reintento
  devuelve la venta original aunque la caja ya se haya cerrado.
- `venta.id_cliente` era `ON DELETE SET NULL`. Con `NOT NULL` esa regla es contradictoria y
  pasa a `RESTRICT`.
- `conversiones` tiene `CHECK (id_unidad_origen <> id_unidad_destino)` y
  `Conversion.factorEntre` ya devuelve 1 para la misma unidad. La conversión
  "Unidad → Unidad (1.0)" del requerimiento **no se siembra**: la base la rechazaría y no
  aporta nada.
- Tres reglas de `arquitectura.md` condicionan el diseño:
  - no se usan `@Query` ni SQL nativo, así que el "INNER JOIN cliente → persona" del ticket
    se compone en el caso de uso (§4.3);
  - todo modelo tiene `fec_c`, `fec_u` y `borrado` (§5.7), así que `movimiento_caja` y
    `pago_venta` llevan también `fec_u`;
  - no se escriben catálogos en el código, así que los tipos de pago, igual que los roles,
    se resuelven por nombre.

## 2. Diseño

### 2.1 Esquema (`databasev4.sql`)

- Tablas nuevas: `tipo_pago`, `sesion_caja`, `movimiento_caja` y `pago_venta`, con las
  columnas y `CHECK` del requerimiento más `fec_u`.
- Índices nuevos:
  - `uq_sesion_caja_abierta` (índice parcial): una sola caja abierta en el local;
  - `idx_movimiento_caja_sesion`.
- Cambios en `venta`:
  - `seq_numero_ticket` + `numero_ticket BIGINT NOT NULL DEFAULT nextval(...)`, con el
    índice único `idx_venta_numero_ticket`;
  - `id_sesion_caja NOT NULL`, con `idx_venta_sesion_caja`;
  - `id_cliente NOT NULL`;
  - `idx_pago_venta_venta` sobre `pago_venta(id_venta)`.
- La sección 7 (INSERT de catálogos) **se retira**: la siembra pasa a ser la única fuente de
  esos datos (`POST api/v1/seed`).

### 2.2 Agregados (contexto `Ventas`)

| Agregado | Contenido |
|---|---|
| `TipoPago` (nuevo) | Catálogo con el CRUD estándar en `api/v1/tipo-pago`. `esEfectivo()` por nombre, igual que `TipoDescuento.esPorcentaje()`. |
| `SesionCaja` (nuevo) | Raíz con entidad hija `MovimientoCaja` (§5.13). Reglas: abrir, registrar movimiento (solo abierta), cerrar (calcula esperado y diferencia). Estado `ABIERTA`/`CERRADA`. |
| `Venta` (cambia) | Guarda `sesionCajaId` y `numeroTicket` (lo asigna la base) y tiene una nueva hija `PagoVenta`. `agregarPago(...)` calcula el cambio; `validarPagos()` exige que la suma de los pagos sea igual al total. |

### 2.3 Endpoints

| Ruta | Rol | Caso de uso |
|---|---|---|
| `POST api/v1/sesion-caja/apertura` | Operador, Admin | `AbrirSesionCajaUseCase`: 409 si ya hay caja abierta |
| `POST api/v1/sesion-caja/movimientos` | Operador, Admin | `RegistrarMovimientoCajaUseCase`: INGRESO/EGRESO en la sesión abierta |
| `POST api/v1/sesion-caja/cierre` | Titular o Admin | `CerrarSesionCajaUseCase`: arqueo y desglose por tipo de pago |
| `GET api/v1/sesion-caja/estado` | Operador, Admin | `ObtenerEstadoCajaUseCase`: si hay turno, desde cuándo y quién |
| `GET api/v1/sesion-caja/{id}`, `GET api/v1/sesion-caja` | Operador, Admin | Historial de turnos |
| `GET api/v1/venta/ticket/{numeroTicket}` | Operador, Admin | `ObtenerVentaPorTicketUseCase`: venta + cliente + persona + cajero + pagos |
| `POST api/v1/seed` | Público (configurable) | `SembrarSistemaUseCase`: idempotente |

La fórmula del arqueo es:

    esperado   = monto_apertura + Σ pagos en efectivo de ventas Completadas
                 + Σ ingresos − Σ egresos
    diferencia = declarado − esperado

**Las ventas anuladas no suman al esperado**: se asume que al anular se devuelve el dinero.

### 2.4 Concurrencia

- **Venta:** lee la sesión abierta con `PESSIMISTIC_READ` (`FOR SHARE`) antes de tomar
  cualquier bloqueo de inventario. Varias ventas pueden ir en paralelo entre sí.
- **Cierre y movimientos de caja:** usan `PESSIMISTIC_WRITE`. Un cierre espera a que
  terminen las ventas en curso, y ninguna venta nueva entra en una sesión que se está
  cerrando. Como el orden es siempre sesión → existencias → producto, no se introduce
  ningún ciclo de bloqueos.
- **Dos aperturas simultáneas:** la segunda choca con `uq_sesion_caja_abierta` y
  `GlobalExceptionHandler` la traduce a 409, igual que la idempotencia.

### 2.5 RBAC (`@RequiereRol("Administrador")` sobre el método)

Quedan restringidos a `Administrador`:

- `crear`, `actualizar` y `eliminar` de `usuario` y `rol`;
- `crear`, `actualizar` y `eliminar` de `producto` (el precio se fija en el alta y en el
  `PUT`);
- `POST venta/{id}/anulacion`;
- `POST existencia/{id}/ajuste`;
- `PUT lote/{id}`, que admite `cantidadRestante` como ajuste de stock.

### 2.6 Siembra

`POST api/v1/seed` es idempotente: busca cada fila por nombre (o por id fijo) y solo crea
las que faltan. Nunca reescribe la contraseña de un administrador que ya existe.

- Se habilita con la variable `SEED_HABILITADO` (por defecto `true`).
- Con `false`, la ruta deja de ser pública y exige un token de `Administrador`.

## 3. Impacto en los tests existentes

| Test | Impacto | Adaptación |
|---|---|---|
| `VentaTest` (12) | `Venta.crear` recibe la sesión | Firma nueva; casos nuevos de pagos y cambio |
| `CrearVentaUseCaseIntegracionTest` (13) | Sin caja abierta → 409; `pagos` obligatorio | Ejecuta la siembra y abre la caja en `@BeforeEach` si no hay una; `vender(detalles, total)` paga en efectivo el total esperado; limpia pagos, ventas y la sesión que abrió |
| `PedidoVentaIntegracionTest` (6) | Igual | Igual |
| `FlujoInventarioIntegracionTest` (7) | Igual, incluida la prueba de dos ventas simultáneas | Igual; ambas ventas comparten la sesión con `FOR SHARE` |
| `AperturaInventarioIntegracionTest` (11) | Igual | Igual |
| `ProteccionDeEndpointsHttpTest` (6) | Sigue siendo válido: la anotación de clase no cambia | Se agregan comprobaciones de los métodos restringidos |
| Resto (dominio puro, seguridad) | Ninguno | — |

Hoy los tests dependen de que la sección 7 de `databasev4.sql` esté cargada. Con el cambio,
cada test de integración ejecuta `SembrarSistemaUseCase` (idempotente), así que ya no
dependen de una base sembrada a mano.

**Tests nuevos:**

- `SesionCajaTest`: dominio del cierre y de los movimientos.
- `CajaIntegracionTest`: apertura duplicada, venta sin caja, arqueo con pagos mixtos,
  movimientos y una anulada, y cierre por un no titular.
- `SembrarSistemaIntegracionTest`: se ejecuta dos veces sin duplicar y el administrador
  puede iniciar sesión.
- Ticket.
- RBAC HTTP.

## 4. Orden de implementación

1. `databasev4.sql` y recreación de la base.
2. Slice `TipoPago` (a partir de `EstadoVenta`) y `obtenerPorNombre` en Rol, Tipo,
   Categoría, Unidad y TipoDescuento.
3. Slice `SesionCaja` + `MovimientoCaja`.
4. `Venta`: sesión, cliente mostrador, `PagoVenta`, ticket y consulta por sesión.
5. Casos de uso de caja y traducción de `uq_sesion_caja_abierta` a 409.
6. RBAC.
7. Siembra + `SecurityConfig`.
8. Adaptación de los tests existentes y tests nuevos. `./mvnw test` en verde.
9. Documentación: `arquitectura.md` (4.11, 4.12 y 6.5), `auth.md` §5, §7 y §8, y `diagnostico.md`.
