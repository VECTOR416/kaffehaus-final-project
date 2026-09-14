package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Datasources.IDescuentoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ObtenerDescuentosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects.DescuentoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa.DescuentoJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa.DescuentoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa.DescuentoJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class DescuentoDatasourceImpl implements IDescuentoDatasource {

    private final DescuentoJpaRepository descuentoJpaRepository;

    public DescuentoDatasourceImpl(DescuentoJpaRepository descuentoJpaRepository) {
        this.descuentoJpaRepository = descuentoJpaRepository;
    }

    @Override
    public Descuento crear(Descuento descuento) {
        DescuentoJpaModel model = DescuentoJpaMapper.toPersistence(descuento);
        return DescuentoJpaMapper.toDomain(descuentoJpaRepository.save(model));
    }

    @Override
    public Descuento actualizar(DescuentoId descuentoId, Descuento descuento) {
        DescuentoJpaModel model = descuentoJpaRepository.findByIdAndBorradoFalse(aUuid(descuentoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El descuento con id '" + descuentoId.getValue() + "' no existe"));

        DescuentoJpaMapper.copiarCambios(descuento, model);

        return DescuentoJpaMapper.toDomain(descuentoJpaRepository.save(model));
    }

    @Override
    public Optional<Descuento> obtenerPorId(DescuentoId descuentoId) {
        return descuentoJpaRepository.findByIdAndBorradoFalse(aUuid(descuentoId.getValue()))
                .map(DescuentoJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Descuento>> obtenerTodos(ObtenerDescuentosRequest obtenerDescuentosRequest) {
        int skip = obtenerDescuentosRequest.getSkip();
        int limit = obtenerDescuentosRequest.getLimit();
        String search = obtenerDescuentosRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<DescuentoJpaModel> pagina = descuentoJpaRepository.findByBorradoFalse(pageable);

        List<Descuento> datos = pagina.getContent().stream()
                .map(DescuentoJpaMapper::toDomain)
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
    public void eliminar(DescuentoId descuentoId) {
        DescuentoJpaModel model = descuentoJpaRepository.findByIdAndBorradoFalse(aUuid(descuentoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El descuento con id '" + descuentoId.getValue() + "' no existe"));

        model.setBorrado(true);
        model.setFecU(OffsetDateTime.now());
        descuentoJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
