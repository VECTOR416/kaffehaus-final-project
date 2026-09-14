package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Infrastructure.Persistence.Jpa;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoPedidoJpaRepository extends JpaRepository<EstadoPedidoJpaModel, UUID>{

    Page<EstadoPedidoJpaModel> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
