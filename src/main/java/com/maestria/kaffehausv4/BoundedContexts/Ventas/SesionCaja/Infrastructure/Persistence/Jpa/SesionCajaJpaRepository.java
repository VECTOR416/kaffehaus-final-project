package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

/**
 * Tres lecturas de la sesion abierta, una por nivel de bloqueo:
 *
 * - find...: sin bloqueo, para consultar el estado.
 * - read...: PESSIMISTIC_WRITE (FOR UPDATE), para cerrarla o registrarle un movimiento.
 * - get...:  PESSIMISTIC_READ (FOR SHARE), para vender. Las ventas no se bloquean entre si,
 *   pero un cierre espera a que terminen las que estan en curso, y ninguna venta entra en una
 *   sesion que se esta cerrando.
 */
public interface SesionCajaJpaRepository extends JpaRepository<SesionCajaJpaModel, UUID>{

    Optional<SesionCajaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<SesionCajaJpaModel> findByBorradoFalseOrderByFechaAperturaDesc(Pageable pageable);
    Optional<SesionCajaJpaModel> findFirstByEstadoAndBorradoFalse(String estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SesionCajaJpaModel> readFirstByEstadoAndBorradoFalse(String estado);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<SesionCajaJpaModel> getFirstByEstadoAndBorradoFalse(String estado);
}
