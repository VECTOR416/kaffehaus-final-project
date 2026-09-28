package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Modificacion de un lote existente. Mantiene la semantica PATCH del proyecto: lo que no
 * llega, no se toca.
 *
 * 'detalles' es opcional. Cuando viene, el caso de uso hace una conciliacion por id: las
 * lineas con 'id' se corrigen, las que no lo traen se agregan y las que faltan se dan de baja
 * logica. No se reemplaza la coleccion entera porque 'existencias' referencia estas lineas
 * con ON DELETE RESTRICT: perder sus identificadores dejaria el stock huerfano.
 */
public class ActualizarLoteRequest {
    private LocalDateTime fechaCompra;
    private String idProveedor;
    private List<ActualizarDetalleLoteRequest> detalles;

    private ActualizarLoteRequest(
        LocalDateTime fechaCompra,
        String idProveedor,
        List<ActualizarDetalleLoteRequest> detalles
    ) {
        this.fechaCompra = fechaCompra;
        this.idProveedor = idProveedor;
        this.detalles = detalles;
    }

    public static ActualizarLoteRequest createFromRequest(Map<String, Object> request){
        LocalDateTime fechaCompra = Validador.fechaOpcional(request.get("fechaCompra"), "fechaCompra");
        String idProveedor = Validador.textoOpcional(request.get("idProveedor"), "idProveedor");

        List<Map<String, Object>> detallesTmp = Validador.listaDeObjetosOpcional(request.get("detalles"), "detalles");
        List<ActualizarDetalleLoteRequest> detalles = detallesTmp != null
                ? detallesTmp.stream().map(ActualizarDetalleLoteRequest::createFromRequest).collect(Collectors.toList())
                : null;

        return new ActualizarLoteRequest(fechaCompra, idProveedor, detalles);
    }

    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public String getIdProveedor() {return idProveedor;}
    public List<ActualizarDetalleLoteRequest> getDetalles() {return detalles;}
}
