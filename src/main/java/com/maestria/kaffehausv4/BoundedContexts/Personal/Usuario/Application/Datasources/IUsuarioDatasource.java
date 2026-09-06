package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.ObtenerUsuariosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IUsuarioDatasource {
    Usuario crear(Usuario usuario);
    Usuario actualizar(UsuarioId usuarioId, Usuario usuario);
    Optional<Usuario> obtenerPorId(UsuarioId usuarioId);
    PaginatedResponse<List<Usuario>> obtenerTodos(ObtenerUsuariosRequest obtenerUsuariosRequest);
    void eliminar(UsuarioId usuarioId);
}
