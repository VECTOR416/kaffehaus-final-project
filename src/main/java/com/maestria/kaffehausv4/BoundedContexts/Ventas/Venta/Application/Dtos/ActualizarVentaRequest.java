package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Modificacion de una venta existente. Mantiene la semantica PATCH del proyecto: lo que no
 * llega, no se toca.
 *
 * 'detalles' es opcional. Cuando viene, el caso de uso hace una conciliacion por id: las
 * lineas con 'id' se corrigen, las que no lo traen se agregan y las que faltan se dan de baja
 * logica. No se reemplaza la coleccion entera porque el sistema no usa borrado fisico: cada
 * reemplazo dejaria una fila muerta por linea modificada.
 */
public class ActualizarVentaRequest {
    private LocalDateTime fechaVenta;
    private String idUsuario;
    private String idCliente;
    private String idEstadoVenta;
    private List<ActualizarDetalleVentaRequest> detalles;

    private ActualizarVentaRequest(
        LocalDateTime fechaVenta,
        String idUsuario,
        String idCliente,
        String idEstadoVenta,
        List<ActualizarDetalleVentaRequest> detalles
    ) {
        this.fechaVenta = fechaVenta;
        this.idUsuario = idUsuario;
        this.idCliente = idCliente;
        this.idEstadoVenta = idEstadoVenta;
        this.detalles = detalles;
    }

    public static ActualizarVentaRequest createFromRequest(Map<String, Object> request){
        LocalDateTime fechaVenta = Validador.fechaOpcional(request.get("fechaVenta"), "fechaVenta");
        String idUsuario = Validador.textoOpcional(request.get("idUsuario"), "idUsuario");
        String idCliente = Validador.textoOpcional(request.get("idCliente"), "idCliente");
        String idEstadoVenta = Validador.textoOpcional(request.get("idEstadoVenta"), "idEstadoVenta");

        List<Map<String, Object>> detallesTmp = Validador.listaDeObjetosOpcional(request.get("detalles"), "detalles");
        List<ActualizarDetalleVentaRequest> detalles = detallesTmp != null
                ? detallesTmp.stream().map(ActualizarDetalleVentaRequest::createFromRequest).collect(Collectors.toList())
                : null;

        return new ActualizarVentaRequest(fechaVenta, idUsuario, idCliente, idEstadoVenta, detalles);
    }

    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdCliente() {return idCliente;}
    public String getIdEstadoVenta() {return idEstadoVenta;}
    public List<ActualizarDetalleVentaRequest> getDetalles() {return detalles;}
}
