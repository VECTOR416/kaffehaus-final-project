package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Dtos.PersonaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.RolResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;

public class UsuarioResponse {
    private String id;
    private String correo;
    private PersonaResponse persona;
    private RolResponse rol;
    private boolean borrado;

    private UsuarioResponse(
        String id,
        String correo,
        PersonaResponse persona,
        RolResponse rol,
        boolean borrado
    ) {
        this.id = id;
        this.correo = correo;
        this.persona = persona;
        this.rol = rol;
        this.borrado = borrado;
    }

    public static UsuarioResponse createFromAggregate(Usuario usuario, Persona persona, Rol rol){
        PersonaResponse personaResponse = PersonaResponse.createFromAggregate(persona);
        RolResponse rolResponse = RolResponse.createFromAggregate(rol);
        return new UsuarioResponse(
            usuario.getId().getValue(),
            usuario.getCorreo(),
            personaResponse,
            rolResponse,
            usuario.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getCorreo() {return correo;}
    public PersonaResponse getPersona() {return persona;}
    public RolResponse getRol() {return rol;}
    public boolean isBorrado() {return borrado;}
}
