package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface LoteJpaRepository extends JpaRepository<LoteJpaModel, UUID>{

    Optional<LoteJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<LoteJpaModel> findByBorradoFalse(Pageable pageable);
    List<LoteJpaModel> findByIdInAndBorradoFalse(Collection<UUID> ids);

    // Lotes completos sin cargarlos en el contexto de persistencia (ver LoteLecturaProyeccion).
    List<LoteLecturaProyeccion> findLecturaByIdInAndBorradoFalse(Collection<UUID> ids);

    // Bloqueo del agregado por su raiz (SELECT ... FOR UPDATE, logica.md LOTE-27). Varios lotes se
    // bloquean en orden de id, el orden de GENERAL-15.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LoteJpaModel> readByIdAndBorradoFalse(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<LoteJpaModel> readByIdInAndBorradoFalseOrderByIdAsc(Collection<UUID> ids);
}
