package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pesaje de las existencias abiertas de un producto base (logica.md EXISTENCIA-16,
 * EXISTENCIA-18). Se envia el peso total real, no la diferencia: la diferencia y si es una merma o
 * un ajuste los decide el sistema.
 */
public class RegistrarPesajeRequest {
    private BigDecimal pesoTotal;
    private String motivo;
    private String idUsuario;

    private RegistrarPesajeRequest(BigDecimal pesoTotal, String motivo, String idUsuario) {
        this.pesoTotal = pesoTotal;
        this.motivo = motivo;
        this.idUsuario = idUsuario;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     */
    public static RegistrarPesajeRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        BigDecimal pesoTotal = BigDecimal.valueOf(Validador.numeroRequerido(request.get("pesoTotal"), "pesoTotal"));
        if (pesoTotal.signum() < 0) {
            throw DomainException.throwException(400, "El campo 'pesoTotal' no puede ser negativo");
        }
        String motivo = Validador.textoRequerido(request.get("motivo"), "motivo");
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        return new RegistrarPesajeRequest(pesoTotal, motivo, idUsuario);
    }

    public BigDecimal getPesoTotal() {return pesoTotal;}
    public String getMotivo() {return motivo;}
    public String getIdUsuario() {return idUsuario;}
}
