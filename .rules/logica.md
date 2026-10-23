# Logica de negocio de KaffeHaus v4

Este archivo es la **fuente de la verdad de la logica de negocio**. `arquitectura.md` dice *como*
se construye el sistema; este archivo dice *que reglas* cumple.

**Reglas de uso**

1. Cada decision tiene un identificador estable `<AGREGADO>-NN`. No se renumera ni se reutiliza.
   Una decision nueva toma el siguiente numero libre de su seccion. Una decision que se retira
   no se borra: se marca `(Derogada por <ID>)` o `(Derogada)` y se reduce a una linea.
2. Las decisiones no pueden contradecirse. Antes de cualquier cambio se revisan las de los
   agregados afectados; si el cambio entra en conflicto con alguna, se consulta al usuario como
   resolverlo y despues se actualiza este archivo.
3. Las referencias entre parentesis (`INV-xx`, `VEN-xx`, `CAJ-xx`) son identificadores antiguos
   que el codigo aun cita en sus comentarios. Los documentos que los definian (`inventario.md`,
   `ventas.md` y la antigua seccion 4.11 de `arquitectura.md`) ya no los contienen: este archivo
   es la unica fuente de reglas de negocio y `arquitectura.md` solo describe la arquitectura
   (GENERAL-12).
4. Los conflictos detectados y aun sin resolver estan en la seccion 6, "Pendientes de decision".
   Las decisiones acordadas que el codigo aun no cumple estan en la seccion 7, "Pendientes de
   implementacion".

Extraido del codigo de `src/main` el 2026-10-20. Decisiones del usuario incorporadas el
2026-10-20 (resolucion de P-01 a P-13 y P-15 a P-20, y decisiones de implementacion). Revision de
coherencia el 2026-10-20: PEDIDO-05, PRODUCTO-16, VENTA-11, GENERAL-06 y GENERAL-12 corregidas;
VENTA-15 y SESIONCAJA-12 recogen lo que solo describian los contratos de `arquitectura.md`.

---

## 0. Reglas generales (todos los agregados)

- **GENERAL-01. Borrado siempre logico.** Ninguna fila se elimina. `DELETE` marca `borrado = true`
  y las lecturas excluyen lo borrado. La baja de una raiz arrastra a sus entidades hijas.
- **GENERAL-02. Lo derivado lo calcula el sistema.** Totales, subtotales, costos, stock,
  diferencias, cambio, numero de paso, numero de ticket y estados iniciales nunca se aceptan del
  cliente. Donde cada agregado lo indica, enviarlos responde 400.
- **GENERAL-03. Quien opera es el usuario de la sesion.** El `idUsuario` sale del token; el
  cliente no lo envia y, si llega en el cuerpo, se ignora. Nadie opera a nombre de otro.
- **GENERAL-04. Catalogos con semantica se reconocen por nombre**, nunca por constantes de id:
  roles (`Administrador`, `Operador`), estados de existencia (`Abierta`, `Agotada`), estados de
  venta (`Completada`, `Anulada`), estados de pedido, tipos de movimiento (`Merma`, `Ajuste`,
  `Produccion`, `Apertura`, ver MOVIMIENTO-14), tipos de descuento (`Porcentaje`, `Monto fijo`) y tipo de pago
  `Efectivo`. Si un flujo necesita una fila que no esta registrada responde 404 nombrandola.
- **GENERAL-05. Atomicidad.** Toda operacion que mueve inventario o dinero ocurre en una sola
  transaccion: si una parte falla (por ejemplo, falta stock), no queda nada registrado a medias.
- **GENERAL-06. Orden de bloqueos.** Sesion de caja -> existencias o lineas de lote (procesadas en
  orden de id de producto) -> producto, siempre en ese orden, para no generar ciclos de bloqueo
  (INV-53).
- **GENERAL-07.** *(Derogada por GENERAL-10.)* Escalas, incluido el costo de una linea de venta.
- **GENERAL-08.** *(Derogada por GENERAL-13.)* Permisos con el ajuste de una existencia.
- **GENERAL-09. Vencimiento.** Algo con fecha de vencimiento esta vencido si esa fecha es
  anterior a hoy (el dia de vencimiento aun es vigente). Sin fecha de vencimiento, nunca vence.
- **GENERAL-10. Escalas y redondeo.** Dinero a 2 decimales con `HALF_UP`. Contenido de
  existencias, cantidades de movimientos y cantidades de ingredientes a 4 decimales. Los calculos
  intermedios (factores, costo por unidad de contenido) usan mas precision y se redondean al final.
- **GENERAL-11. El dinero se analiza en reportes.** Producir una receta no calcula dinero, y la venta
  solo registra lo que paga el cliente (VENTA-20). Costos, ganancias y valoraciones se resolveran en
  los reportes.
- **GENERAL-12. Este archivo sustituye a `inventario.md`, `ventas.md` y a las reglas `CAJ-xx` de
  `arquitectura.md`.** Los comentarios del codigo deben citar los identificadores de este archivo
  (`PRODUCTO-16`, `VENTA-23`, `SESIONCAJA-04`...). Las citas antiguas (`INV-xx`, `VEN-xx`, `CAJ-xx`
  y las que remiten a una seccion de `arquitectura.md` por una regla de negocio) se reemplazan a
  medida que se modifica cada archivo, no en un cambio masivo aparte. Equivalencia de las
  `CAJ-xx`: CAJ-01 -> VENTA-02; CAJ-02 -> VENTA-03; CAJ-10 -> VENTA-13; CAJ-11 -> VENTA-14;
  CAJ-12 y CAJ-13 -> VENTA-15; CAJ-14 -> VENTA-12; CAJ-20 -> SESIONCAJA-01; CAJ-21 ->
  SESIONCAJA-03; CAJ-22 -> SESIONCAJA-04; CAJ-23 -> SESIONCAJA-05; CAJ-24 -> SESIONCAJA-06;
  CAJ-25 -> SESIONCAJA-08.
- **GENERAL-13. Permisos.** Todo endpoint de agregado exige rol `Operador` o `Administrador`.
  Solo `Administrador`: crear, actualizar y eliminar `usuario`, `rol` y `producto`; anular una
  `venta`; registrar un pesaje (EXISTENCIA-18); actualizar un `lote`. Cerrar la caja: ver
  SESIONCAJA-05.

---

## 1. Contexto Inventario

### 1.1 Producto

