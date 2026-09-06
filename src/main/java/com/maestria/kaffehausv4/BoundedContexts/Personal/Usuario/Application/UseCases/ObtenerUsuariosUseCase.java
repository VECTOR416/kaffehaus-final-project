package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.ObtenerUsuariosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.UsuarioResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerUsuariosUseCase {
    private final IUsuarioRepository usuarioRepository;
    private final IPersonaRepository personaRepository;
    private final IRolRepository rolRepository;

    public ObtenerUsuariosUseCase(
            IUsuarioRepository usuarioRepository,
            IPersonaRepository personaRepository,
            IRolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
    }

    public PaginatedResponse<List<UsuarioResponse>> execute(ObtenerUsuariosRequest obtenerUsuariosRequest) {
        PaginatedResponse<List<Usuario>> resultado = usuarioRepository.obtenerTodos(obtenerUsuariosRequest);

        List<UsuarioResponse> usuarios = resultado.getData().stream()
                .map(this::toUsuarioResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                usuarios,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }

    private UsuarioResponse toUsuarioResponse(Usuario usuario) {
        Persona persona = personaRepository.obtenerPorId(usuario.getPersonaId())
                .orElseThrow(() -> DomainException.throwException(404, "La persona asociada al usuario no existe"));
        Rol rol = rolRepository.obtenerPorId(usuario.getRolId())
                .orElseThrow(() -> DomainException.throwException(404, "El rol asociado al usuario no existe"));
        return UsuarioResponse.createFromAggregate(usuario, persona, rol);
    }
}
