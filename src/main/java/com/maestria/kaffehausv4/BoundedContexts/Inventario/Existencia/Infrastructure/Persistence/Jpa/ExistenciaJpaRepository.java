package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExistenciaJpaRepository extends JpaRepository<ExistenciaJpaModel, UUID>{

    Optional<ExistenciaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<ExistenciaJpaModel> findByBorradoFalse(Pageable pageable);

    // Orden FEFO. En PostgreSQL un ASC deja los NULL al final: lo que no vence se usa ultimo.
    List<ExistenciaJpaModel> findByIdDetalleLoteInAndBorradoFalseAndContenidoActualGreaterThanOrderByFechaVencimientoAscFecCAsc(
            Collection<UUID> idsDetalleLote, BigDecimal contenidoMinimo);
}
