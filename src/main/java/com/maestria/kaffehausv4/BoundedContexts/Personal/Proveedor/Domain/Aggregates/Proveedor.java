package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates;

import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;

public class Proveedor {
    private ProveedorId id;
    private PersonaId personaId;
    private EntidadId entidadId;
    private boolean borrado;

    private Proveedor(
        ProveedorId id,
        PersonaId personaId,
        EntidadId entidadId,
        boolean borrado
    ) {
        this.id = id;
        this.personaId = personaId;
        this.entidadId = entidadId;
        this.borrado = borrado;
    }

    public static Proveedor restaurar(
        String id,
        PersonaId personaId,
        EntidadId entidadId,
        LocalDateTime fecC,
        LocalDateTime fecU,
        boolean borrado
    ) {
        return new Proveedor(new ProveedorId(id), personaId, entidadId, borrado);
    }

    public static Proveedor crear(PersonaId personaId, EntidadId entidadId) {
        return new Proveedor(null, personaId, entidadId, false);
    }

    public void eliminar() {
        this.borrado = true;
    }

    public void reactivar() {
        this.borrado = false;
    }

    public void actualizar(PersonaId personaId, EntidadId entidadId) {
        this.personaId = personaId != null ? personaId : this.personaId;
        this.entidadId = entidadId;
    }

    public void desvincularEntidad() {
        this.entidadId = null;
    }

    public ProveedorId getId() { return id; }
    public void setId(ProveedorId id) { this.id = id; }

    public PersonaId getPersonaId() { return personaId; }
    public void setPersonaId(PersonaId personaId) { this.personaId = personaId; }

    public EntidadId getEntidadId() { return entidadId; }
    public void setEntidadId(EntidadId entidadId) { this.entidadId = entidadId; }

    public boolean isBorrado() { return borrado; }
    public void setBorrado(boolean borrado) { this.borrado = borrado; }
}
