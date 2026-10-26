package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Infrastructure.Persistence.Jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoPagoJpaRepository extends JpaRepository<TipoPagoJpaModel, UUID>{

    Optional<TipoPagoJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<TipoPagoJpaModel> findByBorradoFalse(Pageable pageable);
    Page<TipoPagoJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Optional<TipoPagoJpaModel> findFirstByNombreIgnoreCaseAndBorradoFalse(String nombre);
    List<TipoPagoJpaModel> findByBorradoFalse();
}
