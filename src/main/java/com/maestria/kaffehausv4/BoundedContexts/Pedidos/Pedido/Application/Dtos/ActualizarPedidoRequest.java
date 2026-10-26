package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Modificacion de un pedido. 'idVenta' no cambia: un pedido es la entrega de una venta
 * concreta (ventas.md VEN-50). 'idEstado' mueve el pedido por sus transiciones (VEN-51). El usuario
 * que cancela, y con ello anula su venta (VEN-52), es el de la sesion.
 */
public class ActualizarPedidoRequest {
    private LocalDateTime fechaEntrega;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String descripcion;
    private String idEstado;
    private String idUsuario;
    private String motivo;
    private String referenciaReembolso;

    private ActualizarPedidoRequest(
        LocalDateTime fechaEntrega,
        BigDecimal latitud,
        BigDecimal longitud,
        String descripcion,
        String idEstado,
        String idUsuario,
        String motivo,
        String referenciaReembolso
    ) {
        this.fechaEntrega = fechaEntrega;
        this.latitud = latitud;
        this.longitud = longitud;
        this.descripcion = descripcion;
        this.idEstado = idEstado;
        this.idUsuario = idUsuario;
        this.motivo = motivo;
        this.referenciaReembolso = referenciaReembolso;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     * 'motivo' y 'referenciaReembolso' solo se usan al cancelar, porque anulan la venta (logica.md
     * PEDIDO-09, VENTA-27).
     */
    public static ActualizarPedidoRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion){
        LocalDateTime fechaEntrega = Validador.fechaOpcional(request.get("fechaEntrega"), "fechaEntrega");
        Double latitudTmp = Validador.numeroOpcional(request.get("latitud"), "latitud");
        BigDecimal latitud = latitudTmp != null ? BigDecimal.valueOf(latitudTmp) : null;
        Double longitudTmp = Validador.numeroOpcional(request.get("longitud"), "longitud");
        BigDecimal longitud = longitudTmp != null ? BigDecimal.valueOf(longitudTmp) : null;
        String descripcion = Validador.textoOpcional(request.get("descripcion"), "descripcion");
        if (request.get("idVenta") != null) {
            throw DomainException.throwException(400, "El campo 'idVenta' de un pedido no se modifica");
        }
        String idEstado = Validador.textoOpcional(request.get("idEstado"), "idEstado");
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        String motivo = Validador.textoOpcional(request.get("motivo"), "motivo");
        String referenciaReembolso = Validador.textoOpcional(request.get("referenciaReembolso"), "referenciaReembolso");

        return new ActualizarPedidoRequest(fechaEntrega, latitud, longitud, descripcion, idEstado, idUsuario, motivo, referenciaReembolso);
    }

    public LocalDateTime getFechaEntrega() {return fechaEntrega;}
    public BigDecimal getLatitud() {return latitud;}
    public BigDecimal getLongitud() {return longitud;}
    public String getDescripcion() {return descripcion;}
    public String getIdEstado() {return idEstado;}
    public String getIdUsuario() {return idUsuario;}
    public String getMotivo() {return motivo;}
    public String getReferenciaReembolso() {return referenciaReembolso;}
}
