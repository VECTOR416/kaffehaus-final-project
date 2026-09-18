package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de las lineas de venta. Vive en el slice de Venta porque las lineas se
 * leen y escriben siempre a traves de VentaDatasourceImpl, dentro del agregado.
 *
 * findByIdVentaIn existe para que el listado paginado resuelva los detalles de toda la
 * pagina en una sola consulta en lugar de una por venta.
 */
public interface DetalleVentaJpaRepository extends JpaRepository<DetalleVentaJpaModel, UUID>{

    List<DetalleVentaJpaModel> findByIdVenta(UUID idVenta);
    List<DetalleVentaJpaModel> findByIdVentaIn(Collection<UUID> idsVenta);
    void deleteByIdVenta(UUID idVenta);
}
