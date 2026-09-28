package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Payload compuesto del alta de una venta: encabezado mas la coleccion de detalles.
 *
 * No admite 'total' (derivado) ni 'idEstadoVenta': una venta nace 'Completada' y el estado lo
 * asigna el sistema (ventas.md VEN-01, VEN-02). 'claveIdempotencia' es opcional: la genera el
 * POS para que un reintento tras un timeout no cobre dos veces (VEN-66).
 */
public class CrearVentaRequest {
    private static final List<String> CAMPOS_DERIVADOS = List.of("total", "idEstadoVenta");

    private LocalDateTime fechaVenta;
    private String idUsuario;
    private String idCliente;
    private String claveIdempotencia;
    private List<DetalleVentaRequest> detalles;

    private CrearVentaRequest(
        LocalDateTime fechaVenta,
        String idUsuario,
        String idCliente,
        String claveIdempotencia,
        List<DetalleVentaRequest> detalles
    ) {
        this.fechaVenta = fechaVenta;
        this.idUsuario = idUsuario;
        this.idCliente = idCliente;
        this.claveIdempotencia = claveIdempotencia;
        this.detalles = detalles;
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
        LocalDateTime fechaVenta = Validador.fechaOpcional(request.get("fechaVenta"), "fechaVenta");
        fechaVenta = fechaVenta != null ? fechaVenta : LocalDateTime.now();
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        String idCliente = Validador.textoOpcional(request.get("idCliente"), "idCliente");
        String claveIdempotencia = Validador.textoOpcional(request.get("claveIdempotencia"), "claveIdempotencia");

        List<DetalleVentaRequest> detalles = Validador.listaDeObjetosRequerida(request.get("detalles"), "detalles").stream()
                .map(DetalleVentaRequest::createFromRequest)
                .collect(Collectors.toList());

        return new CrearVentaRequest(fechaVenta, idUsuario, idCliente, claveIdempotencia, detalles);
    }

    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdCliente() {return idCliente;}
    public String getClaveIdempotencia() {return claveIdempotencia;}
    public List<DetalleVentaRequest> getDetalles() {return detalles;}
}
