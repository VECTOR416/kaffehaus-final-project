package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** 'tipo' es INGRESO o EGRESO; lo valida la raiz. Quien lo registra sale del token. */
public class RegistrarMovimientoCajaRequest {
    private String tipo;
    private BigDecimal monto;
    private String motivo;
    private String idUsuario;

    private RegistrarMovimientoCajaRequest(String tipo, BigDecimal monto, String motivo, String idUsuario) {
        this.tipo = tipo;
        this.monto = monto;
        this.motivo = motivo;
        this.idUsuario = idUsuario;
    }

    public static RegistrarMovimientoCajaRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        String tipo = Validador.textoRequerido(request.get("tipo"), "tipo");
        BigDecimal monto = BigDecimal.valueOf(Validador.numeroRequerido(request.get("monto"), "monto"));
        String motivo = Validador.textoRequerido(request.get("motivo"), "motivo");
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");

        return new RegistrarMovimientoCajaRequest(tipo, monto, motivo, idUsuario);
    }

    public String getTipo() {return tipo;}
    public BigDecimal getMonto() {return monto;}
    public String getMotivo() {return motivo;}
    public String getIdUsuario() {return idUsuario;}
}
