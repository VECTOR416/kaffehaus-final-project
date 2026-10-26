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
Decisiones del usuario incorporadas el 2026-10-23 a partir de `sugerencias_logicas.md` (S-01, S-02,
S-03, S-04, S-10 y la parte en efectivo de S-06): plan de consumo y orden de bloqueos
(GENERAL-14, GENERAL-15), traza completa del inventario (MOVIMIENTO-19 a MOVIMIENTO-22, LOTE-23,
LOTE-24, EXISTENCIA-19), consumo de insumos al vender un MTO con apertura asistida (VENTA-24,
VENTA-30, PRODUCTO-19), reintegro exacto y devolucion del efectivo al anular (VENTA-25, VENTA-27,
SESIONCAJA-13, SESIONCAJA-14) y fecha de la venta (VENTA-28). Cierran P-21, P-25 y P-29, y
parte de P-24. Los numeros VENTA-26 y VENTA-29 quedan reservados para las propuestas de
`sugerencias_logicas.md` aun no decididas, igual que LOTE-25 y LOTE-26 (LOTE-27 se acordo despues,
el 2026-10-23, al resolver la concurrencia entre lineas de un mismo lote).
Segunda tanda de decisiones del usuario del 2026-10-23, a partir de S-05, S-06 (pagos que no son en
efectivo), S-07, S-08, S-09, S-11, S-12, S-13, S-14 y S-17: permisos (GENERAL-17, PEDIDO-09), bajas
con dependencias vivas (GENERAL-18, PRODUCTO-20, LOTE-26, RECETA-09, USUARIO-04), filas semanticas
(GENERAL-19), vencimientos (VENCIMIENTO-02 a VENCIMIENTO-05), reembolso y caja compartida (VENTA-27,
SESIONCAJA-15 a SESIONCAJA-17), cantidades enteras (GENERAL-16, LOTE-25), combinaciones invalidas
(PRODUCTO-18, PRODUCCION-13, RECETA-07, RECETA-08), descuentos (DESCUENTO-05) y credenciales de la
siembra (SIEMBRA-02). Cierran P-22, P-24, P-26 y P-27. Al decidirlas el usuario eligio la version
completa de GENERAL-17 de S-07, la LOTE-26 de S-08 (solo se da de baja un lote sin usar) y la
PRODUCTO-20 de S-08 (409 si es ingrediente de una receta viva; sus recetas y descuentos caen en
cascada). VENTA-26 y VENTA-29 siguen reservados.

---

## 0. Reglas generales (todos los agregados)

- **GENERAL-01. Borrado siempre logico.** Ninguna fila se elimina. `DELETE` marca `borrado = true`
  y las lecturas excluyen lo borrado. La baja de una raiz arrastra a sus entidades hijas.
- **GENERAL-02. Lo derivado lo calcula el sistema.** Totales, subtotales, costos, stock,
  diferencias, cambio, numero de paso, numero de ticket y estados iniciales nunca se aceptan del
  cliente. Donde cada agregado lo indica, enviarlos responde 400. La fecha de una venta tambien la
  fija el sistema, con la unica excepcion de contingencia de VENTA-28.
- **GENERAL-03. Quien opera es el usuario de la sesion.** El `idUsuario` sale del token; el
  cliente no lo envia y, si llega en el cuerpo, se ignora. Nadie opera a nombre de otro.
- **GENERAL-04. Catalogos con semantica se reconocen por nombre**, nunca por constantes de id:
  roles (`Administrador`, `Operador`), estados de existencia (`Abierta`, `Agotada`), estados de
  venta (`Completada`, `Anulada`), estados de pedido, tipos de movimiento (`Apertura`,
  `Produccion`, `Venta`, `Reintegro`, `Merma`, `Ajuste`, `Vencimiento`, ver MOVIMIENTO-22), tipos
  de descuento (`Porcentaje`, `Monto fijo`) y tipo de pago
  `Efectivo`. Si un flujo necesita una fila que no esta registrada responde 404 nombrandola.
- **GENERAL-05. Atomicidad.** Toda operacion que mueve inventario o dinero ocurre en una sola
  transaccion: si una parte falla (por ejemplo, falta stock), no queda nada registrado a medias.
- **GENERAL-06.** *(Derogada por GENERAL-15.)* Orden de bloqueos sin plan de consumo agregado.
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
  (`PRODUCTO-16`, `VENTA-24`, `SESIONCAJA-04`...). Las citas antiguas (`INV-xx`, `VEN-xx`, `CAJ-xx`
  y las que remiten a una seccion de `arquitectura.md` por una regla de negocio) se reemplazan a
  medida que se modifica cada archivo, no en un cambio masivo aparte. Equivalencia de las
  `CAJ-xx`: CAJ-01 -> VENTA-02; CAJ-02 -> VENTA-03; CAJ-10 -> VENTA-13; CAJ-11 -> VENTA-14;
  CAJ-12 y CAJ-13 -> VENTA-15; CAJ-14 -> VENTA-12; CAJ-20 -> SESIONCAJA-01; CAJ-21 ->
  SESIONCAJA-03; CAJ-22 -> SESIONCAJA-04; CAJ-23 -> SESIONCAJA-05; CAJ-24 -> SESIONCAJA-06;
  CAJ-25 -> SESIONCAJA-08.
- **GENERAL-13.** *(Derogada por GENERAL-17.)* Permisos sin la cancelacion de pedidos ni las bajas
  de recetas y catalogos.
- **GENERAL-14. Plan de consumo.** Toda operacion que descuenta inventario (venta, produccion)
  calcula primero, sin bloquear, su plan de consumo: explota recursivamente los MTO por su receta
  vigente (RECETA-06) escalada a `cantidad / rendimiento`, convierte cada requerimiento a la
  unidad actual de su insumo y **agrega por producto** (un insumo usado por dos lineas o por dos
  recetas aparece una sola vez, con la suma). Solo despues, y en orden de id de producto, bloquea y
  descuenta cada entrada del plan (GENERAL-15). Si cualquier entrada no alcanza, falla la operacion
  entera (GENERAL-05) informando **todas** las entradas que faltan, no solo la primera (VENTA-30).
- **GENERAL-15. Orden de bloqueos** (deroga GENERAL-06, INV-53; precisada el 2026-10-23 con
  LOTE-27). Sesion de caja -> los lotes que la operacion va a modificar, todos de una vez y en
  orden de id de lote (LOTE-27) -> por cada producto, en orden de id de producto: sus existencias
  y despues el propio producto -> nada mas. Ningun caso de uso bloquea filas de inventario fuera
  de este orden, tampoco el reintegro (VENTA-25). Si aun asi la base detecta un interbloqueo o
  vence la espera de un bloqueo, la operacion responde 409 pidiendo reintentar.
- **GENERAL-16. Lo que se cuenta por unidades es entero en los documentos** (cierra P-27). La
  `cantidad` de una linea de venta, la `cantidadInicial` de una linea de compra (al darla de alta y
  al corregirla) y las cantidades esperada y real de una produccion son enteras: con decimales
  responde 400. Las fracciones solo existen en las recetas (medio pan en un sandwich, gramos de
  cafe) y en lo que queda en inventario: por eso `cantidadRestante` puede quedar fraccionaria, el
  descuento de unidades sigue admitiendo fracciones (LOTE-17) y la correccion por conteo (LOTE-23)
  y el pesaje (EXISTENCIA-16) aceptan decimales. Lo que ya esta registrado no se revalida.
