package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Infrastructure.Persistence.Jpa;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoJpaRepository extends JpaRepository<PedidoJpaModel, UUID>{

    Page<PedidoJpaModel> findByDescripcionContainingIgnoreCase(String descripcion, Pageable pageable);
}
