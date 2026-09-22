# Propuesta de Implementacion: Modulo de Inventarios y Produccion

> Documento de analisis y propuesta. **No modifica codigo.** Se rige por `arquitectura.md`,
> que es la fuente de verdad arquitectonica: toda propuesta de aqui respeta la plantilla de
> agregado (3.1), la doble indireccion Repository/Datasource (2.3), el borrado logico (4.5)
> y las prohibiciones de la seccion 6.
>
> Estado del codigo auditado: rama `feature-keycloak`, despues del refactor
> `Categoria -> Tipo` + nueva `Categoria` (2026-09-20).

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
Hay entonces tres cifras que pretenden decir lo mismo (`producto.stock`,
`sum(detalle_lote.cantidad_restante)`, `sum(existencias.contenido_actual)`) sin ninguna
regla que las concilie.

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

  (1) ABRIR    descuenta cantidad_restante, crea la existencia
               y escribe un movimiento APERTURA          ── una transaccion

  (2) PRODUCIR lee ingredientes_receta, elige existencias por FEFO,
               descuenta cada contenido_actual, escribe un movimiento
               CONSUMO por cada una con id_produccion    ── una transaccion

  (3) CERRAR   contenido_actual llega a 0 -> estado AGOTADA + fecha_acabado
               el residuo por debajo de escala sale como MERMA
```

**Invariante rectora del modulo** (hoy inexistente, es el corazon de la propuesta):

> Ningun saldo cambia sin un `Movimiento` que lo explique, y ningun `Movimiento` se escribe
> sin cambiar el saldo que dice cambiar. Ambos, siempre, en la misma transaccion.

De ahi se derivan tres reglas verificables:

1. `existencias.contenido_actual` = `contenido_inicial` + suma de sus movimientos con signo.
2. `sum(existencias.contenido_inicial)` de un `detalle_lote`, convertido a la unidad del
   producto, <= `detalle_lote.cantidad_inicial`.
3. `detalle_lote.cantidad_restante` = `cantidad_inicial` - lo ya abierto.

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
```

**Decision abierta — donde entra el producto obtenido.** Una produccion genera producto
terminado que debe volver al inventario, pero `detalle_lote.id_lote` es `NOT NULL` y
`lote.id_proveedor` tambien: hoy no cabe un lote de origen interno. Hay dos salidas y
**conviene decidirla antes de implementar el caso de uso 3.2**:

- **(a)** Hacer `lote.id_proveedor` nullable y anadir `lote.id_produccion UUID NULL`. La
  produccion crea su propio lote de origen interno y el producto terminado queda trazable
  como cualquier compra, con su vencimiento y sus existencias. Es mas trabajo y es lo
  correcto a largo plazo.
- **(b)** La produccion solo suma a `producto.stock`. Es inmediato, pero deja el producto
  terminado sin lote, sin vencimiento y sin posibilidad de abrirse como existencia.

