package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Dtos.PersonaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;

public class ClienteResponse {
    private String id;
    private PersonaResponse persona;
    private boolean borrado;

    private ClienteResponse(String id, PersonaResponse persona, boolean borrado) {
        this.id = id;
        this.persona = persona;
        this.borrado = borrado;
    }

    public static ClienteResponse createFromAggregate(Cliente cliente, Persona persona){
        PersonaResponse personaResponse = PersonaResponse.createFromAggregate(persona);
        return new ClienteResponse(
            cliente.getId().getValue(), 
            personaResponse, 
            cliente.isBorrado()
        );
    }

    public String getId() {return id;}
    public PersonaResponse getPersona() {return persona;}
    public boolean isBorrado() {return borrado;}
}
