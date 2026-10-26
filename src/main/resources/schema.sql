-- Objetos del esquema de PostgreSQL que no se derivan de los JpaModel (arquitectura.md 4.20).
--
-- Se ejecuta en cada arranque, despues de que Hibernate crea o amplia las tablas
-- (spring.jpa.defer-datasource-initialization), asi que cada sentencia es idempotente: sirve
-- igual para una base vacia que para una creada con el script antiguo. Las restricciones CHECK y
-- las claves foraneas se quitan y se vuelven a crear porque PostgreSQL no admite ADD CONSTRAINT
-- IF NOT EXISTS, y Spring separa las sentencias por ';', lo que impide usar bloques DO. Si una
-- sentencia falla, la aplicacion no arranca.

-- Numero de ticket asignado por la base (logica.md VENTA-12).
CREATE SEQUENCE IF NOT EXISTS seq_numero_ticket START WITH 1;
ALTER TABLE venta ALTER COLUMN numero_ticket SET DEFAULT nextval('seq_numero_ticket');

-- Idempotencia de la venta (VENTA-08): dos altas simultaneas con la misma clave no crean dos ventas.
CREATE UNIQUE INDEX IF NOT EXISTS uq_venta_clave_idempotencia
    ON venta (clave_idempotencia) WHERE clave_idempotencia IS NOT NULL;

-- Una sola caja abierta en todo el local (SESIONCAJA-01).
CREATE UNIQUE INDEX IF NOT EXISTS uq_sesion_caja_abierta
    ON sesion_caja (estado) WHERE estado = 'ABIERTA' AND borrado = FALSE;

-- Numero de paso unico entre los pasos vivos de una receta (RECETA-04, arquitectura.md 5.9).
CREATE UNIQUE INDEX IF NOT EXISTS uq_receta_paso
    ON pasos_receta (id_receta, pasos) WHERE borrado = FALSE;

-- Traza completa del inventario (MOVIMIENTO-20, MOVIMIENTO-21). Hibernate agrega las columnas
-- nuevas, pero no quita el NOT NULL que la columna id_existencia tenia en las bases existentes.
ALTER TABLE movimientos ALTER COLUMN id_existencia DROP NOT NULL;
ALTER TABLE movimientos ADD COLUMN IF NOT EXISTS id_detalle_lote UUID;
ALTER TABLE movimientos ADD COLUMN IF NOT EXISTS id_venta UUID;

-- Exactamente un sujeto: una existencia o una linea de lote (MOVIMIENTO-21).
ALTER TABLE movimientos DROP CONSTRAINT IF EXISTS chk_movimiento_sujeto;
ALTER TABLE movimientos ADD CONSTRAINT chk_movimiento_sujeto
    CHECK ((id_existencia IS NULL) <> (id_detalle_lote IS NULL));

-- A lo sumo un documento: una produccion o una venta (MOVIMIENTO-04).
ALTER TABLE movimientos DROP CONSTRAINT IF EXISTS chk_movimiento_documento;
ALTER TABLE movimientos ADD CONSTRAINT chk_movimiento_documento
    CHECK (id_produccion IS NULL OR id_venta IS NULL);

-- Cantidad mayor a cero salvo la apertura, que registra 0 (MOVIMIENTO-08). Sustituye a la
-- version antigua de esta restriccion, que exigia cantidad > 0 y rechazaba la apertura.
ALTER TABLE movimientos DROP CONSTRAINT IF EXISTS chk_movimiento_cantidad;
ALTER TABLE movimientos ADD CONSTRAINT chk_movimiento_cantidad CHECK (cantidad >= 0);

-- Egreso de devolucion generado al anular una venta de un turno cerrado (SESIONCAJA-13).
ALTER TABLE movimiento_caja ADD COLUMN IF NOT EXISTS id_venta UUID;

-- Claves foraneas. Las relaciones entre agregados son columnas UUID planas, sin asociaciones JPA
-- (arquitectura.md 2.4), asi que Hibernate no las crea. Se declaran:
--
-- - NOT VALID: se exigen a las filas nuevas o modificadas, sin revisar las historicas; una base
--   con filas huerfanas de antes sigue arrancando.
-- - DEFERRABLE INITIALLY DEFERRED: se comprueban al confirmar la transaccion, no en cada
--   sentencia. Sin asociaciones, Hibernate no ordena los INSERT por dependencia, y un caso de uso
--   que guarda primero el hijo y despues el padre en la misma transaccion es correcto.
-- - Sin ON DELETE: el borrado es siempre logico (logica.md GENERAL-01); nada se borra en fisico.
--
-- Usan los mismos nombres que el script antiguo, para reemplazar las que ya existan.

