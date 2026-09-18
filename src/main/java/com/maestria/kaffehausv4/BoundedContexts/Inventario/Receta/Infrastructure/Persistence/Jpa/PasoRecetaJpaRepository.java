package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de los pasos. Vive en el slice de Receta porque se leen y escriben siempre
 * a traves de RecetaDatasourceImpl, dentro del agregado.
 *
 * Las lecturas llegan ordenadas por el numero de paso (OrderByPasosAsc) para que la coleccion
 * del agregado se reconstruya ya en su orden de ejecucion; la raiz se apoya en ese orden para
 * renumerar.
 *
 * No declara ningun borrado: las bajas son logicas y viajan como UPDATE del campo 'borrado'.
 */
public interface PasoRecetaJpaRepository extends JpaRepository<PasoRecetaJpaModel, UUID>{

    List<PasoRecetaJpaModel> findByIdRecetaAndBorradoFalseOrderByPasosAsc(UUID idReceta);
    List<PasoRecetaJpaModel> findByIdRecetaInAndBorradoFalseOrderByPasosAsc(Collection<UUID> idsReceta);
}