Este documento asume **(a)** en el caso de uso 3.2 y lo senala donde aplica.

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
  "contenidoInicial": 5000.0000,
  "contenidoActual": 5000.0000,
  "fechaApertura": "2026-09-22T10:15:00",
  "fechaVencimiento": "2027-01-31",
  "idDetalleLote": "uuid",
  "estadoExistencia": { "id": "uuid", "nombre": "Abierta" },
  "unidad": { "id": "uuid", "unidad": "gramo", "abreviatura": "g" },
  "idUsuario": "uuid",
  "movimiento": { "id": "uuid", "cantidad": 5000.0000, "tipoMovimiento": "APERTURA" },
  "detalleLote": { "id": "uuid", "cantidadRestante": 9.0000 }
}
```

Devolver el saldo del lote en la misma respuesta evita que el frontend tenga que recargar.

**Logica de transaccion**

1. Cargar el `Lote` via `ILoteRepository.obtenerPorDetalleId(...)` — 404 si no existe.
2. Validar `idUsuario`, `idUnidad` y el estado "Abierta" del catalogo.
3. **Bloqueo pesimista** sobre `detalle_lote` (ver 4.3) antes de leer `cantidad_restante`.
4. `lote.ajustarCantidadRestante(idDetalle, restanteActual - cantidadAAbrir)` — la raiz
   valida que no quede negativo, per 5.13 toda mutacion de una hija pasa por su raiz.
5. Heredar `fecha_vencimiento` del `detalle_lote` hacia la existencia si esta no la trae.
6. `Existencia.crear(...)` con `contenidoActual = contenidoInicial`.
7. `Movimiento.crear(...)` de tipo `APERTURA`, ligado a la existencia recien creada.
8. Persistir los tres agregados.

**Atomicidad.** Los tres `save` viven bajo la `@Transactional` del caso de uso. Si el paso 4
falla por saldo insuficiente, no queda ni existencia ni movimiento. Precedente en el
proyecto: `CrearUsuarioUseCase` ya escribe `Usuario` + `Persona` en una sola transaccion.

**Casos limite**

- `cantidadAAbrir > cantidad_restante` -> 400 antes de tocar nada.
- Lote vencido (`fecha_vencimiento < hoy`) -> 400, salvo un flag `forzar` con rol elevado.
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
| `consumos` | lista de objetos | no | override manual (ver abajo) |

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
  "productoObtenido": { "idProducto": "uuid", "idDetalleLote": "uuid-nuevo" }
}
```

**Logica de transaccion**

1. Cargar `Receta` con sus ingredientes vivos — 404 si no existe.
2. Calcular el requerimiento por ingrediente: `ingrediente.cantidad * tandas`.
3. Por cada ingrediente, resolver existencias disponibles (estado "Abierta",
   `contenido_actual > 0`), ordenadas por FEFO.
4. **Convertir** cada `contenido_actual` a la unidad del producto via `IConversionRepository`
   + `ConvertidorDeUnidades`. Si falta el factor -> 400 nombrando ambas unidades.
5. **Verificar disponibilidad total ANTES de descontar nada.** Si algun ingrediente no
   alcanza, 409 con el detalle de cuanto falta y de que insumo. Esto evita el caso limite de
   quedarse sin insumo a mitad de receta dejando medio consumo aplicado.
6. Crear la `Produccion` (para tener su id disponible para los movimientos).
7. Por cada existencia elegida: descontar `contenido_actual`, escribir un `Movimiento`
   `CONSUMO` con `id_produccion` = la tanda, y si el saldo llega a 0 marcar la existencia
   como "Agotada" + `fecha_acabado`.
8. Si `diferencia < 0`, escribir ademas un movimiento `MERMA` por la desviacion.
9. Dar de alta el producto obtenido segun la decision (a) de la seccion 2.3.

**Atomicidad.** Todo bajo una `@Transactional`. El paso 5 es la salvaguarda funcional: la
verificacion previa hace que el rollback sea la excepcion y no el camino habitual, porque un
rollback a mitad de una tanda fisica ya ejecutada es confuso para el operador.

**Casos limite**

- Receta sin ingredientes vivos -> `validarRegistrable()` ya lo cubre en el dominio.
- Un ingrediente cubierto por varias existencias parciales -> consumo repartido, un
  movimiento por existencia.
- `cantidadReal > cantidadEsperada` (excedente) -> valido; `diferencia` positiva y el
  excedente entra al inventario. Requiere `motivoDiferencia`.
- Sin factor de conversion entre la unidad de la existencia y la del producto -> 400. No se
  asume 1:1 jamas.

---

### 3.3 `RegistrarVariacionDeProduccionUseCase` — **Nuevo**

Para el caso en que el rendimiento real se conoce despues de cerrar la tanda (fermentacion,
enfriado, control de calidad posterior).

`POST api/v1/produccion/{id}/variacion`

**Entradas:** `cantidadReal` (number, >= 0), `motivoDiferencia` (String, requerido),
`idUsuario`.

**Salidas:** `200 OK`, la `ProduccionResponse` con `diferencia` recalculada y el movimiento
de ajuste generado.

**Logica de transaccion:** recalcular `diferencia = cantidadReal - cantidadEsperada`,
escribir un movimiento `MERMA` o `EXCEDENTE` por el **delta respecto de la variacion
anterior** (no respecto de cero, o se contaria dos veces), y ajustar el stock del producto
obtenido. Todo en una transaccion.

