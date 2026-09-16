package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;

public class UsuarioJpaMapper {
    private UsuarioJpaMapper() {
    }

    public static UsuarioJpaModel toPersistence(Usuario usuario) {
        UsuarioJpaModel model = new UsuarioJpaModel();
        model.setId(usuario.getId() != null ? UUID.fromString(usuario.getId().getValue()) : UUID.randomUUID());
        model.setIdPersona(UUID.fromString(usuario.getPersonaId().getValue()));
        model.setCorreo(usuario.getCorreo());
        model.setContrasena(usuario.getContrasena());
        model.setIdRol(UUID.fromString(usuario.getRolId().getValue()));
        model.setBorrado(usuario.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Usuario usuario, UsuarioJpaModel model) {
        model.setIdPersona(UUID.fromString(usuario.getPersonaId().getValue()));
        model.setCorreo(usuario.getCorreo());
        model.setContrasena(usuario.getContrasena());
        model.setIdRol(UUID.fromString(usuario.getRolId().getValue()));
        model.setBorrado(usuario.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Usuario toDomain(UsuarioJpaModel model) {
        return Usuario.restaurar(
                model.getId().toString(),
                model.getIdPersona().toString(),
                model.getCorreo(),
                model.getContrasena(),
                model.getBorrado() != null && model.getBorrado(),
                new RolId(model.getIdRol().toString()));
    }
}
