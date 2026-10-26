package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa;

import java.util.UUID;

/**
 * Proyeccion cerrada de 'existencias': solo la linea de lote de la que se abrio. Spring Data la
 * resuelve con una consulta derivada que lee esa columna sin cargar la entidad en el contexto
 * de persistencia, de modo que una lectura con bloqueo posterior de la misma existencia trae su
 * contenido vigente (arquitectura.md 5.7).
 */
public interface ExistenciaDetalleLoteProyeccion {
    UUID getIdDetalleLote();
}