- **GENERAL-17. Permisos** (deroga GENERAL-13). Todo endpoint de agregado exige rol `Operador` o
  `Administrador`. Solo `Administrador`:
  - crear, actualizar y eliminar `usuario`, `rol` y `producto`;
  - anular una `venta` y cancelar un `pedido`, que la anula (PEDIDO-09);
  - registrar un pesaje (EXISTENCIA-18) y actualizar un `lote`, incluida la correccion de unidades
    (LOTE-23);
  - cambiar el vencimiento de una existencia (VENCIMIENTO-05);
  - dar de baja una `receta` y las filas de los catalogos: `tipo`, `categoria`, `unidad`,
    `conversion`, `estado de existencia`, `tipo de movimiento`, `tipo de pago`, `estado de venta`,
    `tipo de descuento` y `estado de pedido`;
  - registrar un `EGRESO` en una caja de la que no es titular (SESIONCAJA-15);
  - fijar la `fechaVenta` de una venta de contingencia (VENTA-28).
  Cerrar la caja: SESIONCAJA-05. La baja por vencimiento la hace tambien un Operador
  (VENCIMIENTO-03). Cuando el permiso depende del contenido de la peticion y no del endpoint
  (cancelar un pedido, registrar un egreso, fijar la fecha de la venta), lo comprueba el caso de
  uso con el rol del usuario de la sesion y responde 403.
- **GENERAL-18. No se da de baja lo que otro registro vivo necesita para operar** (409 nombrando
  quien lo usa).
  - `Tipo`, `Categoria` y `Unidad`: si un producto vivo los usa. `Unidad` y `Conversion` siguen
    ademas protegidas por `esBorrable` (CATALOGO-INV-05).
  - `TipoDescuento`: si un descuento vivo lo usa.
  - `TipoPago`: se puede dar de baja (los pagos historicos lo conservan, SESIONCAJA-06), salvo
    `Efectivo` (GENERAL-19).
  - `Rol`: si un usuario vivo lo tiene.
  - `Proveedor` y `Cliente`: se pueden dar de baja; sus lotes y ventas los conservan como historia.
    'Mostrador' nunca (CLIENTE-01).
  - Producto, lote, receta y usuario tienen sus propias reglas: PRODUCTO-20, LOTE-26, RECETA-09 y
    USUARIO-04. Siguen vigentes las que ya impedian bajas: LOTE-07, EXISTENCIA-09, MOVIMIENTO-01,
    PRODUCCION-11, VENTA-10, SESIONCAJA-07, PEDIDO-08 y CLIENTE-01.
- **GENERAL-19. Filas semanticas.** Las filas de catalogo que el sistema reconoce por su nombre
  (GENERAL-04) no se renombran ni se dan de baja (409): roles `Administrador` y `Operador`; estados
  de existencia `Abierta` y `Agotada`; estados de venta `Completada` y `Anulada`; estados de pedido
  `Recibido`, `En preparacion`, `En camino`, `Entregado` y `Cancelado`; tipos de movimiento
  `Produccion`, `Ajuste`, `Merma`, `Apertura`, `Venta`, `Reintegro` y `Vencimiento`; tipos de
  descuento `Porcentaje` y `Monto fijo`; tipo de pago `Efectivo`. El nombre se compara sin
  distinguir mayusculas; reenviar el mismo nombre no es renombrar. Se pueden agregar filas nuevas a
  esos catalogos (otro tipo de pago, otro rol), que no tienen semantica. Los estados de una sesion
  de caja (`ABIERTA`, `CERRADA`) no son un catalogo: son valores fijos del agregado.

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
- **PRODUCTO-08.** *(Derogada por PRODUCTO-19.)* Dos naturalezas, con un MTO cuya venta no
  descontaba nada.
- **PRODUCTO-09.** *(Derogada por PRODUCTO-11.)* `producto.stock` como contador de unidades para
  todo producto.
- **PRODUCTO-10.** *(Derogada por PRODUCTO-16.)* Lo que sube y baja el stock, sin distinguir el
  producto base.
- **PRODUCTO-11. Dos clases de stock.**
  - **Producto base con presentaciones** (por ejemplo, cafe o harina): su stock es la suma del
    contenido de sus existencias abiertas vivas, en su unidad. Sube al abrir una bolsa
    (EXISTENCIA-13), baja cuando la produccion o la venta consumen de el (MOVIMIENTO-19) y con la
    baja por vencimiento (VENCIMIENTO-03), sube con el reintegro de una venta anulada
    (EXISTENCIA-19) y cambia con el pesaje (EXISTENCIA-16).
  - **Cualquier otro producto MTS** (presentaciones, reventa, producto terminado): su stock es un
    contador de unidades (PRODUCTO-16).
  En ambos casos no se modifica a mano (`stock` en el PUT responde 400). Un MTO no tiene stock.
- **PRODUCTO-12.** *(Derogada por PRODUCTO-16.)* Stock de unidades como contador propio.
- **PRODUCTO-16. Stock de unidades.** Es la suma de las unidades disponibles
  (`cantidadRestante`) de las lineas vivas de los lotes vivos del producto. Sube al comprar un lote
  (comprar 3 bolsas de cafe suma 3 a la bolsa) y al producir (PRODUCCION-12). Baja con la venta
  (VENTA-24), cuando una receta lo usa como insumo al producir o al vender un MTO (MOVIMIENTO-19),
  al abrir una unidad (una bolsa menos, EXISTENCIA-13) y con la baja por vencimiento
  (VENCIMIENTO-03). Corregir la cantidad de una linea de lote (LOTE-23) o darla de baja se refleja
  solo. Al anular una venta con reintegro, las unidades vuelven a las lineas de las que salieron
  (VENTA-25, LOTE-24). Se recalcula despues de cada cambio, sin sumar ni restar sobre el valor
  leido. Es stock fisico: incluye lo vencido aun no dado de baja (VENCIMIENTO-02).
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
  `fraccionable` de una linea de lote tampoco interviene (LOTE-25).
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
  - Stock fisico y vigente: ver VENCIMIENTO-02.
- **PRODUCTO-18. Un MTO no tiene inventario propio.** Un producto con `esStockDirecto = false` no
  puede ser presentacion ni producto base de presentaciones (409 al crearlo, al cambiarlo o al
  crear una presentacion que lo tenga por padre), no se compra (linea de lote de un MTO, 400) ni se
  produce (PRODUCCION-01). Su disponibilidad es la de sus insumos (PRODUCTO-17).
- **PRODUCTO-19. Dos naturalezas** (deroga PRODUCTO-08). `esStockDirecto = true` (MTS, reventa,
  insumo): tiene stock (PRODUCTO-11) y, si es un producto base con presentaciones, existencias.
  `esStockDirecto = false` (MTO, bajo demanda): no tiene stock propio; su venta consume los insumos
  de su receta vigente (VENTA-24, MOVIMIENTO-19) y su disponibilidad es la de esos insumos
  (PRODUCTO-17).
