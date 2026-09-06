package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;

public class Cliente {
    private ClienteId id;
    private PersonaId personaId;
    private boolean borrado;

    private Cliente(ClienteId id, PersonaId personaId, boolean borrado) {
        this.id = id;
        this.personaId = personaId;
        this.borrado = borrado;
    }

    public static Cliente restaurar(
        String id,
        String personaId,
        boolean borrado
    ){
        return new Cliente(new ClienteId(id), new PersonaId(personaId), borrado);
    }

    public static Cliente crear(String personaId){
        return new Cliente(null, new PersonaId(personaId), false);
    }

    public void eliminar(){
        this.borrado = true;
    }

    public void reactivar(){
        this.borrado = false;
    }

    public void actualizar(String personaId){
        this.personaId = personaId != null ? new PersonaId(personaId) : this.personaId;
    }

    public ClienteId getId() {return id;}
    public void setId(ClienteId id) {this.id = id;}
    public PersonaId getPersona() {return personaId;}
    public void setPersona(PersonaId persona) {this.personaId = persona;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
