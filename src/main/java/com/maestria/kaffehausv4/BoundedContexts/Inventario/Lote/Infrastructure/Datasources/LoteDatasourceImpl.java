package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Datasources.ILoteDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class LoteDatasourceImpl implements ILoteDatasource {

    private final LoteJpaRepository loteJpaRepository;

    public LoteDatasourceImpl(LoteJpaRepository loteJpaRepository) {
        this.loteJpaRepository = loteJpaRepository;
    }

    @Override
    public Lote crear(Lote lote) {
        LoteJpaModel model = LoteJpaMapper.toPersistence(lote);
        return LoteJpaMapper.toDomain(loteJpaRepository.save(model));
    }

    @Override
    public Lote actualizar(LoteId loteId, Lote lote) {
        LoteJpaModel model = loteJpaRepository.findByIdAndBorradoFalse(aUuid(loteId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId.getValue() + "' no existe"));

        LoteJpaMapper.copiarCambios(lote, model);

        return LoteJpaMapper.toDomain(loteJpaRepository.save(model));
    }

    @Override
    public Optional<Lote> obtenerPorId(LoteId loteId) {
        return loteJpaRepository.findByIdAndBorradoFalse(aUuid(loteId.getValue()))
                .map(LoteJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Lote>> obtenerTodos(ObtenerLotesRequest obtenerLotesRequest) {
        int skip = obtenerLotesRequest.getSkip();
        int limit = obtenerLotesRequest.getLimit();
        String search = obtenerLotesRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<LoteJpaModel> pagina = loteJpaRepository.findByBorradoFalse(pageable);

        List<Lote> datos = pagina.getContent().stream()
                .map(LoteJpaMapper::toDomain)
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
        LoteJpaModel model = loteJpaRepository.findByIdAndBorradoFalse(aUuid(loteId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId.getValue() + "' no existe"));

        model.setBorrado(true);
        model.setFecU(OffsetDateTime.now());
        loteJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
