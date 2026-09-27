# Propuesta de Implementacion: Modulo de Inventarios y Produccion

> Documento de analisis y propuesta. **No modifica codigo.** Se rige por `arquitectura.md`,
> que es la fuente de verdad arquitectonica: toda propuesta de aqui respeta la plantilla de
> agregado (3.1), la doble indireccion Repository/Datasource (2.3), el borrado logico (4.5)
> y las prohibiciones de la seccion 6.
>
> Estado del codigo auditado: rama `feature-keycloak`, despues del refactor
> `Categoria -> Tipo` + nueva `Categoria` (2026-09-20).
>
> **Revision 2026-09-22 — reglas de negocio fijadas por el usuario.** Prevalecen sobre
> cualquier propuesta anterior de este documento:
>
> - **R1.** Producir una receta con insumos abiertos insuficientes lanza un
>   `DomainException`. No se consume nada.
> - **R2.** El frontend resuelve la falta abriendo un `DetalleLote` hacia `existencias`
>   (3.1) y reintenta la produccion.
> - **R3.** Abrir un `DetalleLote` descuenta `producto.stock` del insumo. Para un insumo,
>   `producto.stock` es el **remanente cerrado**: lo comprado que todavia no se abrio.
> - **R4.** Producir crea un **lote de cocina** para el producto obtenido (sin proveedor,
>   ligado a la `produccion`, **costo 0** porque sus insumos ya estaban costeados en sus
>   compras) **y suma** `producto.stock` del producto obtenido, que es la cifra que lee
>   ventas. No toca `producto.stock` de los insumos (ya se desconto al abrir). El producto
>   obtenido no se usa como ingrediente de otra receta: es solo para la venta.
> - **R5.** El inventario debe poder **descartar** unidades que ya no pueden venderse (p. ej.
>   5 sandwiches producidos, 2 vendidos, 3 vencidos -> descarte de 3). El descarte baja
>   `producto.stock` y la `cantidad_restante` de la linea de lote afectada.
> - **R6.** Vender descuenta `producto.stock` **y** `detalle_lote.cantidad_restante`,
>   dejando registro de que lineas de lote abastecieron cada linea de venta. Asi se sabe que
>   lotes aun tienen producto y cuales estan finalizados, con traza de auditoria para
>   inventario y ventas. Aplica a todo producto vendido: comprado o de cocina.
> - **R7.** **FEFO en todo el sistema** (*first expired, first out*): venta, apertura de
>   insumos, consumo de existencias en produccion y descarte priorizan lo que vence antes,
>   para minimizar perdidas. Orden unico: `fecha_vencimiento` ascendente (sin vencimiento
>   al final), luego `lote.fecha_compra` ascendente (FIFO como desempate), luego
>   `detalle_lote.fec_c`. Lo **ya vencido no se vende ni se abre**: se descarta.
>
> **Ampliacion 2026-09-22 — puntos pendientes incorporados** (secciones 2.5-2.7 y 3.13-3.20):
>
> - **R8. Preparacion inmediata.** Las recetas marcadas `preparacion_inmediata` (cafe y sus
>   derivados) no se producen por tandas ni tienen lote o stock propio. Venderlas descuenta
>   de forma **teorica** sus insumos abiertos segun la receta (p. ej. 40 g de cafe), pero
>   **nunca bloquea la venta**: en la vida real se usan 35 o 45 g y esa variacion no se
>   modela. El punto de venta muestra las **unidades maximas** que se pueden preparar con lo
>   que hay en almacen (abierto + cerrado), y esa cifra es la senal para reabastecer antes
>   de quedarse sin insumo. Las existencias se cierran **fisicamente** (el barista indica
>   "se termino"), y el cierre cuadra la diferencia. Se descarta expresamente una tolerancia
>   por insumo: agrega complejidad sin beneficio para este negocio.
> - **R9. Costo real sin romper el costo 0.** El lote de cocina sigue a costo 0, pero cada
>   produccion y cada linea de venta guardan **una foto del costo** de lo que consumieron.
>   Descartes y mermas se valoran con ese costo: las perdidas nunca se reportan en 0.
> - **R10. Vida util abierta.** Un insumo abierto vence en la fecha que llegue antes: el
>   vencimiento del lote o la apertura mas `producto.dias_vida_abierto`.
> - **R11. Pedidos con entrega futura.** Una venta con pedido **no descuenta stock al
>   crearse** sino al **despacharse**, con FEFO respecto de la fecha real de salida. Los
>   pedidos pendientes alimentan el plan de produccion.
> - **R12. Stock cerrado corregible con auditoria.** Conteo fisico, carga inicial y
>   devolucion al proveedor mueven lotes y stock siempre con registro, motivo y usuario.
> - **R13. Anulaciones reversibles.** Anular una produccion (solo si nada de la tanda salio)
>   o una venta devuelve cada cantidad a su origen exacto.
> - **R14. Una sola nocion de "hoy".** Toda comparacion de fechas usa la zona del negocio
>   (`America/La_Paz`) a traves de un reloj inyectable, nunca la del servidor.

---

## 1. Diagnostico del Estado Actual

### 1.1 Que existe hoy

El contexto `Inventario` tiene 12 agregados. Los seis que participan del flujo de compra,
apertura, consumo y produccion son:

| Agregado | Tabla | Rol en el flujo | Estado |
|---|---|---|---|
| `Lote` (+ `DetalleLote`) | `lote`, `detalle_lote` | Compra: stock cerrado | Completo como CRUD compuesto |
| `Existencia` | `existencias` | Stock abierto / fraccionado | CRUD plano, sin vinculo real al lote |
| `Movimiento` | `movimientos` | Trazabilidad del consumo | CRUD plano, **no mueve stock** |
| `Receta` (+ hijas) | `receta`, `ingredientes_receta`, `pasos_receta` | Formula | Completo, rico en invariantes |
| `Produccion` | `produccion` | Ejecucion de una receta | CRUD plano, **no consume nada** |
| `Producto` | `producto` | Catalogo + `stock` | `stock` es un contador suelto |

Catalogos de apoyo: `Unidad`, `Conversion`, `EstadoExistencia`, `TipoMovimiento`, `Tipo`,
`Categoria`.

Los doce agregados exponen exactamente los cinco casos de uso estandar (`Crear`, `Actualizar`,
`Obtener...PorId`, `Obtener...`, `Eliminar`) bajo `api/v1/<kebab>`. **No hay un solo caso de
uso de negocio** fuera de ese CRUD en todo el contexto.

### 1.2 Lo que el refactor del 2026-09-20 ya dejo listo

Tres columnas que este flujo necesitaba y que ya existen:

- `existencias.fecha_apertura` + `existencias.id_usuario` — quien abrio y cuando.
- `detalle_lote.fecha_vencimiento` — caducidad por linea de compra.
- `movimientos.id_usuario` — autoria de cada movimiento.

Son los cimientos de la auditoria. Lo que falta es la **logica que los escriba**.

### 1.3 Brechas contra el modelo de negocio requerido

Ordenadas por severidad.

---

**B1 — `Produccion` no consume nada. Es un formulario, no una operacion.** (critica)

`CrearProduccionUseCase` valida que la receta exista y guarda una fila. No lee
`ingredientes_receta`, no toca `existencias`, no escribe `movimientos`. El requisito
*"auditar con precision cuantos gramos se consumieron por cada tanda"* es hoy imposible:
no hay ni consumo ni vinculo.

Ademas `diferencia` **se recibe del cliente** (`CrearProduccionRequest`), siendo
`cantidadReal - cantidadEsperada`. Eso viola la prohibicion 28 de `arquitectura.md`
("NO aceptar del cliente un valor que el agregado deriva") y permite que el encabezado
mienta sobre sus propias cifras.

`produccion` tampoco tiene `id_usuario`, cuando `lote`, `venta`, `movimientos` y
`existencias` ya lo tienen.

---

**B2 — La apertura de un insumo no existe como operacion.** (critica)

La regla *"toda Existencia debe originarse de un DetalleLote"* esta hoy garantizada
**solo por la FK** `existencias.id_detalle_lote`. `CrearExistenciaUseCase` acepta cualquier
`idDetalleLote` con un `contenidoInicial` arbitrario y **no descuenta
`detalle_lote.cantidad_restante`**.

Consecuencia concreta: un `detalle_lote` con `cantidad_restante = 1` admite cien existencias
de 5000 ml cada una. No hay conservacion de masa entre el stock cerrado y el abierto.

---

**B3 — `Movimiento` registra pero no mueve.** (critica)

`CrearMovimientoUseCase` guarda la fila y devuelve la respuesta. **Nunca actualiza
`existencias.contenido_actual`.** Las dos tablas pueden divergir sin limite, y la unica que
el frontend lee para saber el saldo (`contenido_actual`) no tiene relacion con la suma de
sus movimientos.

Peor: **`tipo_movimiento` no tiene direccion**. Es una tabla de `nombre` libre. Nada en el
esquema ni en el dominio dice si "Consumo" resta y "Devolucion" suma. Aunque el caso de uso
quisiera aplicar el efecto, no tendria con que decidir el signo.

---

**B4 — No se puede atribuir un consumo a una tanda de produccion.** (alta)

`movimientos` referencia `existencias` y `tipo_movimiento`, nada mas. No hay
`id_produccion`. El requisito de auditar por tanda necesita esa columna; sin ella solo
queda correlacionar por ventana de tiempo, que no es auditoria.

---

**B5 — Las unidades no se convierten en ningun punto.** (alta)

- `ingredientes_receta.cantidad` **no lleva unidad**: se asume implicitamente
  `producto.unidad_actual`.
- `existencias.unidad` es una FK propia, que puede ser otra distinta.
- `Conversion` existe como agregado completo... y **ningun caso de uso lo invoca**.
  Verificado: `IConversionRepository` solo se inyecta en los cinco CRUD de `Conversion`.

Una receta que pide `250` de harina contra una existencia en gramos y otra en kilos consume
lo mismo en ambas. Ademas `conversiones` es un grafo de pares sin cierre transitivo: existir
`kg->g` y `g->mg` no da `kg->mg`, y nada garantiza que `kg->g` y `g->kg` sean reciprocos.

---

**B6 — Escalas decimales inconsistentes a lo largo del flujo.** (alta)

| Columna | Escala |
|---|---|
| `detalle_lote.cantidad_inicial` / `cantidad_restante` | `NUMERIC(12, 2)` |
| `existencias.contenido_inicial` / `contenido_actual` | `NUMERIC(12, 4)` |
| `movimientos.cantidad` | `NUMERIC(12, 4)` |
| `ingredientes_receta.cantidad` | `NUMERIC(12, 4)` |
| `producto.stock` | `NUMERIC(12, 2)` |

El flujo cruza escalas justo donde no debe: abrir descuenta de una columna de 2 decimales un
consumo medido en 4. `DetalleLote.ESCALA = 2` redondea con `HALF_UP` en cada mutacion, asi
que devolver un sobrante de `0.004` al lote lo convierte en `0.00` y la masa se pierde.

---

**B7 — `producto.stock` es un tercer contador sin dueno.** (media)

Nadie lo recalcula desde lotes ni existencias; solo se escribe por el CRUD de `Producto`.
Ni siquiera `CrearLoteUseCase` lo incrementa al registrar una compra.

**Resuelto por R3-R6:** `producto.stock` pasa a tener dueno y **una sola definicion para
todo producto**: `producto.stock = sum(detalle_lote.cantidad_restante)` de sus lineas vivas.
Lo que cambia es el origen del lote:

- **Producto comprado (insumo o reventa):** lote de compra. Sube al comprar, baja al abrir
  (3.1), al vender (3.12) y al descartar (3.10). El consumo de existencias no lo toca.
- **Producto obtenido (terminado):** lote de cocina (R4). Sube al producir (3.2), baja al
  vender y al descartar.

Lo que queda por hacer es que **ningun CRUD** pueda escribirlo directamente (ver 3.11).

---

**B8 — La superficie de consulta no alcanza para las vistas pedidas.** (media)

Los `JpaRepository` del contexto solo declaran `findByIdAndBorradoFalse`,
`findByBorradoFalse` y el `...ContainingIgnoreCase` de turno. En concreto faltan:

- `ExistenciaJpaRepository`: no hay filtro por `id_detalle_lote`, por `id_estado_existencia`
  ni por producto. **No se puede listar "los insumos abiertos"** ni saber si un lote ya tiene
  aperturas.
- `MovimientoJpaRepository`: no hay filtro por `id_existencia`. **No hay kardex.**

Ademas `ObtenerExistenciasUseCase` resuelve `EstadoExistencia` y `Unidad` **por fila** dentro
del `map`: N+1 consultas por pagina. `ObtenerMovimientosUseCase` hace lo mismo con
`TipoMovimiento`. `Lote` y `Receta` si resuelven sus hijas en una consulta por pagina; estos
dos no siguieron el patron.

---

**B9 — `estado_existencia` es texto libre que el dominio ignora.** (media)

El agregado `Existencia` guarda un `EstadoExistenciaId` y nunca lo consulta. Nada impide
consumir de una existencia marcada "Agotada" o "Vencida", ni hay transicion de estado al
llegar `contenido_actual` a cero. `fecha_acabado` existe y tampoco se escribe sola.

---

**B10 — Sin control de concurrencia en ningun punto.** (media)

No hay `@Version` en ningun `JpaModel` ni bloqueo pesimista en ningun datasource. Dos
aperturas simultaneas del mismo `detalle_lote`, o dos producciones que consumen la misma
existencia, leen el mismo saldo y escriben ambas. El requisito explicito de *"evitar doble
apertura de un mismo lote"* no tiene hoy ningun mecanismo detras.

---

**B11 — Todo el contexto esta sin autorizacion por rol.** (baja)

`@RequiereRol` existe en `Shared/Presentation/Security` y **no se aplica en ningun
controller** de `BoundedContexts`. Abrir un lote, ajustar stock o anular una produccion
quedan al alcance de cualquier usuario autenticado.

