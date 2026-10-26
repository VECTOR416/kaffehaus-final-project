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
 *
 * 'motivo' explica la correccion por conteo de 'cantidadRestante' y es obligatorio si alguna
 * linea la cambia (logica.md LOTE-23); lo exige el caso de uso, que sabe si cambio. El usuario
 * de la correccion es el de la sesion: el cliente no lo envia.
 */
public class ActualizarLoteRequest {
    private LocalDateTime fechaCompra;
    private String idProveedor;
    private String motivo;
    private String idUsuario;
    private List<ActualizarDetalleLoteRequest> detalles;

    private ActualizarLoteRequest(
        LocalDateTime fechaCompra,
        String idProveedor,
        String motivo,
        String idUsuario,
        List<ActualizarDetalleLoteRequest> detalles
    ) {
        this.fechaCompra = fechaCompra;
        this.idProveedor = idProveedor;
        this.motivo = motivo;
        this.idUsuario = idUsuario;
        this.detalles = detalles;
    }

    /** 'idUsuarioEnSesion' lo pone el controller desde el token con @UsuarioEnSesion. */
    public static ActualizarLoteRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion){
        LocalDateTime fechaCompra = Validador.fechaOpcional(request.get("fechaCompra"), "fechaCompra");
        String idProveedor = Validador.textoOpcional(request.get("idProveedor"), "idProveedor");
        String motivo = Validador.textoOpcional(request.get("motivo"), "motivo");
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");

        List<Map<String, Object>> detallesTmp = Validador.listaDeObjetosOpcional(request.get("detalles"), "detalles");
        List<ActualizarDetalleLoteRequest> detalles = detallesTmp != null
                ? detallesTmp.stream().map(ActualizarDetalleLoteRequest::createFromRequest).collect(Collectors.toList())
                : null;

        return new ActualizarLoteRequest(fechaCompra, idProveedor, motivo, idUsuario, detalles);
    }

    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public String getIdProveedor() {return idProveedor;}
    public String getMotivo() {return motivo;}
    public String getIdUsuario() {return idUsuario;}
    public List<ActualizarDetalleLoteRequest> getDetalles() {return detalles;}
}
