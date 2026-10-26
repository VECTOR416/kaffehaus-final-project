package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Payload compuesto del alta de una venta: encabezado, detalles y pagos.
 *
 * No admite 'total' (derivado) ni 'idEstadoVenta': una venta nace 'Completada' y el estado lo
 * asigna el sistema (ventas.md VEN-01, VEN-02). Tampoco 'idSesionCaja' ni 'numeroTicket': la
 * venta entra en la caja abierta y el ticket lo numera la base (CAJ-01, CAJ-14).
 * 'claveIdempotencia' es opcional: la genera el POS para que un reintento tras un timeout no
 * cobre dos veces (VEN-66). 'idCliente' es opcional: sin el, la venta es del cliente
 * 'Mostrador' (CAJ-02). 'pagos' debe sumar el total (CAJ-11). 'fechaVenta' solo la puede enviar
 * un Administrador, para una venta de contingencia (logica.md VENTA-28).
 */
public class CrearVentaRequest {
    private static final List<String> CAMPOS_DERIVADOS = List.of("total", "idEstadoVenta", "idSesionCaja", "numeroTicket");

    private LocalDateTime fechaVenta;
    private String idUsuario;
    private String idCliente;
    private String claveIdempotencia;
    private List<DetalleVentaRequest> detalles;
    private List<PagoVentaRequest> pagos;

    private CrearVentaRequest(
        LocalDateTime fechaVenta,
        String idUsuario,
        String idCliente,
        String claveIdempotencia,
        List<DetalleVentaRequest> detalles,
        List<PagoVentaRequest> pagos
    ) {
        this.fechaVenta = fechaVenta;
        this.idUsuario = idUsuario;
        this.idCliente = idCliente;
        this.claveIdempotencia = claveIdempotencia;
        this.detalles = detalles;
        this.pagos = pagos;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     */
    public static CrearVentaRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion){
        for (String campo : CAMPOS_DERIVADOS) {
            if (request.get(campo) != null) {
                throw DomainException.throwException(400, "El campo '" + campo + "' de una venta lo asigna el sistema; no se envia");
            }
        }
        // Nula si no se envia: la fija el caso de uso, que sabe si quien vende puede fijarla (VENTA-28).
        LocalDateTime fechaVenta = Validador.fechaOpcional(request.get("fechaVenta"), "fechaVenta");
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        String idCliente = Validador.textoOpcional(request.get("idCliente"), "idCliente");
        String claveIdempotencia = Validador.textoOpcional(request.get("claveIdempotencia"), "claveIdempotencia");

        List<DetalleVentaRequest> detalles = Validador.listaDeObjetosRequerida(request.get("detalles"), "detalles").stream()
                .map(DetalleVentaRequest::createFromRequest)
                .collect(Collectors.toList());

        // Opcional solo porque una venta de total cero no lleva pagos; la suma la exige la raiz.
        Object pagosEnviados = request.get("pagos");
        List<Map<String, Object>> pagosRecibidos = pagosEnviados instanceof List<?> lista && lista.isEmpty()
                ? null
                : Validador.listaDeObjetosOpcional(pagosEnviados, "pagos");
        List<PagoVentaRequest> pagos = pagosRecibidos != null
                ? pagosRecibidos.stream().map(PagoVentaRequest::createFromRequest).collect(Collectors.toList())
                : List.of();

        return new CrearVentaRequest(fechaVenta, idUsuario, idCliente, claveIdempotencia, detalles, pagos);
    }

    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdCliente() {return idCliente;}
    public String getClaveIdempotencia() {return claveIdempotencia;}
    public List<DetalleVentaRequest> getDetalles() {return detalles;}
    public List<PagoVentaRequest> getPagos() {return pagos;}
}
