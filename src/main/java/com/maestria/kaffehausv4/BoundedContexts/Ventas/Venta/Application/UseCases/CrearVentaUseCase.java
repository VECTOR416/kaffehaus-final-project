package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearVentaUseCase {
    private final IVentaRepository ventaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IClienteRepository clienteRepository;
    private final IEstadoVentaRepository estadoVentaRepository;

    public CrearVentaUseCase(
            IVentaRepository ventaRepository,
            IUsuarioRepository usuarioRepository,
            IClienteRepository clienteRepository,
            IEstadoVentaRepository estadoVentaRepository) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.estadoVentaRepository = estadoVentaRepository;
    }

    public VentaResponse execute(CrearVentaRequest crearVentaRequest) {
        UsuarioId usuarioId = new UsuarioId(crearVentaRequest.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + crearVentaRequest.getIdUsuario() + "' no existe"));
        ClienteId clienteId = null;
        if (crearVentaRequest.getIdCliente() != null) {
            clienteId = new ClienteId(crearVentaRequest.getIdCliente());
            clienteRepository.obtenerPorId(clienteId)
                    .orElseThrow(() -> DomainException.throwException(404, "El cliente con id '" + crearVentaRequest.getIdCliente() + "' no existe"));
        }
        EstadoVentaId estadoVentaId = new EstadoVentaId(crearVentaRequest.getIdEstadoVenta());
        EstadoVenta estadoVenta = estadoVentaRepository.obtenerPorId(estadoVentaId)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta con id '" + crearVentaRequest.getIdEstadoVenta() + "' no existe"));

        Venta venta = Venta.crear(
                crearVentaRequest.getFechaVenta(),
                crearVentaRequest.getTotal(),
                usuarioId,
                clienteId,
                estadoVentaId);

        Venta ventaCreada = ventaRepository.crear(venta);

        return VentaResponse.createFromAggregate(ventaCreada, estadoVenta);
    }
}