**Caso limite:** aplicar la variacion dos veces. Se resuelve registrando siempre el delta y
no el absoluto.

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
el movimiento por ese delta con su signo, aplicar el nuevo saldo, y si llega a 0 cerrar la
existencia. Una transaccion. **Nunca** se permite ajustar sin movimiento.

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

---

### 3.6 `CrearMovimientoUseCase` — **Modificacion**

Hoy escribe la fila sin mover el saldo (B3).

**Cambios**

- Inyectar `IExistenciaRepository` (ya lo hace, solo para validar) y **aplicar el efecto**:
  `contenido_actual += cantidad * tipoMovimiento.signo`.
- Rechazar con 400 si el resultado quedaria negativo o por encima de `contenido_inicial`.
- Cerrar la existencia si el saldo llega a 0.
- `TipoMovimiento` gana el campo `signo` en agregado, DTOs y `JpaModel`.

**Entradas:** las actuales (`cantidad`, `idExistencia`, `idTipoMovimiento`, `idUsuario`,
`motivo`, `fechaMovimiento`) mas, opcionalmente, `idProduccion`.

**Salidas:** `MovimientoResponse` mas `saldoResultante` de la existencia afectada.

**Logica de transaccion:** movimiento y saldo se escriben juntos o no se escribe ninguno.

---

### 3.7 `CrearExistenciaUseCase` — **Modificacion (o retiro)**

Hoy permite crear stock abierto de la nada (B2). Dos opciones:

- **Recomendada:** retirar el `POST api/v1/existencia` publico y dejar que toda existencia
  nazca por 3.1. El agregado conserva sus cinco casos de uso internos, pero el alta libre
  deja de exponerse.
- **Alternativa:** mantenerlo restringido a `@RequiereRol("Administrador")` como carga
  inicial de inventario, escribiendo siempre su movimiento de apertura.

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
(dias), `soloConSaldo`. Devuelve existencia + producto + unidad + saldo + dias para vencer,
ordenado por FEFO. Requiere `findByBorradoFalseAndIdEstadoExistencia...` y un filtro por
producto a traves de `detalle_lote`.

**`ObtenerKardexDeExistenciaUseCase`** — `GET api/v1/existencia/{id}/kardex`

Historial completo de una existencia: apertura, consumos (con su produccion), ajustes,
mermas y cierre, con saldo acumulado tras cada movimiento. Requiere
`findByIdExistenciaAndBorradoFalseOrderByFechaMovimientoAsc`.

**`ObtenerHistorialDeMermasUseCase`** — `GET api/v1/movimiento/mermas`

Filtros por rango de fechas, producto y usuario. Agregados por producto y por periodo. Es la
vista que justifica haber puesto `id_usuario` en `movimientos`.

**`ObtenerDisponibilidadDeRecetaUseCase`** — `GET api/v1/receta/{id}/disponibilidad?tandas=N`

Pre-chequeo antes de producir. Por cada ingrediente: requerido, disponible (sumando
existencias abiertas convertidas), faltante y `suficiente` booleano. Es lo que permite al
frontend deshabilitar el boton de producir antes de intentar la operacion.

**`ObtenerTrazabilidadDeLoteUseCase`** — `GET api/v1/lote/{id}/trazabilidad`

De la compra a la merma: detalle de lote -> existencias abiertas -> movimientos ->
producciones. Es la consulta de auditoria completa.

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

Los codigos siguen el estandar del proyecto: `201` en alta, `200` en lectura y modificacion,
`204` en baja logica. Se anade `409 Conflict` para stock insuficiente y colision de
concurrencia, que hoy no se usa en el proyecto y conviene documentar en `arquitectura.md`
seccion 5.9 junto al reparto actual de codigos.

### 4.2 Validaciones recomendadas en cliente

Son **conveniencia de UX, nunca sustituto** de la validacion del servidor: el backend valida
igual en `createFromRequest` y en el dominio.

