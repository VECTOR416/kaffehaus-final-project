package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.Shared.Domain.Security.RolDelSistema;

/**
 * Datos del usuario que acompanan a los tokens. Se compone de tres agregados distintos
 * (Usuario, Persona y el rol), asi que la factoria los recibe ya resueltos por el caso de uso.
 */
public class UsuarioAutenticadoResponse {
    private String id;
    private String email;
    private String nombreCompleto;
    private RolResponse rol;

    private UsuarioAutenticadoResponse(
        String id,
        String email,
        String nombreCompleto,
        RolResponse rol
    ) {
        this.id = id;
        this.email = email;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
    }

    public static UsuarioAutenticadoResponse createFromAggregate(Usuario usuario, Persona persona, RolDelSistema rol){
        return new UsuarioAutenticadoResponse(
            usuario.getId().getValue(),
            usuario.getCorreo(),
            nombreCompletoDe(persona),
            RolResponse.createFromAggregate(rol)
        );
    }

    /** El nombre completo no existe como columna: se arma con los dos campos de Persona. */
    public static String nombreCompletoDe(Persona persona) {
        return (persona.getNombres() + " " + persona.getApellidos()).trim();
    }

    public String getId() {return id;}
    public String getEmail() {return email;}
    public String getNombreCompleto() {return nombreCompleto;}
    public RolResponse getRol() {return rol;}
}
