package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
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
        model.setIdProveedor(lote.getProveedorId() != null ? UUID.fromString(lote.getProveedorId().getValue()) : null);
        model.setIdUsuario(UUID.fromString(lote.getUsuarioId().getValue()));
        model.setEsProduccion(lote.isEsProduccion());
        model.setIdProduccion(lote.getProduccionId() != null ? UUID.fromString(lote.getProduccionId().getValue()) : null);
        model.setBorrado(lote.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Lote lote, LoteJpaModel model) {
        model.setFechaCompra(lote.getFechaCompra());
        model.setTotal(lote.getTotal());
        model.setIdProveedor(lote.getProveedorId() != null ? UUID.fromString(lote.getProveedorId().getValue()) : null);
        model.setIdUsuario(UUID.fromString(lote.getUsuarioId().getValue()));
        model.setEsProduccion(lote.isEsProduccion());
        model.setIdProduccion(lote.getProduccionId() != null ? UUID.fromString(lote.getProduccionId().getValue()) : null);
        model.setBorrado(lote.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    /** El mismo agregado reconstruido desde sus proyecciones de lectura (arquitectura.md 5.7). */
    public static Lote toDomain(LoteLecturaProyeccion lectura, List<DetalleLoteLecturaProyeccion> detallesLectura) {
        List<DetalleLote> detalles = detallesLectura.stream()
                .map(DetalleLoteJpaMapper::toDomain)
                .collect(Collectors.toList());

        return Lote.restaurar(
                lectura.getId().toString(),
                lectura.getFechaCompra(),
                lectura.getTotal(),
                lectura.getIdProveedor() != null ? new ProveedorId(lectura.getIdProveedor().toString()) : null,
                new UsuarioId(lectura.getIdUsuario().toString()),
                lectura.getEsProduccion() != null && lectura.getEsProduccion(),
                lectura.getIdProduccion() != null ? new ProduccionId(lectura.getIdProduccion().toString()) : null,
                lectura.getBorrado() != null && lectura.getBorrado(),
                detalles);
    }

    public static Lote toDomain(LoteJpaModel model, List<DetalleLoteJpaModel> detallesModel) {
        List<DetalleLote> detalles = detallesModel != null
                ? detallesModel.stream().map(DetalleLoteJpaMapper::toDomain).collect(Collectors.toList())
                : Collections.emptyList();

        return Lote.restaurar(
                model.getId().toString(),
                model.getFechaCompra(),
                model.getTotal(),
                model.getIdProveedor() != null ? new ProveedorId(model.getIdProveedor().toString()) : null,
                new UsuarioId(model.getIdUsuario().toString()),
                model.getEsProduccion() != null && model.getEsProduccion(),
                model.getIdProduccion() != null ? new ProduccionId(model.getIdProduccion().toString()) : null,
                model.getBorrado() != null && model.getBorrado(),
                detalles);
    }
}
