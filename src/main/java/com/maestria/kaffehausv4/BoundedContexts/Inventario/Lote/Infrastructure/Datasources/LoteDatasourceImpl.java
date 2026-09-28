package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Datasources;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Datasources.ILoteDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.DetalleLoteJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.DetalleLoteJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.DetalleLoteJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

/**
 * Persistencia del agregado Lote completo: encabezado mas lineas.
 *
 * Maneja los dos JpaRepository porque ambas tablas pertenecen al mismo agregado; esto no
 * contradice la regla de no acoplar datasources entre agregados, ya que detalle_lote dejo de
 * ser uno. La atomicidad la aporta la @Transactional del caso de uso, que envuelve todas las
 * escrituras en una sola transaccion de base de datos.
 *
 * Aqui no hay DELETE: las bajas de lineas son UPDATE del campo 'borrado'. Un borrado fisico
 * chocaria con 'existencias.id_detalle_lote', que es ON DELETE RESTRICT.
 */
@Repository
public class LoteDatasourceImpl implements ILoteDatasource {

    private final LoteJpaRepository loteJpaRepository;
    private final DetalleLoteJpaRepository detalleLoteJpaRepository;

    public LoteDatasourceImpl(
            LoteJpaRepository loteJpaRepository,
            DetalleLoteJpaRepository detalleLoteJpaRepository) {
        this.loteJpaRepository = loteJpaRepository;
        this.detalleLoteJpaRepository = detalleLoteJpaRepository;
    }

    @Override
    public Lote crear(Lote lote) {
        LoteJpaModel model = LoteJpaMapper.toPersistence(lote);
        LoteJpaModel modelGuardado = loteJpaRepository.save(model);

        List<DetalleLoteJpaModel> detalles = detalleLoteJpaRepository.saveAll(
                lote.getDetalles().stream()
                        .map(detalle -> DetalleLoteJpaMapper.toPersistence(detalle, modelGuardado.getId()))
                        .collect(Collectors.toList()));

        return LoteJpaMapper.toDomain(modelGuardado, detalles);
    }

    @Override
    public Lote actualizar(LoteId loteId, Lote lote) {
        UUID id = aUuid(loteId.getValue());

        LoteJpaModel model = loteJpaRepository.findByIdAndBorradoFalse(id)
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId.getValue() + "' no existe"));

        LoteJpaMapper.copiarCambios(lote, model);
        LoteJpaModel modelGuardado = loteJpaRepository.save(model);

        List<DetalleLoteJpaModel> detalles = detalleLoteJpaRepository.saveAll(
                lote.getDetalles().stream()
                        .map(detalle -> conciliar(detalle, id))
                        .collect(Collectors.toList()));

        return LoteJpaMapper.toDomain(modelGuardado, detalles);
    }

    /**
     * Traduce una linea del agregado a su fila: si ya tenia identidad se recupera la fila y se
     * le copian los cambios, conservando su id y con el su vinculo con las existencias; si no,
     * se crea una nueva.
     */
    private DetalleLoteJpaModel conciliar(DetalleLote detalle, UUID idLote) {
        if (detalle.getId() == null) {
            return DetalleLoteJpaMapper.toPersistence(detalle, idLote);
        }

        UUID idDetalle = aUuid(detalle.getId().getValue());
        DetalleLoteJpaModel model = detalleLoteJpaRepository.findById(idDetalle)
                .orElseThrow(() -> DomainException.throwException(404,
                        "El detalle de lote con id '" + detalle.getId().getValue() + "' no existe"));

        DetalleLoteJpaMapper.copiarCambios(detalle, model);

        return model;
    }

    @Override
    public Optional<Lote> obtenerPorId(LoteId loteId) {
        UUID id = aUuid(loteId.getValue());

        return loteJpaRepository.findByIdAndBorradoFalse(id)
                .map(model -> LoteJpaMapper.toDomain(model, detalleLoteJpaRepository.findByIdLoteAndBorradoFalse(id)));
    }

    @Override
    public Optional<Lote> obtenerPorDetalleId(DetalleLoteId detalleLoteId) {
        return detalleLoteJpaRepository.findByIdAndBorradoFalse(aUuid(detalleLoteId.getValue()))
                .map(DetalleLoteJpaModel::getIdLote)
                .flatMap(idLote -> obtenerPorId(new LoteId(idLote.toString())));
    }

    @Override
    public List<Lote> obtenerPorProducto(ProductoId productoId) {
        List<UUID> idsLote = detalleLoteJpaRepository.findByIdProductoAndBorradoFalse(aUuid(productoId.getValue())).stream()
                .map(DetalleLoteJpaModel::getIdLote)
                .distinct()
                .collect(Collectors.toList());

        if (idsLote.isEmpty()) {
            return Collections.emptyList();
        }

        List<LoteJpaModel> lotes = loteJpaRepository.findByIdInAndBorradoFalse(idsLote);
        Map<UUID, List<DetalleLoteJpaModel>> detallesPorLote = detallesDe(lotes);

        return lotes.stream()
                .map(model -> LoteJpaMapper.toDomain(
                        model,
                        detallesPorLote.getOrDefault(model.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    public PaginatedResponse<List<Lote>> obtenerTodos(ObtenerLotesRequest obtenerLotesRequest) {
        int skip = obtenerLotesRequest.getSkip();
        int limit = obtenerLotesRequest.getLimit();
        //String search = obtenerLotesRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<LoteJpaModel> pagina = loteJpaRepository.findByBorradoFalse(pageable);

        Map<UUID, List<DetalleLoteJpaModel>> detallesPorLote = detallesDe(pagina.getContent());

        List<Lote> datos = pagina.getContent().stream()
                .map(model -> LoteJpaMapper.toDomain(
                        model,
                        detallesPorLote.getOrDefault(model.getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                datos,
                skip,
                limit,
                pagina.getTotalElements(),
                skip + datos.size() < pagina.getTotalElements()
        );
    }

    @Override
    public void eliminar(LoteId loteId) {
        UUID id = aUuid(loteId.getValue());

        LoteJpaModel model = loteJpaRepository.findByIdAndBorradoFalse(id)
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId.getValue() + "' no existe"));

        // La baja la decide el agregado: se reconstruye entero, se le pide eliminar y se
        // escriben tanto el encabezado como las lineas que el marco.
        Lote lote = LoteJpaMapper.toDomain(model, detalleLoteJpaRepository.findByIdLoteAndBorradoFalse(id));
        lote.eliminar();

        LoteJpaMapper.copiarCambios(lote, model);
        loteJpaRepository.save(model);

        detalleLoteJpaRepository.saveAll(
                lote.getDetalles().stream()
                        .map(detalle -> conciliar(detalle, id))
                        .collect(Collectors.toList()));
    }

    /** Resuelve los detalles de una pagina completa en una sola consulta, no una por lote. */
    private Map<UUID, List<DetalleLoteJpaModel>> detallesDe(List<LoteJpaModel> lotes) {
        if (lotes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> idsLote = lotes.stream()
                .map(LoteJpaModel::getId)
                .collect(Collectors.toList());

        return detalleLoteJpaRepository.findByIdLoteInAndBorradoFalse(idsLote).stream()
                .collect(Collectors.groupingBy(DetalleLoteJpaModel::getIdLote));
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
