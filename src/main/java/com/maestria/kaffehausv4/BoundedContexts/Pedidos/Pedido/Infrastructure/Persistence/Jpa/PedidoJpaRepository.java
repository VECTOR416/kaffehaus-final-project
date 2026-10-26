package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoJpaRepository extends JpaRepository<PedidoJpaModel, UUID>{

    Optional<PedidoJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<PedidoJpaModel> findByBorradoFalse(Pageable pageable);
    Page<PedidoJpaModel> findByBorradoFalseAndDescripcionContainingIgnoreCase(String descripcion, Pageable pageable);
    Optional<PedidoJpaModel> findFirstByIdVentaAndBorradoFalse(UUID idVenta);
}
