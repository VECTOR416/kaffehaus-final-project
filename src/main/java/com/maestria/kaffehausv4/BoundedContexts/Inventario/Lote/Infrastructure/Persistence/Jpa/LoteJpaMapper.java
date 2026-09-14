package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

public class LoteJpaMapper {
    private LoteJpaMapper() {
    }

    public static LoteJpaModel toPersistence(Lote lote) {
        LoteJpaModel model = new LoteJpaModel();
        model.setId(lote.getId() != null ? UUID.fromString(lote.getId().getValue()) : UUID.randomUUID());
        model.setFechaCompra(lote.getFechaCompra());
        model.setTotal(lote.getTotal());
        model.setIdProveedor(UUID.fromString(lote.getProveedorId().getValue()));
        model.setIdUsuario(UUID.fromString(lote.getUsuarioId().getValue()));
        model.setBorrado(lote.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Lote lote, LoteJpaModel model) {
        model.setFechaCompra(lote.getFechaCompra());
        model.setTotal(lote.getTotal());
        model.setIdProveedor(UUID.fromString(lote.getProveedorId().getValue()));
        model.setIdUsuario(UUID.fromString(lote.getUsuarioId().getValue()));
        model.setBorrado(lote.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Lote toDomain(LoteJpaModel model) {
        return Lote.restaurar(
                model.getId().toString(),
                model.getFechaCompra(),
                model.getTotal(),
                new ProveedorId(model.getIdProveedor().toString()),
                new UsuarioId(model.getIdUsuario().toString()),
                model.getBorrado() != null && model.getBorrado());
    }
}
