package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ArqueoCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.CerrarSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.RecaudacionPorTipoPagoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Cierra la caja abierta con su arqueo (arquitectura.md 4.11, CAJ-22..CAJ-24):
 *
 * 1. Solo el titular del turno o un Administrador (CAJ-23).
 * 2. Bloquea la sesion en exclusiva (FOR UPDATE): espera a que terminen las ventas en curso,
 *    que la tienen con FOR SHARE, y ninguna venta nueva entra mientras se cierra.
 * 3. Suma por tipo de pago lo cobrado en las ventas completadas de la sesion. Las anuladas no
 *    cuentan: al anular se devuelve el dinero (CAJ-24).
 * 4. La raiz calcula el esperado (apertura + efectivo + ingresos - egresos) y la diferencia con
 *    lo declarado, y queda cerrada.
 */
@Service
@Transactional
public class CerrarSesionCajaUseCase {
    // Filas que el flujo resuelve por nombre, igual que el resto del catalogo (auth.md 5).
    private static final String ROL_ADMINISTRADOR = "Administrador";
    private static final String ESTADO_COMPLETADA = "Completada";

    private final ISesionCajaRepository sesionCajaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IRolRepository rolRepository;
    private final IVentaRepository ventaRepository;
    private final IEstadoVentaRepository estadoVentaRepository;
    private final ITipoPagoRepository tipoPagoRepository;

    public CerrarSesionCajaUseCase(
            ISesionCajaRepository sesionCajaRepository,
            IUsuarioRepository usuarioRepository,
            IRolRepository rolRepository,
            IVentaRepository ventaRepository,
            IEstadoVentaRepository estadoVentaRepository,
            ITipoPagoRepository tipoPagoRepository) {
        this.sesionCajaRepository = sesionCajaRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.ventaRepository = ventaRepository;
        this.estadoVentaRepository = estadoVentaRepository;
        this.tipoPagoRepository = tipoPagoRepository;
    }

    public ArqueoCajaResponse execute(CerrarSesionCajaRequest request) {
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + request.getIdUsuario() + "' no existe"));
        boolean esAdministrador = rolRepository.obtenerPorId(usuario.getRolId())
                .map(rol -> ROL_ADMINISTRADOR.equalsIgnoreCase(rol.getNombre()))
                .orElse(false);

        SesionCaja sesionCaja = sesionCajaRepository.obtenerAbiertaParaActualizar()
                .orElseThrow(() -> DomainException.throwException(409, "No hay una caja abierta que cerrar"));
        sesionCaja.exigirQuePuedaCerrar(usuarioId, esAdministrador);

        EstadoVenta completada = estadoVentaRepository.obtenerPorNombre(ESTADO_COMPLETADA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta '" + ESTADO_COMPLETADA + "' no esta registrado"));

        List<Venta> ventas = ventaRepository.obtenerPorSesionCaja(sesionCaja.getId());
        List<Venta> completadas = ventas.stream().filter(venta -> venta.estaEn(completada.getId())).toList();

        Map<String, Recaudacion> recaudacion = recaudar(completadas);
        BigDecimal ventasEnEfectivo = recaudacion.values().stream()
                .filter(Recaudacion::esEfectivo)
                .map(Recaudacion::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVendido = completadas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        sesionCaja.cerrar(request.getMontoCierreDeclarado(), ventasEnEfectivo, request.getMotivoDiferencia(), LocalDateTime.now());
        SesionCaja cerrada = sesionCajaRepository.actualizar(sesionCaja.getId(), sesionCaja);

        List<RecaudacionPorTipoPagoResponse> desglose = new ArrayList<>();
        for (Recaudacion porTipo : recaudacion.values()) {
            desglose.add(RecaudacionPorTipoPagoResponse.crear(porTipo.idTipoPago(), porTipo.nombre(), porTipo.monto(), porTipo.cantidad()));
        }

        return ArqueoCajaResponse.createFromAggregate(
                cerrada,
                ventasEnEfectivo,
                totalVendido,
                completadas.size(),
                ventas.size() - completadas.size(),
                desglose);
    }

    /**
     * Lo cobrado por tipo de pago. Parte de todos los tipos vivos para que el desglose muestre
     * tambien los que no se usaron (Efectivo, Tarjeta y QR siempre aparecen), y agrega al final
     * los de pagos cuyo tipo se dio de baja despues de cobrar.
     */
    private Map<String, Recaudacion> recaudar(List<Venta> completadas) {
        Map<String, Recaudacion> porTipo = new LinkedHashMap<>();
        for (TipoPago tipo : tipoPagoRepository.obtenerVivos()) {
            porTipo.put(tipo.getId().getValue(), new Recaudacion(tipo.getId().getValue(), tipo.getNombre(), tipo.esEfectivo(), BigDecimal.ZERO, 0));
        }
        for (Venta venta : completadas) {
            for (PagoVenta pago : venta.getPagosActivos()) {
                String idTipo = pago.getTipoPagoId().getValue();
                Recaudacion actual = porTipo.getOrDefault(idTipo,
                        new Recaudacion(idTipo, "Tipo de pago dado de baja", false, BigDecimal.ZERO, 0));
                porTipo.put(idTipo, actual.sumar(pago.getMonto()));
            }
        }
        return porTipo;
    }

    private record Recaudacion(String idTipoPago, String nombre, boolean esEfectivo, BigDecimal monto, long cantidad) {
        Recaudacion sumar(BigDecimal importe) {
            return new Recaudacion(idTipoPago, nombre, esEfectivo, monto.add(importe), cantidad + 1);
        }
    }
}
