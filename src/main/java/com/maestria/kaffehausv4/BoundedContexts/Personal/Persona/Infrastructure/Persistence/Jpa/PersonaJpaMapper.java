package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;

/**
 * El agregado y la tabla no usan los mismos nombres: 'nombres' es la columna 'nombre'
 * y 'carnet' es la columna 'dni'. La traduccion vive aqui, que es el unico punto del
 * contexto que conoce ambos vocabularios.
 */
public class PersonaJpaMapper {
    private PersonaJpaMapper() {
    }

    public static PersonaJpaModel toPersistence(Persona persona) {
        PersonaJpaModel model = new PersonaJpaModel();
        model.setId(persona.getId() != null ? UUID.fromString(persona.getId().getValue()) : UUID.randomUUID());
        model.setNombre(persona.getNombres());
        model.setApellidos(persona.getApellidos());
        model.setTelefono(persona.getTelefono());
        model.setDni(persona.getCarnet());
        model.setBorrado(persona.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Persona persona, PersonaJpaModel model) {
        model.setNombre(persona.getNombres());
        model.setApellidos(persona.getApellidos());
        model.setTelefono(persona.getTelefono());
        model.setDni(persona.getCarnet());
        model.setBorrado(persona.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Persona toDomain(PersonaJpaModel model) {
        return Persona.restaurar(
                model.getId().toString(),
                model.getNombre(),
                model.getApellidos(),
                model.getTelefono(),
                model.getDni(),
                model.getBorrado() != null && model.getBorrado());
    }
}
