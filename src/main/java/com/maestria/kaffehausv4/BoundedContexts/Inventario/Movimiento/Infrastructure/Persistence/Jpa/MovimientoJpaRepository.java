package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoJpaRepository extends JpaRepository<MovimientoJpaModel, UUID>{

    Optional<MovimientoJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<MovimientoJpaModel> findByBorradoFalse(Pageable pageable);
    Page<MovimientoJpaModel> findByBorradoFalseAndMotivoContainingIgnoreCase(String motivo, Pageable pageable);
    List<MovimientoJpaModel> findByIdExistenciaInAndBorradoFalse(Collection<UUID> idsExistencia);

    List<MovimientoJpaModel> findByIdVentaAndBorradoFalseOrderByIdAsc(UUID idVenta);
}