---

## 2. Modelo de Dominio y Flujo Operativo

### 2.1 Ciclo de vida propuesto

```
   COMPRA                 APERTURA                CONSUMO              PRODUCCION
┌─────────────┐        ┌──────────────┐       ┌─────────────┐       ┌──────────────┐
│    Lote     │        │  Existencia  │       │  Movimiento │       │  Produccion  │
│ DetalleLote │ ─────► │   (abierta)  │ ────► │   (kardex)  │ ────► │   (tanda)    │
└─────────────┘        └──────────────┘       └─────────────┘       └──────────────┘
 cantidad_restante      contenido_actual        cantidad +/-          cantidad_real
 NUMERIC(12,4)          NUMERIC(12,4)           NUMERIC(12,4)         vs esperada

  (0) COMPRAR  alta de detalle_lote -> producto.stock (insumo) += cantidad_inicial

  (1) ABRIR    descuenta cantidad_restante y producto.stock (insumo),
               crea la existencia y escribe un movimiento APERTURA
                                                          ── una transaccion

  (2) PRODUCIR verifica que las existencias abiertas alcancen (si no:
               DomainException, no se consume nada), elige existencias por
               FEFO, descuenta cada contenido_actual, escribe un movimiento
               CONSUMO por cada una con id_produccion, crea el LOTE DE COCINA
               (costo 0, vencimiento de la tanda) y suma cantidad_real a
               producto.stock del producto obtenido     ── una transaccion
               (producto.stock de los insumos NO cambia)

  (3) CERRAR   cierre FISICO: el operador indica que el envase se termino ->
               estado AGOTADA + fecha_acabado; el residuo teorico que quede
               sale como MERMA. Llegar a 0 en el sistema NO cierra solo.

  (4) DESCARTAR unidades no vendibles (vencidas) -> cantidad_restante de la
               linea y producto.stock -= cantidad, registro en descarte
                                                          ── una transaccion

  (5) VENDER   producto.stock -= cantidad y cantidad_restante por FEFO,
               registra cada asignacion en detalle_venta_lote
                                                          ── una transaccion
```

Tres orígenes de lote, un mismo circuito de salida:

```
 lote de COMPRA   (id_proveedor, precio real) ──┐
 lote INICIAL     (arranque, costo estimado)  ──┼─► detalle_lote ─► VENDER / DESCARTAR (FEFO)
 lote de COCINA   (id_produccion, precio 0)   ──┘        │          DEVOLVER (solo compra)
                                                          │          AJUSTAR por conteo
                                                          └─► ABRIR (compra o inicial)

 receta de PREPARACION INMEDIATA: sin lote ─► VENDER consume existencias abiertas (R8)
```

Flujo ante insumo insuficiente (R1 + R2):

```
 Frontend                          Backend
 POST /produccion/ejecucion  ───►  PlanDeConsumo detecta faltantes
                             ◄───  409 DomainException + lista de faltantes
 elige detalle_lote del insumo
 POST /existencia/apertura   ───►  abre: existencia nueva, producto.stock -=
                             ◄───  201
 POST /produccion/ejecucion  ───►  ahora alcanza: consume y suma stock
                             ◄───  201
```

**Invariante rectora del modulo** (hoy inexistente, es el corazon de la propuesta):

> Ningun saldo cambia sin un `Movimiento` que lo explique, y ningun `Movimiento` se escribe
> sin cambiar el saldo que dice cambiar. Ambos, siempre, en la misma transaccion.

De ahi se derivan cinco reglas verificables:

1. `existencias.contenido_actual` = `contenido_inicial` + suma de sus movimientos con signo.
2. `sum(existencias.cantidad_abierta)` de un `detalle_lote` <= `detalle_lote.cantidad_inicial`.
3. Todo producto con lotes: `producto.stock` = `sum(detalle_lote.cantidad_restante)` de sus
   lineas vivas. Se preserva porque compra, carga inicial, apertura, produccion, venta,
   descarte, devolucion y conteo mueven ambas cifras a la vez, siempre. Los productos de
   preparacion inmediata (R8) no tienen lotes y su `stock` es siempre 0.
4. Por cada `detalle_lote`: `cantidad_inicial` - `cantidad_restante` = lo abierto
   (`existencias.cantidad_abierta`) + lo vendido (`detalle_venta_lote`) + lo descartado
   (`descarte`) - la suma con signo de sus ajustes (`ajuste_lote`).
5. Todo `lote` de cocina tiene `total = 0` y exactamente una `produccion` de origen; toda
   `produccion` viva, no anulada y con `cantidad_real > 0` tiene exactamente un lote de
   cocina vivo.

La regla 4 es la razon por la que venta y descarte guardan su reparto por linea: sin el, la
salida de un lote no tiene explicacion, contra la invariante rectora.

### 2.2 Manejo de unidades y precision

**Regla de unidad canonica.** Toda cantidad se normaliza a `producto.unidad_actual` antes de
compararse o restarse. La unidad de una `Existencia` es solo la unidad **de presentacion** en
que se midio la apertura.

**Conversion.** La operacion `convertir(cantidad, unidadOrigen, unidadDestino)` resuelve asi:

- identidad: si origen == destino, factor 1;
- factor directo: fila `conversiones(origen, destino)` -> `cantidad * factor`;
- factor inverso: fila `conversiones(destino, origen)` -> `cantidad / factor`;
- si no hay ninguno, **error 400 explicito**, nunca un factor asumido.

Se propone **no** implementar cierre transitivo en una primera version: encadenar factores
multiplica el error de redondeo y esconde inconsistencias del catalogo. Es preferible exigir
el par directo o inverso y fallar claro.

Donde vive: `Shared/Application/Medidas/ConvertidorDeUnidades`, funcion pura que recibe el
factor ya resuelto. La **busqueda** del factor queda en el caso de uso via
`IConversionRepository`, como manda la seccion 4.3 (la composicion entre agregados ocurre en
el caso de uso).

> Requiere actualizar antes `arquitectura.md` seccion 3.2, porque anade una entrada al Shared
> Kernel. El propio documento exige ese orden.

**Precision.** Se propone unificar en `NUMERIC(12, 4)` todo lo fraccionable:
`detalle_lote.cantidad_inicial`, `cantidad_restante` y `producto.stock` suben de 2 a 4
decimales. Los importes de dinero (`precio_unitario`, `total`) se quedan en 2.

Regla de redondeo: **una sola vez, al final de la operacion**, con `RoundingMode.HALF_UP`.
Los calculos intermedios de una produccion (rendimiento x ingrediente) se acumulan sin
redondear. La constante `DetalleLote.ESCALA` pasaria de `2` a `4`.

**Residuos.** Al cerrar una existencia puede quedar un saldo por debajo de la escala. No se
descarta en silencio: se registra como movimiento `MERMA` con el residuo exacto, de modo que
el kardex cuadre a cero.

### 2.3 Cambios de esquema que el modelo exige

Sobre `databasev4.sql`:

```sql
-- B3: direccion del movimiento. -1 resta del saldo, +1 suma.
ALTER TABLE tipo_movimiento ADD COLUMN signo SMALLINT NOT NULL DEFAULT -1;
ALTER TABLE tipo_movimiento ADD CONSTRAINT chk_signo CHECK (signo IN (-1, 1));

-- B4: atribucion del consumo a la tanda que lo causo.
ALTER TABLE movimientos ADD COLUMN id_produccion UUID
    REFERENCES produccion(id) ON UPDATE CASCADE ON DELETE RESTRICT;
CREATE INDEX idx_movimientos_produccion ON movimientos(id_produccion);

-- B1: autoria de la tanda.
ALTER TABLE produccion ADD COLUMN id_usuario UUID NOT NULL
    REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT;

-- B6: una sola escala para lo fraccionable.
ALTER TABLE detalle_lote ALTER COLUMN cantidad_inicial  TYPE NUMERIC(12, 4);
ALTER TABLE detalle_lote ALTER COLUMN cantidad_restante TYPE NUMERIC(12, 4);
ALTER TABLE producto     ALTER COLUMN stock             TYPE NUMERIC(12, 4);

-- B10: bloqueo optimista donde compite la concurrencia.
ALTER TABLE existencias  ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE detalle_lote ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- B8: indices para las vistas de frontend.
CREATE INDEX idx_existencias_apertura ON existencias(fecha_apertura);

-- R4 + R12: origen del lote. COMPRA exige proveedor, COCINA exige produccion,
-- INICIAL (carga de arranque) no tiene ninguno de los dos.
ALTER TABLE lote ALTER COLUMN id_proveedor DROP NOT NULL;
ALTER TABLE lote ADD COLUMN id_produccion UUID UNIQUE
    REFERENCES produccion(id) ON UPDATE CASCADE ON DELETE RESTRICT;
ALTER TABLE lote ADD COLUMN origen VARCHAR(10) NOT NULL DEFAULT 'COMPRA'
    CHECK (origen IN ('COMPRA', 'COCINA', 'INICIAL'));
ALTER TABLE lote ADD CONSTRAINT chk_lote_origen CHECK (
       (origen = 'COMPRA'  AND id_proveedor IS NOT NULL AND id_produccion IS NULL)
    OR (origen = 'COCINA'  AND id_proveedor IS NULL     AND id_produccion IS NOT NULL)
    OR (origen = 'INICIAL' AND id_proveedor IS NULL     AND id_produccion IS NULL));

-- R8: recetas que se preparan al vender, sin tanda ni lote.
ALTER TABLE receta ADD COLUMN preparacion_inmediata BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE receta ADD COLUMN dias_vida_util INTEGER CHECK (dias_vida_util >= 0);
ALTER TABLE movimientos ADD COLUMN id_detalle_venta UUID
    REFERENCES detalle_venta(id) ON UPDATE CASCADE ON DELETE RESTRICT;
CREATE INDEX idx_movimientos_detalle_venta ON movimientos(id_detalle_venta);
ALTER TABLE movimientos ADD CONSTRAINT chk_movimiento_causa
    CHECK (NOT (id_produccion IS NOT NULL AND id_detalle_venta IS NOT NULL));

-- R9: foto del costo en el momento del hecho.
ALTER TABLE existencias   ADD COLUMN cantidad_abierta NUMERIC(12, 4) NOT NULL DEFAULT 0;
ALTER TABLE produccion    ADD COLUMN costo_total NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE detalle_venta ADD COLUMN costo NUMERIC(12, 2) NOT NULL DEFAULT 0;

-- R10: vida util una vez abierto.
ALTER TABLE producto ADD COLUMN dias_vida_abierto INTEGER CHECK (dias_vida_abierto >= 0);

-- R13: anulacion de produccion.
ALTER TABLE produccion ADD COLUMN anulada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE produccion ADD COLUMN motivo_anulacion TEXT;

-- R11: el pedido descuenta al despacharse.
ALTER TABLE pedidos ADD COLUMN fecha_despacho TIMESTAMP;
ALTER TABLE pedidos ADD COLUMN id_usuario_despacho UUID
    REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT;
ALTER TABLE venta   ADD COLUMN stock_descontado BOOLEAN NOT NULL DEFAULT TRUE;

-- R12: ajustes auditables de stock cerrado (conteo y devolucion al proveedor).
CREATE TABLE conteo_fisico (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fecha_conteo TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(10) NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR', 'APLICADO', 'ANULADO')),
    observacion TEXT,
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    id_usuario_aplica UUID REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fecha_aplicacion TIMESTAMP,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE detalle_conteo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_conteo_fisico UUID NOT NULL REFERENCES conteo_fisico(id) ON UPDATE CASCADE ON DELETE CASCADE,
    id_detalle_lote UUID REFERENCES detalle_lote(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    id_existencia UUID REFERENCES existencias(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    cantidad_sistema NUMERIC(12, 4) NOT NULL,
    cantidad_contada NUMERIC(12, 4) NOT NULL CHECK (cantidad_contada >= 0),
    motivo TEXT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_detalle_conteo_objetivo CHECK ((id_detalle_lote IS NULL) <> (id_existencia IS NULL))
);

CREATE TABLE ajuste_lote (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_detalle_lote UUID NOT NULL REFERENCES detalle_lote(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    tipo VARCHAR(25) NOT NULL CHECK (tipo IN ('CONTEO', 'DEVOLUCION_PROVEEDOR', 'ANULACION_PRODUCCION')),
    cantidad NUMERIC(12, 4) NOT NULL CHECK (cantidad <> 0),   -- con signo: + entra, - sale
    motivo TEXT NOT NULL,
    id_conteo_fisico UUID REFERENCES conteo_fisico(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fecha_ajuste TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_ajuste_lote_detalle ON ajuste_lote(id_detalle_lote);

-- R5: descarte auditable, una fila por linea de lote afectada. movimientos no sirve
-- porque id_existencia es NOT NULL y lo que se descarta es stock cerrado.
CREATE TABLE descarte (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_detalle_lote UUID NOT NULL REFERENCES detalle_lote(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    cantidad NUMERIC(12, 4) NOT NULL CHECK (cantidad > 0),
    motivo TEXT NOT NULL,
    fecha_descarte TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_descarte_detalle_lote ON descarte(id_detalle_lote);

-- R7: orden FEFO sobre lineas de lote.
CREATE INDEX idx_detalle_lote_fefo ON detalle_lote(id_producto, fecha_vencimiento);

-- R6: que lineas de lote abastecieron cada linea de venta (reparto FEFO).
CREATE TABLE detalle_venta_lote (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_detalle_venta UUID NOT NULL REFERENCES detalle_venta(id) ON UPDATE CASCADE ON DELETE CASCADE,
    id_detalle_lote UUID NOT NULL REFERENCES detalle_lote(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    cantidad NUMERIC(12, 4) NOT NULL CHECK (cantidad > 0),
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_detalle_venta_lote_venta ON detalle_venta_lote(id_detalle_venta);
CREATE INDEX idx_detalle_venta_lote_lote  ON detalle_venta_lote(id_detalle_lote);
CREATE INDEX idx_lote_fecha_compra        ON lote(fecha_compra);
ALTER TABLE detalle_venta ALTER COLUMN cantidad TYPE NUMERIC(12, 4);

-- Escala coherente con producto.stock (12, 4).
ALTER TABLE produccion ALTER COLUMN cantidad_esperada TYPE NUMERIC(12, 4);
ALTER TABLE produccion ALTER COLUMN cantidad_real     TYPE NUMERIC(12, 4);
ALTER TABLE produccion ALTER COLUMN diferencia        TYPE NUMERIC(12, 4);
```

