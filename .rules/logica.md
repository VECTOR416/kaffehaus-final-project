# Logica de negocio de KaffeHaus v4

Este archivo es la **fuente de la verdad de la logica de negocio**. `arquitectura.md` dice *como*
se construye el sistema; este archivo dice *que reglas* cumple.

**Reglas de uso**

1. Cada decision tiene un identificador estable `<AGREGADO>-NN`. No se renumera ni se reutiliza.
   Una decision nueva toma el siguiente numero libre de su seccion. Una decision que se retira
   no se borra: se marca `(Derogada por <ID>)`.
2. Las decisiones no pueden contradecirse. Antes de cualquier cambio se revisan las de los
   agregados afectados; si el cambio entra en conflicto con alguna, se consulta al usuario como
   resolverlo y despues se actualiza este archivo.
3. Las referencias entre parentesis (`INV-xx`, `VEN-xx`, `CAJ-xx`) son los identificadores que
   cita el codigo en sus comentarios. Los `CAJ-xx` estan en `arquitectura.md` 4.11; los
   documentos `inventario.md` y `ventas.md` que definian los `INV-xx` y `VEN-xx` ya no existen
   en el repositorio (ver P-08).
4. Los conflictos detectados y aun sin resolver estan en la seccion 6, "Pendientes de decision".

Extraido del codigo de `src/main` el 2026-10-20.

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
  venta (`Completada`, `Anulada`), estados de pedido, tipos de movimiento (`Receta`, `Ajuste`,
  `Merma`), tipos de descuento (`Porcentaje`, `Monto fijo`) y tipo de pago `Efectivo`. Si un flujo
  necesita una fila que no esta registrada responde 404 nombrandola.
- **GENERAL-05. Atomicidad.** Toda operacion que mueve inventario o dinero ocurre en una sola
  transaccion: si una parte falla (por ejemplo, falta stock), no queda nada registrado a medias.
- **GENERAL-06. Orden de bloqueos.** Sesion de caja -> existencias (lineas procesadas en orden de
  id de producto) -> producto, siempre en ese orden, para no generar ciclos de bloqueo (INV-53).
- **GENERAL-07. Escalas y redondeo.** Dinero a 2 decimales con `HALF_UP`. Contenido de
  existencias, cantidades de movimientos, cantidades de ingredientes y costo de una linea de venta
  a 4 decimales. Los calculos intermedios (factores, costo por unidad de contenido) usan mas
  precision y se redondean al final.
- **GENERAL-08. Permisos.** Todo endpoint de agregado exige rol `Operador` o `Administrador`.
  Solo `Administrador`: crear, actualizar y eliminar `usuario`, `rol` y `producto`; anular una
  `venta`; ajustar una `existencia`; actualizar un `lote`. Cerrar la caja: ver SESIONCAJA-05.
- **GENERAL-09. Vencimiento.** Algo con fecha de vencimiento esta vencido si esa fecha es
  anterior a hoy (el dia de vencimiento aun es vigente). Sin fecha de vencimiento, nunca vence.

---

## 1. Contexto Inventario

### 1.1 Producto

- **PRODUCTO-01. Dos naturalezas.** `esStockDirecto = true` (MTS, reventa, insumo): tiene
  existencias propias y al venderse descuenta de ellas. `esStockDirecto = false` (MTO, bajo
  demanda): no tiene existencias; al venderse descuenta los insumos de su receta (backflush).
- **PRODUCTO-02. El stock es una proyeccion** (INV-20, INV-58): la suma del contenido actual de
  las existencias vivas con contenido del producto y de sus presentaciones, contando solo las que
  estan en la `unidadActual` del producto. Nace en 0, no se modifica a mano (`stock` en el PUT
  responde 400) y nunca es negativo. Solo lo recalcula la operacion interna de recalculo, que
  invocan todas las operaciones que cambian contenido. Un producto MTO no tiene stock propio.
- **PRODUCTO-03. Presentacion.** Un producto con `productoPadre` es una presentacion: la forma de
  compra de un producto base (INV-06). Lo que contiene una presentacion pertenece al producto
  base (INV-08).
- **PRODUCTO-04. Jerarquia de un solo nivel** (INV-07): el padre no puede ser una presentacion,
  un producto no puede ser su propio padre y un producto que ya es base de presentaciones no
  puede tener padre (409).
- **PRODUCTO-05. Precio de lista.** Lo fija un Administrador en el producto; la venta lo copia en
  el momento de vender (VENTA-06).
