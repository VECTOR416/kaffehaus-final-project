package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Datasources.IUsuarioDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.ObtenerUsuariosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class UsuarioRepositoryImpl implements IUsuarioRepository{

    private final IUsuarioDatasource usuarioDatasource;

    public UsuarioRepositoryImpl(IUsuarioDatasource usuarioDatasource) {
        this.usuarioDatasource = usuarioDatasource;
    }

    @Override
    public Usuario crear(Usuario usuario) {
        return this.usuarioDatasource.crear(usuario);
    }

    @Override
    public Usuario actualizar(UsuarioId usuarioId, Usuario usuario) {
        return this.usuarioDatasource.actualizar(usuarioId, usuario);
    }

    @Override
    public Optional<Usuario> obtenerPorId(UsuarioId usuarioId) {
        return this.usuarioDatasource.obtenerPorId(usuarioId);
    }

    @Override
    public Optional<Usuario> obtenerPorCorreo(String correo) {
        return this.usuarioDatasource.obtenerPorCorreo(correo);
    }

    @Override
    public PaginatedResponse<List<Usuario>> obtenerTodos(ObtenerUsuariosRequest obtenerUsuariosRequest) {
        return this.usuarioDatasource.obtenerTodos(obtenerUsuariosRequest);
    }

    @Override
    public void eliminar(UsuarioId usuarioId) {
        this.usuarioDatasource.eliminar(usuarioId);
    }

    @Override
    public long contarPorRol(RolId rolId) {
        return this.usuarioDatasource.contarPorRol(rolId);
    }
}
