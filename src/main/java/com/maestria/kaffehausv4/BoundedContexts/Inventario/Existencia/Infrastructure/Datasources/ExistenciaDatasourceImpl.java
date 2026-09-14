package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Datasources.IExistenciaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ObtenerExistenciasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa.ExistenciaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa.ExistenciaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa.ExistenciaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class ExistenciaDatasourceImpl implements IExistenciaDatasource {

    private final ExistenciaJpaRepository existenciaJpaRepository;

    public ExistenciaDatasourceImpl(ExistenciaJpaRepository existenciaJpaRepository) {
        this.existenciaJpaRepository = existenciaJpaRepository;
    }

    @Override
    public Existencia crear(Existencia existencia) {
        ExistenciaJpaModel model = ExistenciaJpaMapper.toPersistence(existencia);
        return ExistenciaJpaMapper.toDomain(existenciaJpaRepository.save(model));
    }

    @Override
    public Existencia actualizar(ExistenciaId existenciaId, Existencia existencia) {
        ExistenciaJpaModel model = existenciaJpaRepository.findByIdAndBorradoFalse(aUuid(existenciaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La existencia con id '" + existenciaId.getValue() + "' no existe"));

        ExistenciaJpaMapper.copiarCambios(existencia, model);

        return ExistenciaJpaMapper.toDomain(existenciaJpaRepository.save(model));
    }

    @Override
    public Optional<Existencia> obtenerPorId(ExistenciaId existenciaId) {
        return existenciaJpaRepository.findByIdAndBorradoFalse(aUuid(existenciaId.getValue()))
                .map(ExistenciaJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Existencia>> obtenerTodos(ObtenerExistenciasRequest obtenerExistenciasRequest) {
        int skip = obtenerExistenciasRequest.getSkip();
        int limit = obtenerExistenciasRequest.getLimit();
        //String search = obtenerExistenciasRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<ExistenciaJpaModel> pagina = existenciaJpaRepository.findByBorradoFalse(pageable);

        List<Existencia> datos = pagina.getContent().stream()
                .map(ExistenciaJpaMapper::toDomain)
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
    public void eliminar(ExistenciaId existenciaId) {
        ExistenciaJpaModel model = existenciaJpaRepository.findByIdAndBorradoFalse(aUuid(existenciaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La existencia con id '" + existenciaId.getValue() + "' no existe"));

        model.setBorrado(true);
        model.setFecU(OffsetDateTime.now());
        existenciaJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