**Decision tomada (2026-09-22) — donde entra el producto obtenido: lote de cocina + stock.**
Se adopta la opcion (a) del analisis original, reforzada con la suma a `producto.stock`:

- La produccion crea un `lote` con `origen = 'COCINA'`, `id_produccion` = la tanda,
  `id_proveedor` nulo, `id_usuario` = quien produjo y `fecha_compra` = fecha de produccion
  (la columna se lee como "fecha de ingreso").
- Ese lote lleva **una** linea `detalle_lote`: el producto obtenido,
  `cantidad_inicial = cantidad_restante = cantidad_real`, `precio_unitario = 0`,
  `fraccionable = false` y la `fecha_vencimiento` de la tanda.
- Ademas `producto.stock += cantidad_real`. Ventas **solo lee `producto.stock`**, y la
  regla 3 de 2.1 garantiza que coincide con la suma de los lotes.

**Por que costo 0.** Los insumos consumidos ya estan costeados en sus lotes de compra;
darle precio al lote de cocina contaria dos veces el mismo gasto en cualquier reporte de
compras. El dominio actual ya lo admite: `DetalleLote.exigirPrecioValido` solo rechaza
precios negativos y `Lote.total` se deriva de las lineas, asi que queda en 0 solo. El
**costo real de la tanda** no se pierde: se guarda en `produccion.costo_total` (2.5).

**Por que es mas robusto que solo subir el stock.** Cada tanda tiene vencimiento propio y
entra al mismo circuito FEFO que las compras; venta y descarte saben de que tanda sale cada
unidad; y "que tandas siguen con unidades" es simplemente "que lineas tienen
`cantidad_restante > 0`".

**Reglas del lote de cocina en el dominio**

- Nace solo por `Lote.crearDeProduccion(produccionId, fecha, usuarioId, productoId,
  cantidad, fechaVencimiento)`, que fija el precio en 0. `Lote.crear(...)` (compra) exige
  proveedor. El CRUD de `Lote` nunca acepta `idProduccion` del cliente (prohibicion 28).
- `ActualizarLoteUseCase` y `EliminarLoteUseCase` rechazan un lote de cocina con
  `DomainException(409)`: solo lo modifican produccion, venta y descarte.
- No se abre como existencia (3.1 lo rechaza): el terminado no es ingrediente (R4).

### 2.4 Politica FEFO (R7)

**Decision.** Se usa **FEFO con FIFO como desempate**, y no FIFO puro, porque el objetivo
declarado es minimizar perdidas: FIFO vende primero lo comprado primero aunque un lote mas
nuevo venza antes (p. ej. leche comprada el lunes con vencimiento el 30 y otra comprada el
martes que vence el 25: FIFO vende la del lunes y deja vencer la del martes). FEFO nunca
comete ese error, y cuando dos lineas vencen el mismo dia o no tienen vencimiento cae a
FIFO, que es lo razonable.

**Orden unico para toda seleccion automatica**

1. `fecha_vencimiento` ascendente; las lineas **sin** vencimiento van al final (no
   caducan, no hay urgencia).
2. `lote.fecha_compra` ascendente (para lotes de cocina, la fecha de produccion).
3. `detalle_lote.fec_c` ascendente, para que el orden sea determinista.

Para existencias (consumo en produccion) el mismo criterio: `existencias.fecha_vencimiento`,
luego `fecha_apertura`.

**Donde vive.** Una sola clase de dominio pura, `Lote/Domain/AsignadorFefo`: recibe las
lineas candidatas y una cantidad, devuelve el reparto `[(idDetalleLote, cantidad)]` o lanza
`DomainException(409)` si no alcanza. La usan venta (3.12), descarte automatico (3.10) y
la sugerencia de `lotesParaAbrir` (3.9). `PlanDeConsumo` (3.2) aplica el mismo orden sobre
existencias. Un solo criterio evita que venta y apertura "piensen" distinto.

**Lo vencido queda fuera.** Las lineas con `fecha_vencimiento < hoy` **no** entran al
reparto de venta ni a `lotesParaAbrir`, y 3.1 rechaza abrirlas. Siguen contando en
`producto.stock` hasta que se descartan (3.10); por eso:

- `ProductoResponse` expone `stockVencido` derivado (suma de lineas vencidas con saldo), y
  ventas puede mostrar `stock - stockVencido` como lo realmente vendible;
- si una venta pide mas de lo vigente, el error dice cuanto hay vencido pendiente de
  descarte, no solo "stock insuficiente";
- la consulta `GET api/v1/lote/por-vencer` (3.9) es la herramienta diaria para vender o
  usar primero lo proximo a vencer y descartar lo ya vencido.

**Override manual.** El operador puede elegir otra linea explicitamente (apertura siempre
es explicita; en produccion via `consumos`; en descarte via `idDetalleLote`). FEFO es el
**defecto**, no una prohibicion; lo vencido si esta prohibido.

**Fecha de referencia.** `AsignadorFefo` no pregunta la hora: recibe `fechaReferencia`
como parametro. En una venta de mostrador es `hoy`; en el despacho de un pedido (R11) es la
fecha de entrega, de modo que no se asigna una linea que vence antes de llegar al cliente.

### 2.5 Costeo (R9)

**Costo por unidad de contenido de una existencia** (derivado, no se guarda):

```
costo_contenido = detalle_lote.precio_unitario * existencia.cantidad_abierta
                  / existencia.contenido_inicial
```

Ejemplo: bolsa de harina a 50 Bs (`precio_unitario`), se abre 1 (`cantidad_abierta`) con
5000 g (`contenido_inicial`) -> 0,01 Bs/g. Por eso 3.1 pasa a guardar `cantidad_abierta`:
sin ella el costo del gramo no se puede reconstruir. No requiere tabla de conversiones.

**Fotos de costo que se guardan** (calculadas por el servidor en el momento, nunca
recibidas del cliente, y congeladas para que editar un precio de compra despues no
reescriba la historia):

| Hecho | Columna | Calculo |
|---|---|---|
| Produccion (3.2) | `produccion.costo_total` | suma de cada `CONSUMO` x `costo_contenido` de su existencia |
| Venta de producto con lote | `detalle_venta.costo` | suma de cada asignacion x costo unitario de su linea |
| Venta de preparacion inmediata (R8) | `detalle_venta.costo` | suma de sus `CONSUMO` x `costo_contenido` |

**Costo unitario de una linea de lote** (derivado):

- `COMPRA` / `INICIAL`: `precio_unitario`.
- `COCINA`: `produccion.costo_total / produccion.cantidad_real` (se recalcula solo si
  cambia `cantidad_real` por 3.3; `costo_total` no cambia, lo consumido fue lo mismo).

**Valoracion de perdidas.** Descarte = `cantidad` x costo unitario de su linea. Merma =
`cantidad` x `costo_contenido` de su existencia. Asi un sandwich descartado vale lo que
costaron sus insumos, no 0. Lo consume el reporte de perdidas (3.9).

**Margen.** `detalle_venta.subtotal - detalle_venta.costo` da el margen por linea sin
consultas adicionales; es el reporte que mas le importa al negocio y queda gratis.

### 2.6 Vida util abierta (R10)

Al abrir (3.1), el servidor calcula y **no acepta del cliente**:

```
existencia.fecha_vencimiento = min( detalle_lote.fecha_vencimiento,
                                    fecha_apertura::date + producto.dias_vida_abierto )
```

Si alguno de los dos es nulo se usa el otro; si ambos son nulos, la existencia no vence.
Una existencia vencida queda fuera de `PlanDeConsumo` y aparece en el grupo "vencidas" de
`GET api/v1/existencia/abiertas`, de donde el operador la cierra con `MERMA` (3.5), que
la deja en estado `Vencida`.

### 2.7 Tiempo del negocio (R14)

Hoy `Validador` usa `ZoneId.systemDefault()` y los casos de uso llaman
`LocalDateTime.now()`. Si el servidor corre en UTC, desde las 20:00 de La Paz el sistema ya
cree que es manana: un sandwich que vence "hoy" dejaria de venderse cuatro horas antes.

- `Shared/Application/Tiempo/IReloj` con `hoy()` y `ahora()`; implementacion en
  `Shared/Infrastructure/Tiempo/RelojDelNegocio` con la zona de la propiedad
  `app.zona-horaria=America/La_Paz`.
- Los casos de uso reciben `IReloj` por constructor y pasan `hoy` al dominio como
  parametro (`AsignadorFefo`, `PlanDeConsumo`, `Existencia.estaVencida(hoy)`). El dominio
  sigue sin conocer relojes ni frameworks.
- `Validador` usa la misma zona en lugar de `systemDefault()`.
- Beneficio lateral: los tests fijan la fecha con un reloj falso y prueban vencimientos
  sin esperar.

> Anade dos entradas al Shared Kernel: va al Bloque 0 junto a `ConvertidorDeUnidades`.

### 2.8 `producto.id_producto_padre` — alcance

El campo existe y ningun flujo define su efecto sobre el stock. Se fija: **es solo
agrupacion de catalogo y reportes** (p. ej. "Leches" agrupa entera, deslactosada y de
almendra). **No** transfiere stock, **no** implica conversion y **no** participa en FEFO:
el stock vive siempre en el producto concreto. Las presentaciones de compra (caja de 12 vs
unidad) se resuelven al abrir, con `cantidadAAbrir` / `contenidoInicial` (3.1), no con la
jerarquia. Los reportes de 3.9 aceptan `idProductoPadre` como filtro para ver totales por
grupo.

> **Por confirmar con el negocio.** Si la intencion original era modelar presentaciones con
> conversion automatica, este punto debe revisarse antes del Bloque 1.

---

## 3. Catalogo de Casos de Uso

Notacion: cada caso indica **Tipo**, **Entradas**, **Salidas** y **Logica de transaccion**.
Todos los nuevos respetan la plantilla obligatoria: `@Service @Transactional`, un unico
metodo publico `execute(...)`, dependencias solo de interfaces `I*Repository` por
constructor, y DTO de entrada construido con `createFromRequest(Map)` + `Validador`.

> Nota de arquitectura: varios de estos casos rompen el molde de "cinco casos de uso por
> agregado" (5.1) y anaden verbos fuera del CRUD estandar (prohibicion 25). `arquitectura.md`
> lo permite explicitamente si se **documenta primero**; la seccion 5 de este documento pone
> esa actualizacion como tarea previa a escribir codigo.

---

### 3.1 `AbrirDetalleLoteUseCase` — **Nuevo**

El caso de uso que hoy no existe y que sostiene la regla de origen (B2).

`POST api/v1/existencia/apertura`

**Entradas**

| Campo | Tipo | Obligatorio | Validacion |
|---|---|---|---|
| `idDetalleLote` | String (UUID) | si | `textoRequerido`; la linea existe y esta viva |
| `cantidadAAbrir` | number | si | `numeroRequerido`; > 0; <= `cantidad_restante` |
| `contenidoInicial` | number | si | `numeroRequerido`; > 0; contenido fraccionado real |
| `idUnidad` | String (UUID) | si | `textoRequerido`; unidad de la existencia |
| `idUsuario` | String (UUID) | si | `textoRequerido`; quien abre |
| `fechaApertura` | fecha | no | `fechaOpcional`, por defecto `LocalDateTime.now()` |

`cantidadAAbrir` se expresa en la unidad del producto (cuantas piezas/kg del lote se abren);
`contenidoInicial` en `idUnidad` (cuanto contiene realmente lo abierto). Ejemplo: abrir
`1` bolsa (`cantidadAAbrir`) que contiene `5000` gramos (`contenidoInicial`, `idUnidad` = g).

**Salidas** — `201 CREATED`

```json
{
  "id": "uuid-existencia",
  "cantidadAbierta": 1.0000,
  "contenidoInicial": 5000.0000,
  "contenidoActual": 5000.0000,
  "costoContenido": 0.0100,
  "fechaApertura": "2026-09-22T10:15:00",
  "fechaVencimiento": "2027-01-31",
  "idDetalleLote": "uuid",
  "estadoExistencia": { "id": "uuid", "nombre": "Abierta" },
  "unidad": { "id": "uuid", "unidad": "gramo", "abreviatura": "g" },
  "idUsuario": "uuid",
  "movimiento": { "id": "uuid", "cantidad": 5000.0000, "tipoMovimiento": "APERTURA" },
  "detalleLote": { "id": "uuid", "cantidadRestante": 9.0000 },
  "producto": { "id": "uuid", "nombre": "Harina 000", "stock": 23.0000 }
}
```

Devolver el saldo del lote y el remanente del insumo (`producto.stock`, R3) en la misma
respuesta evita que el frontend tenga que recargar.

**Logica de transaccion**

1. Cargar el `Lote` via `ILoteRepository.obtenerPorDetalleId(...)` — 404 si no existe.
   Si es un **lote de cocina** -> `DomainException(409)`: el producto obtenido no es
   ingrediente (R4).
2. Validar `idUsuario`, `idUnidad` y el estado "Abierta" del catalogo.
3. **Bloqueo pesimista** sobre `detalle_lote` (ver 4.3) antes de leer `cantidad_restante`.
4. `lote.ajustarCantidadRestante(idDetalle, restanteActual - cantidadAAbrir)` — la raiz
   valida que no quede negativo, per 5.13 toda mutacion de una hija pasa por su raiz.