- **PRODUCTO-01.** *(Derogada por PRODUCTO-08.)* Naturalezas MTS/MTO con descuento de existencias
  o de receta al venderse.
- **PRODUCTO-02.** *(Derogada por PRODUCTO-11.)* `producto.stock` como proyeccion del contenido
  abierto de las existencias.
- **PRODUCTO-03. Presentacion.** Un producto con `productoPadre` es una presentacion: la forma de
  compra de un producto base (INV-06). Lo que contiene una presentacion pertenece al producto
  base (INV-08).
- **PRODUCTO-04. Jerarquia de un solo nivel** (INV-07): el padre no puede ser una presentacion,
  un producto no puede ser su propio padre y un producto que ya es base de presentaciones no
  puede tener padre (409).
- **PRODUCTO-05. Precio de lista.** Lo fija un Administrador en el producto; la venta lo copia en
  el momento de vender (VENTA-06).
- **PRODUCTO-06. Bajo minimo.** Un producto esta bajo minimo cuando `stock < stockMinimo`.
- **PRODUCTO-07.** *(Derogada por PRODUCTO-17.)* Consulta de inventario que trataba todo MTS como
  producto con existencias.
- **PRODUCTO-08. Dos naturalezas.** `esStockDirecto = true` (MTS, reventa, insumo): tiene stock de
  unidades y, si es insumo, existencias. `esStockDirecto = false` (MTO, bajo demanda): no tiene
  stock propio y su venta no descuenta nada (VENTA-23).
- **PRODUCTO-09.** *(Derogada por PRODUCTO-11.)* `producto.stock` como contador de unidades para
  todo producto.
- **PRODUCTO-10.** *(Derogada por PRODUCTO-16.)* Lo que sube y baja el stock, sin distinguir el
  producto base.
- **PRODUCTO-11. Dos clases de stock.**
  - **Producto base con presentaciones** (por ejemplo, cafe o harina): su stock es la suma del
    contenido de sus existencias abiertas vivas, en su unidad. Sube al abrir una bolsa
    (EXISTENCIA-13), baja cuando una receta consume de el (MOVIMIENTO-18) y cambia con el pesaje
    (EXISTENCIA-16).
  - **Cualquier otro producto MTS** (presentaciones, reventa, producto terminado): su stock es un
    contador de unidades (PRODUCTO-16).
  En ambos casos no se modifica a mano (`stock` en el PUT responde 400). Un MTO no tiene stock.
- **PRODUCTO-12.** *(Derogada por PRODUCTO-16.)* Stock de unidades como contador propio.
- **PRODUCTO-16. Stock de unidades.** Es la suma de las unidades disponibles
  (`cantidadRestante`) de las lineas vivas de los lotes vivos del producto. Sube al comprar un lote
  (comprar 3 bolsas de cafe suma 3 a la bolsa) y al producir (PRODUCCION-12). Baja con la venta
  (VENTA-23), cuando una receta lo usa como insumo (MOVIMIENTO-18), al abrir una unidad (una bolsa
  menos, EXISTENCIA-13). Corregir la cantidad de una
  linea de lote o darla de baja se refleja solo. Al anular una venta con reintegro, las unidades
  vuelven a sus lineas (LOTE-18). Se recalcula despues de cada cambio, sin sumar ni restar sobre el
  valor leido.
- **PRODUCTO-13. El stock nunca es negativo.** Toda operacion que lo dejaria por debajo de cero se
  rechaza con 409 (por ejemplo, una venta sin stock suficiente).
- **PRODUCTO-14. Que se abre y que tiene existencias.** Ser vendible o ser insumo no lo decide:
  un producto puede ser ambas cosas (el pan se vende y es insumo; la bolsa de harina se vende y se
  abre para la harina base). Lo decide la jerarquia del producto, que ya esta en sus datos:
  - **Solo una presentacion se abre** (`productoPadre` informado; por ejemplo, la bolsa de harina
    de 1 kg). Al abrirse se vuelve existencia de su producto base (EXISTENCIA-13).
  - **Solo un producto base con presentaciones tiene existencias** (otras presentaciones apuntan a
    el; por ejemplo, la harina).
  - **Todo lo demas se maneja por unidades** (sin padre y sin presentaciones; por ejemplo, el pan o
    una gaseosa): no se abre ni tiene existencias; se vende y se usa en recetas descontando
    unidades de su stock (PRODUCTO-16).
  `tipo` (`Producto` / `Insumo`) es solo una clasificacion visual sin efecto en ninguna regla, y el
  `fraccionable` de una linea de lote tampoco interviene.
- **PRODUCTO-15. Lo que no cambia en un producto con inventario.** `esStockDirecto`,
  `unidadActual` y `productoPadre` responden 409 si el producto tiene stock, existencias vivas,
  lineas de lote vivas o presentaciones: cambiarlos cambiaria como se maneja su inventario
  (PRODUCTO-11, PRODUCTO-14). El resto de sus datos (nombre, precio, categoria...) se cambia
  libremente.
- **PRODUCTO-17. Consulta de inventario de un producto** (`GET api/v1/producto/{id}/inventario`,
  INV-30, INV-31, INV-46). Es una lectura sin bloqueos; la validacion definitiva la hace la
  operacion que descuenta.
  - Producto base con presentaciones: saldo abierto (sus existencias), saldo cerrado (las bolsas
    cerradas de sus presentaciones por el contenido que daria abrirlas; las que no tienen conversion
    posible no cuentan), valor (contenido abierto por su costo por unidad de contenido), disponible
    abierto vigente y disponible total vigente (abierto + cerrado, sin lo vencido).
  - Producto por unidades: saldo abierto = unidades disponibles de sus lineas de lote vivas;
    disponible = las de lineas no vencidas; nada cerrado; valor = unidades por su precio de compra
    (lo producido no tiene costo, GENERAL-11).
  - MTO: unidades enteras que alcanzan todos sus insumos, `min(floor(saldo / consumo por
    unidad))`, explotando recursivamente insumos MTO. Sin receta viva o sin ingredientes, 0. Un
    ciclo entre recetas responde 409.

### 1.2 Unidad y Conversion

- **CONVERSION-01. Factor entre unidades** (INV-09): misma unidad = 1; fila directa = su factor;
  fila inversa = 1 / factor; si no, el camino mas corto de hasta 4 saltos por el grafo de
  conversiones vivas con factor mayor a cero, multiplicando factores. Sin camino, no hay
  conversion posible.