-- Contexto Inventario
ALTER TABLE conversiones DROP CONSTRAINT IF EXISTS conversiones_id_unidad_origen_fkey;
ALTER TABLE conversiones ADD CONSTRAINT conversiones_id_unidad_origen_fkey
    FOREIGN KEY (id_unidad_origen) REFERENCES unidad (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE conversiones DROP CONSTRAINT IF EXISTS conversiones_id_unidad_destino_fkey;
ALTER TABLE conversiones ADD CONSTRAINT conversiones_id_unidad_destino_fkey
    FOREIGN KEY (id_unidad_destino) REFERENCES unidad (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

ALTER TABLE producto DROP CONSTRAINT IF EXISTS producto_unidad_actual_fkey;
ALTER TABLE producto ADD CONSTRAINT producto_unidad_actual_fkey
    FOREIGN KEY (unidad_actual) REFERENCES unidad (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE producto DROP CONSTRAINT IF EXISTS producto_id_tipo_fkey;
ALTER TABLE producto ADD CONSTRAINT producto_id_tipo_fkey
    FOREIGN KEY (id_tipo) REFERENCES tipo (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE producto DROP CONSTRAINT IF EXISTS producto_id_categoria_fkey;
ALTER TABLE producto ADD CONSTRAINT producto_id_categoria_fkey
    FOREIGN KEY (id_categoria) REFERENCES categoria (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE producto DROP CONSTRAINT IF EXISTS producto_id_producto_padre_fkey;
ALTER TABLE producto ADD CONSTRAINT producto_id_producto_padre_fkey
    FOREIGN KEY (id_producto_padre) REFERENCES producto (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

ALTER TABLE lote DROP CONSTRAINT IF EXISTS lote_id_proveedor_fkey;
ALTER TABLE lote ADD CONSTRAINT lote_id_proveedor_fkey
    FOREIGN KEY (id_proveedor) REFERENCES proveedor (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE lote DROP CONSTRAINT IF EXISTS lote_id_usuario_fkey;
ALTER TABLE lote ADD CONSTRAINT lote_id_usuario_fkey
    FOREIGN KEY (id_usuario) REFERENCES usuario (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE lote DROP CONSTRAINT IF EXISTS lote_id_produccion_fkey;
ALTER TABLE lote ADD CONSTRAINT lote_id_produccion_fkey
    FOREIGN KEY (id_produccion) REFERENCES produccion (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

ALTER TABLE detalle_lote DROP CONSTRAINT IF EXISTS detalle_lote_id_lote_fkey;
ALTER TABLE detalle_lote ADD CONSTRAINT detalle_lote_id_lote_fkey
    FOREIGN KEY (id_lote) REFERENCES lote (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE detalle_lote DROP CONSTRAINT IF EXISTS detalle_lote_id_producto_fkey;
ALTER TABLE detalle_lote ADD CONSTRAINT detalle_lote_id_producto_fkey
    FOREIGN KEY (id_producto) REFERENCES producto (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

ALTER TABLE existencias DROP CONSTRAINT IF EXISTS existencias_id_detalle_lote_fkey;
ALTER TABLE existencias ADD CONSTRAINT existencias_id_detalle_lote_fkey
    FOREIGN KEY (id_detalle_lote) REFERENCES detalle_lote (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE existencias DROP CONSTRAINT IF EXISTS existencias_id_estado_existencia_fkey;
ALTER TABLE existencias ADD CONSTRAINT existencias_id_estado_existencia_fkey
    FOREIGN KEY (id_estado_existencia) REFERENCES estado_existencia (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE existencias DROP CONSTRAINT IF EXISTS existencias_unidad_fkey;
ALTER TABLE existencias ADD CONSTRAINT existencias_unidad_fkey
    FOREIGN KEY (unidad) REFERENCES unidad (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE existencias DROP CONSTRAINT IF EXISTS existencias_id_usuario_fkey;
ALTER TABLE existencias ADD CONSTRAINT existencias_id_usuario_fkey
    FOREIGN KEY (id_usuario) REFERENCES usuario (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

ALTER TABLE movimientos DROP CONSTRAINT IF EXISTS movimientos_id_existencia_fkey;
ALTER TABLE movimientos ADD CONSTRAINT movimientos_id_existencia_fkey
    FOREIGN KEY (id_existencia) REFERENCES existencias (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE movimientos DROP CONSTRAINT IF EXISTS movimientos_id_detalle_lote_fkey;
ALTER TABLE movimientos ADD CONSTRAINT movimientos_id_detalle_lote_fkey
    FOREIGN KEY (id_detalle_lote) REFERENCES detalle_lote (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE movimientos DROP CONSTRAINT IF EXISTS movimientos_id_tipo_movimiento_fkey;
ALTER TABLE movimientos ADD CONSTRAINT movimientos_id_tipo_movimiento_fkey
    FOREIGN KEY (id_tipo_movimiento) REFERENCES tipo_movimiento (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE movimientos DROP CONSTRAINT IF EXISTS movimientos_id_usuario_fkey;
ALTER TABLE movimientos ADD CONSTRAINT movimientos_id_usuario_fkey
    FOREIGN KEY (id_usuario) REFERENCES usuario (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE movimientos DROP CONSTRAINT IF EXISTS movimientos_id_produccion_fkey;
ALTER TABLE movimientos ADD CONSTRAINT movimientos_id_produccion_fkey
    FOREIGN KEY (id_produccion) REFERENCES produccion (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE movimientos DROP CONSTRAINT IF EXISTS movimientos_id_venta_fkey;
ALTER TABLE movimientos ADD CONSTRAINT movimientos_id_venta_fkey
    FOREIGN KEY (id_venta) REFERENCES venta (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

ALTER TABLE receta DROP CONSTRAINT IF EXISTS receta_id_producto_obtenido_fkey;
ALTER TABLE receta ADD CONSTRAINT receta_id_producto_obtenido_fkey
    FOREIGN KEY (id_producto_obtenido) REFERENCES producto (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE ingredientes_receta DROP CONSTRAINT IF EXISTS ingredientes_receta_id_receta_fkey;
ALTER TABLE ingredientes_receta ADD CONSTRAINT ingredientes_receta_id_receta_fkey
    FOREIGN KEY (id_receta) REFERENCES receta (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE ingredientes_receta DROP CONSTRAINT IF EXISTS ingredientes_receta_id_producto_fkey;
ALTER TABLE ingredientes_receta ADD CONSTRAINT ingredientes_receta_id_producto_fkey
    FOREIGN KEY (id_producto) REFERENCES producto (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE pasos_receta DROP CONSTRAINT IF EXISTS pasos_receta_id_receta_fkey;
ALTER TABLE pasos_receta ADD CONSTRAINT pasos_receta_id_receta_fkey
    FOREIGN KEY (id_receta) REFERENCES receta (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

ALTER TABLE produccion DROP CONSTRAINT IF EXISTS produccion_id_receta_fkey;
ALTER TABLE produccion ADD CONSTRAINT produccion_id_receta_fkey
    FOREIGN KEY (id_receta) REFERENCES receta (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

-- Contexto Personal
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_id_persona_fkey;
ALTER TABLE usuario ADD CONSTRAINT usuario_id_persona_fkey
    FOREIGN KEY (id_persona) REFERENCES persona (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_id_rol_fkey;
ALTER TABLE usuario ADD CONSTRAINT usuario_id_rol_fkey
    FOREIGN KEY (id_rol) REFERENCES rol (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE cliente DROP CONSTRAINT IF EXISTS cliente_id_persona_fkey;
ALTER TABLE cliente ADD CONSTRAINT cliente_id_persona_fkey
    FOREIGN KEY (id_persona) REFERENCES persona (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE proveedor DROP CONSTRAINT IF EXISTS proveedor_id_persona_fkey;
ALTER TABLE proveedor ADD CONSTRAINT proveedor_id_persona_fkey
    FOREIGN KEY (id_persona) REFERENCES persona (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE proveedor DROP CONSTRAINT IF EXISTS proveedor_id_entidad_fkey;
ALTER TABLE proveedor ADD CONSTRAINT proveedor_id_entidad_fkey
    FOREIGN KEY (id_entidad) REFERENCES entidades (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

-- Contexto Ventas
ALTER TABLE sesion_caja DROP CONSTRAINT IF EXISTS sesion_caja_id_usuario_fkey;
ALTER TABLE sesion_caja ADD CONSTRAINT sesion_caja_id_usuario_fkey
    FOREIGN KEY (id_usuario) REFERENCES usuario (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE movimiento_caja DROP CONSTRAINT IF EXISTS movimiento_caja_id_sesion_caja_fkey;
ALTER TABLE movimiento_caja ADD CONSTRAINT movimiento_caja_id_sesion_caja_fkey
    FOREIGN KEY (id_sesion_caja) REFERENCES sesion_caja (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE movimiento_caja DROP CONSTRAINT IF EXISTS movimiento_caja_id_usuario_fkey;
ALTER TABLE movimiento_caja ADD CONSTRAINT movimiento_caja_id_usuario_fkey
    FOREIGN KEY (id_usuario) REFERENCES usuario (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE movimiento_caja DROP CONSTRAINT IF EXISTS movimiento_caja_id_venta_fkey;
ALTER TABLE movimiento_caja ADD CONSTRAINT movimiento_caja_id_venta_fkey
    FOREIGN KEY (id_venta) REFERENCES venta (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

ALTER TABLE venta DROP CONSTRAINT IF EXISTS venta_id_usuario_fkey;
ALTER TABLE venta ADD CONSTRAINT venta_id_usuario_fkey
    FOREIGN KEY (id_usuario) REFERENCES usuario (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE venta DROP CONSTRAINT IF EXISTS venta_id_cliente_fkey;
ALTER TABLE venta ADD CONSTRAINT venta_id_cliente_fkey
    FOREIGN KEY (id_cliente) REFERENCES cliente (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE venta DROP CONSTRAINT IF EXISTS venta_id_sesion_caja_fkey;
ALTER TABLE venta ADD CONSTRAINT venta_id_sesion_caja_fkey
    FOREIGN KEY (id_sesion_caja) REFERENCES sesion_caja (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE venta DROP CONSTRAINT IF EXISTS venta_id_estado_venta_fkey;
ALTER TABLE venta ADD CONSTRAINT venta_id_estado_venta_fkey
    FOREIGN KEY (id_estado_venta) REFERENCES estado_venta (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE venta DROP CONSTRAINT IF EXISTS venta_id_usuario_anulacion_fkey;
ALTER TABLE venta ADD CONSTRAINT venta_id_usuario_anulacion_fkey
    FOREIGN KEY (id_usuario_anulacion) REFERENCES usuario (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

ALTER TABLE detalle_venta DROP CONSTRAINT IF EXISTS detalle_venta_id_venta_fkey;
ALTER TABLE detalle_venta ADD CONSTRAINT detalle_venta_id_venta_fkey
    FOREIGN KEY (id_venta) REFERENCES venta (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE detalle_venta DROP CONSTRAINT IF EXISTS detalle_venta_id_producto_fkey;
ALTER TABLE detalle_venta ADD CONSTRAINT detalle_venta_id_producto_fkey
    FOREIGN KEY (id_producto) REFERENCES producto (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE pago_venta DROP CONSTRAINT IF EXISTS pago_venta_id_venta_fkey;
ALTER TABLE pago_venta ADD CONSTRAINT pago_venta_id_venta_fkey
    FOREIGN KEY (id_venta) REFERENCES venta (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE pago_venta DROP CONSTRAINT IF EXISTS pago_venta_id_tipo_pago_fkey;
ALTER TABLE pago_venta ADD CONSTRAINT pago_venta_id_tipo_pago_fkey
    FOREIGN KEY (id_tipo_pago) REFERENCES tipo_pago (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

ALTER TABLE descuentos DROP CONSTRAINT IF EXISTS descuentos_id_producto_fkey;
ALTER TABLE descuentos ADD CONSTRAINT descuentos_id_producto_fkey
    FOREIGN KEY (id_producto) REFERENCES producto (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE descuentos DROP CONSTRAINT IF EXISTS descuentos_id_tipo_descuento_fkey;
ALTER TABLE descuentos ADD CONSTRAINT descuentos_id_tipo_descuento_fkey
    FOREIGN KEY (id_tipo_descuento) REFERENCES tipo_descuento (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;

-- Contexto Pedidos
ALTER TABLE pedidos DROP CONSTRAINT IF EXISTS pedidos_id_venta_fkey;
ALTER TABLE pedidos ADD CONSTRAINT pedidos_id_venta_fkey
    FOREIGN KEY (id_venta) REFERENCES venta (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE pedidos DROP CONSTRAINT IF EXISTS pedidos_id_estado_fkey;
ALTER TABLE pedidos ADD CONSTRAINT pedidos_id_estado_fkey
    FOREIGN KEY (id_estado) REFERENCES estado_pedido (id) DEFERRABLE INITIALLY DEFERRED NOT VALID;