5. Calcular `fecha_vencimiento` de la existencia segun 2.6 (R10); no se acepta del cliente.
6. `Existencia.crear(...)` con `contenidoActual = contenidoInicial` y
   `cantidadAbierta = cantidadAAbrir` (necesaria para la regla 2 y el costeo de 2.5).
7. `Movimiento.crear(...)` de tipo `APERTURA`, ligado a la existencia recien creada.
8. **R3:** cargar el `Producto` del `detalle_lote` via `IProductoRepository` y llamar
   `producto.descontarStock(cantidadAAbrir)`. El metodo de dominio lanza
   `DomainException(409)` si el stock quedaria negativo: eso delata una divergencia con
   `cantidad_restante` (regla 3 de 2.1) y no debe silenciarse.
9. Persistir los cuatro agregados (`Lote`, `Existencia`, `Movimiento`, `Producto`).

**Atomicidad.** Los cuatro `save` viven bajo la `@Transactional` del caso de uso. Si el
paso 4 u 8 falla, no queda ni existencia, ni movimiento, ni stock alterado. Precedente en el
proyecto: `CrearUsuarioUseCase` ya escribe `Usuario` + `Persona` en una sola transaccion.

**Unidad del descuento.** `cantidadAAbrir` ya esta en `producto.unidad_actual`, igual que
`cantidad_restante` y `producto.stock`, asi que el paso 8 no necesita conversion.

**Casos limite**

- `cantidadAAbrir > cantidad_restante` -> `DomainException(409)` antes de tocar nada.
- Linea vencida (`fecha_vencimiento < hoy`) -> `DomainException(409)` "vencido, debe
  descartarse" (R7). Sin flag `forzar`: abrir algo vencido no se justifica en una cocina.
- El operador elige una linea que no es la primera por FEFO -> se permite (la apertura es
  explicita), pero la respuesta incluye `advertenciaFefo` con la linea que vence antes,
  para que el frontend pueda avisarlo.
- Doble apertura concurrente -> la resuelve el bloqueo del paso 3.

---

### 3.2 `EjecutarProduccionUseCase` — **Nuevo**

El caso de uso central. Sustituye en la practica a `CrearProduccionUseCase` (B1).

`POST api/v1/produccion/ejecucion`

**Entradas**

| Campo | Tipo | Obligatorio | Validacion |
|---|---|---|---|
| `idReceta` | String (UUID) | si | existe y esta viva |
| `tandas` | number | si | > 0; multiplicador del rendimiento base |
| `cantidadReal` | number | si | >= 0; lo realmente obtenido |
| `motivoDiferencia` | String | condicional | obligatorio si `cantidadReal != cantidadEsperada` |
| `idUsuario` | String (UUID) | si | quien ejecuta |
| `fechaProduccion` | fecha | no | por defecto ahora |
| `fechaVencimiento` | fecha | **si** | `>= fechaProduccion`; vencimiento de la tanda, va al lote de cocina |
| `consumos` | lista de objetos | no | override manual (ver abajo) |

`fechaVencimiento` es obligatoria porque sin ella la tanda iria al final del orden FEFO
(2.4), justo lo contrario de lo que necesita un perecedero de cocina. Si se quiere evitar
que el operador la escriba cada vez, se usa la columna
`receta.dias_vida_util` (2.3): el frontend precarga `fechaProduccion + dias`; el backend sigue exigiendola.

**`cantidadEsperada` y `diferencia` NO se reciben.** Son derivados:
`cantidadEsperada = receta.cantidad * tandas`, `diferencia = cantidadReal - cantidadEsperada`.
Esto corrige la violacion de la prohibicion 28.

`consumos` es opcional y permite al operador elegir de que existencias descontar
(`[{ idExistencia, cantidad }]`). Si se omite, el sistema selecciona por **FEFO**
(*first expired, first out*): primero las de vencimiento mas proximo, luego las de apertura
mas antigua.

**Salidas** — `201 CREATED`

```json
{
  "id": "uuid-produccion",
  "cantidadEsperada": 20.0000,
  "cantidadReal": 18.5000,
  "diferencia": -1.5000,
  "motivoDiferencia": "Masa pegada al bol",
  "fechaProduccion": "2026-09-22T11:00:00",
  "receta": { "id": "uuid", "titulo": "Pan de masa madre" },
  "idUsuario": "uuid",
  "consumos": [
    { "idExistencia": "uuid", "idProducto": "uuid", "nombreProducto": "Harina 000",
      "cantidadConsumida": 2000.0000, "unidad": "g",
      "saldoResultante": 3000.0000, "idMovimiento": "uuid" }
  ],
  "productoObtenido": { "idProducto": "uuid", "nombre": "Pan de masa madre",
                        "stockResultante": 42.5000 },
  "loteCocina": { "idLote": "uuid", "idDetalleLote": "uuid", "cantidad": 18.5000,
                  "precioUnitario": 0.00, "fechaVencimiento": "2026-09-24" },
  "costoTotal": 46.25,
  "costoUnitario": 2.50
}
```

**Error por insumo insuficiente (R1)** — `409`, lanzado como `DomainException` y traducido
por el `GlobalExceptionHandler` existente:

```json
{
  "codigo": 409,
  "tipo": "Domain Exception",
  "mensaje": "Insumos insuficientes para producir 'Pan de masa madre' x2: 'Harina 000' requiere 2000.0000 g y hay 1500.0000 g abiertos (faltan 500.0000 g); 'Levadura' requiere 40.0000 g y hay 0.0000 g abiertos (faltan 40.0000 g)",
  "timestamp": "..."
}
```

El mensaje enumera **todos** los faltantes, no solo el primero, para que el operador abra
todo lo necesario de una vez. Si el frontend necesita los faltantes estructurados (para
ofrecer directamente los `detalle_lote` a abrir), los obtiene de
`GET api/v1/receta/{id}/disponibilidad` (3.9) en vez de parsear el mensaje: asi el contrato
de `ErrorResponse` no cambia.

**Logica de transaccion**

1. Cargar `Receta` con sus ingredientes vivos — 404 si no existe. Si es de
   `preparacion_inmediata` -> `DomainException(409)`: esas recetas se consumen al vender
   (R8, 3.12), no se producen por tanda.
2. Calcular el requerimiento por ingrediente: `ingrediente.cantidad * tandas`.
3. Por cada ingrediente, resolver existencias disponibles (estado "Abierta",
   `contenido_actual > 0`, **no vencidas**), ordenadas por FEFO (2.4).
4. **Convertir** cada `contenido_actual` a la unidad del producto via `IConversionRepository`
   + `ConvertidorDeUnidades`. Si falta el factor -> 400 nombrando ambas unidades.
5. **Verificar disponibilidad total ANTES de descontar nada (R1).** El caso de uso entrega
   requerimientos y existencias convertidas a una clase de dominio pura,
   `Produccion/Domain/PlanDeConsumo`, cuyo `armar(requerimientos, disponibles)` devuelve el
   reparto FEFO o lanza `DomainException.throwException(409, ...)` enumerando todos los
   faltantes. La regla vive en el dominio, no en el caso de uso, como exige la plantilla
   de agregado. Esto evita tambien quedarse sin insumo a mitad de receta dejando medio
   consumo aplicado.
6. Crear la `Produccion` (para tener su id disponible para los movimientos). Al terminar
   el paso 7 se fija `costo_total` = suma de cada consumo x `costo_contenido` (2.5).
7. Por cada existencia del plan: `existencia.consumir(cantidad)` (que a su vez lanza
   `DomainException` si excede `contenido_actual`, como segunda red), escribir un
   `Movimiento` `CONSUMO` con `id_produccion` = la tanda. Si el saldo llega a 0 la
   existencia **sigue abierta** hasta su cierre fisico (3.5).
8. **R4 — lote de cocina:** `Lote.crearDeProduccion(produccion.id, fechaProduccion,
   idUsuario, receta.id_producto_obtenido, cantidadReal, fechaVencimiento)`, con
   `precio_unitario = 0` y por tanto `total = 0` (2.3). Si `cantidadReal = 0` no se crea
   lote (una tanda fallida no deja una linea vacia) y la regla 5 de 2.1 admite esa
   excepcion.
9. **R4 — stock:** cargar el producto obtenido y llamar
   `producto.agregarStock(cantidadReal)`. Lote y stock suben juntos: regla 3 de 2.1.
10. **No** se toca `producto.stock` de ningun insumo: ese remanente ya se desconto al abrir
    (3.1). Tampoco se escribe `MERMA` por `diferencia < 0`: la desviacion ya queda
    registrada en la propia `produccion` (`diferencia` + `motivo_diferencia`).
11. Persistir `Produccion`, las `Existencia` y `Movimiento` tocados, el lote de cocina y
    el `Producto`.

**Atomicidad.** Todo bajo una `@Transactional`. El paso 5 es la salvaguarda funcional: la
verificacion previa hace que el rollback sea la excepcion y no el camino habitual, porque un
rollback a mitad de una tanda fisica ya ejecutada es confuso para el operador.

**Por que las existencias y no `producto.stock`.** La disponibilidad se mide contra lo
**abierto** (`existencias.contenido_actual`), no contra `producto.stock` del insumo. Un
insumo con mucho remanente cerrado pero nada abierto produce el error de R1, y la salida es
abrir (R2). Asi el consumo siempre sale de una existencia trazable a su `detalle_lote`.

**Casos limite**

- Receta sin ingredientes vivos -> `validarRegistrable()` ya lo cubre en el dominio.
- Un ingrediente cubierto por varias existencias parciales -> consumo repartido, un
  movimiento por existencia.
- `cantidadReal > cantidadEsperada` (excedente) -> valido; `diferencia` positiva y todo
  `cantidadReal` entra al lote de cocina y a `producto.stock`. Requiere `motivoDiferencia`.
- `consumos` manual que elige una existencia que no es la primera por FEFO -> se permite,
  pero nunca una vencida.
- Receta cuyo producto obtenido figura como ingrediente de otra receta -> contradice R4.
  Se recomienda que `Receta.validarRegistrable()` rechace ingredientes que sean
  `id_producto_obtenido` de alguna receta viva (validacion en el caso de uso de
  alta/edicion de receta, que es quien puede consultar las demas).
- Sin factor de conversion entre la unidad de la existencia y la del producto -> 400. No se
  asume 1:1 jamas.

---

### 3.3 `RegistrarVariacionDeProduccionUseCase` — **Nuevo**

Para el caso en que el rendimiento real se conoce despues de cerrar la tanda (fermentacion,
enfriado, control de calidad posterior).

`POST api/v1/produccion/{id}/variacion`

**Entradas:** `cantidadReal` (number, >= 0), `motivoDiferencia` (String, requerido),
`idUsuario`.

**Salidas:** `200 OK`, la `ProduccionResponse` con `diferencia` recalculada, el lote de
cocina actualizado y el `stockResultante`.

**Logica de transaccion:** recalcular `diferencia = cantidadReal - cantidadEsperada` y
aplicar el **delta respecto del `cantidadReal` anterior** (no respecto de cero, o se
contaria dos veces) a dos sitios a la vez:

- la linea del lote de cocina, via su raiz: `cantidad_inicial` y `cantidad_restante` +=
  delta (metodo de dominio `Lote.corregirRendimiento(delta)`, exclusivo de lotes de cocina);
- `producto.stock`: `agregarStock(delta)` o `descontarStock(-delta)`.

Todo en una transaccion.

**Casos limite**

- Aplicar la variacion dos veces: se resuelve ajustando siempre el delta y no el absoluto.
- Variacion negativa mayor que lo que queda en la linea de cocina (parte ya se vendio o se
  descarto): `DomainException(409)`; lo ya vendido no se puede "desproducir".
- Si lo que se quiere expresar es que unidades ya contadas se echaron a perder, el caso
  correcto es el descarte (3.10), no la variacion.

---

### 3.4 `AjustarExistenciaUseCase` — **Nuevo**

Ajuste manual con auditoria obligatoria. Reemplaza el uso de `ActualizarExistenciaUseCase`
para corregir saldos.

`POST api/v1/existencia/{id}/ajuste`

**Entradas**

| Campo | Tipo | Obligatorio | Validacion |
|---|---|---|---|
| `contenidoAjustado` | number | si | >= 0; <= `contenidoInicial` |
| `idTipoMovimiento` | String (UUID) | si | debe ser de ajuste (`MERMA`, `AJUSTE`, `DEVOLUCION`) |
| `motivo` | String | **si** | `textoRequerido`; un ajuste sin motivo no es auditable |
| `idUsuario` | String (UUID) | si | quien ajusta |

**Salidas:** `200 OK` con la existencia actualizada y el movimiento generado.

**Logica de transaccion:** calcular `delta = contenidoAjustado - contenidoActual`, escribir
el movimiento por ese delta con su signo y aplicar el nuevo saldo. No cierra la existencia:
el cierre es siempre fisico (3.5). Una transaccion. **Nunca** se permite ajustar sin movimiento.

No toca `producto.stock`: el ajuste corrige lo **abierto**, y el stock mide lo **cerrado**
en lotes (R3). Lo mismo vale para 3.5.

**Autorizacion:** `@RequiereRol("Administrador")`. Es la operacion que mas se presta a
encubrir faltantes.

---

### 3.5 `CerrarExistenciaUseCase` — **Nuevo**

`POST api/v1/existencia/{id}/cierre`

**Entradas:** `motivo` (String, opcional), `idUsuario`.

**Salidas:** `200 OK` con la existencia en estado "Agotada" y `fecha_acabado` fijada.

**Logica:** si queda saldo residual, se escribe un movimiento `MERMA` por el residuo exacto
antes de cerrar, de modo que el kardex cuadre a cero. Idempotente: cerrar una existencia ya
cerrada devuelve 200 sin escribir un segundo movimiento.

