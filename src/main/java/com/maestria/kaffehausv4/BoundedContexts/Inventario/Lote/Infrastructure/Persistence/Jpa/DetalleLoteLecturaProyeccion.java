package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Proyeccion cerrada de 'detalle_lote' con todas las columnas que reconstruyen la linea. Spring
 * Data la resuelve con una consulta derivada que no carga la entidad en el contexto de
 * persistencia, de modo que una lectura con bloqueo posterior de la misma linea trae su estado
 * vigente (arquitectura.md 5.7).
 */
public interface DetalleLoteLecturaProyeccion {
    UUID getId();
    UUID getIdLote();
    UUID getIdProducto();
    BigDecimal getCantidadInicial();
    BigDecimal getCantidadRestante();
    BigDecimal getPrecioUnitario();
    Boolean getFraccionable();
    LocalDate getFechaVencimiento();
    Boolean getBorrado();
}