- **CONVERSION-02.** No se registra una conversion de una unidad a si misma (lo prohibe la base,
  y CONVERSION-01 ya devuelve 1).
- **CONVERSION-03. Unidades y conversiones sembradas:** `Kilogramo`, `Gramo`, `Litro`,
  `Mililitro`, `Unidad`; `kg -> g` y `l -> ml` con factor 1000.

### 1.3 Lote (compras y lotes de produccion)

- **LOTE-01. Un solo origen por lote.** Compra: tiene proveedor y no produccion. Produccion: tiene
  produccion y no proveedor. `POST api/v1/lote` solo crea compras (proveedor obligatorio); los
  lotes de produccion los crea exclusivamente la produccion (PRODUCCION-12). El origen no cambia:
  dar proveedor a un lote de produccion responde 400.
- **LOTE-02. Un lote tiene al menos una linea viva** (400).
- **LOTE-03. Total derivado.** `total = suma(cantidadInicial x precioUnitario)` de las lineas
  vivas. No se recibe, se recalcula al mutar lineas y no cambia por consumo, apertura ni ajuste de
  unidades restantes. Es el costo historico de la compra.
- **LOTE-04. Linea de lote.** Producto existente obligatorio; `cantidadInicial > 0`;
  `precioUnitario >= 0` (precio por unidad comprada); `fraccionable` por defecto falso;
  `fechaVencimiento` opcional. `fechaCompra` por defecto es ahora.
- **LOTE-05.** *(Derogada por LOTE-19.)* Unidades cerradas, con el lote de produccion naciendo en 0.
- **LOTE-19. Unidades disponibles.** `cantidadRestante` cuenta las unidades de la linea que aun no
  se abrieron, vendieron ni usaron en una receta. En una compra nace igual a `cantidadInicial`; en
  un lote de produccion, igual a `cantidadReal` (PRODUCCION-12). Siempre
  `0 <= cantidadRestante <= cantidadInicial`, y la cantidad inicial no puede corregirse por debajo
  de la restante.
- **LOTE-17. Descuento de unidades.** La venta o la receta que usa un producto por unidades saca
  esas unidades de sus lineas de lote en el orden de LOTE-09: lotes vivos, lineas vivas con
  unidades disponibles y no vencidas; primero la que vence antes, despues la del lote mas antiguo y
  la de menor id. Reparte entre varias lineas si hace falta y admite fracciones. Si las unidades
  vigentes no alcanzan, 409. Bloquea las lineas antes de descontar. No registra movimiento.
- **LOTE-18. Devolucion de unidades.** El reintegro de una venta anulada devuelve las unidades a las
  lineas vivas del producto que tienen espacio (`cantidadRestante < cantidadInicial`), en el mismo
  orden de LOTE-17 (sin filtrar vencidas) y sin superar la cantidad inicial de ninguna. Si no cabe
  todo, 409.
- **LOTE-06. Modificacion.** El PUT concilia lineas por id (con id se corrige, sin id se agrega,
  ausente se da de baja). No se cambia el producto de una linea existente (400). `cantidadRestante`
  solo se admite en el PUT, como ajuste explicito de inventario. El usuario que registro la compra
  no se corrige.
- **LOTE-07. No se da de baja una linea ni un lote con existencias con contenido** (409, INV-63):
  su stock quedaria sin origen. Primero se consumen o se ajustan por pesaje (EXISTENCIA-16).
- **LOTE-08. Apertura elegida por el usuario** (INV-10, INV-13, INV-14, INV-34). El sistema nunca
  abre unidades por su cuenta. Se abre una unidad entera (`cantidadRestante >= 1`) de un lote vivo;
  un detalle vencido no se abre. Debe abrirse el detalle que toca de esa presentacion segun
  LOTE-09; si se pide otro, 409 indicando cual corresponde. El usuario elige la presentacion desde
  la lista de LOTE-14.
- **LOTE-09. Orden de apertura (FEFO).** Entre las lineas vivas de lotes vivos con unidades
  cerradas y no vencidas: primero la que vence antes (sin vencimiento al final), despues la del
  lote mas antiguo (`fechaCompra`) y, como desempate, la de menor id.
- **LOTE-10.** *(Derogada por LOTE-14.)* Consulta de aperturables con el propio producto base como
  fuente.
- **LOTE-11.** *(Derogada por VENCIMIENTO-01.)* Vencimiento de unidades cerradas: abrir y dar de
  baja como `Merma`.
- **LOTE-12.** *(Derogada por LOTE-15.)* Costo por unidad de contenido tambien para lotes de
  produccion.
- **LOTE-14. El usuario elige que bolsa abrir de una lista.** Cuando hace falta mas de un insumo
  base, el sistema muestra la lista de presentaciones que lo dan (consulta por producto base; una
  presentacion responde 400): una fila por presentacion con su bolsa mas proxima a vencer segun
  LOTE-09, sus unidades cerradas, el contenido de una bolsa y su costo por unidad de contenido. La
  lista se ordena por vencimiento, de la mas proxima a vencer a la mas lejana. El usuario elige la
  fila; dentro de esa presentacion se abre siempre la bolsa que indica la fila (LOTE-08).
- **LOTE-15. Costo por unidad de contenido.** Solo existe para lo comprado:
  `precioUnitario / contenidoInicial` de la existencia abierta. Lo usan la lista de aperturables
  (LOTE-14) y la consulta de inventario (PRODUCTO-17) como dato informativo.
- **LOTE-16. Correccion de bolsas cerradas.** Corregir `cantidadRestante` en el PUT de un lote
  (LOTE-06, solo Administrador) ajusta el stock de la presentacion (PRODUCTO-16) sin registrar
  movimiento: los movimientos solo trazan existencias abiertas (MOVIMIENTO-15). Queda la fecha de
  modificacion de la linea. Se revisara al disenar los reportes.
- **LOTE-13. Que se compra en un lote.** Una linea de compra es de una presentacion o de un
  producto por unidades (PRODUCTO-14). Una linea de un producto base con presentaciones responde
  400: para comprarlo suelto se registra una presentacion (por ejemplo, "Harina suelta 1 kg").

### 1.4 Existencia

- **EXISTENCIA-01.** *(Derogada por EXISTENCIA-17.)* Origenes de una existencia, incluido el
  reintegro de una venta.
- **EXISTENCIA-02.** *(Derogada por EXISTENCIA-13.)* Apertura con movimiento `Ajuste` de entrada y
  recalculo del stock.
