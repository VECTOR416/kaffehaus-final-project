package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories;

import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;

public interface IPersonaRepository {
    Persona crear(Persona persona);
    Persona actualizar(PersonaId personaId, Persona persona);
    Optional<Persona> obtenerPorId(PersonaId personaId);
}