- **PRODUCTO-20. Baja de un producto** (GENERAL-18). Responde 409 si tiene stock distinto de 0,
  lineas de lote vivas con unidades, existencias con contenido abiertas desde sus lineas,
  presentaciones vivas, o si es ingrediente de una receta viva (nombrandola). Lo agotado no impide
  la baja: es historia. Sus recetas vivas (como producto obtenido) y sus descuentos vivos se dan de
  baja en la misma transaccion.

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
- **LOTE-04. Linea de lote.** Producto existente obligatorio; `cantidadInicial > 0` y entera
  (GENERAL-16); `precioUnitario >= 0` (precio por unidad comprada); `fraccionable` por defecto
  falso y sin efecto (LOTE-25); `fechaVencimiento` opcional. `fechaCompra` por defecto es ahora.
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
  vigentes no alcanzan, 409 (`STOCK_INSUFICIENTE`, VENTA-30). Antes de descontar bloquea los
  lotes de esas lineas (LOTE-27). Registra un movimiento por linea tocada, por la cantidad que le toco, del tipo de la
  operacion (`Venta` o `Produccion`) y con su documento (MOVIMIENTO-20, MOVIMIENTO-21).
- **LOTE-18.** *(Derogada por LOTE-24.)* Devolucion de unidades a cualquier linea con espacio.
- **LOTE-06. Modificacion.** El PUT concilia lineas por id (con id se corrige, sin id se agrega,
  ausente se da de baja). No se cambia el producto de una linea existente (400). `cantidadRestante`
  solo se admite en el PUT, como correccion por conteo (LOTE-23). El usuario que registro la compra
  no se corrige.
- **LOTE-07. No se da de baja una linea ni un lote con existencias con contenido** (409, INV-63):
  su stock quedaria sin origen. Primero se consumen o se ajustan por pesaje (EXISTENCIA-16).
  Ampliada por LOTE-26.
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
- **LOTE-16.** *(Derogada por LOTE-23.)* Correccion de bolsas cerradas sin registrar movimiento.
- **LOTE-13. Que se compra en un lote.** Una linea de compra es de una presentacion o de un
  producto por unidades (PRODUCTO-14). Una linea de un producto base con presentaciones responde
  400: para comprarlo suelto se registra una presentacion (por ejemplo, "Harina suelta 1 kg"). Una
  linea de un MTO tambien responde 400 (PRODUCTO-18).
- **LOTE-23. Correccion de unidades por conteo** (deroga LOTE-16, cierra P-29). Corregir
  `cantidadRestante` de una linea en el PUT de un lote (LOTE-06, solo Administrador) exige
  `motivo` (400 si falta) y registra sobre esa linea un movimiento `Merma` si se contaron menos o
  `Ajuste` si se contaron mas, por la diferencia y con ese motivo (MOVIMIENTO-22). Si la cantidad
  no cambia no registra nada. Es el equivalente del pesaje (EXISTENCIA-16) para lo que se cuenta
  por unidades. El stock se recalcula (PRODUCTO-16).
- **LOTE-24. Devolucion de unidades** (deroga LOTE-18). El reintegro de una venta anulada
  (VENTA-25) devuelve a cada linea exactamente lo que salio de ella segun sus movimientos `Venta`,
  aunque la linea ya este vencida. La devolucion nunca deja `cantidadRestante` por encima de
  `cantidadInicial` (409); como vuelve lo que salio, solo puede fallar si la linea se corrigio
  despues (LOTE-23). Cada devolucion registra un movimiento `Reintegro` sobre la linea.
- **LOTE-25. `fraccionable` no interviene en ninguna regla** (GENERAL-16). Se conserva en la
  tabla y en el contrato por compatibilidad; ninguna operacion lo consulta.
- **LOTE-26. Baja de un lote** (amplia LOTE-07, GENERAL-18). Solo se da de baja un lote de compra
  que no se uso: todas sus lineas vivas con `cantidadRestante = cantidadInicial` y ninguna
  existencia nacida de ellas, este como este. Sirve para corregir un registro equivocado; lo usado
  se corrige con LOTE-23. Un lote de produccion no se da de baja (409), como su produccion
  (PRODUCCION-11). La misma regla aplica a dar de baja una linea en el PUT (LOTE-06): 409 si la
  linea ya se uso o si el lote es de produccion.
- **LOTE-27. El lote se bloquea entero.** El lote es la unidad de consistencia de sus lineas:
  toda operacion que modifica un lote existente (descuento de unidades por venta o produccion,
  reintegro, apertura, baja por vencimiento, PUT y baja) bloquea el lote por su raiz antes de leer sus lineas, de modo
  que ninguna otra operacion cambia una linea del mismo lote mientras tanto. Una operacion que
  modifica varios lotes los bloquea todos de una vez, en orden de id de lote (GENERAL-15). La
  salida elige que lotes bloquear con una lectura previa sin bloqueo (los que cubren lo
  requerido en el orden de LOTE-17); si con el bloqueo tomado ya no alcanzan, lo que falta se
  informa como faltante (VENTA-30) y el cliente reintenta. Crear un lote no bloquea nada.

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
  responde 400 (el contenido cambia por apertura, produccion o pesaje, EXISTENCIA-16). Solo lo
  hace un Administrador y la fecha no puede pasar la de su linea de lote (VENCIMIENTO-05).
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
  **cantidad 0** (MOVIMIENTO-22): solo deja constancia de que se abrio, sin mover contenido. El
  stock de la presentacion baja una unidad y el del producto base sube en el contenido de la bolsa
  (PRODUCTO-11, PRODUCTO-16). El usuario decide desde su pantalla que bolsa abrir cuando le hace
  falta mas insumo base (LOTE-08).
- **EXISTENCIA-14. Bloqueo.** Toda operacion que cambia el contenido bloquea la existencia antes de
  leerla (INV-52).
- **EXISTENCIA-15. Una existencia agotada solo revive por pesaje o por reintegro.** Si el pesaje
  dice que hay mas (EXISTENCIA-16), una existencia agotada puede volver a tener contenido: vuelve a
  `Abierta` y pierde su `fechaAcabado`. La otra unica operacion que la revive es el reintegro de
  una venta anulada (EXISTENCIA-19).
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
  presentaciones (PRODUCTO-14). Su contenido solo cambia por el consumo de la produccion o de la
  venta (MOVIMIENTO-19), por el reintegro de una venta anulada (EXISTENCIA-19), por el pesaje del
  usuario (EXISTENCIA-16) y por la baja por vencimiento (VENCIMIENTO-03).
- **EXISTENCIA-19. Reintegro a una existencia** (amplia EXISTENCIA-15). El reintegro de una venta
  anulada (VENTA-25) devuelve a cada existencia exactamente lo que salio de ella segun sus
  movimientos `Venta`, aunque ya este vencida, y registra un movimiento `Reintegro` sobre ella. Una
  existencia `Agotada` que recibe un reintegro vuelve a `Abierta` y pierde su `fechaAcabado`. El
  contenido nunca supera el inicial (409); como vuelve lo que salio, solo puede fallar si un pesaje
  posterior (EXISTENCIA-16) la relleno. La bloquea antes de leerla (EXISTENCIA-14).

### 1.5 Movimiento (trazabilidad del inventario)

- **MOVIMIENTO-01. Inmutable.** PUT y DELETE responden 409; ni siquiera hay baja logica. Un error se
  corrige con un movimiento compensatorio.
- **MOVIMIENTO-02.** *(Derogada por MOVIMIENTO-08.)* Cantidad siempre positiva.
- **MOVIMIENTO-03.** *(Derogada por MOVIMIENTO-14.)* Tipos `Receta`, `Ajuste` y `Merma` con su uso
  anterior.
