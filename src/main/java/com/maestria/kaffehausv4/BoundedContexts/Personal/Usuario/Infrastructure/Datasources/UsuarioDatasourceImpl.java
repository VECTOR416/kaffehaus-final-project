package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Datasources.IUsuarioDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.ObtenerUsuariosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class UsuarioDatasourceImpl implements IUsuarioDatasource {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public UsuarioDatasourceImpl(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public Usuario crear(Usuario usuario) {
        UsuarioJpaModel model = UsuarioJpaMapper.toPersistence(usuario);
        return UsuarioJpaMapper.toDomain(usuarioJpaRepository.save(model));
    }

    @Override
    public Usuario actualizar(UsuarioId usuarioId, Usuario usuario) {
        UsuarioJpaModel model = usuarioJpaRepository.findByIdAndBorradoFalse(aUuid(usuarioId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + usuarioId.getValue() + "' no existe"));

        UsuarioJpaMapper.copiarCambios(usuario, model);

        return UsuarioJpaMapper.toDomain(usuarioJpaRepository.save(model));
    }

    @Override
    public Optional<Usuario> obtenerPorId(UsuarioId usuarioId) {
        return usuarioJpaRepository.findByIdAndBorradoFalse(aUuid(usuarioId.getValue()))
                .map(UsuarioJpaMapper::toDomain);
    }

    @Override
    public Optional<Usuario> obtenerPorCorreo(String correo) {
        return usuarioJpaRepository.findByCorreoIgnoreCaseAndBorradoFalse(correo)
                .map(UsuarioJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Usuario>> obtenerTodos(ObtenerUsuariosRequest obtenerUsuariosRequest) {
        int skip = obtenerUsuariosRequest.getSkip();
        int limit = obtenerUsuariosRequest.getLimit();
        String search = obtenerUsuariosRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<UsuarioJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? usuarioJpaRepository.findByBorradoFalse(pageable)
                : usuarioJpaRepository.findByBorradoFalseAndCorreoContainingIgnoreCase(search.trim(), pageable);

        List<Usuario> datos = pagina.getContent().stream()
                .map(UsuarioJpaMapper::toDomain)
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
    public void eliminar(UsuarioId usuarioId) {
        UsuarioJpaModel model = usuarioJpaRepository.findByIdAndBorradoFalse(aUuid(usuarioId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + usuarioId.getValue() + "' no existe"));

        Usuario usuario = UsuarioJpaMapper.toDomain(model);
        usuario.eliminar();

        UsuarioJpaMapper.copiarCambios(usuario, model);
        usuarioJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }

    @Override
    public long contarPorRol(RolId rolId) {
        return usuarioJpaRepository.countByIdRolAndBorradoFalse(aUuid(rolId.getValue()));
    }
}
