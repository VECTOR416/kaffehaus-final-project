package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Datasources.IRolDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.ObtenerRolesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class RolDatasourceImpl implements IRolDatasource {

    private final RolJpaRepository rolJpaRepository;

    public RolDatasourceImpl(RolJpaRepository rolJpaRepository) {
        this.rolJpaRepository = rolJpaRepository;
    }

    @Override
    public Rol crear(Rol rol) {
        RolJpaModel model = RolJpaMapper.toPersistence(rol);
        return RolJpaMapper.toDomain(rolJpaRepository.save(model));
    }

    @Override
    public Rol actualizar(RolId rolId, Rol rol) {
        RolJpaModel model = rolJpaRepository.findByIdAndBorradoFalse(aUuid(rolId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El rol con id '" + rolId.getValue() + "' no existe"));

        RolJpaMapper.copiarCambios(rol, model);

        return RolJpaMapper.toDomain(rolJpaRepository.save(model));
    }

    @Override
    public Optional<Rol> obtenerPorId(RolId rolId) {
        return rolJpaRepository.findByIdAndBorradoFalse(aUuid(rolId.getValue()))
                .map(RolJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Rol>> obtenerTodos(ObtenerRolesRequest obtenerRolesRequest) {
        int skip = obtenerRolesRequest.getSkip();
        int limit = obtenerRolesRequest.getLimit();
        String search = obtenerRolesRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<RolJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? rolJpaRepository.findByBorradoFalse(pageable)
                : rolJpaRepository.findByBorradoFalseAndNombreContainingIgnoreCase(search.trim(), pageable);

        List<Rol> datos = pagina.getContent().stream()
                .map(RolJpaMapper::toDomain)
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
    public void eliminar(RolId rolId) {
        RolJpaModel model = rolJpaRepository.findByIdAndBorradoFalse(aUuid(rolId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El rol con id '" + rolId.getValue() + "' no existe"));

        Rol rol = RolJpaMapper.toDomain(model);
        rol.eliminar();

        RolJpaMapper.copiarCambios(rol, model);
        rolJpaRepository.save(model);
    }

    @Override
    public Optional<Rol> obtenerPorNombre(String nombre) {
        return rolJpaRepository.findFirstByNombreIgnoreCaseAndBorradoFalse(nombre)
                .map(RolJpaMapper::toDomain);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