- **PRODUCTO-06. Bajo minimo.** Un producto esta bajo minimo cuando `stock < stockMinimo`.
- **PRODUCTO-07. Consulta de inventario de un producto** (INV-30, INV-31, INV-46). Es una lectura
  sin bloqueos; la validacion definitiva la hace la venta en su transaccion (INV-54).
  - MTS: saldo abierto (existencias), saldo cerrado (unidades cerradas de los lotes por el
    contenido que daria abrirlas; las que no tienen conversion posible no cuentan), valor
    (contenido abierto por su costo por unidad de contenido), disponible abierto vigente y
    disponible total vigente (abierto + cerrado, sin lo vencido).
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
  lotes de produccion los crea exclusivamente la produccion (PRODUCCION-04). El origen no cambia:
  dar proveedor a un lote de produccion responde 400.
- **LOTE-02. Un lote tiene al menos una linea viva** (400).
- **LOTE-03. Total derivado.** `total = suma(cantidadInicial x precioUnitario)` de las lineas
  vivas. No se recibe, se recalcula al mutar lineas y no cambia por consumo, apertura ni ajuste de
  unidades restantes. Es el costo historico de la compra.
- **LOTE-04. Linea de lote.** Producto existente obligatorio; `cantidadInicial > 0`;
  `precioUnitario >= 0` (precio por unidad comprada); `fraccionable` por defecto falso;
  `fechaVencimiento` opcional. `fechaCompra` por defecto es ahora.
- **LOTE-05. Unidades cerradas.** `cantidadRestante` cuenta las unidades aun cerradas. En una
  compra nace igual a `cantidadInicial`; en un lote de produccion nace en 0 porque se abre entero.
  Siempre `0 <= cantidadRestante <= cantidadInicial`, y la cantidad inicial no puede corregirse
  por debajo de la restante.
- **LOTE-06. Modificacion.** El PUT concilia lineas por id (con id se corrige, sin id se agrega,
  ausente se da de baja). No se cambia el producto de una linea existente (400). `cantidadRestante`
  solo se admite en el PUT, como ajuste explicito de inventario. El usuario que registro la compra
  no se corrige.
- **LOTE-07. No se da de baja una linea ni un lote con existencias con contenido** (409, INV-63):
  su stock quedaria sin origen. Primero se consumen o se dan de baja con merma o vencimiento.
- **LOTE-08. Apertura elegida por el usuario** (INV-10, INV-13, INV-14, INV-34). El sistema nunca
  abre unidades por su cuenta. Se abre una unidad entera (`cantidadRestante >= 1`) de un lote vivo;
  un detalle vencido no se abre (se registra su vencimiento, LOTE-11). Debe abrirse el detalle que
  toca de esa presentacion segun LOTE-09; si se pide otro, 409 indicando cual corresponde.
- **LOTE-09. Orden de apertura (FEFO).** Entre las lineas vivas de lotes vivos con unidades
  cerradas y no vencidas: primero la que vence antes (sin vencimiento al final), despues la del
  lote mas antiguo (`fechaCompra`) y, como desempate, la de menor id.
- **LOTE-10. Consulta de aperturables.** Se consulta por producto base (una presentacion responde
  400). Devuelve una fila por fuente (el propio producto y cada presentacion) con el detalle que
  toca abrir, sus unidades cerradas, el contenido de una unidad y su costo por unidad de
  contenido, ordenadas por LOTE-09. La primera es la sugerida; el usuario puede elegir otra fuente.
- **LOTE-11. Vencimiento de unidades cerradas** (INV-42). Solo de un detalle vencido con unidades
  cerradas: cada unidad entera se abre y su contenido se da de baja de inmediato con un movimiento
  `Merma`, dejando la existencia `Agotada`.
- **LOTE-12. Costo por unidad de contenido.** Compra: `precioUnitario / contenidoInicial` de la
  existencia abierta. Produccion: el propio `precioUnitario` de la linea.

### 1.4 Existencia

- **EXISTENCIA-01. No se crean a mano** (409, INV-12). Una existencia nace solo al abrir una unidad
  cerrada (LOTE-08), al registrar una produccion (PRODUCCION-04) o al reintegrar sobre una
  existencia agotada (MOVIMIENTO-07). Asi todo contenido lo explica un movimiento.
- **EXISTENCIA-02. Apertura.** Crea una existencia `Abierta` del producto base: contenido = factor
  de conversion de la unidad de la presentacion a la del base (sin conversion o con contenido nulo,
  409), en la unidad actual del base y con el vencimiento del detalle. Descuenta la unidad del lote,
  registra un movimiento `Ajuste` de entrada y recalcula el stock del base.
