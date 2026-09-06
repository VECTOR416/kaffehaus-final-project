package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Repositories;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Datasources.IPersonaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;

@Repository 
public class PersonaRepositoryImpl implements  IPersonaRepository{

    private final IPersonaDatasource personaDatasource;

    public PersonaRepositoryImpl(IPersonaDatasource personaDatasource) {
        this.personaDatasource = personaDatasource;
    }

    @Override
    public Persona crear(Persona persona) {
        return this.personaDatasource.crear(persona);
    }

    @Override
    public Persona actualizar(PersonaId personaId, Persona persona) {
        return this.personaDatasource.actualizar(personaId, persona);
    }

    @Override
    public Optional<Persona> obtenerPorId(PersonaId personaId) {
        return this.personaDatasource.obtenerPorId(personaId);
    }

}
