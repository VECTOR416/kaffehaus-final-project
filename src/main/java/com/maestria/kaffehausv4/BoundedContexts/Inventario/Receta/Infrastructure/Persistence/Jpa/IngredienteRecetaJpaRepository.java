package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de los ingredientes. Vive en el slice de Receta porque se leen y escriben
 * siempre a traves de RecetaDatasourceImpl, dentro del agregado.
 *
 * No declara ningun borrado: las bajas son logicas y viajan como UPDATE del campo 'borrado'.
 */
public interface IngredienteRecetaJpaRepository extends JpaRepository<IngredienteRecetaJpaModel, UUID>{

    List<IngredienteRecetaJpaModel> findByIdRecetaAndBorradoFalse(UUID idReceta);
    List<IngredienteRecetaJpaModel> findByIdRecetaInAndBorradoFalse(Collection<UUID> idsReceta);

    // Recetas que usan un producto (logica.md PRODUCTO-20).
    List<IngredienteRecetaJpaModel> findByIdProductoAndBorradoFalse(UUID idProducto);
}
