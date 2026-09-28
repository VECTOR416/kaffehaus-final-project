package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de las lineas de lote. Vive en el slice de Lote porque las lineas se leen y
 * escriben siempre a traves de LoteDatasourceImpl, dentro del agregado.
 *
 * No declara ningun borrado: las bajas son logicas y viajan como UPDATE del campo 'borrado',
 * que ademas es lo unico compatible con la FK ON DELETE RESTRICT de 'existencias'.
 *
 * findByIdLoteInAndBorradoFalse existe para que el listado paginado resuelva los detalles de
 * toda la pagina en una sola consulta en lugar de una por lote. findByIdProductoAndBorradoFalse
 * localiza las lineas de un producto para descontar su inventario.
 */
public interface DetalleLoteJpaRepository extends JpaRepository<DetalleLoteJpaModel, UUID>{

    Optional<DetalleLoteJpaModel> findByIdAndBorradoFalse(UUID id);
    List<DetalleLoteJpaModel> findByIdLoteAndBorradoFalse(UUID idLote);
    List<DetalleLoteJpaModel> findByIdLoteInAndBorradoFalse(Collection<UUID> idsLote);
    List<DetalleLoteJpaModel> findByIdProductoAndBorradoFalse(UUID idProducto);
}
