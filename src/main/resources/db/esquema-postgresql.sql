-- Objetos del esquema de PostgreSQL que no se derivan de los JpaModel (arquitectura.md 4.20).
--
-- Se ejecuta en cada arranque, despues de que Hibernate crea o amplia las tablas
-- (spring.jpa.defer-datasource-initialization), asi que cada sentencia es idempotente: sirve
-- igual para una base vacia que para una creada con el script antiguo. Las restricciones CHECK
-- se quitan y se vuelven a crear porque PostgreSQL no admite ADD CONSTRAINT IF NOT EXISTS, y
-- Spring separa las sentencias por ';', lo que impide usar bloques DO. Si una sentencia falla,
-- la aplicacion no arranca.

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
