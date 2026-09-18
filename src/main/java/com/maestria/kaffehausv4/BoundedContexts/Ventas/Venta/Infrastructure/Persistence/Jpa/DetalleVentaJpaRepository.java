package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de las lineas de venta. Vive en el slice de Venta porque las lineas se
 * leen y escriben siempre a traves de VentaDatasourceImpl, dentro del agregado.
 *
 * No declara ningun borrado: las bajas son logicas y viajan como UPDATE del campo 'borrado'.
 *
 * findByIdVentaInAndBorradoFalse existe para que el listado paginado resuelva los detalles de
 * toda la pagina en una sola consulta en lugar de una por venta.
 */
public interface DetalleVentaJpaRepository extends JpaRepository<DetalleVentaJpaModel, UUID>{

    List<DetalleVentaJpaModel> findByIdVentaAndBorradoFalse(UUID idVenta);
    List<DetalleVentaJpaModel> findByIdVentaInAndBorradoFalse(Collection<UUID> idsVenta);
}