- **EXISTENCIA-03. Consumo.** La cantidad consumida es mayor a cero y no supera el contenido actual
  (409). El contenido nunca queda negativo. Al llegar a 0 registra `fechaAcabado` y pasa a
  `Agotada`.
- **EXISTENCIA-04.** *(Derogada por EXISTENCIA-15.)* Una existencia agotada nunca revive.
- **EXISTENCIA-05.** *(Derogada por EXISTENCIA-16.)* Merma con cantidad libre.
- **EXISTENCIA-06.** *(Derogada por VENCIMIENTO-01.)* Baja por vencimiento de una existencia.
- **EXISTENCIA-07.** *(Derogada por EXISTENCIA-16.)* Ajuste por conteo registrado siempre como
  `Ajuste`.
- **EXISTENCIA-08. Modificacion.** El PUT solo admite `fechaVencimiento`; cualquier otro campo
  responde 400 (el contenido cambia por apertura, produccion o pesaje, EXISTENCIA-16).
- **EXISTENCIA-09. Baja.** Solo de una existencia agotada (409 si aun tiene contenido).
- **EXISTENCIA-10.** *(Derogada por EXISTENCIA-14.)* Bloqueo y recalculo del stock del producto
  base.
- **EXISTENCIA-11.** *(Derogada por EXISTENCIA-17.)* Existencias nacidas tambien de una
  produccion.
- **EXISTENCIA-12.** *(Derogada por EXISTENCIA-16.)* Pesaje de una sola existencia.
- **EXISTENCIA-13. Apertura.** Abrir una bolsa cerrada crea una existencia `Abierta` del producto
  base: contenido = factor de conversion de la unidad de la presentacion a la del base (sin
  conversion o con contenido nulo, 409), en la unidad actual del base y con el vencimiento del
  detalle. Descuenta la unidad cerrada del lote y registra un movimiento `Apertura` de
  **cantidad 0** (MOVIMIENTO-14): solo deja constancia de que se abrio, sin mover contenido. El
  stock de la presentacion baja una unidad y el del producto base sube en el contenido de la bolsa
  (PRODUCTO-11, PRODUCTO-16). El usuario decide desde su pantalla que bolsa abrir cuando le hace
  falta mas insumo base (LOTE-08).
- **EXISTENCIA-14. Bloqueo.** Toda operacion que cambia el contenido bloquea la existencia antes de
  leerla (INV-52).
- **EXISTENCIA-15. Una existencia agotada solo revive por pesaje.** Si el pesaje dice que hay mas
  (EXISTENCIA-16), una existencia agotada puede volver a tener contenido: vuelve a `Abierta` y
  pierde su `fechaAcabado`. Ninguna otra operacion la revive.
- **EXISTENCIA-16. Pesaje por producto base** (solo Administrador). El usuario informa desde el
  frontend el peso total real de las existencias abiertas de un producto base; el sistema lo
  compara con la suma de su contenido actual y decide el tipo: si la realidad tiene **menos**, es
  `Merma`; si tiene **mas**, es `Ajuste`; si coincide no registra nada. La diferencia se reparte
  empezando por la existencia del ultimo movimiento y siguiendo hacia atras:
  - `Merma`: se descuenta de cada existencia hasta dejarla en 0, y se sigue con la anterior.
  - `Ajuste`: se rellena cada existencia hasta su contenido inicial, incluidas las agotadas
    (EXISTENCIA-15), y se sigue con la anterior. Si la diferencia supera lo que cabe en todas,
    responde 409.
  Un movimiento por existencia tocada, por la cantidad que le toco. Es la unica forma de registrar
  mermas y ajustes.
- **EXISTENCIA-18. Contrato del pesaje.** `POST api/v1/producto/{id}/pesaje` con `pesoTotal`
  (`>= 0`, en la unidad del producto base) y `motivo`. Solo Administrador. `{id}` debe ser un
  producto base con presentaciones (400 si no). Responde las existencias tocadas. Sustituye a
  `POST api/v1/existencia/{id}/ajuste`, que se retira.
- **EXISTENCIA-17. Origen de una existencia.** No se crean a mano (409, INV-12). Una existencia
  nace solo al abrir una presentacion (EXISTENCIA-13) y siempre pertenece a un producto base con
  presentaciones (PRODUCTO-14). Su contenido solo cambia por el consumo de una receta
  (MOVIMIENTO-18) y por el pesaje del usuario (EXISTENCIA-16).

### 1.5 Movimiento (trazabilidad de existencias)

- **MOVIMIENTO-01. Inmutable.** PUT y DELETE responden 409; ni siquiera hay baja logica. Un error se
  corrige con un movimiento compensatorio.
- **MOVIMIENTO-02.** *(Derogada por MOVIMIENTO-08.)* Cantidad siempre positiva.
- **MOVIMIENTO-03.** *(Derogada por MOVIMIENTO-14.)* Tipos `Receta`, `Ajuste` y `Merma` con su uso
  anterior.
- **MOVIMIENTO-04.** `produccionId` solo se informa en los movimientos de una produccion.
- **MOVIMIENTO-05.** *(Derogada por VENTA-23.)* Enlace de los movimientos de una venta por motivo.
- **MOVIMIENTO-06.** *(Derogada por MOVIMIENTO-18.)* Salida de inventario compartida por produccion
  y venta.
- **MOVIMIENTO-07.** *(Derogada por VENTA-22.)* Reintegro de existencias al anular una venta.
- **MOVIMIENTO-08. Cantidad.** Es una magnitud mayor a cero; el tipo da el sentido. Unica
  excepcion: el movimiento de apertura, de cantidad 0 (EXISTENCIA-13).
- **MOVIMIENTO-09.** *(Derogada por MOVIMIENTO-14.)* Tres tipos de movimiento, sin `Apertura`.
- **MOVIMIENTO-10.** *(Derogada por MOVIMIENTO-18.)* Consumo de insumos siempre desde existencias.
- **MOVIMIENTO-11.** *(Derogada por MOVIMIENTO-15.)* Proposito, con la entrada del producto
  terminado como movimiento.
- **MOVIMIENTO-12.** *(Derogada por MOVIMIENTO-14.)* Tipos de movimiento con `Produccion` como
  entrada del producto terminado.
