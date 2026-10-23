package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface ExistenciaJpaRepository extends JpaRepository<ExistenciaJpaModel, UUID>{

    Optional<ExistenciaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<ExistenciaJpaModel> findByBorradoFalse(Pageable pageable);

    // Orden FEFO. En PostgreSQL un ASC deja los NULL al final: lo que no vence se usa ultimo.
    List<ExistenciaJpaModel> findByIdDetalleLoteInAndBorradoFalseAndContenidoActualGreaterThanOrderByFechaVencimientoAscFecCAsc(
            Collection<UUID> idsDetalleLote, BigDecimal contenidoMinimo);

    // Mismas consultas con SELECT ... FOR UPDATE (inventario.md INV-52). El prefijo 'read' solo
    // las distingue de las 'find'; el desempate por id fija el orden de bloqueo (INV-53).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ExistenciaJpaModel> readByIdDetalleLoteInAndBorradoFalseAndContenidoActualGreaterThanOrderByFechaVencimientoAscFecCAscIdAsc(
            Collection<UUID> idsDetalleLote, BigDecimal contenidoMinimo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExistenciaJpaModel> readByIdAndBorradoFalse(UUID id);

    // Todas las vivas, con o sin contenido, para el pesaje (logica.md EXISTENCIA-16).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ExistenciaJpaModel> readByIdDetalleLoteInAndBorradoFalseOrderByIdAsc(Collection<UUID> idsDetalleLote);
}
