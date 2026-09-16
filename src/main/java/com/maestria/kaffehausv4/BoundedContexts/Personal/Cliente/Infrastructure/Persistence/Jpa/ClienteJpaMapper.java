package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;

public class ClienteJpaMapper {
    private ClienteJpaMapper() {
    }

    public static ClienteJpaModel toPersistence(Cliente cliente) {
        ClienteJpaModel model = new ClienteJpaModel();
        model.setId(cliente.getId() != null ? UUID.fromString(cliente.getId().getValue()) : UUID.randomUUID());
        model.setIdPersona(UUID.fromString(cliente.getPersona().getValue()));
        model.setBorrado(cliente.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Cliente cliente, ClienteJpaModel model) {
        model.setIdPersona(UUID.fromString(cliente.getPersona().getValue()));
        model.setBorrado(cliente.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Cliente toDomain(ClienteJpaModel model) {
        return Cliente.restaurar(
                model.getId().toString(),
                model.getIdPersona().toString(),
                model.getBorrado() != null && model.getBorrado());
    }
}