**Es la unica forma de cerrar una existencia.** Ningun consumo la cierra al llegar a 0: el
saldo es teorico (recetas con variacion real, R8) y solo quien tiene el envase en la mano
sabe si se termino. `GET api/v1/existencia/abiertas` destaca las existencias con saldo 0
para recordar cerrarlas. Motivos sugeridos en el frontend: "se termino" (residuo = diferencia
de consumo), "vencio", "se echo a perder".

---

### 3.6 `CrearMovimientoUseCase` — **Modificacion**

Hoy escribe la fila sin mover el saldo (B3).

**Cambios**

- Inyectar `IExistenciaRepository` (ya lo hace, solo para validar) y **aplicar el efecto**:
  `contenido_actual += cantidad * tipoMovimiento.signo`.
- Rechazar con 400 si el resultado quedaria negativo o por encima de `contenido_inicial`.
- No cerrar la existencia al llegar a 0: el cierre es fisico (3.5).
- `TipoMovimiento` gana el campo `signo` en agregado, DTOs y `JpaModel`.

**Entradas:** las actuales (`cantidad`, `idExistencia`, `idTipoMovimiento`, `idUsuario`,
`motivo`, `fechaMovimiento`) mas, opcionalmente, `idProduccion`.

**Salidas:** `MovimientoResponse` mas `saldoResultante` de la existencia afectada.

**Logica de transaccion:** movimiento y saldo se escriben juntos o no se escribe ninguno.

---

### 3.7 `CrearExistenciaUseCase` — **Modificacion (o retiro)**

Hoy permite crear stock abierto de la nada (B2). Dos opciones:

- **Decidido:** retirar el `POST api/v1/existencia` publico y dejar que toda existencia
  nazca por 3.1 o por la carga inicial (3.15), que cubre el caso de insumos ya abiertos al
  arrancar el sistema. El agregado conserva sus cinco casos de uso internos, pero el alta
  libre deja de exponerse.

En ambos casos `ActualizarExistenciaUseCase` deja de aceptar `contenidoActual`: ese campo
solo se mueve por 3.4 o 3.6.

---

### 3.8 `CrearProduccionUseCase` — **Modificacion**

- Dejar de recibir `cantidadEsperada` y `diferencia`; derivarlos de la receta y `tandas`.
- Exigir `idUsuario`.
- Exigir `motivoDiferencia` cuando hay desviacion.
- Queda como registro historico sin consumo; la operacion real es 3.2.

---

### 3.9 Consultas para el frontend — **Nuevos**

Todas `@Transactional(readOnly = true)` y sin N+1: resuelven sus referencias en una consulta
por pagina, como ya hacen `LoteDatasourceImpl` y `RecetaDatasourceImpl`.

**`ObtenerInsumosAbiertosUseCase`** — `GET api/v1/existencia/abiertas`

Dashboard de insumos abiertos. Filtros: `idProducto`, `idEstadoExistencia`, `porVencer`
(dias), `soloConSaldo`. Devuelve existencia + producto + unidad + saldo + dias para vencer
(con la vida util abierta de 2.6), ordenado por FEFO y separado en **vigentes** y
**vencidas** (estas con accion directa a 3.5). Requiere `findByBorradoFalseAndIdEstadoExistencia...` y un filtro por
producto a traves de `detalle_lote`.

**`ObtenerKardexDeExistenciaUseCase`** — `GET api/v1/existencia/{id}/kardex`

Historial completo de una existencia: apertura, consumos (con su produccion), ajustes,
mermas y cierre, con saldo acumulado tras cada movimiento. Requiere
`findByIdExistenciaAndBorradoFalseOrderByFechaMovimientoAsc`.

**`ObtenerHistorialDeMermasUseCase`** — `GET api/v1/movimiento/mermas`

Filtros por rango de fechas, producto y usuario. Agregados por producto y por periodo. Es la
vista que justifica haber puesto `id_usuario` en `movimientos`.

**`ObtenerDisponibilidadDeRecetaUseCase`** — `GET api/v1/receta/{id}/disponibilidad?tandas=N`

Pre-chequeo antes de producir y **fuente estructurada para resolver el error de R1**. Por
cada ingrediente: requerido, disponible abierto (sumando existencias convertidas), faltante,
`suficiente` booleano, remanente cerrado (`producto.stock`) y, si falta, la lista de
`detalle_lote` vivos **de compra** del insumo con `cantidad_restante > 0` y **no vencidos**,
ordenada por FEFO (`lotesParaAbrir`, calculada con `AsignadorFefo`). El primero de la lista
es el que el frontend debe proponer.

```json
{
  "idReceta": "uuid", "tandas": 2, "suficiente": false,
  "ingredientes": [
    { "idProducto": "uuid", "nombre": "Harina 000", "unidad": "g",
      "requerido": 2000.0000, "disponibleAbierto": 1500.0000, "faltante": 500.0000,
      "suficiente": false, "remanenteCerrado": 24.0000,
      "lotesParaAbrir": [
        { "idDetalleLote": "uuid", "cantidadRestante": 10.0000,
          "fechaVencimiento": "2027-01-31" }
      ] }
  ]
}
```

Con esto el frontend deshabilita el boton de producir o, tras recibir el `409`, ofrece
abrir directamente el `detalle_lote` sugerido con 3.1 (R2). Si `remanenteCerrado` tambien
es 0, el mensaje al operador es "hay que comprar", no "hay que abrir".

**`ObtenerDescartesUseCase`** — `GET api/v1/producto/descartes`

Historial de descartes (3.10) con filtros por rango de fechas, producto, origen (compra o
cocina), produccion y usuario. Complementa a mermas: mermas es perdida de insumo abierto,
descartes es perdida de stock cerrado (terminado o comprado).

**`ObtenerLotesPorVencerUseCase`** — `GET api/v1/lote/por-vencer?dias=N`

La vista operativa de R7. Lineas de lote con `cantidad_restante > 0` en dos grupos:
**vencidas** (pendientes de descarte, con boton directo a 3.10) y **por vencer** en los
proximos `N` dias, ordenadas por FEFO. Filtros: `idProducto`, `origen` (compra / cocina),
`vendible`. Es lo que permite al negocio vender, promocionar o usar primero lo proximo a
vencer antes de que se convierta en perdida.

**`ObtenerReporteDePerdidasUseCase`** — `GET api/v1/inventario/perdidas?desde&hasta`

Perdidas **valoradas en Bs** segun 2.5: descartes (stock cerrado) + mermas (abierto),
agrupadas por producto, grupo (`idProductoPadre`), origen, motivo y usuario, con totales
por periodo. Es la metrica objetivo de R7: si FEFO funciona, esta cifra baja.

**`ObtenerProductosBajoMinimoUseCase`** — `GET api/v1/producto/bajo-minimo`

Productos cuyo **disponible** esta por debajo de `stock_minimo`:

- insumo: `producto.stock` (cerrado) + abierto vigente expresado en unidad del producto
  (`contenido_actual * cantidad_abierta / contenido_inicial` por existencia, sin tabla de
  conversiones), porque lo abierto tambien se puede usar;
- terminado o reventa: `stock - stockVencido`;
- preparacion inmediata: no aplica (no tiene stock); se vigilan sus insumos.

Devuelve disponible, minimo y faltante. Es la lista de compras / produccion del dia.
`ProductoResponse` gana `disponible` y `bajoMinimo` derivados con el mismo calculo.

**`ObtenerCatalogoDeVentaUseCase`** — `GET api/v1/producto/catalogo-venta`

Lo que el punto de venta necesita en una sola llamada. El frontend de ventas deja de leer
`producto.stock` crudo y lee esta vista. Para cada producto `vendible = true`:

**Disponible de un insumo**, en la unidad del producto (misma cifra que `bajo-minimo`):

```
disponible(insumo) = stock cerrado vigente (producto.stock - stockVencido)
                   + sum( contenido_actual * cantidad_abierta / contenido_inicial )
                     de sus existencias abiertas vigentes
```

**Unidades que salen de una receta** (el minimo manda: el insumo que se acaba primero):

```
unidades(receta) = min sobre ingredientes de
                   floor( disponible(insumo) / (ingrediente.cantidad / receta.cantidad) )
```

| Tipo de producto | Campos | Significado |
|---|---|---|
| Preparacion inmediata (cafe y derivados) | `unidadesMaximas`, `insumoLimitante` | Cuantos se pueden preparar con todo lo que hay en almacen; que insumo reabastecer |
| Con receta por tanda (sandwich, pan) | `vendible`, `producibles`, `insumoLimitante` | `vendible` = ya hecho y vigente; `producibles` = cuantos mas se podrian hacer con los insumos |
| Comprado para reventa | `vendible` | `stock - stockVencido` |

```json
[
  { "idProducto": "uuid", "nombre": "Latte", "tipo": "PREPARACION_INMEDIATA",
    "precio": 18.00, "unidadesMaximas": 42,
    "insumoLimitante": { "idProducto": "uuid", "nombre": "Leche entera", "disponible": 8.4000, "unidad": "L" } },
  { "idProducto": "uuid", "nombre": "Sandwich de pollo", "tipo": "TANDA",
    "precio": 25.00, "vendible": 3, "producibles": 12,
    "insumoLimitante": { "idProducto": "uuid", "nombre": "Pan de molde", "disponible": 24.0000, "unidad": "u" } },
  { "idProducto": "uuid", "nombre": "Agua 500 ml", "tipo": "REVENTA",
    "precio": 6.00, "vendible": 30 }
]
```

`unidadesMaximas` es **orientativo**: se calcula con la receta teorica (40 g) y el consumo
real varia. No bloquea ventas (R8); su funcion es que el personal vea caer la cifra y
reabastezca antes de quedarse sin insumo. Un mismo insumo (el cafe) limita a todos sus
derivados a la vez, y cada uno muestra su propia cifra segun lo que pide su receta.

Como el cafe cerrado cuenta en `unidadesMaximas`, la cifra no cae a 0 al terminarse la bolsa
abierta si quedan bolsas en almacen: el barista abre la siguiente (3.1) cuando fisicamente
la necesita.

**`ObtenerRequerimientosDePedidosUseCase`** — `GET api/v1/pedido/requerimientos?fecha=`

Suma por producto de los pedidos pendientes de despacho con entrega en esa fecha, contra el
vendible actual: cuanto hay que producir para cumplirlos. Es el plan de cocina de R11.

**`ObtenerTrazabilidadDeLoteUseCase`** — `GET api/v1/lote/{id}/trazabilidad`

De la compra a la merma: detalle de lote -> existencias abiertas -> movimientos ->
producciones -> lote de cocina resultante -> ventas y descartes de esa tanda. Es la
consulta de auditoria completa, y gracias al lote de cocina recorre la cadena entera: de un
sandwich vendido se llega al lote de harina con que se hizo.

---

### 3.10 `DescartarProductoUseCase` — **Nuevo** (R5)

Baja de stock cerrado que ya no puede venderse ni usarse. Ejemplo: se producen 5
sandwiches (lote de cocina de 5), se venden 2, y los 3 restantes vencen al final del dia.
Con el lote de cocina, el descarte ya no se limita a terminados: sirve igual para una
botella de reventa o un insumo cerrado que vencio, porque todo producto tiene lotes.

`POST api/v1/producto/{id}/descarte`

Vive en un agregado nuevo `Descarte` (tabla `descarte`, seccion 2.3) con sus cinco casos de
uso estandar mas este verbo. Se separa de `Producto` y de `Lote` porque cada descarte es un
hecho con autoria y motivo propios, igual que un `Movimiento`.

**Entradas**

| Campo | Tipo | Obligatorio | Validacion |
|---|---|---|---|
| `cantidad` | number | si | `numeroRequerido`; > 0 |
| `motivo` | String | **si** | `textoRequerido`; ej. "Vencido fin de jornada" |
| `idUsuario` | String (UUID) | si | quien descarta |
| `idDetalleLote` | String (UUID) | no | linea concreta (p. ej. "la tanda de esta manana"); si se omite, FEFO |
| `fechaDescarte` | fecha | no | por defecto ahora |

**Salidas** — `201 CREATED`

```json
{
  "producto": { "id": "uuid", "nombre": "Sandwich de pollo", "stockResultante": 0.0000 },
  "descartes": [
    { "id": "uuid-descarte", "idDetalleLote": "uuid", "origen": "COCINA",
      "idProduccion": "uuid", "cantidad": 3.0000, "lineaFinalizada": true }
  ],
  "motivo": "Vencido fin de jornada",
  "fechaDescarte": "2026-09-22T20:00:00",
  "idUsuario": "uuid"
}
```

**Logica de transaccion**

1. Cargar el `Producto` — 404 si no existe.
2. Resolver las lineas: si viene `idDetalleLote`, esa sola (debe ser del producto y tener
   saldo suficiente); si no, `AsignadorFefo` sobre las lineas con saldo **incluyendo las
   vencidas**, que son justamente las primeras que deben salir. Bloqueo pesimista sobre
   las lineas elegidas.
3. Por cada linea: `lote.ajustarCantidadRestante(idDetalle, restante - cantidad)` via su
   raiz, y `Descarte.crear(idDetalleLote, cantidad, motivo, idUsuario, fecha)`.
4. `producto.descontarStock(cantidad)` — `DomainException(409)` si excede el stock.
5. Persistir `Lote`(s), `Descarte`(s) y `Producto`.

**Atomicidad:** linea de lote, stock y registro de descarte se escriben juntos o ninguno.

**Lo abierto no se descarta aqui.** Una existencia abierta que se echa a perder sigue su
circuito propio: `MERMA` via 3.4/3.5. El descarte solo toca stock cerrado en lotes.

**Autorizacion:** `@RequiereRol` de operador; el historial (3.9) para administrador.

---

### 3.11 Modificaciones de CRUD que exigen R3/R4 — **Modificacion**

