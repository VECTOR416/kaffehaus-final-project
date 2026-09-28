-- Habilitación de extensión para generación de UUIDs (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. IDENTIDAD, ROLES Y ACTORES
-- ============================================================================

CREATE TABLE rol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE persona (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    telefono VARCHAR(25),
    dni VARCHAR(25) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE entidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL,
    nit VARCHAR(50) NOT NULL,
    telefono VARCHAR(25),
    descripcion TEXT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_persona UUID NOT NULL REFERENCES persona(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    correo VARCHAR(255) NOT NULL,
    contrasena VARCHAR(255) NOT NULL,
    id_rol UUID NOT NULL REFERENCES rol(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE cliente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_persona UUID NOT NULL REFERENCES persona(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE proveedor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_persona UUID NOT NULL REFERENCES persona(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    id_entidad UUID REFERENCES entidades(id) ON UPDATE CASCADE ON DELETE SET NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================================
-- 2. UNIDADES Y CONVERSIONES
-- ============================================================================

CREATE TABLE unidad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unidad VARCHAR(50) NOT NULL,
    abreviatura VARCHAR(10),
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    es_borrable BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE conversiones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_unidad_origen UUID NOT NULL REFERENCES unidad(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    id_unidad_destino UUID NOT NULL REFERENCES unidad(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    factor_conversion NUMERIC(12, 4) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    es_borrable BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_distintas_unidades CHECK (id_unidad_origen <> id_unidad_destino)
);

-- ============================================================================
-- 3. PRODUCTOS Y RECETARIO
-- ============================================================================

CREATE TABLE tipo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE categoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    es_borrable BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE producto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL,
    stock NUMERIC(12, 2) NOT NULL DEFAULT 0,
    precio NUMERIC(12, 2) NOT NULL DEFAULT 0,
    stock_minimo NUMERIC(12, 2) NOT NULL DEFAULT 0,
    unidad_actual UUID NOT NULL REFERENCES unidad(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    id_tipo UUID NOT NULL REFERENCES tipo(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    id_categoria UUID NOT NULL REFERENCES categoria(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    vendible BOOLEAN NOT NULL DEFAULT TRUE,
    -- TRUE: stock fisico directo (MTS, reventa, pre-elaborados). FALSE: se prepara al pedirlo
    -- bajo receta (MTO) y su venta descuenta los insumos de la receta (backflush).
    es_stock_directo BOOLEAN NOT NULL DEFAULT TRUE,
    id_producto_padre UUID REFERENCES producto(id) ON UPDATE CASCADE ON DELETE SET NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    -- 'stock' es la proyeccion del contenido abierto de sus existencias (inventario.md INV-20).
    CONSTRAINT chk_producto_stock CHECK (stock >= 0),
    CONSTRAINT chk_producto_padre CHECK (id_producto_padre IS NULL OR id_producto_padre <> id)
);

CREATE TABLE receta (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_producto_obtenido UUID NOT NULL REFERENCES producto(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    cantidad NUMERIC(12, 2) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE ingredientes_receta (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_receta UUID NOT NULL REFERENCES receta(id) ON UPDATE CASCADE ON DELETE CASCADE,
    id_producto UUID NOT NULL REFERENCES producto(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    cantidad NUMERIC(12, 4) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE pasos_receta (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_receta UUID NOT NULL REFERENCES receta(id) ON UPDATE CASCADE ON DELETE CASCADE,
    pasos INT NOT NULL,
    descripcion TEXT NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

-- La unicidad del numero de paso aplica solo a los pasos vivos: con borrado logico, una fila
-- dada de baja conserva su numero y bloquearia el alta de un paso nuevo con esa posicion.
CREATE UNIQUE INDEX uq_receta_paso ON pasos_receta (id_receta, pasos) WHERE borrado = FALSE;

CREATE TABLE produccion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_receta UUID NOT NULL REFERENCES receta(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    cantidad_esperada NUMERIC(12, 2) NOT NULL,
    cantidad_real NUMERIC(12, 2) NOT NULL,
    diferencia NUMERIC(12, 2) NOT NULL,
    motivo_diferencia TEXT,
    fecha_produccion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================================
-- 4. INVENTARIO FÍSICO, LOTES Y EXISTENCIAS
-- ============================================================================

CREATE TABLE estado_existencia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE tipo_movimiento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

-- Un lote nace de una compra (proveedor) o de una produccion interna (MTS), nunca de ambas.
CREATE TABLE lote (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fecha_compra TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_proveedor UUID REFERENCES proveedor(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    total NUMERIC(12, 2) NOT NULL DEFAULT 0,
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    es_produccion BOOLEAN NOT NULL DEFAULT FALSE,
    id_produccion UUID REFERENCES produccion(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_lote_origen CHECK (
        (es_produccion = FALSE AND id_proveedor IS NOT NULL AND id_produccion IS NULL)
        OR (es_produccion = TRUE AND id_produccion IS NOT NULL AND id_proveedor IS NULL)
    )
);

CREATE TABLE detalle_lote (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_lote UUID NOT NULL REFERENCES lote(id) ON UPDATE CASCADE ON DELETE CASCADE,
    id_producto UUID NOT NULL REFERENCES producto(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    cantidad_inicial NUMERIC(12, 2) NOT NULL,
    cantidad_restante NUMERIC(12, 2) NOT NULL,
    precio_unitario NUMERIC(12, 2) NOT NULL,
    fraccionable BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_vencimiento DATE,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    -- 'cantidad_restante' cuenta unidades aun cerradas (inventario.md INV-56).
    CONSTRAINT chk_detalle_lote_restante CHECK (cantidad_restante >= 0 AND cantidad_restante <= cantidad_inicial)
);

CREATE TABLE existencias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_detalle_lote UUID NOT NULL REFERENCES detalle_lote(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    contenido_inicial NUMERIC(12, 4) NOT NULL,
    contenido_actual NUMERIC(12, 4) NOT NULL,
    id_estado_existencia UUID NOT NULL REFERENCES estado_existencia(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fecha_acabado TIMESTAMP,
    fecha_vencimiento DATE,
    fecha_apertura TIMESTAMP,
    unidad UUID NOT NULL REFERENCES unidad(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    id_usuario UUID REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    -- Saldo nunca negativo ni por encima de lo que cabia al abrir (inventario.md INV-55).
    CONSTRAINT chk_existencia_saldo CHECK (contenido_actual >= 0 AND contenido_actual <= contenido_inicial)
);

CREATE TABLE movimientos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_existencia UUID NOT NULL REFERENCES existencias(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    cantidad NUMERIC(12, 4) NOT NULL,
    fecha_movimiento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_tipo_movimiento UUID NOT NULL REFERENCES tipo_movimiento(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    motivo TEXT,
    -- Consumos de insumos y entradas de producto terminado de una produccion (MTS). Las
    -- salidas por venta no guardan id_detalle_venta: se identifican por su tipo y su motivo.
    id_produccion UUID REFERENCES produccion(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    -- Magnitud siempre positiva; la direccion la da el tipo (inventario.md INV-41, INV-57).
    CONSTRAINT chk_movimiento_cantidad CHECK (cantidad > 0)
);

-- ============================================================================
-- 5. VENTAS, DESCUENTOS Y LOGÍSTICA
-- ============================================================================

CREATE TABLE estado_venta (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE tipo_descuento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE descuentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_producto UUID NOT NULL REFERENCES producto(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    descuento NUMERIC(12, 2) NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_final TIMESTAMP NOT NULL,
    id_tipo_descuento UUID NOT NULL REFERENCES tipo_descuento(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE tipo_pago (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL UNIQUE,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

-- Turno de caja: quien lo abre es el responsable del dinero fisico hasta cerrarlo.
CREATE TABLE sesion_caja (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fecha_apertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre TIMESTAMP,
    monto_apertura NUMERIC(12, 2) NOT NULL CHECK (monto_apertura >= 0),
    -- Efectivo contado al cerrar, el que el sistema esperaba y la diferencia (declarado - esperado).
    monto_cierre_declarado NUMERIC(12, 2),
    monto_cierre_esperado NUMERIC(12, 2),
    diferencia NUMERIC(12, 2),
    motivo_diferencia TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA', 'CERRADA')),
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

-- Una sola caja abierta en todo el local a la vez.
CREATE UNIQUE INDEX uq_sesion_caja_abierta ON sesion_caja (estado) WHERE estado = 'ABIERTA' AND borrado = FALSE;

-- Ingresos y egresos menores de efectivo durante el turno. Inmutables, como los movimientos
-- de inventario; 'fec_u' existe solo por la convencion de auditoria (arquitectura.md 5.7).
CREATE TABLE movimiento_caja (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_sesion_caja UUID NOT NULL REFERENCES sesion_caja(id) ON UPDATE CASCADE ON DELETE CASCADE,
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('INGRESO', 'EGRESO')),
    monto NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    motivo VARCHAR(255) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE SEQUENCE IF NOT EXISTS seq_numero_ticket START WITH 1 INCREMENT BY 1;

CREATE TABLE venta (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Numero correlativo que se imprime en el ticket y por el que se recupera la venta.
    numero_ticket BIGINT NOT NULL DEFAULT nextval('seq_numero_ticket'),
    fecha_venta TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Empleado que cobra; el responsable del dinero es el titular de la sesion de caja.
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    id_sesion_caja UUID NOT NULL REFERENCES sesion_caja(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    -- Sin cliente identificado se usa el cliente 'Mostrador' que siembra POST api/v1/seed.
    id_cliente UUID NOT NULL REFERENCES cliente(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    total NUMERIC(12, 2) NOT NULL DEFAULT 0,
    id_estado_venta UUID NOT NULL REFERENCES estado_venta(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    -- La genera el POS para que un reintento no cobre dos veces (ventas.md VEN-66).
    clave_idempotencia VARCHAR(100),
    -- Quien anulo la venta y por que (ventas.md VEN-40). Nulos mientras este 'Completada'.
    motivo_anulacion TEXT,
    id_usuario_anulacion UUID REFERENCES usuario(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_venta_total CHECK (total >= 0)
);

CREATE UNIQUE INDEX uq_venta_clave_idempotencia ON venta (clave_idempotencia) WHERE clave_idempotencia IS NOT NULL;
CREATE UNIQUE INDEX idx_venta_numero_ticket ON venta (numero_ticket);

CREATE TABLE detalle_venta (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_venta UUID NOT NULL REFERENCES venta(id) ON UPDATE CASCADE ON DELETE CASCADE,
    id_producto UUID NOT NULL REFERENCES producto(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    precio_unitario NUMERIC(12, 2) NOT NULL,
    cantidad NUMERIC(12, 2) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    descuento_aplicado NUMERIC(12, 2) NOT NULL DEFAULT 0,
    -- Costo de lo que el inventario desconto por esta linea (ventas.md VEN-23). Derivado.
    costo_total NUMERIC(12, 4) NOT NULL DEFAULT 0,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_detalle_venta_importes CHECK (
        precio_unitario >= 0 AND cantidad > 0 AND descuento_aplicado >= 0 AND subtotal >= 0 AND costo_total >= 0)
);

-- Pagos de una venta: admite pagos mixtos. 'cambio' es derivado (monto_recibido - monto) y
-- solo es distinto de cero en efectivo.
CREATE TABLE pago_venta (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_venta UUID NOT NULL REFERENCES venta(id) ON UPDATE CASCADE ON DELETE CASCADE,
    id_tipo_pago UUID NOT NULL REFERENCES tipo_pago(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    monto NUMERIC(12, 2) NOT NULL,
    monto_recibido NUMERIC(12, 2) NOT NULL,
    cambio NUMERIC(12, 2) NOT NULL DEFAULT 0,
    referencia VARCHAR(100),
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_pago_venta_importes CHECK (monto > 0 AND monto_recibido >= monto AND cambio >= 0)
);

CREATE TABLE estado_pedido (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE pedidos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_venta UUID NOT NULL UNIQUE REFERENCES venta(id) ON UPDATE CASCADE ON DELETE CASCADE,
    fecha_entrega TIMESTAMP NOT NULL,
    latitud NUMERIC(10, 8) NOT NULL,
    longitud NUMERIC(11, 8) NOT NULL,
    descripcion TEXT,
    id_estado UUID NOT NULL REFERENCES estado_pedido(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    fec_c TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fec_u TIMESTAMP,
    borrado BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================================
-- 6. ÍNDICES ESTRATÉGICOS
-- ============================================================================

CREATE INDEX idx_persona_dni ON persona(dni);
CREATE INDEX idx_entidades_nit ON entidades(nit);
CREATE INDEX idx_usuario_correo ON usuario(correo);
CREATE INDEX idx_existencias_estado ON existencias(id_estado_existencia);
CREATE INDEX idx_existencias_detalle_lote ON existencias(id_detalle_lote);
CREATE INDEX idx_movimientos_existencia ON movimientos(id_existencia);
CREATE INDEX idx_movimientos_usuario ON movimientos(id_usuario);
CREATE INDEX idx_movimientos_produccion ON movimientos(id_produccion);
CREATE INDEX idx_lote_produccion ON lote(id_produccion);
CREATE INDEX idx_detalle_lote_producto ON detalle_lote(id_producto);
CREATE INDEX idx_producto_tipo ON producto(id_tipo);
CREATE INDEX idx_producto_categoria ON producto(id_categoria);
CREATE INDEX idx_existencias_usuario ON existencias(id_usuario);
CREATE INDEX idx_ingredientes_receta_receta ON ingredientes_receta(id_receta);
CREATE INDEX idx_pasos_receta_receta ON pasos_receta(id_receta);
CREATE INDEX idx_detalle_venta_venta ON detalle_venta(id_venta);
CREATE INDEX idx_pedidos_venta ON pedidos(id_venta);
CREATE INDEX idx_venta_sesion_caja ON venta(id_sesion_caja);
CREATE INDEX idx_pago_venta_venta ON pago_venta(id_venta);
CREATE INDEX idx_movimiento_caja_sesion ON movimiento_caja(id_sesion_caja);

-- ============================================================================
-- 7. CATALOGOS Y DATOS INICIALES
-- ============================================================================
-- Este script solo crea el esquema. Los catalogos que el negocio resuelve por nombre
-- (tipo_movimiento, estado_existencia, estado_venta, tipo_descuento, estado_pedido,
-- tipo_pago), los roles, el administrador inicial, las unidades y conversiones estandar y el
-- cliente 'Mostrador' los siembra POST api/v1/seed (SembrarSistemaUseCase), que es idempotente.
-- Primer arranque: ejecutar este script en una base vacia, levantar la aplicacion y llamar
-- una vez a POST api/v1/seed.
