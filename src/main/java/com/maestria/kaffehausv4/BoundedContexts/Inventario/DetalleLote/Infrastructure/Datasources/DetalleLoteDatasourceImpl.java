package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Datasources.IDetalleLoteDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.ObtenerDetallesLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Infrastructure.Persistence.Jpa.DetalleLoteJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Infrastructure.Persistence.Jpa.DetalleLoteJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Infrastructure.Persistence.Jpa.DetalleLoteJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class DetalleLoteDatasourceImpl implements IDetalleLoteDatasource {

    private final DetalleLoteJpaRepository detalleLoteJpaRepository;

    public DetalleLoteDatasourceImpl(DetalleLoteJpaRepository detalleLoteJpaRepository) {
        this.detalleLoteJpaRepository = detalleLoteJpaRepository;
    }

    @Override
    public DetalleLote crear(DetalleLote detalleLote) {
        DetalleLoteJpaModel model = DetalleLoteJpaMapper.toPersistence(detalleLote);
        return DetalleLoteJpaMapper.toDomain(detalleLoteJpaRepository.save(model));
    }

    @Override
    public DetalleLote actualizar(DetalleLoteId detalleLoteId, DetalleLote detalleLote) {
        DetalleLoteJpaModel model = detalleLoteJpaRepository.findByIdAndBorradoFalse(aUuid(detalleLoteId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + detalleLoteId.getValue() + "' no existe"));

        DetalleLoteJpaMapper.copiarCambios(detalleLote, model);

        return DetalleLoteJpaMapper.toDomain(detalleLoteJpaRepository.save(model));
    }

    @Override
    public Optional<DetalleLote> obtenerPorId(DetalleLoteId detalleLoteId) {
        return detalleLoteJpaRepository.findByIdAndBorradoFalse(aUuid(detalleLoteId.getValue()))
                .map(DetalleLoteJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<DetalleLote>> obtenerTodos(ObtenerDetallesLoteRequest obtenerDetallesLoteRequest) {
        int skip = obtenerDetallesLoteRequest.getSkip();
        int limit = obtenerDetallesLoteRequest.getLimit();
        //String search = obtenerDetallesLoteRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<DetalleLoteJpaModel> pagina = detalleLoteJpaRepository.findByBorradoFalse(pageable);

        List<DetalleLote> datos = pagina.getContent().stream()
                .map(DetalleLoteJpaMapper::toDomain)
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
    public void eliminar(DetalleLoteId detalleLoteId) {
        DetalleLoteJpaModel model = detalleLoteJpaRepository.findByIdAndBorradoFalse(aUuid(detalleLoteId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + detalleLoteId.getValue() + "' no existe"));

        DetalleLote detalleLote = DetalleLoteJpaMapper.toDomain(model);
        detalleLote.eliminar();

        DetalleLoteJpaMapper.copiarCambios(detalleLote, model);
        detalleLoteJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
