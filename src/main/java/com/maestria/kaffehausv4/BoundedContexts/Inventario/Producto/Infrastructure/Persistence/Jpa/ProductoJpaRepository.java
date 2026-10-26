package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface ProductoJpaRepository extends JpaRepository<ProductoJpaModel, UUID>{

    Optional<ProductoJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<ProductoJpaModel> findByBorradoFalse(Pageable pageable);
    Page<ProductoJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
    List<ProductoJpaModel> findByIdProductoPadreAndBorradoFalse(UUID idProductoPadre);

    // Prefijo 'read' en lugar de 'find' solo para distinguirla de findByIdAndBorradoFalse:
    // misma consulta derivada, con SELECT ... FOR UPDATE (inventario.md INV-52).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductoJpaModel> readByIdAndBorradoFalse(UUID id);

    // Bajas de catalogos con productos vivos (logica.md GENERAL-18).
    boolean existsByIdTipoAndBorradoFalse(UUID idTipo);
    boolean existsByIdCategoriaAndBorradoFalse(UUID idCategoria);
    boolean existsByUnidadActualAndBorradoFalse(UUID unidadActual);
}