- **`CrearLoteUseCase` / `ActualizarLoteUseCase`:** hoy no tocan `producto.stock`. Al dar
  de alta una linea de compra, `producto.agregarStock(cantidad_inicial)`; al corregir
  `cantidad_inicial` de una linea aun no abierta, ajustar por el delta; al borrar una linea
  sin aperturas, `descontarStock(cantidad_restante)`. Sin esto la regla 3 no se cumple desde
  el primer lote. Ademas: `CrearLoteUseCase` exige `idProveedor` y nunca acepta
  `idProduccion`; `ActualizarLoteUseCase` y `EliminarLoteUseCase` rechazan lotes de cocina
  con `DomainException(409)` (2.3).
- **`LoteResponse`:** gana `origen` (`COMPRA` / `COCINA`), `idProduccion` y, por linea,
  `finalizado` y `vencido` derivados.
- **`CrearProductoUseCase` / `ActualizarProductoUseCase`:** dejan de aceptar `stock`. Un
  producto nace con `stock = 0` y su stock solo cambia por compra, apertura, produccion,
  venta o descarte. `Producto.actualizar(...)` pierde el parametro `stock`, y el agregado
  gana `agregarStock(BigDecimal)` y `descontarStock(BigDecimal)` con la validacion de no
  negativo como `DomainException`.
- **Ventas:** ver 3.12.

---

### 3.12 `CrearVentaUseCase` — **Modificacion** (R6)

Hoy `CrearVentaUseCase` (contexto `Ventas`) solo valida que cada producto exista via
`IProductoRepository`; no descuenta nada. Ya compone agregados de `Inventario` en el caso de
uso, asi que inyectar ademas `ILoteRepository` sigue el mismo precedente (seccion 4.3 de
`arquitectura.md`).

`POST api/v1/venta` — mismas entradas y salidas que hoy; la respuesta de cada linea gana
`lotes: [{ idDetalleLote, cantidad }]` y `stockResultante`.

**Logica de transaccion**, por cada linea de venta:

0. **Si la venta tiene pedido con entrega futura (R11):** no se ejecuta nada de lo que
   sigue; la venta se guarda con `stock_descontado = false` y el descuento ocurre en el
   despacho (3.17). Si no hay pedido o se entrega en el acto, se sigue normalmente.
1. Cargar el `Producto`. Si es producto obtenido de una receta de
   **preparacion inmediata (R8)**, ir a la rama de abajo en lugar de los pasos 2-6.
2. Cargar sus `detalle_lote` vivos con `cantidad_restante > 0` y **no vencidos** —de compra,
   cocina o inicial, da igual— con **bloqueo pesimista**, como en 3.1.
3. `AsignadorFefo` (2.4) reparte la cantidad: toma de la linea que vence antes hasta
   agotarla y sigue con la siguiente. Si lo vigente no alcanza, `DomainException(409)`:
   "Stock insuficiente de 'Sandwich de pollo': se piden 4, hay 2 vigentes y 3 vencidos
   pendientes de descarte".
4. Por cada linea tocada: `lote.ajustarCantidadRestante(idDetalle, restante - asignado)` a
   traves de su raiz (5.13). Una linea que llega a 0 queda **finalizada**.
5. Registrar cada asignacion como hija de la linea de venta en `detalle_venta_lote`.
6. `producto.descontarStock(cantidad)`. Nunca falla si el paso 3 aprobo; si falla, delata
   una divergencia de la regla 3 y no debe silenciarse.
7. Fijar `detalle_venta.costo` (2.5) y persistir `Venta` (con sus asignaciones), los
   `Lote` tocados y los `Producto`.

**Rama de preparacion inmediata (R8)** — un latte vendido descuenta leche y cafe
**teoricos**, sin bloquear nunca:

1. Requerimiento teorico por ingrediente = `ingrediente.cantidad * cantidadVendida /
   receta.cantidad`.
2. Recorrer las existencias abiertas vigentes del ingrediente por FEFO y descontar hasta
   cubrir el requerimiento **o hasta agotar su saldo teorico**, lo que ocurra antes. No se
   usa el modo estricto de `PlanDeConsumo` (ese es para tandas, R1): aqui **no hay 409 por
   insumo insuficiente**. El barista tiene el cafe en la mano; el sistema no lo contradice.
3. Por cada existencia tocada: `consumir`, `Movimiento` `CONSUMO` con
   `id_detalle_venta` = la linea (no `id_produccion`). La existencia **no se cierra** al
   llegar a 0 (ver "cierre fisico" abajo).
4. Si una parte del requerimiento no se pudo descontar (saldo teorico en 0), la venta se
   registra igual y la respuesta incluye `advertencias`, p. ej. *"Cafe en grano: el saldo
   teorico se agoto. Si la bolsa se termino, cierrala y registra la apertura de la
   siguiente"*. Esa parte no descontada simplemente no se registra: el conteo o el cierre
   de la siguiente bolsa absorben la diferencia.
5. `detalle_venta.costo` = suma de lo efectivamente descontado x `costo_contenido`. No hay
   lote ni `producto.stock` que tocar.

**Cierre fisico y variacion de receta.** Como cada cafe usa 35-45 g y el sistema descuenta
40, el saldo teorico se separa del real. Se resuelve sin campos nuevos:

- **Se uso mas que la receta:** la bolsa se vacia con saldo teorico positivo. El barista la
  cierra (3.5) y el residuo sale como movimiento `MERMA` con motivo "diferencia de
  consumo". El kardex cuadra a cero.
- **Se uso menos que la receta:** el saldo teorico llega a 0 con cafe en la bolsa. La
  existencia sigue abierta, las ventas continuan (paso 4, con advertencia) y cuando la
  bolsa realmente se termina el barista la cierra sin residuo.
- En ambos casos, el indicador de **unidades maximas** del catalogo (3.9) baja con cada
  venta y avisa a tiempo que hay que reabastecer; y el conteo fisico (3.13) corrige
  cualquier deriva acumulada.

Reglas de la receta de preparacion inmediata: una sola receta viva de este tipo por
producto, y un producto no puede tener a la vez recetas por tanda e inmediatas (lo valida
el caso de uso de alta/edicion de receta).

**Atomicidad.** Venta, stock y lotes se escriben juntos o ninguno. Una linea sin stock
anula la venta completa.

**FEFO, no FIFO puro.** Ver la decision en 2.4. La tabla `detalle_venta_lote` es
independiente del criterio: si algun dia cambia el orden, solo cambia `AsignadorFefo`.

**Lote finalizado.** No hace falta columna nueva: una linea esta finalizada cuando
`cantidad_restante = 0`, y un `lote` cuando todas sus lineas lo estan. Las respuestas de
`Lote` exponen ambos como booleanos derivados (`finalizado`), sin aceptarlos del cliente
(prohibicion 28).

**Anulacion y baja de una venta.** Ver 3.19.

**Consulta asociada** — `GET api/v1/lote/{id}/ventas`: que ventas consumieron cada linea
del lote. Para un lote de cocina responde "de esta tanda se vendieron N y se descartaron M".

---

### 3.13 `ConteoFisico` — **Nuevo agregado** (R12)

Toma de inventario periodica. Raiz `ConteoFisico` con hijas `DetalleConteo` (tablas
`conteo_fisico`, `detalle_conteo`), plantilla de agregado compuesto como `Lote`. Cubre
stock cerrado (lineas de lote) y abierto (existencias) en un solo recorrido.

**Flujo en tres pasos**

1. `POST api/v1/conteo-fisico` — crea el conteo en `BORRADOR`. Filtros opcionales
   (`idProducto`, `idProductoPadre`, `idCategoria`, `incluirAbiertos`) para contar por
   zonas. El servidor precarga una linea por cada `detalle_lote` con saldo y, si se pide,
   por cada existencia abierta, con `cantidad_sistema` = saldo **en ese momento**.
2. `PUT api/v1/conteo-fisico/{id}` — el operador carga `cantidad_contada` (y `motivo` si
   difiere) linea por linea; se puede guardar parcial las veces que haga falta. Solo en
   `BORRADOR`.
3. `POST api/v1/conteo-fisico/{id}/aplicacion` — ver 3.14.

**Salidas** de lectura: cada linea con `cantidadSistema`, `cantidadContada`, `diferencia`
(derivada, nunca recibida) y su valoracion en Bs (2.5), mas totales del conteo.

**Autorizacion:** crear y cargar, operador; aplicar, administrador.

---

### 3.14 `AplicarConteoFisicoUseCase` — **Nuevo** (R12)

`POST api/v1/conteo-fisico/{id}/aplicacion` — **Entradas:** `idUsuario`. **Salidas:**
`200 OK` con el conteo `APLICADO` y los ajustes generados.

**Logica de transaccion**

1. Cargar el conteo; si no esta en `BORRADOR` -> `DomainException(409)`. Toda linea con
   diferencia debe tener `motivo` -> si no, 400.
2. Por cada linea con `diferencia = cantidad_contada - cantidad_sistema` distinta de 0:
   - **linea de lote:** aplicar el **delta** (no el absoluto) sobre el saldo actual. Asi
     las ventas ocurridas entre el conteo y la aplicacion no se pierden. Via raiz `Lote`;
     `producto.agregarStock` / `descontarStock`; fila `ajuste_lote` tipo `CONTEO` con
     `id_conteo_fisico`. Si el delta dejaria la linea negativa -> 409 nombrando la linea.
   - **existencia:** movimiento `AJUSTE` por el delta con el motivo de la linea (misma
     logica que 3.4).
3. Marcar `APLICADO`, `id_usuario_aplica`, `fecha_aplicacion`.

**Atomicidad:** todo el conteo se aplica o nada. Un conteo aplicado es inmutable; para
corregirlo se hace otro conteo.

---

### 3.15 `RegistrarInventarioInicialUseCase` — **Nuevo** (R12)

Arranque del sistema con mercaderia que ya esta en el local y no tiene compra registrada.

`POST api/v1/lote/inventario-inicial` — `@RequiereRol("Administrador")`

**Entradas**

| Campo | Tipo | Obligatorio | Validacion |
|---|---|---|---|
| `idUsuario` | String (UUID) | si | quien carga |
| `fecha` | fecha | no | por defecto ahora |
| `lineas` | lista | si | al menos una |
| `lineas[].idProducto` | String (UUID) | si | producto vivo, no de preparacion inmediata |
| `lineas[].cantidadCerrada` | number | si | >= 0; stock sin abrir |
| `lineas[].costoUnitario` | number | si | >= 0; costo estimado, para valorar perdidas y margen |
| `lineas[].fechaVencimiento` | fecha | no | vencimiento de lo cargado |
| `lineas[].abierto` | objeto | no | `{ cantidadAbierta, contenidoActual, contenidoInicial, idUnidad, fechaApertura }` si hay uno ya abierto |

**Logica de transaccion:** crea **un** lote `origen = 'INICIAL'` con una linea por producto
(`cantidad_inicial` = cerrada + abierta, `cantidad_restante` = cerrada,
`precio_unitario` = costo estimado). Suma `producto.stock` por lo cerrado. Por cada
`abierto`, crea la existencia (con `cantidad_abierta`, vencimiento de 2.6) y su movimiento
`APERTURA`. Todo en una transaccion.

**Caso limite:** puede usarse mas de una vez (carga por zonas), pero si el producto ya
tiene movimientos de venta o produccion, responde 409 sugiriendo un conteo (3.13): la carga
inicial es para arrancar, no para corregir.

---

### 3.16 `DevolverAProveedorUseCase` — **Nuevo** (R12)

`POST api/v1/lote/{id}/devolucion`

**Entradas:** `lineas: [{ idDetalleLote, cantidad }]`, `motivo` (requerido), `idUsuario`.

**Logica de transaccion:** solo lotes `COMPRA` (cocina e inicial -> 409). Por linea: saldo
suficiente y **no abierto** en esa cantidad (lo abierto no se devuelve), descuento via raiz
`Lote`, `producto.descontarStock`, fila `ajuste_lote` tipo `DEVOLUCION_PROVEEDOR` con
cantidad negativa. `lote.total` **no cambia** (es el historico de la compra); la respuesta
incluye `montoADevolver` = cantidad x `precio_unitario` como referencia para la nota de
credito. No cuenta como perdida en el reporte de 3.9.

---

### 3.17 `DespacharPedidoUseCase` — **Nuevo** (R11)

Contexto `Pedidos`. Compone `IVentaRepository`, `ILoteRepository`, `IProductoRepository` y
`IExistenciaRepository` en el caso de uso, como ya hace `CrearVentaUseCase` con Inventario.

`POST api/v1/pedido/{id}/despacho` — **Entradas:** `idUsuario`. **Salidas:** `200 OK` con
el pedido despachado y, por linea de venta, sus asignaciones de lote o consumos.

**Logica de transaccion**

1. Cargar pedido y venta; si `venta.stock_descontado = true` -> 409 (ya despachado).
2. Ejecutar, por cada linea de la venta, **exactamente** los pasos 1-7 de 3.12 (o la rama
   de preparacion inmediata), con `fechaReferencia = pedidos.fecha_entrega` para FEFO:
   no se asigna una linea que vence antes de la entrega.
3. Si algo no alcanza: `DomainException(409)` con los faltantes. El frontend deriva a
   producir (o abrir) y reintenta; `GET api/v1/pedido/requerimientos` anticipa este caso.
4. `venta.stock_descontado = true`, `pedidos.fecha_despacho`, `id_usuario_despacho` y
   estado del pedido a "Despachado".

**Anular un pedido no despachado:** no hay stock que reponer; solo se anula la venta.

---

### 3.18 `AnularProduccionUseCase` — **Nuevo** (R13)

Para producciones **registradas por error** (receta equivocada, cantidad mal tipeada). No
es para comida que se hizo y se echo a perder: eso es descarte (3.10).

