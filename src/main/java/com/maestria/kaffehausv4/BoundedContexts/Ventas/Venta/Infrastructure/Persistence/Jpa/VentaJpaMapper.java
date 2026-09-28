package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;

/**
 * Mapea el agregado Venta. toDomain reconstruye la raiz junto con sus lineas y sus pagos: no
 * existe una Venta de dominio a medias. 'numero_ticket' no se escribe nunca: lo asigna la base.
 */
public class VentaJpaMapper {
    private VentaJpaMapper() {
    }

    public static VentaJpaModel toPersistence(Venta venta) {
        VentaJpaModel model = new VentaJpaModel();
        model.setId(venta.getId() != null ? UUID.fromString(venta.getId().getValue()) : UUID.randomUUID());
        model.setFechaVenta(venta.getFechaVenta());
        model.setTotal(venta.getTotal());
        model.setIdUsuario(UUID.fromString(venta.getUsuarioId().getValue()));
        model.setIdCliente(venta.getClienteId() != null ? UUID.fromString(venta.getClienteId().getValue()) : null);
        model.setIdSesionCaja(UUID.fromString(venta.getSesionCajaId().getValue()));
        model.setIdEstadoVenta(UUID.fromString(venta.getEstadoVentaId().getValue()));
        model.setClaveIdempotencia(venta.getClaveIdempotencia());
        model.setMotivoAnulacion(venta.getMotivoAnulacion());
        model.setIdUsuarioAnulacion(venta.getUsuarioAnulacionId() != null ? UUID.fromString(venta.getUsuarioAnulacionId().getValue()) : null);
        model.setBorrado(venta.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Venta venta, VentaJpaModel model) {
        model.setFechaVenta(venta.getFechaVenta());
        model.setTotal(venta.getTotal());
        model.setIdUsuario(UUID.fromString(venta.getUsuarioId().getValue()));
        model.setIdCliente(venta.getClienteId() != null ? UUID.fromString(venta.getClienteId().getValue()) : null);
        model.setIdEstadoVenta(UUID.fromString(venta.getEstadoVentaId().getValue()));
        model.setClaveIdempotencia(venta.getClaveIdempotencia());
        model.setMotivoAnulacion(venta.getMotivoAnulacion());
        model.setIdUsuarioAnulacion(venta.getUsuarioAnulacionId() != null ? UUID.fromString(venta.getUsuarioAnulacionId().getValue()) : null);
        model.setBorrado(venta.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Venta toDomain(VentaJpaModel model, List<DetalleVentaJpaModel> detallesModel, List<PagoVentaJpaModel> pagosModel) {
        List<DetalleVenta> detalles = detallesModel != null
                ? detallesModel.stream().map(DetalleVentaJpaMapper::toDomain).collect(Collectors.toList())
                : Collections.emptyList();
        List<PagoVenta> pagos = pagosModel != null
                ? pagosModel.stream().map(PagoVentaJpaMapper::toDomain).collect(Collectors.toList())
                : Collections.emptyList();

        return Venta.restaurar(
                model.getId().toString(),
                model.getNumeroTicket(),
                model.getFechaVenta(),
                model.getTotal(),
                new UsuarioId(model.getIdUsuario().toString()),
                model.getIdCliente() != null ? new ClienteId(model.getIdCliente().toString()) : null,
                model.getIdSesionCaja() != null ? new SesionCajaId(model.getIdSesionCaja().toString()) : null,
                new EstadoVentaId(model.getIdEstadoVenta().toString()),
                model.getClaveIdempotencia(),
                model.getMotivoAnulacion(),
                model.getIdUsuarioAnulacion() != null ? new UsuarioId(model.getIdUsuarioAnulacion().toString()) : null,
                model.getBorrado() != null && model.getBorrado(),
                detalles,
                pagos);
    }
}