- **MOVIMIENTO-13.** *(Derogada por MOVIMIENTO-18.)* Consumo de insumos que devolvia su costo.
- **MOVIMIENTO-16.** *(Derogada por MOVIMIENTO-18.)* Consumo de insumos con el producto por unidades restado del stock.
- **MOVIMIENTO-18. Consumo de insumos** (INV-32), usado solo por la produccion. Segun el insumo
  (PRODUCTO-14):
  - **Producto base con presentaciones** (harina): consume de sus existencias abiertas, con
    contenido y no vencidas a la fecha de la operacion, en la unidad del producto, en orden FEFO
    (primero la bolsa abierta mas proxima a vencerse; despues la abierta antes y la de menor id),
    bloqueadas, repartiendo entre varias si hace falta. El sistema elige la existencia, no el
    usuario. Un movimiento `Produccion` por existencia tocada. Si el contenido abierto no alcanza,
    409; si hay bolsas cerradas vigentes que alcanzarian, el mensaje pide abrir una.
  - **Producto por unidades** (pan): descuenta unidades de sus lineas de lote (LOTE-17). No registra
    movimiento, porque no hay existencia que trazar.
  - **Insumo MTO:** explota su receta vigente (RECETA-06) escalada a `cantidad / rendimiento` y
    recurre por cada ingrediente. MTO sin receta, 409; ciclo entre recetas, 409.
  Cualquier 409 revierte la produccion entera. Nunca abre bolsas por su cuenta. Solo mueve
  cantidades: no calcula el dinero de lo consumido (GENERAL-11).
- **MOVIMIENTO-17. No hay alta manual de movimientos.** `POST api/v1/movimiento` responde 409,
  igual que su PUT y su DELETE (MOVIMIENTO-01): un movimiento solo lo genera la operacion que
  explica (MOVIMIENTO-15). Los movimientos solo se consultan.
- **MOVIMIENTO-14. Tipos de movimiento.** Son cuatro:
  - `Merma`: el pesaje encontro menos de lo que dice el sistema (EXISTENCIA-16).
  - `Ajuste`: el pesaje encontro mas de lo que dice el sistema (EXISTENCIA-16).
  - `Produccion`: los insumos que una receta consumio de existencias al producirse
    (MOVIMIENTO-18). Sustituye al antiguo tipo `Receta`. Es solo el nombre del tipo de movimiento:
    el agregado `Receta`, su dominio y sus tablas conservan su nombre.
  - `Apertura`: se abrio una presentacion; cantidad 0 (EXISTENCIA-13).
  No existen `Ajuste positivo` ni `Ajuste negativo`.
- **MOVIMIENTO-15. Proposito.** Los movimientos son la trazabilidad de las existencias de los
  productos base. Solo los generan el consumo de una receta (MOVIMIENTO-18), el pesaje
  (EXISTENCIA-16) y la apertura de una presentacion (EXISTENCIA-13). Las ventas, la produccion del
  terminado y el consumo de productos por unidades no generan movimientos.

### 1.6 Receta

- **RECETA-01. Rendimiento.** `cantidad` (lo que rinde la receta) mayor a cero.
- **RECETA-02. Una receta tiene al menos un ingrediente y al menos un paso vivos** (400).
- **RECETA-03. Ingredientes.** Producto existente y que no sea presentacion: las recetas se
  expresan en productos base (INV-08). Cantidad mayor a cero. Un producto no se repite entre los
  ingredientes vivos. El producto de un ingrediente no se cambia (se da de baja y se agrega otro).
- **RECETA-04. Pasos.** Descripcion obligatoria. El numero de paso no se envia: el orden del array
  es la secuencia y la raiz asigna 1..N sin huecos, renumerando en cada alta, baja o reorden.
- **RECETA-05. Modificacion.** El PUT concilia ingredientes y pasos por id, cada coleccion por
  separado; la coleccion no enviada queda intacta. Las bajas se aplican antes que las altas.
- **RECETA-06. Receta vigente.** Un producto puede tener varias recetas; la vigente es la viva mas
  reciente (por fecha de creacion). Es la que usan el consumo de insumos MTO y la disponibilidad
  MTO.

### 1.7 Produccion (MTS por lotes)

- **PRODUCCION-01.** Solo se produce un producto MTS; producir un MTO responde 409.
- **PRODUCCION-02. Datos.** `cantidadEsperada > 0`; `cantidadReal >= 0` (0 = tanda perdida);
  `diferencia = cantidadReal - cantidadEsperada`, derivada; receta y `fechaVencimiento`
  obligatorias; `fechaProduccion` por defecto es ahora.
- **PRODUCCION-03.** *(Derogada por PRODUCCION-06.)* Consumo de insumos con movimientos `Receta`.
- **PRODUCCION-04.** *(Derogada por PRODUCCION-12.)* Producto terminado con movimiento `Receta` y
  recalculo del stock.
- **PRODUCCION-05. Una produccion registrada es fija.** Solo se corrigen `motivoDiferencia` y
  `fechaProduccion`; cantidades, diferencia y receta responden 400.
- **PRODUCCION-06. Consumo de insumos.** Cada ingrediente de la receta por
  `cantidadEsperada / rendimiento`, con MOVIMIENTO-18 y movimientos `Produccion` (MOVIMIENTO-14)
  vinculados a la produccion. Si falta un insumo no queda produccion, consumo ni lote.
- **PRODUCCION-07.** *(Derogada por PRODUCCION-12.)* Producto terminado como lote y existencia
  abierta, sin subir el stock.
- **PRODUCCION-08.** *(Derogada por PRODUCCION-12.)* Producto terminado con su forma de entrada
  pendiente.
- **PRODUCCION-09.** *(Derogada por PRODUCCION-12.)* Producto terminado con lote a costo de los
  insumos.
- **PRODUCCION-10.** *(Derogada por PRODUCCION-12.)* Producto terminado con lote sin unidades disponibles.
- **PRODUCCION-12. Producto terminado.** El terminado se maneja por unidades (PRODUCTO-14). Si
  `cantidadReal > 0` (por ejemplo, 20 panes), crea un lote de produccion con una sola linea de
  `cantidadReal` unidades disponibles, con el vencimiento indicado y sin costo (precio unitario 0,
  total 0: producir no calcula dinero, GENERAL-11). Su stock sube en esas unidades (PRODUCTO-16). No
  crea existencia ni movimiento de entrada. Venderlos o usarlos en otra receta descuenta de ese lote
  (LOTE-17). Con `cantidadReal = 0` (tanda perdida) no crea lote ni sube nada.
- **PRODUCCION-11. Una produccion no se da de baja** (409): ya movio stock e insumos. Un error se
  corrige con el pesaje (EXISTENCIA-16); la produccion se conserva como registro.

