package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.TicketVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Recupera una venta por su numero de ticket (arquitectura.md 4.11, CAJ-14) con lo que el ticket imprime:
 * el nombre del cliente (cliente -> persona), el del cajero (usuario -> persona) y el de cada
 * tipo de pago. El "join" se compone aqui, con los repositorios de cada agregado, porque el
 * proyecto no escribe consultas a mano (arquitectura.md 4.3).
 */
@Service
@Transactional(readOnly = true)
public class ObtenerVentaPorTicketUseCase {
    private final IVentaRepository ventaRepository;
    private final IEstadoVentaRepository estadoVentaRepository;
    private final IClienteRepository clienteRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IPersonaRepository personaRepository;
    private final ITipoPagoRepository tipoPagoRepository;

    public ObtenerVentaPorTicketUseCase(
            IVentaRepository ventaRepository,
            IEstadoVentaRepository estadoVentaRepository,
            IClienteRepository clienteRepository,
            IUsuarioRepository usuarioRepository,
            IPersonaRepository personaRepository,
            ITipoPagoRepository tipoPagoRepository) {
        this.ventaRepository = ventaRepository;
        this.estadoVentaRepository = estadoVentaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.tipoPagoRepository = tipoPagoRepository;
    }

    public TicketVentaResponse execute(String numeroTicket) {
        Long numero = aNumeroDeTicket(numeroTicket);

        Venta venta = ventaRepository.obtenerPorNumeroTicket(numero)
                .orElseThrow(() -> DomainException.throwException(404, "No existe una venta con el ticket '" + numeroTicket + "'"));

        EstadoVenta estadoVenta = estadoVentaRepository.obtenerPorId(venta.getEstadoVentaId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta asociado a la venta no existe"));

        Cliente cliente = clienteRepository.obtenerPorId(venta.getClienteId())
                .orElseThrow(() -> DomainException.throwException(404, "El cliente asociado a la venta no existe"));
        Persona personaCliente = personaRepository.obtenerPorId(cliente.getPersona()).orElse(null);

        Persona personaCajero = usuarioRepository.obtenerPorId(venta.getUsuarioId())
                .flatMap(usuario -> personaRepository.obtenerPorId(usuario.getPersonaId()))
                .orElse(null);

        Map<String, TipoPago> tiposPago = new HashMap<>();
        for (PagoVenta pago : venta.getPagosActivos()) {
            tiposPago.computeIfAbsent(pago.getTipoPagoId().getValue(),
                    id -> tipoPagoRepository.obtenerPorId(pago.getTipoPagoId()).orElse(null));
        }

        return TicketVentaResponse.createFromAggregate(venta, estadoVenta, personaCliente, personaCajero, tiposPago);
    }

    private static Long aNumeroDeTicket(String numeroTicket) {
        try {
            long numero = Long.parseLong(numeroTicket.trim());
            if (numero <= 0) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException | NullPointerException e) {
            throw DomainException.throwException(400, "El numero de ticket '" + numeroTicket + "' no es valido");
        }
    }
}
