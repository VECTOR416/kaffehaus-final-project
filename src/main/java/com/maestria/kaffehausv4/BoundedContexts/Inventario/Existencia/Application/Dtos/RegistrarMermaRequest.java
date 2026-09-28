package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** Merma de una existencia (inventario.md INV-42): cantidad perdida y motivo obligatorio. */
public class RegistrarMermaRequest {
    private BigDecimal cantidad;
    private String motivo;
    private String idUsuario;

    private RegistrarMermaRequest(BigDecimal cantidad, String motivo, String idUsuario) {
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.idUsuario = idUsuario;
    }

    public static RegistrarMermaRequest createFromRequest(Map<String, Object> request) {
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));
        String motivo = Validador.textoRequerido(request.get("motivo"), "motivo");
        String idUsuario = Validador.textoRequerido(request.get("idUsuario"), "idUsuario");
        return new RegistrarMermaRequest(cantidad, motivo, idUsuario);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public String getMotivo() {return motivo;}
    public String getIdUsuario() {return idUsuario;}
}
