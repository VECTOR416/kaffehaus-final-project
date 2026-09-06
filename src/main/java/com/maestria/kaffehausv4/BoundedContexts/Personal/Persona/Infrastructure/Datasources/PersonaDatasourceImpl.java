package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Datasources;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Datasources.IPersonaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;

@Repository 
public class PersonaDatasourceImpl implements IPersonaDatasource {

    @Override
    public Persona crear(Persona persona) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'crear'");
    }

    @Override
    public Persona actualizar(PersonaId personaId, Persona persona) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'actualizar'");
    }

    @Override
    public Optional<Persona> obtenerPorId(PersonaId personaId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'obtenerPorId'");
    }

}