- **EXISTENCIA-03. Consumo.** La cantidad consumida es mayor a cero y no supera el contenido actual
  (409). El contenido nunca queda negativo. Al llegar a 0 registra `fechaAcabado` y pasa a
  `Agotada`.
- **EXISTENCIA-04. Una existencia agotada no revive** (INV-05): ni por ajuste ni por reintegro.
- **EXISTENCIA-05. Merma** (INV-42): cantidad mayor a cero con su motivo; movimiento `Merma`.
- **EXISTENCIA-06. Vencimiento** (INV-42): solo de una existencia vencida y no agotada; da de baja
  todo su contenido con un movimiento `Merma` (motivo por defecto "Vencimiento").
- **EXISTENCIA-07. Ajuste por conteo fisico** (INV-43, solo Administrador): el contado es `>= 0`
  y no supera el contenido inicial (409); no se ajusta una agotada. La diferencia se registra como
  movimiento `Ajuste` por su valor absoluto; si el conteo coincide no se registra nada.
- **EXISTENCIA-08. Modificacion.** El PUT solo admite `fechaVencimiento`; cualquier otro campo
  responde 400 (el contenido cambia por apertura, merma o ajuste).
- **EXISTENCIA-09. Baja.** Solo de una existencia agotada (409 si aun tiene contenido).
- **EXISTENCIA-10. Bloqueo.** Toda operacion que cambia el contenido bloquea la existencia antes de
  leerla (INV-52) y recalcula despues el stock del producto base.

### 1.5 Movimiento (libro mayor de inventario)

- **MOVIMIENTO-01. Inmutable.** PUT y DELETE responden 409; ni siquiera hay baja logica. Un error se
  corrige con un movimiento compensatorio.
- **MOVIMIENTO-02. Cantidad siempre positiva.** Es una magnitud; el tipo clasifica y la direccion
  (entrada o salida) se lee del motivo.
- **MOVIMIENTO-03. Tipos.** `Receta`: insumos que consume una receta (produccion o venta MTO) y
  entrada del producto terminado de una produccion. `Ajuste`: apertura, salida por venta de un
  producto MTS y ajuste por conteo. `Merma`: mermas, vencimientos y reintegros vencidos.
- **MOVIMIENTO-04.** `produccionId` solo se informa en los consumos y la entrada de una produccion.
- **MOVIMIENTO-05.** Un movimiento no guarda el detalle de venta (INV-33): los de una venta se
  enlazan por su motivo, `Venta '<id>'`.
- **MOVIMIENTO-06. Salida de inventario** (INV-32), unica regla de descuento, usada por produccion y
  venta:
  - MTS: consume de las existencias abiertas, con contenido y no vencidas a la fecha de la
    operacion, del producto y de sus presentaciones, en la unidad del producto, en orden FEFO
    (vence antes, abierta antes, menor id), bloqueadas, repartiendo entre varias si hace falta. Un
    movimiento por existencia tocada. Recalcula el stock.
  - MTO: explota la receta vigente (RECETA-06) escalada a `cantidad / rendimiento` y recurre por
    cada ingrediente. MTO sin receta, 409; ciclo entre recetas, 409.
  - Si el stock abierto no alcanza: 409 y la operacion entera revierte. Si hay unidades cerradas
    vigentes que alcanzarian, el mensaje pide abrir una presentacion. Nunca abre por su cuenta.
  - Devuelve el costo de lo consumido (cantidad x costo por unidad de contenido, LOTE-12).
- **MOVIMIENTO-07. Reintegro** (VEN-42, VEN-43). Por cada salida citada por el motivo de origen,
  un compensatorio del mismo tipo: si la existencia sigue viva se le suma sin superar su contenido
  inicial; si esta agotada se abre una existencia nueva sobre el mismo detalle, con la misma unidad
  y vencimiento. Si lo reintegrado esta vencido se da de baja en el acto como `Merma`. Es todo o
  nada, bloquea las existencias en orden de id y recalcula el stock de cada producto base tocado.

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
  reciente (por fecha de creacion). Es la que usan el backflush y la disponibilidad MTO.

### 1.7 Produccion (MTS por lotes)

- **PRODUCCION-01.** Solo se produce un producto MTS; producir un MTO responde 409.
- **PRODUCCION-02. Datos.** `cantidadEsperada > 0`; `cantidadReal >= 0` (0 = tanda perdida);
  `diferencia = cantidadReal - cantidadEsperada`, derivada; receta y `fechaVencimiento`
  obligatorias; `fechaProduccion` por defecto es ahora.
- **PRODUCCION-03. Consumo de insumos.** Cada ingrediente de la receta por
  `cantidadEsperada / rendimiento`, con MOVIMIENTO-06 y movimientos `Receta` vinculados a la
  produccion. Si falta stock de un insumo no queda produccion, consumo ni lote.