### 1.8 Catalogos de inventario (Tipo, Categoria, EstadoExistencia, TipoMovimiento)

- **CATALOGO-INV-01.** *(Derogada por CATALOGO-INV-03.)* Siembra con tipos de movimiento `Receta`,
  `Ajuste` y `Merma`.
- **CATALOGO-INV-02.** *(Derogada por CATALOGO-INV-03.)* Siembra sin el tipo `Apertura`.
- **CATALOGO-INV-03.** *(Derogada por CATALOGO-INV-04 y CATALOGO-INV-05.)* Catalogos sin reglas
  propias.
- **CATALOGO-INV-04.** Tipo, EstadoExistencia y TipoMovimiento son CRUD sin reglas propias. Se
  siembran: tipos `Producto` e `Insumo`; categorias `Cafes`, `Bebidas`, `Pasteleria`, `Insumos`;
  estados de existencia `Abierta` y `Agotada`; tipos de movimiento `Produccion`, `Ajuste`, `Merma`
  y `Apertura`.
- **CATALOGO-INV-05. `esBorrable`.** Una `Categoria`, `Unidad` o `Conversion` con
  `esBorrable = false` no se da de baja (409): protege los registros base de los que dependen otros
  flujos (por ejemplo, sin la conversion kg -> g no se pueden abrir bolsas). Lo que crea la siembra
  nace con `esBorrable = false`.

### 1.9 Vencimientos

- **VENCIMIENTO-01. El vencimiento de insumos abiertos y cerrados aun no esta disenado.** Se
  definira mas adelante. Hasta entonces no hay operaciones de baja por vencimiento: las existentes
  (merma libre y vencimientos de existencia y de lote) se retiran. Siguen vigentes GENERAL-09, que
  un detalle vencido no se abre (LOTE-08) y que la produccion no consume existencias vencidas
  (MOVIMIENTO-18).

---

## 2. Contexto Ventas

### 2.1 Venta

- **VENTA-01. Nace `Completada` con su stock ya descontado**, en la misma transaccion (VEN-01,
  VEN-21, VENTA-23). Si la venta falla no queda venta ni pagos (VEN-22).
- **VENTA-02. Requiere caja abierta** (409 si no la hay). Pertenece a la sesion abierta en la que
  se cobro, que se toma con bloqueo compartido antes que el inventario (CAJ-01).
- **VENTA-03. Siempre tiene cliente.** Sin `idCliente`, la venta es del cliente 'Mostrador'
  (CAJ-02). El usuario de la venta es el empleado de la sesion que cobra.
- **VENTA-04.** *(Derogada por VENTA-21.)* Prohibicion de vender presentaciones.
- **VENTA-05. Fecha.** `fechaVenta` opcional, por defecto ahora. Define la vigencia de descuentos.
- **VENTA-06. Importes derivados** (VEN-13, VEN-14). Precio = precio de lista del producto;
  descuento = el vigente de mayor importe para el cliente (no se acumulan); subtotal =
  `precio x cantidad - descuento`; total = suma de subtotales. Enviar `total`, `idEstadoVenta`,
  `idSesionCaja`, `numeroTicket`, `precioUnitario`, `descuentoAplicado`, `subtotal` o `costoTotal`
  responde 400. El descuento nunca supera el importe de la linea. La linea no lleva costo
  (VENTA-20).
- **VENTA-07.** *(Derogada por VENTA-23.)* Descuento de existencias (MTS) y de insumos (MTO) con
  movimientos al vender.
- **VENTA-08. Idempotencia** (VEN-66). Una `claveIdempotencia` ya usada devuelve la venta existente
  sin crear otra, aunque su caja ya se haya cerrado.
- **VENTA-09. Inmutable** (VEN-30). Lo unico que se corrige es el cliente (VEN-31): el PUT solo
  admite `idCliente` y cualquier otro campo responde 400. Una venta anulada no admite cambios (409,
  VEN-32).
- **VENTA-10. No se da de baja** (409, VEN-33): se anula.
- **VENTA-11. Anulacion** (VEN-40, VEN-44). La pide un Administrador o la provoca la cancelacion de
  un pedido (PEDIDO-05). Solo una venta `Completada`, siempre con motivo; registra quien anula.
  Lineas, importes y pagos se conservan. `reintegrar` es opcional (falso); su efecto lo
  dice VENTA-22.
- **VENTA-12. Ticket** (CAJ-14). El numero lo asigna la base (secuencia unica). Se busca por numero
  positivo (otro valor, 400) y devuelve cliente, cajero y tipos de pago resueltos.

**Pagos (entidad hija de Venta)**

- **VENTA-13. Pago mixto** (CAJ-10): una venta admite varios pagos de distintos tipos.
- **VENTA-14. Los pagos suman exactamente el total** (400 si no, CAJ-11). Una venta de total cero
  no lleva pagos.
- **VENTA-15. Cada pago.** Tipo de pago existente, `monto > 0` y `referencia` opcional (por
  ejemplo, el numero de operacion de una tarjeta). En efectivo, `montoRecibido` (por
  defecto igual al monto) debe cubrir el monto y `cambio = montoRecibido - monto` (CAJ-12). En
  cualquier otro tipo, un `montoRecibido` distinto del monto responde 400 (CAJ-13). `cambio` nunca
  se envia (400).
- **VENTA-16. Los pagos son inmutables**, como la venta.

**Inventario**

- **VENTA-17.** *(Derogada por VENTA-23.)* Descuento de `producto.stock` con el rechazo por stock
  insuficiente y el costo aun pendientes.
- **VENTA-18.** *(Derogada por VENTA-22.)* Reintegro de unidades a `producto.stock`.
- **VENTA-22. Reintegro al anular.** Anular con `reintegrar = true` devuelve las unidades que
  desconto cada linea de un producto por unidades a sus lineas de lote (LOTE-18). Un producto MTO no
  devuelve nada. Si no se pueden devolver todas, la anulacion entera falla (409) y la venta sigue
  `Completada`. Sin reintegro el inventario no cambia.
- **VENTA-19.** *(Derogada por VENTA-23.)* Venta que resta de un contador de stock.
- **VENTA-23. Lo que descuenta la venta.** Cada linea de un producto por unidades (incluida una
  presentacion, como una bolsa cerrada) descuenta su cantidad de las lineas de lote del producto
  (LOTE-17); si las unidades vigentes no alcanzan, la venta entera se rechaza con 409
  (PRODUCTO-13). Un producto MTO no descuenta nada. La venta no consume existencias ni registra
  movimientos: las existencias solo se usan para insumos, con recetas y pesajes.
