package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Cliente de la cafeteria. Existe un cliente comodin, 'Mostrador', con id fijo: es el de toda
 * venta en la que no se identifica al comprador (arquitectura.md 4.11, CAJ-02), porque venta.id_cliente es
 * obligatorio. Lo siembra POST api/v1/seed y no se puede dar de baja.
 */
public class Cliente {
    public static final String ID_MOSTRADOR = "00000000-0000-0000-0000-000000000001";

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
        if (esMostrador()) {
            throw DomainException.throwException(409, "El cliente 'Mostrador' es el de las ventas sin cliente identificado; no se da de baja");
        }
        this.borrado = true;
    }

    public boolean esMostrador() {
        return this.id != null && ID_MOSTRADOR.equals(this.id.getValue());
    }

    public void actualizar(String personaId){
        this.personaId = personaId != null ? new PersonaId(personaId) : this.personaId;
    }

    public ClienteId getId() {return id;}
    public PersonaId getPersona() {return personaId;}
    public boolean isBorrado() {return borrado;}
}
