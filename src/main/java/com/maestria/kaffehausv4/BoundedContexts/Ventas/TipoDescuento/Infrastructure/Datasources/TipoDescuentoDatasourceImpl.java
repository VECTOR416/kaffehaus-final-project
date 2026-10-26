package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Datasources.ITipoDescuentoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.ObtenerTiposDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Infrastructure.Persistence.Jpa.TipoDescuentoJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Infrastructure.Persistence.Jpa.TipoDescuentoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Infrastructure.Persistence.Jpa.TipoDescuentoJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class TipoDescuentoDatasourceImpl implements ITipoDescuentoDatasource {

    private final TipoDescuentoJpaRepository tipoDescuentoJpaRepository;

    public TipoDescuentoDatasourceImpl(TipoDescuentoJpaRepository tipoDescuentoJpaRepository) {
        this.tipoDescuentoJpaRepository = tipoDescuentoJpaRepository;
    }

    @Override
    public TipoDescuento crear(TipoDescuento tipoDescuento) {
        TipoDescuentoJpaModel model = TipoDescuentoJpaMapper.toPersistence(tipoDescuento);
        return TipoDescuentoJpaMapper.toDomain(tipoDescuentoJpaRepository.save(model));
    }

    @Override
    public TipoDescuento actualizar(TipoDescuentoId tipoDescuentoId, TipoDescuento tipoDescuento) {
        TipoDescuentoJpaModel model = tipoDescuentoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoDescuentoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento con id '" + tipoDescuentoId.getValue() + "' no existe"));

        TipoDescuentoJpaMapper.copiarCambios(tipoDescuento, model);

        return TipoDescuentoJpaMapper.toDomain(tipoDescuentoJpaRepository.save(model));
    }

    @Override
    public Optional<TipoDescuento> obtenerPorId(TipoDescuentoId tipoDescuentoId) {
        return tipoDescuentoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoDescuentoId.getValue()))
                .map(TipoDescuentoJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<TipoDescuento>> obtenerTodos(ObtenerTiposDescuentoRequest obtenerTiposDescuentoRequest) {
        int skip = obtenerTiposDescuentoRequest.getSkip();
        int limit = obtenerTiposDescuentoRequest.getLimit();
        String search = obtenerTiposDescuentoRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<TipoDescuentoJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? tipoDescuentoJpaRepository.findByBorradoFalse(pageable)
                : tipoDescuentoJpaRepository.findByBorradoFalseAndNombreContainingIgnoreCase(search.trim(), pageable);

        List<TipoDescuento> datos = pagina.getContent().stream()
                .map(TipoDescuentoJpaMapper::toDomain)
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
    public void eliminar(TipoDescuentoId tipoDescuentoId) {
        TipoDescuentoJpaModel model = tipoDescuentoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoDescuentoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento con id '" + tipoDescuentoId.getValue() + "' no existe"));

        TipoDescuento tipoDescuento = TipoDescuentoJpaMapper.toDomain(model);
        tipoDescuento.eliminar();

        TipoDescuentoJpaMapper.copiarCambios(tipoDescuento, model);
        tipoDescuentoJpaRepository.save(model);
    }

    @Override
    public Optional<TipoDescuento> obtenerPorNombre(String nombre) {
        return tipoDescuentoJpaRepository.findFirstByNombreIgnoreCaseAndBorradoFalse(nombre)
                .map(TipoDescuentoJpaMapper::toDomain);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
