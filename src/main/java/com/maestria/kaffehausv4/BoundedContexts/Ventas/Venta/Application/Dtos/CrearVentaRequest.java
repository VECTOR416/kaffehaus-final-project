package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Payload compuesto del alta de una venta: encabezado mas la coleccion de detalles.
 *
 * Ya no acepta 'total'. El total es un valor derivado que calcula el agregado Venta a partir
 * de sus lineas, de forma que encabezado y detalle no puedan contradecirse.
 */
public class CrearVentaRequest {
    private LocalDateTime fechaVenta;
    private String idUsuario;
    private String idCliente;
    private String idEstadoVenta;
    private List<DetalleVentaRequest> detalles;

    private CrearVentaRequest(
        LocalDateTime fechaVenta,
        String idUsuario,
        String idCliente,
        String idEstadoVenta,
        List<DetalleVentaRequest> detalles
    ) {
        this.fechaVenta = fechaVenta;
        this.idUsuario = idUsuario;
        this.idCliente = idCliente;
        this.idEstadoVenta = idEstadoVenta;
        this.detalles = detalles;
    }

    public static CrearVentaRequest createFromRequest(Map<String, Object> request){
        LocalDateTime fechaVenta = Validador.fechaOpcional(request.get("fechaVenta"), "fechaVenta");
        fechaVenta = fechaVenta != null ? fechaVenta : LocalDateTime.now();
        String idUsuario = Validador.textoRequerido(request.get("idUsuario"), "idUsuario");
        String idCliente = Validador.textoOpcional(request.get("idCliente"), "idCliente");
        String idEstadoVenta = Validador.textoRequerido(request.get("idEstadoVenta"), "idEstadoVenta");

        List<DetalleVentaRequest> detalles = Validador.listaDeObjetosRequerida(request.get("detalles"), "detalles").stream()
                .map(DetalleVentaRequest::createFromRequest)
                .collect(Collectors.toList());

        return new CrearVentaRequest(fechaVenta, idUsuario, idCliente, idEstadoVenta, detalles);
    }

    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdCliente() {return idCliente;}
    public String getIdEstadoVenta() {return idEstadoVenta;}
    public List<DetalleVentaRequest> getDetalles() {return detalles;}
}
