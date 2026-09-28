package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de los pagos de una venta. Vive en el slice de Venta porque los pagos se leen y
 * escriben siempre a traves de VentaDatasourceImpl, dentro del agregado.
 */
public interface PagoVentaJpaRepository extends JpaRepository<PagoVentaJpaModel, UUID>{

    List<PagoVentaJpaModel> findByIdVentaAndBorradoFalse(UUID idVenta);
    List<PagoVentaJpaModel> findByIdVentaInAndBorradoFalse(Collection<UUID> idsVenta);
}