`POST api/v1/produccion/{id}/anulacion` — **Entradas:** `motivo` (requerido), `idUsuario`.
`@RequiereRol("Administrador")`.

**Logica de transaccion**

1. Si ya esta anulada -> 200 sin cambios (idempotente).
2. La linea del lote de cocina debe estar **intacta** (`cantidad_restante =
   cantidad_inicial`): si algo se vendio o descarto -> 409 "no se puede anular: ya salieron
   N unidades".
3. Por cada `CONSUMO` de la tanda: movimiento `DEVOLUCION` por la misma cantidad a la
   misma existencia; si estaba `Agotada`, vuelve a `Abierta` y se limpia `fecha_acabado`.
   Si la existencia ya se cerro con merma o vencio, la devolucion se registra igual y la
   existencia queda para cierre (3.5).
4. Lote de cocina: `ajuste_lote` tipo `ANULACION_PRODUCCION` por `-cantidad`, linea a 0 y
   baja logica del lote; `producto.descontarStock(cantidad_real)`.
5. `produccion.anulada = true`, `motivo_anulacion`.

---

### 3.19 `AnularVentaUseCase` — **Nuevo** (R13)

Sustituye la reposicion que antes se describia dentro de `EliminarVentaUseCase`.

`POST api/v1/venta/{id}/anulacion` — **Entradas:** `motivo`, `idUsuario`.

**Logica de transaccion**

- **Venta con stock descontado, linea con lotes:** recorrer `detalle_venta_lote` y devolver
  cada cantidad **a la misma linea de lote**, mas `producto.agregarStock`. Si esa linea ya
  vencio, la cantidad vuelve igual y aparece en "vencidas" para descarte: es lo correcto,
  el producto devuelto no rejuvenece.
- **Linea de preparacion inmediata:** lo preparado no vuelve al insumo. Si la anulacion es
  antes de preparar (pedido no despachado) no hay nada que revertir; si es despues, los
  consumos se quedan y el costo aparece como perdida con motivo "venta anulada".
- **Venta con pedido no despachado:** solo cambia el estado; no hubo stock.
- Estado de la venta a "Anulada". `EliminarVentaUseCase` pasa a exigir que la venta este
  anulada antes de la baja logica. `ActualizarVentaUseCase` no permite cambiar cantidades
  de lineas con stock descontado; se anula y se crea otra venta.

---

### 3.20 Reloj del negocio — **Modificacion transversal** (R14)

- Crear `IReloj` + `RelojDelNegocio` (2.7) y la propiedad `app.zona-horaria`.
- Sustituir cada `LocalDate.now()` / `LocalDateTime.now()` de casos de uso por
  `reloj.hoy()` / `reloj.ahora()` (hoy son 4 archivos).
- `Validador`: reemplazar `ZoneId.systemDefault()` por la zona configurada.
- Todo `fechaX` "por defecto ahora" de este documento significa `reloj.ahora()`.

---

## 4. Requerimientos y Recomendaciones para Frontend

### 4.1 Endpoints

Todos REST bajo `api/v1/`, coherentes con la seccion 5.6 de `arquitectura.md`. El proyecto
no usa GraphQL ni deberia introducirlo (prohibicion 24 sobre anadir infraestructura).

| Metodo | Ruta | Caso de uso | Rol sugerido |
|---|---|---|---|
| `POST` | `api/v1/existencia/apertura` | 3.1 Abrir insumo | Operador |
| `POST` | `api/v1/existencia/{id}/ajuste` | 3.4 Ajustar | Administrador |
| `POST` | `api/v1/existencia/{id}/cierre` | 3.5 Cerrar | Operador |
| `GET` | `api/v1/existencia/abiertas` | 3.9 Dashboard | Operador |
| `GET` | `api/v1/existencia/{id}/kardex` | 3.9 Kardex | Operador |
| `POST` | `api/v1/produccion/ejecucion` | 3.2 Ejecutar | Operador |
| `POST` | `api/v1/produccion/{id}/variacion` | 3.3 Variacion | Operador |
| `GET` | `api/v1/receta/{id}/disponibilidad` | 3.9 Pre-chequeo | Operador |
| `GET` | `api/v1/movimiento/mermas` | 3.9 Mermas | Administrador |
| `GET` | `api/v1/lote/{id}/trazabilidad` | 3.9 Trazabilidad | Administrador |
| `POST` | `api/v1/producto/{id}/descarte` | 3.10 Descartar stock cerrado | Operador |
| `GET` | `api/v1/producto/descartes` | 3.9 Historial de descartes | Administrador |
| `GET` | `api/v1/lote/por-vencer` | 3.9 Vencidos y por vencer | Operador |
| `POST` | `api/v1/venta` | 3.12 Venta con descuento FEFO (modificado) | Operador |
| `GET` | `api/v1/lote/{id}/ventas` | 3.12 Ventas por lote | Administrador |
| `POST` | `api/v1/venta/{id}/anulacion` | 3.19 Anular venta | Administrador |
| `GET` | `api/v1/producto/catalogo-venta` | 3.9 Catalogo del punto de venta | Operador |
| `GET` | `api/v1/producto/bajo-minimo` | 3.9 Alertas de minimo | Operador |
| `GET` | `api/v1/inventario/perdidas` | 3.9 Perdidas valoradas | Administrador |
| `POST` | `api/v1/produccion/{id}/anulacion` | 3.18 Anular produccion | Administrador |
| `POST` | `api/v1/pedido/{id}/despacho` | 3.17 Despachar pedido | Operador |
| `GET` | `api/v1/pedido/requerimientos` | 3.9 Plan de cocina por pedidos | Operador |
| `POST` | `api/v1/conteo-fisico` | 3.13 Iniciar conteo | Operador |
| `PUT` | `api/v1/conteo-fisico/{id}` | 3.13 Cargar cantidades | Operador |
| `GET` | `api/v1/conteo-fisico/{id}` | 3.13 Ver conteo y diferencias | Operador |
| `POST` | `api/v1/conteo-fisico/{id}/aplicacion` | 3.14 Aplicar conteo | Administrador |
| `POST` | `api/v1/lote/inventario-inicial` | 3.15 Carga inicial | Administrador |
| `POST` | `api/v1/lote/{id}/devolucion` | 3.16 Devolucion a proveedor | Administrador |

Los codigos siguen el estandar del proyecto: `201` en alta, `200` en lectura y modificacion,
`204` en baja logica. Se anade `409 Conflict` para stock insuficiente y colision de
concurrencia, que hoy no se usa en el proyecto y conviene documentar en `arquitectura.md`
seccion 5.9 junto al reparto actual de codigos.

### 4.2 Validaciones recomendadas en cliente

Son **conveniencia de UX, nunca sustituto** de la validacion del servidor: el backend valida
igual en `createFromRequest` y en el dominio.

- **Antes de abrir:** mostrar `cantidad_restante` del detalle de lote y bloquear el envio si
  la cantidad supera el saldo. No ofrecer lineas vencidas. Preseleccionar la primera por
  FEFO y mostrar `advertenciaFefo` si el operador elige otra.
- **Al producir:** exigir `fechaVencimiento` de la tanda (precargada con
  `receta.dias_vida_util`).
- **Al vender:** leer `GET api/v1/producto/catalogo-venta`, no `producto.stock` crudo.
  Mostrar `vendible` (y `producibles`) para productos con stock y `unidadesMaximas` para
  cafe y derivados, con el `insumoLimitante`. Resaltar cuando `unidadesMaximas` cae por
  debajo de un umbral (p. ej. 20) para que se reabastezca a tiempo. Si hay vencidos, avisar
  para que se descarten.
- **Venta de preparacion inmediata:** nunca se bloquea. Si la respuesta trae
  `advertencias`, mostrarlas como aviso no intrusivo con accion directa a "cerrar bolsa" y
  "abrir siguiente" (3.5 / 3.1).
- **Al crear una venta con pedido futuro:** avisar que el stock se descontara al despachar
  y mostrar si hoy alcanza (consultando `pedido/requerimientos`), sin bloquear.
- **Al contar:** permitir guardar parcial, resaltar lineas con diferencia y exigir motivo
  en ellas antes de enviar a aplicar; mostrar el impacto en Bs.
- **Antes de producir:** llamar a `GET api/v1/receta/{id}/disponibilidad?tandas=N` al cambiar
  el numero de tandas y deshabilitar el boton mientras algun ingrediente sea insuficiente,
  mostrando cuanto falta y de que.
- **Si producir devuelve `409` (R1/R2):** recargar la disponibilidad, mostrar por cada
  faltante los `lotesParaAbrir` y ofrecer el boton "Abrir" que llama a
  `POST api/v1/existencia/apertura`. Tras abrir, reintentar la produccion. Si
  `remanenteCerrado` es 0, indicar que hay que comprar.
- **Al descartar:** partir idealmente de `GET api/v1/lote/por-vencer` (grupo "vencidas"),
  enviar el `idDetalleLote` de la linea elegida, impedir cantidades mayores que su saldo y
  exigir motivo.
- **Al ajustar:** exigir motivo en el formulario y mostrar el delta calculado
  (`nuevo - actual`) antes de confirmar, para que el operador vea lo que va a registrar.
- **Decimales:** enviar numeros con hasta 4 decimales. No formatear con separador de miles
  en el payload. Nunca redondear en cliente: el redondeo es del servidor, una sola vez.
- **Unidades:** mostrar siempre la abreviatura junto a la cifra. Si el producto y la
  existencia usan unidades distintas, mostrar ambas.

### 4.3 Estados y concurrencia

**El problema real:** dos operadores abriendo el mismo lote, o dos producciones consumiendo
la misma existencia, a la vez.

**Solucion de servidor** (es donde tiene que resolverse):