- **PRODUCCION-04. Producto terminado.** Si `cantidadReal > 0`: lote de produccion con una sola
  linea a costo unitario = costo de los insumos / `cantidadReal`, sin unidades cerradas; una
  existencia `Abierta` con `cantidadReal` en la unidad del producto y el vencimiento indicado; un
  movimiento `Receta` de entrada vinculado a la produccion; recalculo del stock. Desde ahi se vende o
  se consume como cualquier producto MTS.
- **PRODUCCION-05. Una produccion registrada es fija.** Solo se corrigen `motivoDiferencia` y
  `fechaProduccion`; cantidades, diferencia y receta responden 400.

### 1.8 Catalogos de inventario (Tipo, Categoria, EstadoExistencia, TipoMovimiento)

- **CATALOGO-INV-01.** Son CRUD sin reglas propias. Se siembran: tipos `Producto` e `Insumo`;
  categorias `Cafes`, `Bebidas`, `Pasteleria`, `Insumos`; estados de existencia `Abierta` y
  `Agotada`; tipos de movimiento `Receta`, `Ajuste` y `Merma`.

---

## 2. Contexto Ventas

### 2.1 Venta

- **VENTA-01. Nace `Completada` con su inventario ya descontado**, en la misma transaccion. Si
  falta stock responde 409 y no queda venta, pagos ni movimientos (VEN-01, VEN-21, VEN-22).
- **VENTA-02. Requiere caja abierta** (409 si no la hay). Pertenece a la sesion abierta en la que
  se cobro, que se toma con bloqueo compartido antes que el inventario (CAJ-01).
- **VENTA-03. Siempre tiene cliente.** Sin `idCliente`, la venta es del cliente 'Mostrador'
  (CAJ-02). El usuario de la venta es el empleado de la sesion que cobra.
- **VENTA-04. Lineas.** Al menos una. Cada linea admite solo `idProducto` y `cantidad > 0`. El
  producto debe existir, ser vendible (400) y no ser una presentacion: se vende su producto base
  (400) (VEN-10, VEN-11).
- **VENTA-05. Fecha.** `fechaVenta` opcional, por defecto ahora. Define la vigencia de descuentos
  y que existencias estan vencidas al descontar.
- **VENTA-06. Importes derivados** (VEN-13, VEN-14). Precio = precio de lista del producto;
  descuento = el vigente de mayor importe para el cliente (no se acumulan); subtotal =
  `precio x cantidad - descuento`; total = suma de subtotales. Enviar `total`, `idEstadoVenta`,
  `idSesionCaja`, `numeroTicket`, `precioUnitario`, `descuentoAplicado`, `subtotal` o `costoTotal`
  responde 400. El descuento nunca supera el importe de la linea.
- **VENTA-07. Descuento de inventario.** Cada linea, en orden de id de producto, pasa por
  MOVIMIENTO-06: un MTS con movimientos `Ajuste`, un MTO con movimientos `Receta` de sus insumos;
  el motivo cita la venta (MOVIMIENTO-05). El costo devuelto queda en la linea; el costo de la venta
  es la suma (VEN-23).
- **VENTA-08. Idempotencia** (VEN-66). Una `claveIdempotencia` ya usada devuelve la venta existente
  sin crear otra, aunque su caja ya se haya cerrado.
- **VENTA-09. Inmutable** (VEN-30). Lo unico que se corrige es el cliente (VEN-31): el PUT solo
  admite `idCliente` y cualquier otro campo responde 400. Una venta anulada no admite cambios (409,
  VEN-32).
- **VENTA-10. No se da de baja** (409, VEN-33): se anula.
- **VENTA-11. Anulacion** (VEN-40..VEN-44). La pide un Administrador o la provoca la cancelacion de
  un pedido (PEDIDO-05). Solo una venta `Completada`, siempre con motivo; registra quien anula.
  Lineas, importes, costos, pagos y movimientos se conservan. `reintegrar` es opcional (falso):
  sin reintegro el inventario no cambia (VEN-41); con reintegro se aplica MOVIMIENTO-07 y, si una
  parte no se puede reintegrar, la anulacion entera falla y la venta sigue `Completada` (VEN-43).
- **VENTA-12. Ticket** (CAJ-14). El numero lo asigna la base (secuencia unica). Se busca por numero
  positivo (otro valor, 400) y devuelve cliente, cajero y tipos de pago resueltos.

**Pagos (entidad hija de Venta)**

