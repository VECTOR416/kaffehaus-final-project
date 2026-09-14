package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Datasources.IPasoRecetaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.ObtenerPasosRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.ValueObjects.PasoRecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Infrastructure.Persistence.Jpa.PasoRecetaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Infrastructure.Persistence.Jpa.PasoRecetaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Infrastructure.Persistence.Jpa.PasoRecetaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class PasoRecetaDatasourceImpl implements IPasoRecetaDatasource {

    private final PasoRecetaJpaRepository pasoRecetaJpaRepository;

    public PasoRecetaDatasourceImpl(PasoRecetaJpaRepository pasoRecetaJpaRepository) {
        this.pasoRecetaJpaRepository = pasoRecetaJpaRepository;
    }

    @Override
    public PasoReceta crear(PasoReceta pasoReceta) {
        PasoRecetaJpaModel model = PasoRecetaJpaMapper.toPersistence(pasoReceta);
        return PasoRecetaJpaMapper.toDomain(pasoRecetaJpaRepository.save(model));
    }

    @Override
    public PasoReceta actualizar(PasoRecetaId pasoRecetaId, PasoReceta pasoReceta) {
        PasoRecetaJpaModel model = pasoRecetaJpaRepository.findByIdAndBorradoFalse(aUuid(pasoRecetaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El paso de receta con id '" + pasoRecetaId.getValue() + "' no existe"));

        PasoRecetaJpaMapper.copiarCambios(pasoReceta, model);

        return PasoRecetaJpaMapper.toDomain(pasoRecetaJpaRepository.save(model));
    }

    @Override
    public Optional<PasoReceta> obtenerPorId(PasoRecetaId pasoRecetaId) {
        return pasoRecetaJpaRepository.findByIdAndBorradoFalse(aUuid(pasoRecetaId.getValue()))
                .map(PasoRecetaJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<PasoReceta>> obtenerTodos(ObtenerPasosRecetaRequest obtenerPasosRecetaRequest) {
        int skip = obtenerPasosRecetaRequest.getSkip();
        int limit = obtenerPasosRecetaRequest.getLimit();
        String search = obtenerPasosRecetaRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<PasoRecetaJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? pasoRecetaJpaRepository.findByBorradoFalse(pageable)
                : pasoRecetaJpaRepository.findByBorradoFalseAndDescripcionContainingIgnoreCase(search.trim(), pageable);

        List<PasoReceta> datos = pagina.getContent().stream()
                .map(PasoRecetaJpaMapper::toDomain)
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
    public void eliminar(PasoRecetaId pasoRecetaId) {
        PasoRecetaJpaModel model = pasoRecetaJpaRepository.findByIdAndBorradoFalse(aUuid(pasoRecetaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El paso de receta con id '" + pasoRecetaId.getValue() + "' no existe"));

        PasoReceta pasoReceta = PasoRecetaJpaMapper.toDomain(model);
        pasoReceta.eliminar();

        PasoRecetaJpaMapper.copiarCambios(pasoReceta, model);
        pasoRecetaJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