1. **Bloqueo pesimista** en las dos operaciones que compiten por un saldo. En
   `DetalleLoteJpaRepository` y `ExistenciaJpaRepository`, un derivado anotado con
   `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Serializa las aperturas sobre la misma linea sin
   afectar al resto.
2. **Bloqueo optimista** (`@Version`) como red de seguridad ante escrituras por otras vias.
   Una `OptimisticLockException` se traduce a `409` en el `GlobalExceptionHandler`.

`@Lock` sobre un query method derivado no infringe la prohibicion 18 (`@Query`, JPQL, SQL
nativo o Criteria API): sigue siendo un derivado. Aun asi conviene anotarlo en
`arquitectura.md` seccion 5.7 como excepcion documentada.

**Solucion de cliente** (complementaria, no suficiente):

- Deshabilitar el boton mientras la peticion esta en vuelo; nunca confiar solo en eso.
- Ante `409`, **recargar el saldo** y mostrar el valor real en vez de reintentar a ciegas.
- Refrescar el dashboard de insumos abiertos tras cada operacion, o usar `ETag` /
  `If-Match` si mas adelante se quiere deteccion de version en el propio HTTP.
- **Idempotencia:** para apertura y produccion, aceptar una cabecera
  `Idempotency-Key` opcional que evite duplicar la operacion si el usuario reintenta tras un
  timeout de red. Requiere una tabla de claves; es mejora de segunda fase.

### 4.4 Contrato de error

Se mantiene el `ErrorResponse` actual (`codigo`, `tipo`, `mensaje`, `timestamp`). Para stock
insuficiente conviene que el `mensaje` nombre el insumo y las dos cifras, porque es el error
que el operador vera mas seguido:

```
"El insumo 'Harina 000' no alcanza: se requieren 2000.0000 g y hay 1500.0000 g disponibles"
```

Todos los errores de saldo de este modulo (insumo insuficiente, apertura que excede el
lote, linea vencida, lote de cocina que no admite la operacion, stock que quedaria negativo
al vender, descartar o ajustar) son `DomainException` con codigo `409`, lanzados desde el
agregado, `PlanDeConsumo` o `AsignadorFefo`. Los errores de forma de la
peticion siguen siendo `400` desde `Validador`.

### 4.5 Mapa de pantallas para el frontend

Cada pantalla con los endpoints que consume y su accion principal. Sirve como indice para
construir el frontend en el orden del plan (seccion 5).

| Pantalla | Rol | Lee | Escribe | Notas |
|---|---|---|---|---|
| **Punto de venta** | Operador | `producto/catalogo-venta` | `venta`, `existencia/apertura`, `existencia/{id}/cierre` (desde advertencias) | `vendible`, `producibles` y `unidadesMaximas` en vivo; refrescar tras cada venta |
| **Pedidos** | Operador | `pedido/requerimientos` | `venta` (con pedido), `pedido/{id}/despacho` | Despachar es lo que descuenta stock |
| **Cocina / produccion** | Operador | `receta/{id}/disponibilidad`, `pedido/requerimientos` | `produccion/ejecucion`, `existencia/apertura`, `produccion/{id}/variacion` | Flujo R1 -> R2 -> reintento; exigir vencimiento de tanda |
| **Insumos abiertos** | Operador | `existencia/abiertas`, `existencia/{id}/kardex` | `existencia/{id}/cierre`, `existencia/{id}/ajuste` | Grupos vigentes / vencidas |
| **Vencimientos** | Operador | `lote/por-vencer` | `producto/{id}/descarte` | Pantalla diaria de R7: vender primero, descartar vencido |
| **Compras y lotes** | Administrador | `lote`, `lote/{id}/trazabilidad`, `lote/{id}/ventas` | `lote`, `lote/{id}/devolucion` | Mostrar `origen`, `finalizado`, `vencido` por linea |
| **Conteo fisico** | Operador / Administrador | `conteo-fisico/{id}` | `conteo-fisico`, `PUT conteo-fisico/{id}`, `.../aplicacion` | Guardado parcial; aplicar solo administrador |
| **Carga inicial** | Administrador | `producto` | `lote/inventario-inicial` | Solo al arrancar; asistente por zonas |
| **Alertas** | Operador | `producto/bajo-minimo` | — | Lista de compras / produccion del dia |
| **Reportes** | Administrador | `inventario/perdidas`, `movimiento/mermas`, `producto/descartes` | — | Perdidas en Bs, margen por venta |
| **Anulaciones** | Administrador | `venta/{id}`, `produccion/{id}` | `venta/{id}/anulacion`, `produccion/{id}/anulacion` | Mostrar antes que se repondra y a donde |

**Contrato comun para todas las pantallas**

- Cantidades con 4 decimales, montos con 2; siempre la abreviatura de unidad al lado.
- Fechas de vencimiento como `LocalDate` (`yyyy-MM-dd`) interpretadas en la zona del
  negocio; el frontend no calcula "vencido" por su cuenta: usa los booleanos `vencido`,
  `finalizado`, `bajoMinimo` que devuelve el servidor.
- Todo `409` trae un `mensaje` listo para mostrar al operador; el frontend no lo parsea,
  recarga la vista estructurada correspondiente (disponibilidad, catalogo, por-vencer).

---

## 5. Plan de Migracion e Implementacion

Ordenado por dependencia: cada bloque necesita el anterior. Los bloques 0 a 3 son la ruta
minima para que el flujo del negocio funcione de punta a punta.

### Bloque 0 — Documentar primero (bloqueante)

`arquitectura.md` es normativa y exige actualizarse antes que el codigo. Sin esto, todo lo
demas la contradice.

- [ ] Registrar los verbos fuera del CRUD (`/apertura`, `/ajuste`, `/cierre`, `/ejecucion`,
      `/variacion`) como excepcion documentada a la prohibicion 25.
- [ ] Anadir `Shared/Application/Medidas/ConvertidorDeUnidades` a la tabla del Shared Kernel.
- [ ] Documentar `@Lock` sobre derivados como excepcion en la seccion 5.7.
- [ ] Anadir `409` al reparto de codigos de la seccion 5.9.
- [ ] Registrar la decision de 2.3 (lote de cocina + stock, costo 0) y las reglas R1-R7.
- [ ] Registrar la politica FEFO de 2.4 y la clase de dominio `AsignadorFefo`.
- [ ] Registrar el nuevo agregado `Descarte` y los verbos `/descarte` y `/por-vencer`.
- [ ] Registrar R6: `CrearVentaUseCase` compone `ILoteRepository` y la hija
      `detalle_venta_lote` del agregado `Venta`.
- [ ] Registrar R8-R14: agregado `ConteoFisico`, tabla `ajuste_lote`, los verbos
      `/aplicacion`, `/inventario-inicial`, `/devolucion`, `/despacho`, `/anulacion`, y
      la composicion de `DespacharPedidoUseCase` con repositorios de Ventas e Inventario.
- [ ] Anadir `Shared/Application/Tiempo/IReloj` y `Shared/Infrastructure/Tiempo/RelojDelNegocio`
      al Shared Kernel (2.7).
- [ ] Confirmar con el negocio el alcance de `id_producto_padre` (2.8).

### Bloque 1 — Esquema y catalogos

- [ ] Aplicar el DDL de la seccion 2.3 a `databasev4.sql`.
- [ ] `TipoMovimiento`: anadir `signo` a agregado, DTOs, `JpaModel` y `JpaMapper`.
- [ ] Sembrar el catalogo de tipos: `APERTURA (+1)`, `CONSUMO (-1)`, `MERMA (-1)`,
      `AJUSTE (+/-)`, `DEVOLUCION (+1)`, `EXCEDENTE (+1)`.
- [ ] Sembrar `estado_existencia`: `Abierta`, `Agotada`, `Vencida`, `Descartada`.
- [ ] `Produccion`: anadir `usuarioId`; `Movimiento`: anadir `produccionId` opcional.
- [ ] Subir a escala 4 `DetalleLote.ESCALA`, `detalle_lote.cantidad_*`, `producto.stock` y
      las cantidades de `produccion`.
- [ ] `Producto`: `agregarStock` / `descontarStock` en el agregado; retirar `stock` de los
      DTO de alta y edicion (3.11).
- [ ] `Lote`: `origen` (`COMPRA`/`COCINA`/`INICIAL`), `id_proveedor` y `produccionId`
      opcionales con su invariante, fabricas `crearDeProduccion(...)` y
      `crearInicial(...)`, y `corregirRendimiento(delta)`.
- [ ] `Receta`: `preparacionInmediata` y `diasVidaUtil`. `Producto`: `diasVidaAbierto`.
- [ ] `Existencia`: `cantidadAbierta`. `Produccion`: `costoTotal`, `anulada`,
      `motivoAnulacion`. `DetalleVenta`: `costo`. `Venta`: `stockDescontado`.
      `Movimiento`: `detalleVentaId` opcional. `Pedido`: `fechaDespacho`,
      `usuarioDespachoId`.
- [ ] Tabla `ajuste_lote` (con su agregado) y agregado compuesto `ConteoFisico`.
- [ ] `IReloj` + `RelojDelNegocio` y `app.zona-horaria` (3.20). Va antes que cualquier caso
      de uso que compare fechas.
- [ ] Crear la tabla `descarte` y el agregado `Descarte` con su plantilla completa.
- [ ] Crear `detalle_venta_lote` como hija de `Venta` (dominio, JpaModel, mapper) y subir
      `detalle_venta.cantidad` a escala 4.

### Bloque 2 — Conversion de unidades

- [ ] Escribir `ConvertidorDeUnidades` (funcion pura, sin dependencias de framework).
- [ ] Anadir a `IConversionRepository` / `IConversionDatasource` el derivado
      `obtenerPorUnidades(origen, destino)` — firmas identicas en ambos puertos (5.11).
- [ ] Tests unitarios: directo, inverso, identidad, factor ausente, redondeo en el borde.

### Bloque 3 — El flujo operativo

- [ ] **`AsignadorFefo`** con sus tests de dominio. Lo usan 3.1 (sugerencia), 3.10 y 3.12.
- [ ] **3.11** `CrearLoteUseCase` / `ActualizarLoteUseCase` suman `producto.stock` y
      protegen los lotes de cocina. Va primero: sin el, el stock nunca sube.
- [ ] **3.1 `AbrirDetalleLoteUseCase`** con su bloqueo pesimista y el descuento de
      `producto.stock`. Es el desbloqueante: sin el, no hay existencias legitimas de donde
      consumir.
- [ ] **3.6 `CrearMovimientoUseCase`** modificado para aplicar el saldo.
- [ ] **3.4 `AjustarExistenciaUseCase`** y **3.5 `CerrarExistenciaUseCase`**.
- [ ] **3.2 `EjecutarProduccionUseCase`** con `PlanDeConsumo` (FEFO + `DomainException`
      por faltantes), lote de cocina a costo 0 y suma de stock del producto obtenido.
- [ ] **3.3 `RegistrarVariacionDeProduccionUseCase`** sobre lote de cocina y stock.
- [ ] **3.10 `DescartarProductoUseCase`** por linea de lote.
- [ ] **3.12 `CrearVentaUseCase`** con `AsignadorFefo`, exclusion de vencidos, bloqueo
      pesimista, rama de preparacion inmediata, foto de costo y diferimiento por pedido.
- [ ] **3.15 `RegistrarInventarioInicialUseCase`**. Necesario antes de usar el sistema en
      el local real.
- [ ] **3.17 `DespacharPedidoUseCase`**, **3.19 `AnularVentaUseCase`**,
      **3.18 `AnularProduccionUseCase`**.
- [ ] **3.13 / 3.14 Conteo fisico** y **3.16 `DevolverAProveedorUseCase`**.
- [ ] **3.7 / 3.8**: restringir el alta libre de existencias y derivar `diferencia`.

### Bloque 4 — Consultas y rendimiento

- [ ] Derivados nuevos en `ExistenciaJpaRepository` (por detalle de lote, por estado) y en
      `MovimientoJpaRepository` (por existencia, por produccion, por rango de fechas).
- [ ] Corregir el N+1 de `ObtenerExistenciasUseCase` y `ObtenerMovimientosUseCase`
      siguiendo el patron de `LoteDatasourceImpl`.
- [ ] Los once casos de uso de consulta de la seccion 3.9 (incluye `lotesParaAbrir`,
      descartes, `por-vencer`, perdidas, bajo minimo, catalogo de venta y requerimientos
      de pedidos).
- [ ] `stockVencido`, `disponible` y `bajoMinimo` derivados en `ProductoResponse`.

### Bloque 5 — Endurecimiento

- [ ] `@Version` en `ExistenciaJpaModel` y `DetalleLoteJpaModel`; traducir
      `OptimisticLockException` a `409` en `GlobalExceptionHandler`.
- [ ] `@RequiereRol("Administrador")` sobre ajuste, mermas y trazabilidad.
- [ ] Consulta de conciliacion que verifique las cinco reglas de la seccion 2.1 y reporte
      divergencias.

### Bloque 6 — Pruebas

- [ ] Tests de dominio puro, sin Spring, para las invariantes nuevas (saldo no negativo,
      conservacion de masa lote/existencia, calculo de `diferencia`, `PlanDeConsumo`
      enumerando todos los faltantes, `descontarStock` rechazando negativos).
- [ ] Test de integracion del flujo R1 -> R2: producir falla con 409 sin tocar nada, abrir
      descuenta `producto.stock` del insumo, reintentar produce, crea un lote de cocina con
      `total = 0` y solo sube el stock del producto obtenido.
- [ ] Test de descarte: tanda de 5, 2 vendidos, 3 descartados -> linea de cocina y stock en
      0, linea finalizada; un cuarto descarte falla con 409.
- [ ] Test de venta FEFO: dos lineas, A comprada antes pero vence el 30 (10 u) y B comprada
      despues pero vence el 25 (5 u); vender 7 -> B queda en 0 (finalizada), A en 8, stock
      en 8, dos filas en `detalle_venta_lote`; anular la venta repone 5 a B y 2 a A.
- [ ] Test de vencidos: una linea vencida con saldo no se vende ni se abre (409) y si se
      descarta; el mensaje de venta informa cuanto hay vencido.
- [ ] Test de lote de cocina protegido: actualizar, eliminar o abrir uno falla con 409.
- [ ] Test de preparacion inmediata: vender 2 lattes descuenta leche y cafe teoricos con
      movimientos ligados a la linea de venta; con saldo teorico en 0 la venta se registra
      igual, con advertencia y sin 409; la existencia no se cierra sola; producir esa
      receta por tanda falla con 409.
- [ ] Test de unidades maximas: cafe 40 g por latte, 1 bolsa abierta con 400 g y 1 cerrada
      de 1 kg -> `unidadesMaximas` = 35; si la leche alcanza para 20, muestra 20 y
      `insumoLimitante` = leche.
- [ ] Test de cierre fisico: bolsa con saldo teorico 150 g cerrada como "se termino" ->
      `MERMA` de 150 g y saldo 0; cerrarla otra vez no escribe nada.
- [ ] Test de costeo: bolsa de 50 Bs con 5000 g, tanda que consume 2000 g -> costo_total
      20; descartar la mitad de la tanda se valora en 10, no en 0.
- [ ] Test de vida util abierta: leche con vencimiento a 30 dias y `dias_vida_abierto = 3`
      abierta hoy vence en 3 dias; al cuarto dia no entra en `PlanDeConsumo`.
- [ ] Test de pedido: crear venta con entrega futura no toca stock; despachar descuenta con
      FEFO respecto de la entrega y excluye una linea que vence antes; segundo despacho, 409.
- [ ] Test de conteo: sistema 10, contado 8, venta de 3 entre conteo y aplicacion -> la
      linea queda en 5 (se aplica el delta -2 sobre 7), con su `ajuste_lote`.
- [ ] Test de anulacion de produccion: intacta -> devuelve insumos y retira stock; con una
      unidad vendida -> 409.
- [ ] Test de reloj: con reloj falso a las 21:00 de La Paz (01:00 UTC del dia siguiente),
      un producto que vence hoy aun se vende.
- [ ] Tests de integracion `@SpringBootTest` sin `@Transactional` de clase, para observar el
      rollback real: apertura que excede el saldo, produccion con insumo insuficiente a
      mitad de receta, ajuste sin motivo.
- [ ] Un test de concurrencia con dos hilos abriendo el mismo `detalle_lote`, que verifique
      que uno de los dos falla.
- [ ] Los tests nuevos van bajo `src/test/java/...` espejando el paquete de produccion, y
      limpian en `@AfterEach` con `deleteAll(...)`, como el resto del proyecto.

---

## Resumen ejecutivo

El modulo tiene **el esquema casi completo y la logica de negocio practicamente ausente**.
Las seis tablas del flujo existen y estan bien relacionadas; lo que no existe es una sola
operacion que las recorra. Hoy `Produccion` no consume, `Movimiento` no mueve y `Existencia`
no se origina de nada verificable.

Las tres brechas criticas (B1, B2, B3) se cierran con dos casos de uso nuevos —
`AbrirDetalleLoteUseCase` y `EjecutarProduccionUseCase` — mas la modificacion de
`CrearMovimientoUseCase`. Las reglas R1-R7 fijan ademas el papel de `producto.stock`: para
todo producto es la suma de lo que queda en sus lotes. Los lotes son de compra (con costo)
o de cocina (costo 0, uno por produccion, con el vencimiento de la tanda). Venta y descarte
descuentan lote y stock a la vez, por **FEFO**, y guardan el reparto por linea, lo que da
traza de auditoria compartida entre inventario y ventas y minimiza perdidas por
vencimiento. R8-R14 cierran lo que una cafeteria real necesita ademas: bebidas que
consumen insumos al venderse, costo y perdidas en Bs, vida util una vez abierto, pedidos
que descuentan al despacharse, conteo fisico, carga inicial, devoluciones, anulaciones
reversibles y una sola nocion de "hoy". Todo sostenido por una sola invariante:

> Ningun saldo cambia sin un movimiento que lo explique.

Todo lo demas de este documento es consecuencia de esa frase.
