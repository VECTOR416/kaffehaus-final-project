package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Datasources.IConversionDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ObtenerConversionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.ValueObjects.ConversionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Persistence.Jpa.ConversionJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Persistence.Jpa.ConversionJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Persistence.Jpa.ConversionJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class ConversionDatasourceImpl implements IConversionDatasource {

    private final ConversionJpaRepository conversionJpaRepository;

    public ConversionDatasourceImpl(ConversionJpaRepository conversionJpaRepository) {
        this.conversionJpaRepository = conversionJpaRepository;
    }

    @Override
    public Conversion crear(Conversion conversion) {
        ConversionJpaModel model = ConversionJpaMapper.toPersistence(conversion);
        return ConversionJpaMapper.toDomain(conversionJpaRepository.save(model));
    }

    @Override
    public Conversion actualizar(ConversionId conversionId, Conversion conversion) {
        ConversionJpaModel model = conversionJpaRepository.findByIdAndBorradoFalse(aUuid(conversionId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La conversion con id '" + conversionId.getValue() + "' no existe"));

        ConversionJpaMapper.copiarCambios(conversion, model);

        return ConversionJpaMapper.toDomain(conversionJpaRepository.save(model));
    }

    @Override
    public Optional<Conversion> obtenerPorId(ConversionId conversionId) {
        return conversionJpaRepository.findByIdAndBorradoFalse(aUuid(conversionId.getValue()))
                .map(ConversionJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Conversion>> obtenerTodos(ObtenerConversionesRequest obtenerConversionesRequest) {
        int skip = obtenerConversionesRequest.getSkip();
        int limit = obtenerConversionesRequest.getLimit();
        String search = obtenerConversionesRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<ConversionJpaModel> pagina = conversionJpaRepository.findByBorradoFalse(pageable);

        List<Conversion> datos = pagina.getContent().stream()
                .map(ConversionJpaMapper::toDomain)
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
    public void eliminar(ConversionId conversionId) {
        ConversionJpaModel model = conversionJpaRepository.findByIdAndBorradoFalse(aUuid(conversionId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La conversion con id '" + conversionId.getValue() + "' no existe"));

        model.setBorrado(true);
        model.setFecU(OffsetDateTime.now());
        conversionJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