- **MOVIMIENTO-04.** `produccionId` solo se informa en los movimientos de una produccion, y
  `ventaId` solo en los de una venta o de su anulacion (MOVIMIENTO-21). Nunca los dos a la vez.
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
- **MOVIMIENTO-18.** *(Derogada por MOVIMIENTO-19.)* Consumo de insumos usado solo por la
  produccion, sin movimientos para los productos por unidades.
- **MOVIMIENTO-17. No hay alta manual de movimientos.** `POST api/v1/movimiento` responde 409,
  igual que su PUT y su DELETE (MOVIMIENTO-01): un movimiento solo lo genera la operacion que
  explica (MOVIMIENTO-20). Los movimientos solo se consultan.
- **MOVIMIENTO-14.** *(Derogada por MOVIMIENTO-22.)* Cuatro tipos de movimiento, solo sobre
  existencias.
- **MOVIMIENTO-15.** *(Derogada por MOVIMIENTO-20.)* Proposito limitado a las existencias de los
  productos base.
- **MOVIMIENTO-19. Salida de inventario** (deroga MOVIMIENTO-18, INV-32). Es la unica forma de
  descontar inventario de la produccion (PRODUCCION-06) y de la venta (VENTA-24). Recibe todas las
  lineas de la operacion de una vez (producto, cantidad), su tipo (`Produccion` o `Venta`), su
  documento y su fecha, y aplica el plan de consumo (GENERAL-14) en el orden de GENERAL-15. Por
  cada entrada del plan, segun el producto (PRODUCTO-14):
  - **Producto base con presentaciones** (harina, leche): consume de sus existencias abiertas, con
    contenido y no vencidas a la fecha de la operacion, en la unidad del producto, en orden FEFO
    (primero la bolsa abierta mas proxima a vencerse; despues la abierta antes y la de menor id),
    bloqueadas, repartiendo entre varias si hace falta. El sistema elige la existencia, no el
    usuario. Un movimiento por existencia tocada.
  - **Producto por unidades o presentacion** (pan, bolsa cerrada): descuenta unidades de sus
    lineas de lote (LOTE-17), con un movimiento por linea tocada.
  - **MTO:** ya llega explotado en el plan; no se descuenta por si mismo. MTO sin receta viva, 409;
    ciclo entre recetas, 409 nombrando el ciclo.
  Si una entrada no alcanza, la operacion entera responde 409 con un error estructurado
  (VENTA-30): `APERTURA_REQUERIDA` si todo lo que falta se resuelve abriendo bolsas cerradas
  vigentes, `STOCK_INSUFICIENTE` en cualquier otro caso. Nunca abre bolsas por su cuenta
  (LOTE-08). Solo mueve cantidades: no calcula el dinero de lo consumido (GENERAL-11).
- **MOVIMIENTO-20. Proposito** (deroga MOVIMIENTO-15). Los movimientos son la traza de **todo** el
  inventario: toda variacion de `existencias.contenido_actual` o de
  `detalle_lote.cantidad_restante` posterior a la creacion de esa fila registra un movimiento, en
  la misma transaccion que la variacion. Las filas nacen por su propio documento, que es su traza
  de origen: la compra (`lote`), la produccion (`lote` de produccion, PRODUCCION-12) y la apertura
  (la existencia apunta a la linea que se abrio, EXISTENCIA-13). La unidad que la apertura
  descuenta de su linea queda trazada por el movimiento `Apertura` de la existencia nacida; no
  genera un segundo movimiento.
- **MOVIMIENTO-21. Sujeto y documento del movimiento.** Cada movimiento apunta a **exactamente
  uno** de dos sujetos: una existencia (`idExistencia`) o una linea de lote (`idDetalleLote`).
  Ademas puede llevar el documento que lo explica: `idProduccion` o `idVenta` (MOVIMIENTO-04). Es
  una invariante del agregado `Movimiento`.
- **MOVIMIENTO-22. Tipos y sentido** (deroga MOVIMIENTO-14). Cada tipo tiene un sentido fijo; la
  cantidad sigue siendo una magnitud mayor a cero (MOVIMIENTO-08):

  | Tipo | Sentido | Lo genera | Sobre |
  |------|---------|-----------|-------|
  | `Apertura` | ninguno, cantidad 0 | abrir una presentacion (EXISTENCIA-13) | existencia |
  | `Produccion` | salida | consumo de insumos al producir (MOVIMIENTO-19) | existencia o linea |
  | `Venta` | salida | consumo al vender (VENTA-24, MOVIMIENTO-19) | existencia o linea |
  | `Reintegro` | entrada | devolucion al anular una venta (VENTA-25) | existencia o linea |
  | `Merma` | salida | el pesaje o el conteo encontro menos (EXISTENCIA-16, LOTE-23) | existencia o linea |
  | `Ajuste` | entrada | el pesaje o el conteo encontro mas (EXISTENCIA-16, LOTE-23) | existencia o linea |
  | `Vencimiento` | salida | baja de lo vencido decidida por el usuario (VENCIMIENTO-03) | existencia o linea |

  `Produccion` sustituye al antiguo tipo `Receta`; es solo el nombre del tipo de movimiento: el
  agregado `Receta`, su dominio y sus tablas conservan su nombre. No existen `Ajuste positivo` ni
  `Ajuste negativo`: el tipo da el sentido.

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
- **RECETA-07. Validaciones al guardar** (amplia RECETA-03). Al crear o modificar una receta, el
  producto obtenido no puede ser uno de sus ingredientes vivos (400), y la receta no puede formar un
  ciclo con las recetas vigentes de sus ingredientes MTO, recorridas recursivamente (409 nombrando
  el ciclo). La validacion de MOVIMIENTO-19 se mantiene como defensa.
- **RECETA-08. Un MTO vendible necesita receta.** Vender un MTO sin receta viva responde 409
  (VENTA-24), y no se da de baja la ultima receta viva de un MTO vendible (RECETA-09).
- **RECETA-09. Baja de una receta** (GENERAL-18; solo Administrador, GENERAL-17). Se da de baja
  salvo que sea la ultima receta viva de un MTO vendible (409, RECETA-08). Las producciones que la
  usaron la conservan como historia.

### 1.7 Produccion (MTS por lotes)

- **PRODUCCION-01.** Solo se produce un producto MTS; producir un MTO responde 409.
- **PRODUCCION-02. Datos.** `cantidadEsperada > 0`; `cantidadReal >= 0` (0 = tanda perdida); ambas
  enteras (GENERAL-16);
  `diferencia = cantidadReal - cantidadEsperada`, derivada; receta y `fechaVencimiento`
  obligatorias; `fechaProduccion` por defecto es ahora.
- **PRODUCCION-03.** *(Derogada por PRODUCCION-06.)* Consumo de insumos con movimientos `Receta`.
- **PRODUCCION-04.** *(Derogada por PRODUCCION-12.)* Producto terminado con movimiento `Receta` y
  recalculo del stock.
- **PRODUCCION-05. Una produccion registrada es fija.** Solo se corrigen `motivoDiferencia` y
  `fechaProduccion`; cantidades, diferencia y receta responden 400.
- **PRODUCCION-06. Consumo de insumos.** Cada ingrediente de la receta por
  `cantidadEsperada / rendimiento`, entregados juntos a MOVIMIENTO-19, con movimientos `Produccion`
  (MOVIMIENTO-22) vinculados a la produccion, tanto sobre existencias como sobre lineas de lote. Si
  falta un insumo no queda produccion, consumo ni lote, y el error es el de VENTA-30.
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
- **PRODUCCION-13. Que se produce** (amplia PRODUCCION-01). El producto obtenido de una produccion
  es MTS y se maneja por unidades o es una presentacion (PRODUCTO-14). Un producto base con
  presentaciones no se produce (409): para producirlo suelto se registra una presentacion, igual
  que para comprarlo (LOTE-13).

