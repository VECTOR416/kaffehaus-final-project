package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Datasources.IEstadoExistenciaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.ObtenerEstadosExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Persistence.Jpa.EstadoExistenciaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Persistence.Jpa.EstadoExistenciaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Persistence.Jpa.EstadoExistenciaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class EstadoExistenciaDatasourceImpl implements IEstadoExistenciaDatasource {

    private final EstadoExistenciaJpaRepository estadoExistenciaJpaRepository;

    public EstadoExistenciaDatasourceImpl(EstadoExistenciaJpaRepository estadoExistenciaJpaRepository) {
        this.estadoExistenciaJpaRepository = estadoExistenciaJpaRepository;
    }

    @Override
    public EstadoExistencia crear(EstadoExistencia estadoExistencia) {
        EstadoExistenciaJpaModel model = EstadoExistenciaJpaMapper.toPersistence(estadoExistencia);
        return EstadoExistenciaJpaMapper.toDomain(estadoExistenciaJpaRepository.save(model));
    }

    @Override
    public EstadoExistencia actualizar(EstadoExistenciaId estadoExistenciaId, EstadoExistencia estadoExistencia) {
        EstadoExistenciaJpaModel model = estadoExistenciaJpaRepository.findByIdAndBorradoFalse(aUuid(estadoExistenciaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia con id '" + estadoExistenciaId.getValue() + "' no existe"));

        EstadoExistenciaJpaMapper.copiarCambios(estadoExistencia, model);

        return EstadoExistenciaJpaMapper.toDomain(estadoExistenciaJpaRepository.save(model));
    }

    @Override
    public Optional<EstadoExistencia> obtenerPorId(EstadoExistenciaId estadoExistenciaId) {
        return estadoExistenciaJpaRepository.findByIdAndBorradoFalse(aUuid(estadoExistenciaId.getValue()))
                .map(EstadoExistenciaJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<EstadoExistencia>> obtenerTodos(ObtenerEstadosExistenciaRequest obtenerEstadosExistenciaRequest) {
        int skip = obtenerEstadosExistenciaRequest.getSkip();
        int limit = obtenerEstadosExistenciaRequest.getLimit();
        String search = obtenerEstadosExistenciaRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<EstadoExistenciaJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? estadoExistenciaJpaRepository.findByBorradoFalse(pageable)
                : estadoExistenciaJpaRepository.findByBorradoFalseAndNombreContainingIgnoreCase(search.trim(), pageable);

        List<EstadoExistencia> datos = pagina.getContent().stream()
                .map(EstadoExistenciaJpaMapper::toDomain)
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
    public void eliminar(EstadoExistenciaId estadoExistenciaId) {
        EstadoExistenciaJpaModel model = estadoExistenciaJpaRepository.findByIdAndBorradoFalse(aUuid(estadoExistenciaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia con id '" + estadoExistenciaId.getValue() + "' no existe"));

        EstadoExistencia estadoExistencia = EstadoExistenciaJpaMapper.toDomain(model);
        estadoExistencia.eliminar();

        EstadoExistenciaJpaMapper.copiarCambios(estadoExistencia, model);
        estadoExistenciaJpaRepository.save(model);
    }

    @Override
    public Optional<EstadoExistencia> obtenerPorNombre(String nombre) {
        return estadoExistenciaJpaRepository.findFirstByNombreIgnoreCaseAndBorradoFalse(nombre)
                .map(EstadoExistenciaJpaMapper::toDomain);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