- **VENTA-13. Pago mixto** (CAJ-10): una venta admite varios pagos de distintos tipos.
- **VENTA-14. Los pagos suman exactamente el total** (400 si no, CAJ-11). Una venta de total cero
  no lleva pagos.
- **VENTA-15. Cada pago.** Tipo de pago existente y `monto > 0`. En efectivo, `montoRecibido` (por
  defecto igual al monto) debe cubrir el monto y `cambio = montoRecibido - monto` (CAJ-12). En
  cualquier otro tipo, un `montoRecibido` distinto del monto responde 400 (CAJ-13). `cambio` nunca
  se envia (400).
- **VENTA-16. Los pagos son inmutables**, como la venta.

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
  inventario salio al vender, no al despachar.
- **PEDIDO-02.** Solo se crea sobre una venta `Completada` y una venta tiene a lo sumo un pedido
  (409, VEN-63).
- **PEDIDO-03. Alta.** Nace `Recibido` (VEN-51); `idEstado` no se envia (400). `fechaEntrega`,
  `latitud` y `longitud` obligatorias.
- **PEDIDO-04. Transiciones de estado.** `Recibido -> En preparacion | Cancelado`;
  `En preparacion -> En camino | Cancelado`; `En camino -> Entregado | Cancelado`. `Entregado` y
  `Cancelado` son finales: el pedido ya no admite ningun cambio (409). Otra transicion, 409.
- **PEDIDO-05. Cancelar anula la venta** en la misma transaccion (VEN-52): con reintegro de
  inventario solo si el pedido estaba `Recibido` (nada se preparo); sin reintegro en los demas.
- **PEDIDO-06.** La venta de un pedido no se cambia (`idVenta` en el PUT, 400).
- **PEDIDO-07. Estados sembrados:** `Recibido`, `En preparacion`, `En camino`, `Entregado`,
  `Cancelado` (se comparan sin distinguir mayusculas).

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

Conflictos o vacios detectados al extraer este archivo. **No se resuelven sin consultar al
usuario**; al decidir cada uno se registra la decision en su agregado y se retira de aqui.

- **P-01. Catalogo de tipos de movimiento.** El codigo usa tres tipos (`Receta`, `Ajuste`,
  `Merma`) y lee la direccion del motivo (MOVIMIENTO-02, MOVIMIENTO-03). `arquitectura.md` 4.9
  describe nueve tipos (`Apertura`, `Consumo produccion`, `Ingreso produccion`, `Venta`,
  `Consumo venta`, `Merma`, `Vencimiento`, `Ajuste positivo`, `Ajuste negativo`, mas
  `Devolucion venta` en 4.6) y dice que la direccion la da el tipo. Hay que decidir cual vale y
  alinear el otro documento.
- **P-02. Alta manual de movimientos.** `POST api/v1/movimiento` registra un movimiento sin tocar
  ninguna existencia, lo que permite que el libro mayor deje de explicar el contenido
  (fundamento de EXISTENCIA-01 y MOVIMIENTO-01).
- **P-03. `esBorrable` sin efecto.** `Categoria`, `Unidad` y `Conversion` guardan `esBorrable`,
  pero ninguna baja lo consulta: hoy todo se puede dar de baja.
- **P-04. Baja de una produccion.** `DELETE api/v1/produccion/{id}` la marca borrada sin tocar su
  lote, su existencia ni sus movimientos, que siguen vivos (ver PRODUCCION-05).
- **P-05. Baja de un pedido.** `DELETE api/v1/pedido/{id}` no tiene restricciones de estado ni
  efecto sobre la venta, a diferencia de la cancelacion (PEDIDO-05).
- **P-06. Ajuste de unidades cerradas sin traza.** Corregir `cantidadRestante` en el PUT de un lote
  (LOTE-06) no registra ningun movimiento: las unidades cerradas quedan fuera del libro mayor.
- **P-07. Cambios de producto con inventario.** Cambiar `esStockDirecto` o `unidadActual` de un
  producto con existencias no esta restringido; las existencias en otra unidad dejan de contar en
  su stock (PRODUCTO-02) y un MTS pasado a MTO deja existencias huerfanas.
- **P-08. Documentos de reglas desaparecidos.** El codigo cita `inventario.md` (INV-xx) y
  `ventas.md` (VEN-xx), que no estan en el repositorio. Decidir si este archivo los sustituye y,
  en ese caso, si los comentarios del codigo deben pasar a citar los identificadores de aqui.
- **P-09. Limites conocidos de caja** (`arquitectura.md` 4.11). Anular una venta de una sesion ya
  cerrada no corrige su arqueo guardado, y no se valida que un egreso supere el efectivo
  disponible.