- **Antes de abrir:** mostrar `cantidad_restante` del detalle de lote y bloquear el envio si
  la cantidad supera el saldo. Avisar si `fecha_vencimiento` ya paso.
- **Antes de producir:** llamar a `GET api/v1/receta/{id}/disponibilidad?tandas=N` al cambiar
  el numero de tandas y deshabilitar el boton mientras algun ingrediente sea insuficiente,
  mostrando cuanto falta y de que.
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
- [ ] Decidir y registrar la opcion (a) o (b) de la seccion 2.3 para el producto obtenido.

### Bloque 1 — Esquema y catalogos

- [ ] Aplicar el DDL de la seccion 2.3 a `databasev4.sql`.
- [ ] `TipoMovimiento`: anadir `signo` a agregado, DTOs, `JpaModel` y `JpaMapper`.
- [ ] Sembrar el catalogo de tipos: `APERTURA (+1)`, `CONSUMO (-1)`, `MERMA (-1)`,
      `AJUSTE (+/-)`, `DEVOLUCION (+1)`, `EXCEDENTE (+1)`.
- [ ] Sembrar `estado_existencia`: `Abierta`, `Agotada`, `Vencida`, `Descartada`.
- [ ] `Produccion`: anadir `usuarioId`; `Movimiento`: anadir `produccionId` opcional.
- [ ] Subir a escala 4 `DetalleLote.ESCALA`, `detalle_lote.cantidad_*` y `producto.stock`.

### Bloque 2 — Conversion de unidades

- [ ] Escribir `ConvertidorDeUnidades` (funcion pura, sin dependencias de framework).
- [ ] Anadir a `IConversionRepository` / `IConversionDatasource` el derivado
      `obtenerPorUnidades(origen, destino)` — firmas identicas en ambos puertos (5.11).
- [ ] Tests unitarios: directo, inverso, identidad, factor ausente, redondeo en el borde.

### Bloque 3 — El flujo operativo

- [ ] **3.1 `AbrirDetalleLoteUseCase`** con su bloqueo pesimista. Es el desbloqueante: sin
      el, no hay existencias legitimas de donde consumir.
- [ ] **3.6 `CrearMovimientoUseCase`** modificado para aplicar el saldo.
- [ ] **3.4 `AjustarExistenciaUseCase`** y **3.5 `CerrarExistenciaUseCase`**.
- [ ] **3.2 `EjecutarProduccionUseCase`** con verificacion previa de disponibilidad y FEFO.
- [ ] **3.3 `RegistrarVariacionDeProduccionUseCase`**.
- [ ] **3.7 / 3.8**: restringir el alta libre de existencias y derivar `diferencia`.

### Bloque 4 — Consultas y rendimiento

- [ ] Derivados nuevos en `ExistenciaJpaRepository` (por detalle de lote, por estado) y en
      `MovimientoJpaRepository` (por existencia, por produccion, por rango de fechas).
- [ ] Corregir el N+1 de `ObtenerExistenciasUseCase` y `ObtenerMovimientosUseCase`
      siguiendo el patron de `LoteDatasourceImpl`.
- [ ] Los cinco casos de uso de consulta de la seccion 3.9.

### Bloque 5 — Endurecimiento

- [ ] `@Version` en `ExistenciaJpaModel` y `DetalleLoteJpaModel`; traducir
      `OptimisticLockException` a `409` en `GlobalExceptionHandler`.
- [ ] `@RequiereRol("Administrador")` sobre ajuste, mermas y trazabilidad.
- [ ] Decidir el destino de `producto.stock`: o se deriva en cada movimiento, o se retira
      del agregado. Mantenerlo como esta es la peor de las tres opciones.
- [ ] Consulta de conciliacion que verifique las tres reglas de la seccion 2.1 y reporte
      divergencias.

### Bloque 6 — Pruebas

- [ ] Tests de dominio puro, sin Spring, para las invariantes nuevas (saldo no negativo,
      conservacion de masa lote/existencia, calculo de `diferencia`).
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
`CrearMovimientoUseCase`, sostenidos por una sola invariante:

> Ningun saldo cambia sin un movimiento que lo explique.

Todo lo demas de este documento es consecuencia de esa frase.
