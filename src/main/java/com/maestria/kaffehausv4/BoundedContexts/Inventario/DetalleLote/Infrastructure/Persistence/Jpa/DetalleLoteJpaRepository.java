package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleLoteJpaRepository extends JpaRepository<DetalleLoteJpaModel, UUID>{

    Optional<DetalleLoteJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<DetalleLoteJpaModel> findByBorradoFalse(Pageable pageable);
}
