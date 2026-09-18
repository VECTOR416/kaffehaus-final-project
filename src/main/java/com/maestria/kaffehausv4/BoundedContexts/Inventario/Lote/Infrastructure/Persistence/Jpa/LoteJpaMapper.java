package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

/**
 * Mapea el agregado Lote. toDomain reconstruye la raiz junto con sus lineas vivas: no existe
 * un Lote de dominio a medias, sin detalles.
 */
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

    public static Lote toDomain(LoteJpaModel model, List<DetalleLoteJpaModel> detallesModel) {
        List<DetalleLote> detalles = detallesModel != null
                ? detallesModel.stream().map(DetalleLoteJpaMapper::toDomain).collect(Collectors.toList())
                : Collections.emptyList();

        return Lote.restaurar(
                model.getId().toString(),
                model.getFechaCompra(),
                model.getTotal(),
                new ProveedorId(model.getIdProveedor().toString()),
                new UsuarioId(model.getIdUsuario().toString()),
                model.getBorrado() != null && model.getBorrado(),
                detalles);
    }
}
