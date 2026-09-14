package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ActualizarVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarVentaUseCase {
    private final IVentaRepository ventaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IClienteRepository clienteRepository;
    private final IEstadoVentaRepository estadoVentaRepository;

    public ActualizarVentaUseCase(
            IVentaRepository ventaRepository,
            IUsuarioRepository usuarioRepository,
            IClienteRepository clienteRepository,
            IEstadoVentaRepository estadoVentaRepository) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.estadoVentaRepository = estadoVentaRepository;
    }

    public VentaResponse execute(String ventaId, ActualizarVentaRequest actualizarVentaRequest) {
        VentaId id = new VentaId(ventaId);

        Venta venta = ventaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + ventaId + "' no existe"));

        UsuarioId usuarioId = null;
        if (actualizarVentaRequest.getIdUsuario() != null) {
            usuarioId = new UsuarioId(actualizarVentaRequest.getIdUsuario());
            usuarioRepository.obtenerPorId(usuarioId)
                    .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + actualizarVentaRequest.getIdUsuario() + "' no existe"));
        }
        ClienteId clienteId = null;
        if (actualizarVentaRequest.getIdCliente() != null) {
            clienteId = new ClienteId(actualizarVentaRequest.getIdCliente());
            clienteRepository.obtenerPorId(clienteId)
                    .orElseThrow(() -> DomainException.throwException(404, "El cliente con id '" + actualizarVentaRequest.getIdCliente() + "' no existe"));
        }
        EstadoVentaId estadoVentaId = null;
        if (actualizarVentaRequest.getIdEstadoVenta() != null) {
            estadoVentaId = new EstadoVentaId(actualizarVentaRequest.getIdEstadoVenta());
            estadoVentaRepository.obtenerPorId(estadoVentaId)
                    .orElseThrow(() -> DomainException.throwException(404, "El estado de venta con id '" + actualizarVentaRequest.getIdEstadoVenta() + "' no existe"));
        }

        venta.actualizar(
                actualizarVentaRequest.getFechaVenta(),
                actualizarVentaRequest.getTotal(),
                usuarioId,
                clienteId,
                estadoVentaId);

        Venta ventaActualizada = ventaRepository.actualizar(id, venta);

        return toResponse(ventaActualizada);
    }

    private VentaResponse toResponse(Venta venta) {
        EstadoVenta estadoVenta = estadoVentaRepository.obtenerPorId(venta.getEstadoVentaId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta asociado a la venta no existe"));
        return VentaResponse.createFromAggregate(venta, estadoVenta);
    }
}