### 1.8 Catalogos de inventario (Tipo, Categoria, EstadoExistencia, TipoMovimiento)

- **CATALOGO-INV-01.** *(Derogada por CATALOGO-INV-03.)* Siembra con tipos de movimiento `Receta`,
  `Ajuste` y `Merma`.
- **CATALOGO-INV-02.** *(Derogada por CATALOGO-INV-03.)* Siembra sin el tipo `Apertura`.
- **CATALOGO-INV-03.** *(Derogada por CATALOGO-INV-04 y CATALOGO-INV-05.)* Catalogos sin reglas
  propias.
- **CATALOGO-INV-04.** Tipo, EstadoExistencia y TipoMovimiento son CRUD sin mas reglas que las
  generales de bajas (GENERAL-18), filas semanticas (GENERAL-19) y permisos (GENERAL-17). Se
  siembran: tipos `Producto` e `Insumo`; categorias `Cafes`, `Bebidas`, `Pasteleria`, `Insumos`;
  estados de existencia `Abierta` y `Agotada`; tipos de movimiento `Produccion`, `Ajuste`, `Merma`,
  `Apertura`, `Venta`, `Reintegro` y `Vencimiento` (MOVIMIENTO-22).
- **CATALOGO-INV-05. `esBorrable`.** Una `Categoria`, `Unidad` o `Conversion` con
  `esBorrable = false` no se da de baja (409): protege los registros base de los que dependen otros
  flujos (por ejemplo, sin la conversion kg -> g no se pueden abrir bolsas). Lo que crea la siembra
  nace con `esBorrable = false`.

### 1.9 Vencimientos

- **VENCIMIENTO-01.** *(Derogada por VENCIMIENTO-02 a VENCIMIENTO-05, cierra P-22.)* Vencimientos
  sin disenar y sin baja de lo vencido.

Siguen vigentes GENERAL-09 (vencido es anterior a hoy), que un detalle vencido no se abre (LOTE-08)
y que ni la produccion ni la venta consumen existencias o unidades vencidas (MOVIMIENTO-19).

- **VENCIMIENTO-02. Stock fisico y stock vigente.** `producto.stock` es el stock **fisico**: lo que
  hay en el local, vencido o no. No depende del reloj, porque es una proyeccion que solo se
  recalcula al operar (PRODUCTO-16). Lo vendible o consumible es el stock **vigente**, que se
  calcula en la consulta, nunca se guarda, y que cada operacion vuelve a validar. La consulta de
  inventario (PRODUCTO-17) devuelve, para un MTS, `stock` (fisico), `stockVigente` (su saldo
  abierto no vencido a hoy) y `stockVencido` (la diferencia), para que el frontend no muestre como
  disponible lo vencido. Un MTO no tiene stock: ambos son nulos.
