package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Datasources.IProveedorDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ObtenerProveedoresRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa.ProveedorJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa.ProveedorJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa.ProveedorJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class ProveedorDatasourceImpl implements IProveedorDatasource {

    private final ProveedorJpaRepository proveedorJpaRepository;

    public ProveedorDatasourceImpl(ProveedorJpaRepository proveedorJpaRepository) {
        this.proveedorJpaRepository = proveedorJpaRepository;
    }

    @Override
    public Proveedor crear(Proveedor proveedor) {
        ProveedorJpaModel model = ProveedorJpaMapper.toPersistence(proveedor);
        return ProveedorJpaMapper.toDomain(proveedorJpaRepository.save(model));
    }

    @Override
    public Proveedor actualizar(ProveedorId proveedorId, Proveedor proveedor) {
        ProveedorJpaModel model = proveedorJpaRepository.findByIdAndBorradoFalse(aUuid(proveedorId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El proveedor con id '" + proveedorId.getValue() + "' no existe"));

        ProveedorJpaMapper.copiarCambios(proveedor, model);

        return ProveedorJpaMapper.toDomain(proveedorJpaRepository.save(model));
    }

    @Override
    public Optional<Proveedor> obtenerPorId(ProveedorId proveedorId) {
        return proveedorJpaRepository.findByIdAndBorradoFalse(aUuid(proveedorId.getValue()))
                .map(ProveedorJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Proveedor>> obtenerTodos(ObtenerProveedoresRequest obtenerProveedoresRequest) {
        int skip = obtenerProveedoresRequest.getSkip();
        int limit = obtenerProveedoresRequest.getLimit();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<ProveedorJpaModel> pagina = proveedorJpaRepository.findByBorradoFalse(pageable);

        List<Proveedor> datos = pagina.getContent().stream()
                .map(ProveedorJpaMapper::toDomain)
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
    public void eliminar(ProveedorId proveedorId) {
        ProveedorJpaModel model = proveedorJpaRepository.findByIdAndBorradoFalse(aUuid(proveedorId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El proveedor con id '" + proveedorId.getValue() + "' no existe"));

        Proveedor proveedor = ProveedorJpaMapper.toDomain(model);
        proveedor.eliminar();

        ProveedorJpaMapper.copiarCambios(proveedor, model);
        proveedorJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
