package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

/**
 * Repositorio JPA de las lineas de lote. Vive en el slice de Lote porque las lineas se leen y
 * escriben siempre a traves de LoteDatasourceImpl, dentro del agregado.
 *
 * No declara ningun borrado: las bajas son logicas y viajan como UPDATE del campo 'borrado',
 * que ademas es lo unico compatible con la FK ON DELETE RESTRICT de 'existencias'.
 *
 * findByIdLoteInAndBorradoFalse existe para que el listado paginado resuelva los detalles de
 * toda la pagina en una sola consulta en lugar de una por lote. findByIdProductoInAndBorradoFalse
 * localiza las lineas de un insumo base y de sus presentaciones.
 *
 * readByIdAndBorradoFalse es la misma consulta que findByIdAndBorradoFalse con SELECT ... FOR
 * UPDATE; el prefijo 'read' solo la distingue (inventario.md INV-52).
 */
public interface DetalleLoteJpaRepository extends JpaRepository<DetalleLoteJpaModel, UUID>{

    Optional<DetalleLoteJpaModel> findByIdAndBorradoFalse(UUID id);
    List<DetalleLoteJpaModel> findByIdLoteAndBorradoFalse(UUID idLote);
    List<DetalleLoteJpaModel> findByIdLoteInAndBorradoFalse(Collection<UUID> idsLote);
    List<DetalleLoteJpaModel> findByIdProductoInAndBorradoFalse(Collection<UUID> idsProducto);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DetalleLoteJpaModel> readByIdAndBorradoFalse(UUID id);
}
