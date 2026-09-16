package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Datasources.IUsuarioDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.ObtenerUsuariosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository 
public class UsuarioDatasourceImpl implements IUsuarioDatasource{

    @Override
    public Usuario crear(Usuario usuario) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'crear'");
    }

    @Override
    public Usuario actualizar(UsuarioId usuarioId, Usuario usuario) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'actualizar'");
    }

    @Override
    public Optional<Usuario> obtenerPorId(UsuarioId usuarioId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'obtenerPorId'");
    }

    @Override
    public PaginatedResponse<List<Usuario>> obtenerTodos(ObtenerUsuariosRequest obtenerUsuariosRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'obtenerTodos'");
    }

    @Override
    public void eliminar(UsuarioId usuarioId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'eliminar'");
    }

}
