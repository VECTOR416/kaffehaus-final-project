package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Proyeccion cerrada de 'lote' con todas las columnas que reconstruyen el agregado. Spring Data
 * la resuelve con una consulta derivada que no carga la entidad en el contexto de persistencia,
 * de modo que una lectura con bloqueo posterior del mismo lote trae su estado vigente
 * (arquitectura.md 5.7).
 */
public interface LoteLecturaProyeccion {
    UUID getId();
    LocalDateTime getFechaCompra();
    BigDecimal getTotal();
    UUID getIdProveedor();
    UUID getIdUsuario();
    Boolean getEsProduccion();
    UUID getIdProduccion();
    Boolean getBorrado();
}