- **VENTA-21. Que se vende.** Al menos una linea. Cada linea admite solo `idProducto` y
  `cantidad > 0`. El producto debe existir y ser vendible (400) (VEN-10). Se puede vender una
  presentacion por unidad (una bolsa de harina cerrada) y cualquier producto por unidades o MTO. Un
  producto base con presentaciones no se vende (400): su stock vive en existencias, que la venta no
  toca (PRODUCTO-14, VENTA-23).
- **VENTA-20. Sin costo de venta.** La linea de venta no guarda costo (`costoTotal` desaparece) y
  el sistema no calcula ganancia por venta. Lo que importa es lo que paga el cliente: subtotal =
  `precio x cantidad - descuento vigente` y total = suma de subtotales (VENTA-06).

### 2.2 SesionCaja (turno de caja)

- **SESIONCAJA-01. Una sola caja abierta en todo el local** (409, CAJ-20).
- **SESIONCAJA-02. Apertura.** A nombre del usuario de la sesion, que es el titular y responsable
  del dinero, con `montoApertura >= 0`.
- **SESIONCAJA-03. Movimientos de caja** (CAJ-21). `INGRESO` o `EGRESO`, `monto > 0`, motivo
  obligatorio de hasta 255 caracteres, solo en la caja abierta (409). Son inmutables.
- **SESIONCAJA-04. Arqueo y cierre** (CAJ-22). `esperado = apertura + efectivo de las ventas
  completadas + ingresos - egresos`; `diferencia = declarado - esperado`. Cuenta el `monto` del
  pago, no lo recibido (el cambio ya salio). `montoCierreDeclarado >= 0`, `motivoDiferencia`
  opcional; enviar `montoCierreEsperado` o `diferencia` responde 400. La sesion queda `CERRADA` y
  ya no admite cambios.
- **SESIONCAJA-05. Solo el titular o un Administrador cierran la caja** (403, CAJ-23).
- **SESIONCAJA-06. Las ventas anuladas no suman al esperado** (CAJ-24): se asume que al anular se
  devolvio el dinero. El arqueo las cuenta aparte y desglosa lo cobrado por tipo de pago, incluidos
  los tipos sin uso y los dados de baja despues de cobrar.
- **SESIONCAJA-07. Una sesion de caja no se da de baja** (409).
- **SESIONCAJA-08. Estado de la caja** (CAJ-25): si hay turno abierto, desde cuando, el fondo y el
  nombre del responsable.
- **SESIONCAJA-09. Concurrencia.** Las ventas toman la sesion con bloqueo compartido y no se
  bloquean entre si; cierre y movimientos la toman en exclusiva, de modo que un cierre espera a las
  ventas en curso y ninguna venta nueva entra en una caja que se esta cerrando.
- **SESIONCAJA-10. Un egreso no deja la caja en negativo.** Un `EGRESO` que dejaria el efectivo
  esperado (apertura + efectivo de las ventas completadas + ingresos - egresos) por debajo de cero
  responde 409.
- **SESIONCAJA-11. Anular una venta de una caja ya cerrada esta permitido** (por ejemplo, un reclamo
  al dia siguiente) y no corrige el arqueo guardado de esa caja: la anulacion queda registrada con
  su fecha y se reflejara en los reportes.
- **SESIONCAJA-12. Historial de turnos.** La lista de sesiones de caja se ordena de la mas reciente
  a la mas antigua (por fecha de apertura).

### 2.3 Descuento

- **DESCUENTO-01. Tipo aplicable.** Solo `Porcentaje` o `Monto fijo`; otro tipo responde 409.
- **DESCUENTO-02. Valores.** `descuento > 0`; un porcentaje no supera 100; `fechaInicio <=
  fechaFinal` (ambas obligatorias en el alta). Se revalida tras cada modificacion.
- **DESCUENTO-03. Vigencia.** Un descuento aplica a una venta si esta activo (`estado`), vivo, es
  del producto y `fechaInicio <= fechaVenta <= fechaFinal`.
- **DESCUENTO-04. Importe sobre una linea.** Porcentaje: `precio x cantidad x valor / 100`. Monto
  fijo: `min(valor, precio) x cantidad`. Nunca negativo ni mayor que `precio x cantidad`.

### 2.4 Catalogos de ventas (TipoPago, EstadoVenta, TipoDescuento)

- **CATALOGO-VEN-01.** CRUD sin reglas propias. Se siembran: tipos de pago `Efectivo`, `Tarjeta`,
  `QR`; estados de venta `Completada`, `Anulada`; tipos de descuento `Porcentaje`, `Monto fijo`.
  `Efectivo` es el unico tipo de pago con semantica propia (da cambio y cuenta en el arqueo).

---

## 3. Contexto Pedidos

### 3.1 Pedido

- **PEDIDO-01. Un pedido es la entrega a domicilio de una venta ya completada** (VEN-50). El
  stock salio al vender, no al despachar.
- **PEDIDO-02.** Solo se crea sobre una venta `Completada` y una venta tiene a lo sumo un pedido
  (409, VEN-63).
- **PEDIDO-03. Alta.** Nace `Recibido` (VEN-51); `idEstado` no se envia (400). `fechaEntrega`,
  `latitud` y `longitud` obligatorias.
- **PEDIDO-04. Transiciones de estado.** `Recibido -> En preparacion | Cancelado`;
  `En preparacion -> En camino | Cancelado`; `En camino -> Entregado | Cancelado`. `Entregado` y
  `Cancelado` son finales: el pedido ya no admite ningun cambio (409). Otra transicion, 409.
- **PEDIDO-05. Cancelar anula la venta** en la misma transaccion (VEN-52): con reintegro solo si el
  pedido estaba `Recibido` (nada se preparo); sin reintegro en los demas. El reintegro devuelve
  las unidades a sus lineas de lote (VENTA-22, LOTE-18).
- **PEDIDO-06.** La venta de un pedido no se cambia (`idVenta` en el PUT, 400).
- **PEDIDO-07. Estados sembrados:** `Recibido`, `En preparacion`, `En camino`, `Entregado`,
  `Cancelado` (se comparan sin distinguir mayusculas).
- **PEDIDO-08. Un pedido no se da de baja** (409): la unica salida es cancelarlo (PEDIDO-04,
  PEDIDO-05), que anula su venta.

