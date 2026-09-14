package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

public class Usuario {
    private UsuarioId id;
    private PersonaId personaId;
    private String correo;
    private String contrasena;
    private RolId rolId;
    private boolean borrado;

    private Usuario(
        UsuarioId id,
        PersonaId personaId,
        String correo,
        String contrasena,
        RolId rolId,
        boolean borrado
    ){
        this.id = id;
        this.personaId = personaId;
        this.correo = correo;
        this.contrasena = contrasena;
        this.rolId = rolId;
        this.borrado = borrado;
    }

    public static Usuario restaurar(
        String id,
        String personaId,
        String correo,
        String contrasena,
        boolean borrado,
        RolId rolId
    ){
        return new Usuario(new UsuarioId(id), new PersonaId(personaId), correo, contrasena, rolId, borrado);
    }

    public static Usuario crear(
        PersonaId personaId,
        String correo,
        String contrasena,
        RolId rolId
    ){
        return new Usuario(null, personaId, correo, contrasena, rolId, false);
    }

    public void actualizar(
        String personaId,
        String correo,
        String contrasena,
        String rolId
    ){
        this.personaId = personaId != null ? new PersonaId(personaId) : this.personaId;
        this.correo = correo != null ? correo : this.correo;
        this.contrasena = contrasena != null ? contrasena : this.contrasena;
        this.rolId = rolId != null ? new RolId(rolId) : this.rolId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public UsuarioId getId() {return id;}
    public PersonaId getPersonaId() {return personaId;}
    public String getCorreo() {return correo;}
    public String getContrasena() {return contrasena;}
    public RolId getRolId() {return rolId;}
    public boolean isBorrado() {return borrado;}
}
