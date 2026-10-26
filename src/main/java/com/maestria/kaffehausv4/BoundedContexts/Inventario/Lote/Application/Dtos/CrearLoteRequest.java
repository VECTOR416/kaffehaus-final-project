package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Payload compuesto del alta de un lote: encabezado mas la coleccion de detalles.
 *
 * Ya no acepta 'total'. El total es el costo de la compra, un valor derivado que calcula el
 * agregado Lote a partir de sus lineas, de forma que encabezado y detalle no se contradigan.
 */
public class CrearLoteRequest {
    private LocalDateTime fechaCompra;
    private String idProveedor;
    private String idUsuario;
    private List<DetalleLoteRequest> detalles;

    private CrearLoteRequest(
        LocalDateTime fechaCompra,
        String idProveedor,
        String idUsuario,
        List<DetalleLoteRequest> detalles
    ) {
        this.fechaCompra = fechaCompra;
        this.idProveedor = idProveedor;
        this.idUsuario = idUsuario;
        this.detalles = detalles;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     */
    public static CrearLoteRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion){
        LocalDateTime fechaCompra = Validador.fechaOpcional(request.get("fechaCompra"), "fechaCompra");
        fechaCompra = fechaCompra != null ? fechaCompra : LocalDateTime.now();
        String idProveedor = Validador.textoRequerido(request.get("idProveedor"), "idProveedor");
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");

        List<DetalleLoteRequest> detalles = Validador.listaDeObjetosRequerida(request.get("detalles"), "detalles").stream()
                .map(DetalleLoteRequest::createFromRequest)
                .collect(Collectors.toList());

        return new CrearLoteRequest(fechaCompra, idProveedor, idUsuario, detalles);
    }

    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public String getIdProveedor() {return idProveedor;}
    public String getIdUsuario() {return idUsuario;}
    public List<DetalleLoteRequest> getDetalles() {return detalles;}
}
