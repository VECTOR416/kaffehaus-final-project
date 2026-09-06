package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Datasources;

import java.util.Optional;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;

public interface IPersonaDatasource {
    Persona crear(Persona persona);
    Persona actualizar(PersonaId personaId, Persona persona);
    Optional<Persona> obtenerPorId(PersonaId personaId);
}