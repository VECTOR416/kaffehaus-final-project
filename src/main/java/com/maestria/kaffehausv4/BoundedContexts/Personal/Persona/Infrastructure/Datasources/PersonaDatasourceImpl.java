package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Datasources;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Datasources.IPersonaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Este contexto no expone 'obtenerTodos' ni 'eliminar' porque Persona no tiene
 * controlador: se crea, se lee y se actualiza siempre a traves de Cliente, Proveedor
 * o Usuario, que son sus duenos.
 */
@Repository
public class PersonaDatasourceImpl implements IPersonaDatasource {

    private final PersonaJpaRepository personaJpaRepository;

    public PersonaDatasourceImpl(PersonaJpaRepository personaJpaRepository) {
        this.personaJpaRepository = personaJpaRepository;
    }

    @Override
    public Persona crear(Persona persona) {
        PersonaJpaModel model = PersonaJpaMapper.toPersistence(persona);
        return PersonaJpaMapper.toDomain(personaJpaRepository.save(model));
    }

    @Override
    public Persona actualizar(PersonaId personaId, Persona persona) {
        PersonaJpaModel model = personaJpaRepository.findByIdAndBorradoFalse(aUuid(personaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La persona con id '" + personaId.getValue() + "' no existe"));

        PersonaJpaMapper.copiarCambios(persona, model);

        return PersonaJpaMapper.toDomain(personaJpaRepository.save(model));
    }

    @Override
    public Optional<Persona> obtenerPorId(PersonaId personaId) {
        return personaJpaRepository.findByIdAndBorradoFalse(aUuid(personaId.getValue()))
                .map(PersonaJpaMapper::toDomain);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
