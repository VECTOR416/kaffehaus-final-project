package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoPedidoJpaRepository extends JpaRepository<EstadoPedidoJpaModel, UUID>{

    Optional<EstadoPedidoJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<EstadoPedidoJpaModel> findByBorradoFalse(Pageable pageable);
    Page<EstadoPedidoJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
