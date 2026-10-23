-- Lo que el esquema de PostgreSQL define fuera de las entidades y las pruebas necesitan.
-- Se ejecuta despues de que Hibernate crea las tablas (defer-datasource-initialization).
--
-- No se reproducen los indices unicos parciales (uq_sesion_caja_abierta, uq_receta_paso):
-- H2 no admite indices con WHERE. Lo que dependa de ellos no se prueba aqui.

CREATE SEQUENCE IF NOT EXISTS seq_numero_ticket START WITH 1;

ALTER TABLE venta ALTER COLUMN numero_ticket SET DEFAULT NEXT VALUE FOR seq_numero_ticket;

-- Restricciones de la traza del inventario (MOVIMIENTO-21, MOVIMIENTO-04, MOVIMIENTO-08), como en
-- src/main/resources/db/esquema-postgresql.sql. Hibernate crea las tablas en cada contexto, asi
-- que aqui no hace falta quitarlas antes de agregarlas.
ALTER TABLE movimientos ADD CONSTRAINT chk_movimiento_sujeto
    CHECK ((id_existencia IS NULL) <> (id_detalle_lote IS NULL));
ALTER TABLE movimientos ADD CONSTRAINT chk_movimiento_documento
    CHECK (id_produccion IS NULL OR id_venta IS NULL);
ALTER TABLE movimientos ADD CONSTRAINT chk_movimiento_cantidad CHECK (cantidad >= 0);