---

## 4. Contexto Personal

### 4.1 Persona

- **PERSONA-01. No tiene alta propia ni endpoint.** Se crea y actualiza dentro de Usuario, Cliente
  y Proveedor, en la misma transaccion. Si se envia un `idPersona` existente, se reutiliza esa
  persona y se refrescan sus datos; asi una misma persona puede ser usuario, cliente y proveedor.

### 4.2 Usuario

- **USUARIO-01.** Solo un Administrador crea, modifica o da de baja usuarios.
- **USUARIO-02.** Un usuario tiene un rol existente y su contrasena se guarda hasheada (PBKDF2).
  Se pueden cambiar correo, contrasena y rol; la persona no se reapunta, se actualiza en sitio.
- **USUARIO-03.** Un usuario dado de baja no inicia sesion ni renueva su token (SEGURIDAD-03).

### 4.3 Rol

- **ROL-01. Los roles son filas de la tabla `rol`**, no constantes. Su nombre viaja en el token y
  es contra el que se comprueban los permisos. Se siembran `Administrador` y `Operador`. Solo un
  Administrador los gestiona.

### 4.4 Cliente

- **CLIENTE-01. Cliente 'Mostrador'.** Tiene id fijo (`00000000-0000-0000-0000-000000000001`), es
  el de toda venta sin cliente identificado, lo crea la siembra y no se da de baja (409).
- **CLIENTE-02.** Un cliente es una persona; al modificarlo se actualizan los datos de su persona.

### 4.5 Proveedor y Entidad

- **PROVEEDOR-01.** Un proveedor es una persona de contacto y, opcionalmente, una entidad que debe
  existir. La entidad se puede reapuntar; enviarla nula la deja como estaba.
- **ENTIDAD-01.** Empresa (nombre, NIT, telefono, descripcion). CRUD sin reglas propias.

---

## 5. Autenticacion y siembra (Shared)

- **SEGURIDAD-01. Login con correo y contrasena.** Mismo 401 si el correo no existe o la
  contrasena no coincide, para no revelar que correos estan registrados.
- **SEGURIDAD-02. Login con Google.** Google solo acredita la identidad: el correo debe pertenecer
  a un usuario ya registrado (401 si no) y nunca se crea un usuario.
- **SEGURIDAD-03. Renovacion de token.** Vuelve a leer usuario y rol: un usuario dado de baja deja
  de renovar y un cambio de rol se aplica al renovar. Un usuario sin rol valido, 403.
- **SEGURIDAD-04.** La autenticacion es de solo lectura: no escribe nada en la base.
- **SIEMBRA-01.** `POST api/v1/seed` es idempotente: siembra lo que falta (roles, administrador
  `admin@gmail.com`, catalogos, unidades, conversiones y cliente 'Mostrador') y nunca reescribe la
  contrasena del administrador. Es publica mientras `SEED_HABILITADO` sea verdadero; si no, solo
  la usa un Administrador.

---

## 6. Pendientes de decision

Conflictos o vacios detectados. **No se resuelven sin consultar al usuario**; al decidir cada uno
se registra la decision en su agregado y se retira de aqui. Resueltos el 2026-10-20: P-01
(MOVIMIENTO-14, EXISTENCIA-13, VENCIMIENTO-01), P-10 (PRODUCTO-11, PRODUCTO-16, PRODUCTO-13,
VENTA-23), P-11 (VENTA-20), P-12 (VENTA-22), P-13 (MOVIMIENTO-14), P-15 (EXISTENCIA-16), P-16
(PRODUCTO-14, PRODUCCION-12, MOVIMIENTO-18), P-17 (LOTE-08, LOTE-14), P-18 (VENTA-21) y P-19
(LOTE-13), P-20 (GENERAL-11, PRODUCCION-12, MOVIMIENTO-18), P-02 (MOVIMIENTO-17), P-03
(CATALOGO-INV-05), P-04 (PRODUCCION-11), P-05 (PEDIDO-08), P-06 (LOTE-16), P-07 (PRODUCTO-15),
P-08 (GENERAL-12) y P-09 (SESIONCAJA-10, SESIONCAJA-11).

No hay pendientes abiertos.

---

## 7. Pendientes de implementacion

Decisiones ya acordadas que el codigo de `src/main` todavia no cumple. Hoy no hay ninguna.

Implementadas el 2026-10-20: I-01 (tipo `Receta` renombrado a `Produccion`, solo como tipo de
movimiento), I-02 (el pesaje registra `Merma` o `Ajuste` segun el signo de la diferencia), I-03
(retirados merma libre y vencimientos de existencia y de lote) e I-04 (la apertura registra un
movimiento `Apertura` de cantidad 0). En una base ya sembrada, `POST api/v1/seed` agrega
`Produccion` y `Apertura`; la fila antigua `Receta` queda en el catalogo con sus movimientos
historicos.

Implementadas el 2026-10-20 (segunda tanda): I-09 (`POST api/v1/movimiento` responde 409), I-10
(bajas que respetan `esBorrable`; la siembra crea categorias no borrables; en una base ya sembrada
las categorias existentes conservan su valor), I-11 (una produccion no se da de baja), I-12 (un
pedido no se da de baja), I-13 (el PUT de un producto con inventario no cambia su naturaleza, su
unidad ni su padre) e I-14 (un egreso no deja la caja en negativo).

Implementadas el 2026-10-20 (tercera tanda): I-05 (dos clases de stock: contenido abierto para un
producto base con presentaciones y unidades disponibles en lotes para el resto; la compra rechaza
lineas de un producto base con presentaciones; la apertura resta una bolsa a la presentacion), I-06
(la venta descuenta unidades de los lotes, rechaza sin stock vigente, no registra movimientos ni
costo, admite presentaciones y rechaza un producto base con presentaciones; la anulacion con
reintegro devuelve las unidades a sus lineas), I-07 (la produccion no calcula costo y deja el
terminado como unidades de su lote, sin existencia ni movimiento de entrada), I-08 (pesaje por
producto base en `POST api/v1/producto/{id}/pesaje`; se retira `POST api/v1/existencia/{id}/ajuste`)
e I-15 (unidades descontadas y devueltas en las lineas de lote). Despues se retiro la columna
`detalle_venta.costo_total` del mapeo; en una base existente se elimina con
`ALTER TABLE detalle_venta DROP COLUMN costo_total;` (`ddl-auto: update` no borra columnas).