- **VENCIMIENTO-03. Baja por vencimiento, decidida por el usuario.** El sistema nunca da de baja
  por su cuenta (mismo criterio que LOTE-08). Un Operador o un Administrador da de baja:
  - las unidades restantes de una linea de lote vencida
    (`POST api/v1/lote/detalles/{idDetalleLote}/vencimiento`): `cantidadRestante` pasa a 0;
  - el contenido de una existencia vencida (`POST api/v1/existencia/{id}/vencimiento`):
    `contenidoActual` pasa a 0 y queda `Agotada` con `fechaAcabado`.
  Solo se da de baja lo que esta vencido a hoy segun GENERAL-09 (409 si sigue vigente) y tiene algo
  que dar de baja (409 si ya esta en 0). `motivo` es opcional (por defecto "Baja por
  vencimiento"). Registra un movimiento `Vencimiento` por la cantidad dada de baja, con el usuario
  de la sesion (MOVIMIENTO-20, MOVIMIENTO-22), y recalcula el stock del producto: el de la linea, o
  el producto base de la existencia (PRODUCTO-11, PRODUCTO-16). Bloquea el lote por su raiz
  (LOTE-27) o la existencia (EXISTENCIA-14) y despues el producto (GENERAL-15). Responde el lote o
  la existencia actualizados.
- **VENCIMIENTO-04. Alerta de vencimiento** (`GET api/v1/lote/vencimientos?dias=3`). Lista las
  lineas vivas de lotes vivos con unidades disponibles y las existencias vivas con contenido cuya
  fecha de vencimiento es anterior o igual a hoy + `dias` (incluye lo ya vencido). `dias` es un
  entero `>= 0`, por defecto 3 (otro valor, 400). Cada fila dice si es una linea o una existencia,
  su id, su lote y su linea, el producto (el de la linea, o el producto base de la existencia), la
  cantidad y su unidad, la fecha de vencimiento y si ya esta vencida. Se ordena por fecha de
  vencimiento (y por id como desempate). Lo que no vence no aparece. Es una lectura sin bloqueos y
  la puede pedir un Operador.
- **VENCIMIENTO-05. Una existencia no vence despues que su bolsa.** El PUT de una existencia
  (EXISTENCIA-08, solo Administrador) no puede poner una fecha de vencimiento posterior a la de la
  linea de lote de la que se abrio (409). Si la linea no vence, cualquier fecha es valida. Asi no
  se "rejuvenece" un insumo para poder usarlo.

---

## 2. Contexto Ventas

### 2.1 Venta

- **VENTA-01. Nace `Completada` con su stock ya descontado**, en la misma transaccion (VEN-01,
  VEN-21, VENTA-24). Si la venta falla no queda venta ni pagos (VEN-22).
- **VENTA-02. Requiere caja abierta** (409 si no la hay). Pertenece a la sesion abierta en la que
  se cobro, que se toma con bloqueo compartido antes que el inventario (CAJ-01).
- **VENTA-03. Siempre tiene cliente.** Sin `idCliente`, la venta es del cliente 'Mostrador'
  (CAJ-02). El usuario de la venta es el empleado de la sesion que cobra.
- **VENTA-04.** *(Derogada por VENTA-21.)* Prohibicion de vender presentaciones.
- **VENTA-05.** *(Derogada por VENTA-28.)* `fechaVenta` opcional y libre.
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
  un pedido (PEDIDO-05, PEDIDO-09). Solo una venta `Completada`, siempre con motivo; registra quien
  anula. Lineas, importes y pagos se conservan. `reintegrar` es opcional (falso); su efecto lo
  dice VENTA-25. La devolucion del dinero, incluida la `referenciaReembolso`, la dice VENTA-27.
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
- **VENTA-22.** *(Derogada por VENTA-25.)* Reintegro de unidades a cualquier linea con espacio, sin
  devolver nada de un MTO.
- **VENTA-19.** *(Derogada por VENTA-23.)* Venta que resta de un contador de stock.
- **VENTA-23.** *(Derogada por VENTA-24.)* Venta que solo descontaba productos por unidades y no
  registraba movimientos.
- **VENTA-21. Que se vende.** Al menos una linea. Cada linea admite solo `idProducto` y
  `cantidad > 0` entera (GENERAL-16). El producto debe existir y ser vendible (400) (VEN-10). Se puede vender una
  presentacion por unidad (una bolsa de harina cerrada) y cualquier producto por unidades o MTO. Un
  producto base con presentaciones no se vende directamente (400): su contenido solo sale como
  insumo de la receta de un MTO (VENTA-24) o de una produccion (PRODUCTO-14).
- **VENTA-20. Sin costo de venta.** La linea de venta no guarda costo (`costoTotal` desaparece) y
  el sistema no calcula ganancia por venta. Lo que importa es lo que paga el cliente: subtotal =
  `precio x cantidad - descuento vigente` y total = suma de subtotales (VENTA-06).
- **VENTA-24. Lo que descuenta la venta** (deroga VENTA-23, cierra P-21). La venta entrega todas
  sus lineas de una vez a la salida de inventario (MOVIMIENTO-19), que arma el plan de consumo
  (GENERAL-14):
  - un producto por unidades o una presentacion descuenta sus unidades de las lineas de lote
    (LOTE-17);
  - un MTO explota su receta vigente escalada a `cantidad / rendimiento` y consume sus insumos
    (existencias o unidades, recursivamente). Un MTO sin receta viva no se vende (409).
  Cada descuento registra un movimiento `Venta` con el `idVenta` (MOVIMIENTO-21). Lo vencido se
  juzga a la fecha de la venta (VENTA-28). Si algo no alcanza, la venta entera se rechaza con 409
  segun VENTA-30 (PRODUCTO-13).
- **VENTA-25. Reintegro al anular** (deroga VENTA-22). Con `reintegrar = true`, por cada movimiento
  `Venta` de esa venta se registra un movimiento `Reintegro` con su `idVenta` que devuelve la misma
  cantidad **al mismo sujeto**: la misma linea de lote (LOTE-24) o la misma existencia
  (EXISTENCIA-19). Incluye los insumos de los MTO: con PEDIDO-05 solo se reintegra cuando el pedido
  estaba `Recibido`, es decir, cuando nada se preparo. Procesa los sujetos en el orden de
  GENERAL-15. Si no se puede devolver todo, la anulacion entera falla (409) y la venta sigue
  `Completada`. Sin reintegro el inventario no cambia. Una venta sin movimientos `Venta`
  (anterior a la traza completa) no admite reintegro automatico (409, provisional, P-30).
- **VENTA-27. Devolucion del dinero al anular** (cierra P-24). Al anular se devuelve el monto
  pagado en `Efectivo` (la suma del `monto` de esos pagos, no lo recibido):
  - **Venta de la caja abierta:** no se registra nada mas; SESIONCAJA-06 ya la excluye del
    esperado.
  - **Venta de una caja ya cerrada:** se registra en la caja abierta, en la misma transaccion, un
    `EGRESO` por ese monto con motivo "Devolucion del ticket N" y el `idVenta` (SESIONCAJA-13). Si
    no hay caja abierta, la anulacion responde 409 pidiendo abrirla; si el egreso dejaria la caja
    en negativo, 409 (SESIONCAJA-10). Si la venta no tuvo pagos en efectivo, no se toca la caja.
  - **Tarjeta, QR u otro medio:** no tocan la caja. La anulacion (y la cancelacion de un pedido,
    PEDIDO-09) acepta una `referenciaReembolso` opcional, texto de hasta 255 caracteres (por
    ejemplo, el numero de la operacion de reverso), que queda guardada en la venta como constancia
    del reembolso hecho fuera del sistema. No se exige ni se valida contra los pagos.
- **VENTA-28. Fecha de la venta y contingencia** (deroga VENTA-05, cierra P-25; acordada con el
  usuario el 2026-10-23).
  - Por defecto toda venta toma la fecha y hora del servidor en el momento de registrarse.
  - Un usuario sin rol `Administrador` que envie `fechaVenta` recibe 400: no puede fijarla.
  - Un `Administrador` puede enviar una `fechaVenta` manual para asentar ventas fisicas atrasadas
    (corte de luz, falla de red), siempre que `fechaApertura de la caja abierta <= fechaVenta <=
    ahora + 1 minuto` (el minuto tolera el desfase de relojes). Fuera de ese rango, 400.
  - La venta de contingencia se asienta siempre en la caja abierta que toma la venta (VENTA-02),
    nunca en una cerrada, de modo que su arqueo la incluye.
  - La fecha manual es la fecha de la venta a todos los efectos: define los descuentos vigentes
    (DESCUENTO-03) y el criterio de vencimiento del consumo (VENTA-24).
  - `fec_c` sigue registrando el instante real de creacion: la diferencia entre `fec_c` y
    `fecha_venta` identifica las ventas de contingencia y el usuario de la venta, al Administrador
    que las asento. No hay columna nueva.
  El permiso depende del cuerpo de la peticion, no del endpoint: lo comprueba el caso de uso con el
  rol del usuario de la sesion.
- **VENTA-30. Apertura asistida en dos pasos** (cierra P-21 junto con VENTA-24; acordada con el
  usuario el 2026-10-23). Cuando una operacion necesita un insumo base cuyo contenido abierto
  vigente no alcanza pero existen bolsas cerradas vigentes que alcanzarian, el sistema **no abre
  envases por su cuenta** (LOTE-08): revierte la transaccion entera y responde **409 con el codigo
  de error `APERTURA_REQUERIDA`** y el detalle de lo que falta. Con esos datos el cliente:
  1. abre la bolsa sugerida (`POST api/v1/lote/detalles/{idDetalleLote}/apertura`, EXISTENCIA-13,
     sin cambios);
  2. reintenta de inmediato la misma venta con la misma `claveIdempotencia`.
  Precisiones:
  - **Bolsa sugerida:** la primera fila de la lista de aperturables del insumo (LOTE-14), es decir
    la bolsa vigente mas proxima a vencer entre todas sus presentaciones. Al abrirla se aplica
    LOTE-08, asi que la sugerencia nunca contradice la apertura.
  - **Todos los faltantes a la vez:** como el plan de consumo evalua todo antes de descontar
    (GENERAL-14), el error lista **todos** los insumos que requieren apertura, cada uno con lo
    requerido, lo disponible abierto, su unidad, su bolsa sugerida y cuantas bolsas de esa
    presentacion hacen falta para cubrir el faltante (`bolsasNecesarias`).
  - **`STOCK_INSUFICIENTE`:** si algun faltante no se resuelve abriendo (un producto por unidades
    sin unidades vigentes suficientes, o un insumo base cuyo abierto mas sus bolsas cerradas
    vigentes no alcanza), el 409 lleva el codigo `STOCK_INSUFICIENTE`, lista todos los faltantes y
    no sugiere apertura.
  - **Idempotencia:** la venta rechazada no queda registrada (GENERAL-05), asi que la clave sigue
    libre y el reintento crea la venta una sola vez (VENTA-08). Si entre la apertura y el reintento
    otra operacion consume esa bolsa, el reintento puede volver a responder `APERTURA_REQUERIDA`;
    el cliente repite el ciclo.
  - **Produccion:** como la salida de inventario es comun (MOVIMIENTO-19), la produccion responde
    con los mismos errores estructurados.
  El formato del error estructurado esta en `arquitectura.md` 4.16.

### 2.2 SesionCaja (turno de caja)

- **SESIONCAJA-01. Una sola caja abierta en todo el local** (409, CAJ-20).
- **SESIONCAJA-02. Apertura.** A nombre del usuario de la sesion, que es el titular y responsable
  del dinero, con `montoApertura >= 0`.
- **SESIONCAJA-03. Movimientos de caja** (CAJ-21). `INGRESO` o `EGRESO`, `monto > 0`, motivo
  obligatorio de hasta 255 caracteres, solo en la caja abierta (409). Son inmutables. Los registra
  el usuario (con el permiso de SESIONCAJA-15) o, al anular una venta de un turno cerrado, el
  sistema (SESIONCAJA-13).
- **SESIONCAJA-04. Arqueo y cierre** (CAJ-22). `esperado = apertura + efectivo de las ventas
  completadas + ingresos - egresos`; `diferencia = declarado - esperado`. Cuenta el `monto` del
  pago, no lo recibido (el cambio ya salio). `montoCierreDeclarado >= 0`, `motivoDiferencia`
  opcional; enviar `montoCierreEsperado` o `diferencia` responde 400. La sesion queda `CERRADA` y
  ya no admite cambios.
- **SESIONCAJA-05. Solo el titular o un Administrador cierran la caja** (403, CAJ-23).
- **SESIONCAJA-06. Las ventas anuladas no suman al esperado** (CAJ-24): el efectivo de una venta
  anulada en su mismo turno se devuelve de esa caja; el de una venta de un turno ya cerrado, con el
  egreso de VENTA-27. El arqueo las cuenta aparte y desglosa lo cobrado por tipo de pago, incluidos
  los tipos sin uso y los dados de baja despues de cobrar.
- **SESIONCAJA-07. Una sesion de caja no se da de baja** (409).
- **SESIONCAJA-08. Estado de la caja** (CAJ-25): si hay turno abierto, desde cuando, el fondo, el
  nombre del responsable y si se abrio un dia anterior (SESIONCAJA-17).
- **SESIONCAJA-09. Concurrencia.** Las ventas toman la sesion con bloqueo compartido y no se
  bloquean entre si; cierre y movimientos la toman en exclusiva, de modo que un cierre espera a las
  ventas en curso y ninguna venta nueva entra en una caja que se esta cerrando.
- **SESIONCAJA-10. Un egreso no deja la caja en negativo.** Un `EGRESO` que dejaria el efectivo
  esperado (apertura + efectivo de las ventas completadas + ingresos - egresos) por debajo de cero
  responde 409.
- **SESIONCAJA-11. Anular una venta de una caja ya cerrada esta permitido** (por ejemplo, un reclamo
  al dia siguiente) y no corrige el arqueo guardado de esa caja (SESIONCAJA-14): la anulacion queda
  registrada con su fecha y el efectivo se devuelve desde la caja abierta (VENTA-27).
- **SESIONCAJA-12. Historial de turnos.** La lista de sesiones de caja se ordena de la mas reciente
  a la mas antigua (por fecha de apertura).
- **SESIONCAJA-13. Movimientos de caja generados por el sistema.** Un movimiento de caja puede
  llevar `idVenta`; solo lo pone el sistema, en el `EGRESO` de devolucion de VENTA-27, a nombre del
  usuario que anula. Cuenta en el arqueo como cualquier egreso (SESIONCAJA-04). El cliente no
  envia `idVenta` en un movimiento de caja (400).
- **SESIONCAJA-14. El arqueo guardado no cambia** (precisa SESIONCAJA-11). La devolucion se refleja
  en el turno en que se entrego el dinero, no en el turno de la venta.
- **SESIONCAJA-15. Quien opera la caja** (cierra P-26). Cualquier Operador o Administrador vende y
  registra `INGRESO` en la caja abierta: es normal que atiendan varias personas. Un `EGRESO`
  (sacar dinero) solo lo registra el titular del turno o un Administrador (403). El egreso de
  devolucion que genera el sistema al anular (SESIONCAJA-13) no pasa por esta regla: lo provoca una
  anulacion, que ya exige Administrador (GENERAL-17). Cada venta y cada movimiento guardan quien lo
  hizo.
- **SESIONCAJA-16. Arqueo por cajero.** El arqueo del cierre desglosa, ademas de por tipo de pago
  (SESIONCAJA-06), lo vendido por cada usuario del turno en las ventas completadas: su total, lo
  cobrado en efectivo y cuantas ventas hizo.
- **SESIONCAJA-17. Turno de un dia anterior.** Una caja abierta en un dia anterior sigue operativa
  (hay turnos que pasan la medianoche), pero el estado de la caja (SESIONCAJA-08) lo indica con
  `abiertaDesdeDiaAnterior` (la fecha de apertura es anterior a hoy) para que el frontend pida
  cerrarla. Sin caja abierta es falso.

### 2.3 Descuento

- **DESCUENTO-01. Tipo aplicable.** Solo `Porcentaje` o `Monto fijo`; otro tipo responde 409.
- **DESCUENTO-02. Valores.** `descuento > 0`; un porcentaje no supera 100; `fechaInicio <=
  fechaFinal` (ambas obligatorias en el alta). Se revalida tras cada modificacion.
- **DESCUENTO-03. Vigencia.** Un descuento aplica a una venta si esta activo (`estado`), vivo, es
  del producto y `fechaInicio <= fechaVenta <= fechaFinal`.
- **DESCUENTO-04. Importe sobre una linea.** Porcentaje: `precio x cantidad x valor / 100`. Monto
  fijo: `min(valor, precio) x cantidad`. Nunca negativo ni mayor que `precio x cantidad`.
- **DESCUENTO-05. Solo para lo que se vende.** El producto de un descuento es vendible y no es un
  producto base con presentaciones (409), al crearlo y al cambiarle el producto. Si el producto deja
  de ser vendible o se da de baja (PRODUCTO-20), sus descuentos vivos se dan de baja en la misma
  transaccion.

### 2.4 Catalogos de ventas (TipoPago, EstadoVenta, TipoDescuento)

- **CATALOGO-VEN-01.** CRUD sin mas reglas que las generales de bajas (GENERAL-18), filas
  semanticas (GENERAL-19) y permisos (GENERAL-17). Se siembran: tipos de pago `Efectivo`, `Tarjeta`,
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
  lo descontado a sus sujetos de origen (VENTA-25) y el efectivo se devuelve segun VENTA-27.
- **PEDIDO-06.** La venta de un pedido no se cambia (`idVenta` en el PUT, 400).
- **PEDIDO-07. Estados sembrados:** `Recibido`, `En preparacion`, `En camino`, `Entregado`,
  `Cancelado` (se comparan sin distinguir mayusculas).
- **PEDIDO-08. Un pedido no se da de baja** (409): la unica salida es cancelarlo (PEDIDO-04,
  PEDIDO-05), que anula su venta.
- **PEDIDO-09. Cancelar un pedido es anular una venta** (GENERAL-17). La transicion a `Cancelado`
  solo la hace un Administrador (403), igual que anular. Las demas transiciones (PEDIDO-04) siguen
  abiertas a Operador y Administrador. La cancelacion exige `motivo` (400 si falta), que pasa tal
  cual a la anulacion de la venta, y acepta la `referenciaReembolso` opcional de VENTA-27.

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
- **USUARIO-04. Baja de un usuario** (GENERAL-18). Nadie se da de baja a si mismo, ni se da de baja
  al ultimo usuario vivo con rol `Administrador`, ni al titular de la caja abierta (409).

### 4.3 Rol

- **ROL-01. Los roles son filas de la tabla `rol`**, no constantes. Su nombre viaja en el token y
  es contra el que se comprueban los permisos. Se siembran `Administrador` y `Operador`, que no se
  renombran ni se dan de baja (GENERAL-19); otro rol no se da de baja si un usuario vivo lo tiene
  (GENERAL-18). Solo un Administrador los gestiona.

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
- **SIEMBRA-02. Credenciales iniciales.** La contrasena del administrador inicial no esta en el
  codigo: se toma de la configuracion del entorno (`SEED_ADMIN_CONTRASENA`). Si el administrador
  aun no existe y la contrasena no esta configurada, la siembra responde 409 y no siembra nada; si
  ya existe, no la necesita (SIEMBRA-01). `SEED_HABILITADO` es falso por defecto: se activa solo
  para la primera siembra de una base nueva y se vuelve a desactivar despues.

---

## 6. Pendientes de decision

Conflictos o vacios detectados. **No se resuelven sin consultar al usuario**; al decidir cada uno
se registra la decision en su agregado y se retira de aqui. Resueltos el 2026-10-20: P-01
(MOVIMIENTO-14, EXISTENCIA-13, VENCIMIENTO-01), P-10 (PRODUCTO-11, PRODUCTO-16, PRODUCTO-13,
VENTA-23), P-11 (VENTA-20), P-12 (VENTA-22), P-13 (MOVIMIENTO-14), P-15 (EXISTENCIA-16), P-16
(PRODUCTO-14, PRODUCCION-12, MOVIMIENTO-18), P-17 (LOTE-08, LOTE-14), P-18 (VENTA-21) y P-19
(LOTE-13), P-20 (GENERAL-11, PRODUCCION-12, MOVIMIENTO-18), P-02 (MOVIMIENTO-17), P-03
(CATALOGO-INV-05), P-04 (PRODUCCION-11), P-05 (PEDIDO-08), P-06 (LOTE-16), P-07 (PRODUCTO-15),
P-08 (GENERAL-12) y P-09 (SESIONCAJA-10, SESIONCAJA-11). Resueltos el 2026-10-23: P-21
(VENTA-24, VENTA-30, PRODUCTO-19), P-25 (VENTA-28), P-29 (LOTE-23, MOVIMIENTO-20) y la parte en
efectivo de P-24 (VENTA-27, SESIONCAJA-13, SESIONCAJA-14). Resueltos el 2026-10-23 (segunda
tanda): P-22 (VENCIMIENTO-02 a VENCIMIENTO-05), el resto de P-24 (VENTA-27, `referenciaReembolso`),
P-26 (SESIONCAJA-15 a SESIONCAJA-17) y P-27 (GENERAL-16, LOTE-25).

Abiertos el 2026-10-23, al revisar si el sistema puede usarse en un entorno real. Las opciones
listadas son solo orientativas; ninguna esta elegida. El usuario aplazo P-23 y P-28 (S-15 y S-16)
a una etapa posterior.

- **P-23. Costos, ganancias y reportes.** GENERAL-11 deja el dinero para los reportes, que no
  estan definidos. Lo producido tiene costo 0 (PRODUCCION-12) y la venta no guarda costo
  (VENTA-20). Por decidir: que reportes se necesitan (ventas, ganancia, valoracion de inventario,
  mermas) y con que metodo se costea lo producido y lo vendido.
- **P-28. Facturacion e impuestos.** No hay ninguna decision sobre comprobantes fiscales ni
  impuestos. El ticket (VENTA-12) es un numero interno y la secuencia deja huecos cuando una venta
  se revierte. Por decidir: si el sistema debe emitir o registrar facturas, que datos fiscales
  pide del cliente y si los huecos de numeracion son aceptables.

Abierto el 2026-10-23, al implementar VENTA-25:

- **P-30. Reintegro de una venta sin traza.** VENTA-25 devuelve lo que dicen los movimientos
  `Venta` de la venta, pero las ventas registradas antes de la traza completa (MOVIMIENTO-20) no
  tienen ninguno, y no hay forma de saber a que linea o existencia devolver sin inventarlo.
  **Decision provisional del usuario (2026-10-23): las ventas previas a la migracion no admiten
  reintegro automatico.** Anular una venta sin movimientos `Venta` con `reintegrar = true`
  responde 409 pidiendo anularla sin reintegro; sin reintegro se anula normalmente. Queda abierto
  como pendiente hasta confirmar que no hay ventas antiguas que conservar; si las hay, decidir si
  se mantiene la regla o se define otra forma de devolverlas.

---

## 7. Pendientes de implementacion

Decisiones ya acordadas que el codigo de `src/main` todavia no cumple. Hoy no hay ninguna.

Implementadas el 2026-10-23 (cuarta tanda), con la segunda tanda de decisiones de ese dia:
I-24 (GENERAL-17: bajas de recetas y catalogos y PUT de existencia solo para Administrador;
cancelar un pedido exige Administrador y motivo, PEDIDO-09), I-25 (GENERAL-18, PRODUCTO-20,
LOTE-26, RECETA-09 y USUARIO-04: bajas rechazadas con dependencias vivas y cascada de recetas y
descuentos del producto), I-26 (GENERAL-19: filas semanticas protegidas en sus agregados), I-27
(VENCIMIENTO-02 a VENCIMIENTO-05: `stockVigente` y `stockVencido` en la consulta de inventario,
`POST api/v1/lote/detalles/{id}/vencimiento`, `POST api/v1/existencia/{id}/vencimiento`,
`GET api/v1/lote/vencimientos` y el tope del vencimiento de una existencia), I-28
(`venta.referencia_reembolso`, VENTA-27), I-29 (SESIONCAJA-15 a SESIONCAJA-17: egreso solo del
titular o de un Administrador, arqueo por cajero y `abiertaDesdeDiaAnterior`), I-30 (GENERAL-16:
cantidades enteras en venta, compra y produccion), I-31 (PRODUCTO-18, PRODUCCION-13, RECETA-07 y
DESCUENTO-05) e I-32 (SIEMBRA-02: contrasena inicial por `SEED_ADMIN_CONTRASENA` y
`SEED_HABILITADO` falso por defecto). En una base existente, `ddl-auto: update` agrega
`venta.referencia_reembolso` y `schema.sql` crea las claves foraneas al arrancar.

Implementadas el 2026-10-23: I-20 (el PUT de un lote exige `motivo`, a nivel del lote, cuando una
linea cambia su `cantidadRestante`, y registra sobre ella un movimiento `Merma` o `Ajuste` por la
diferencia), I-16 (error estructurado con `error` y `detalle`; errores
`APERTURA_REQUERIDA` y `STOCK_INSUFICIENTE`), I-17 (plan de consumo agregado por producto y una
sola salida por operacion, en orden de id de producto), I-18 (`movimientos` con `id_detalle_lote`
e `id_venta`, `id_existencia` nulo y restricciones de sujeto y documento en
`db/esquema-postgresql.sql`; movimientos sobre lineas de lote al vender y al producir; siembra de
`Venta`, `Reintegro` y `Vencimiento`), I-19 (la venta de un MTO consume sus insumos, con apertura
asistida), I-21 (el reintegro devuelve a los sujetos de los movimientos `Venta`), I-22 (egreso de
devolucion con `id_venta` al anular una venta de un turno cerrado) e I-23 (fecha del servidor y
excepcion de contingencia para Administrador). En una base existente, `POST api/v1/seed` agrega
los tipos de movimiento nuevos y el script de esquema ajusta `movimientos` y `movimiento_caja` al
arrancar.

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
