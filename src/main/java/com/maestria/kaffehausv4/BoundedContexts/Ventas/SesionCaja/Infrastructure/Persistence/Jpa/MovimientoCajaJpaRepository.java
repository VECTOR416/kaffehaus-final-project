package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de los movimientos de caja. Vive en el slice de SesionCaja porque los
 * movimientos se leen y escriben siempre a traves de SesionCajaDatasourceImpl.
 */
public interface MovimientoCajaJpaRepository extends JpaRepository<MovimientoCajaJpaModel, UUID>{

    List<MovimientoCajaJpaModel> findByIdSesionCajaAndBorradoFalseOrderByFecCAsc(UUID idSesionCaja);
    List<MovimientoCajaJpaModel> findByIdSesionCajaInAndBorradoFalse(Collection<UUID> idsSesionCaja);
}
