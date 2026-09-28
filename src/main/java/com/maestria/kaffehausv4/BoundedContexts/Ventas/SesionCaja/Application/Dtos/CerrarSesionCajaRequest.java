package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Cierre de caja: el efectivo contado y, si se quiere, el motivo de la diferencia. El esperado
 * y la diferencia los calcula el sistema; si llegan en el cuerpo se rechazan.
 */
public class CerrarSesionCajaRequest {
    private static final List<String> CAMPOS_DERIVADOS = List.of("montoCierreEsperado", "diferencia");

    private BigDecimal montoCierreDeclarado;
    private String motivoDiferencia;
    private String idUsuario;

    private CerrarSesionCajaRequest(BigDecimal montoCierreDeclarado, String motivoDiferencia, String idUsuario) {
        this.montoCierreDeclarado = montoCierreDeclarado;
        this.motivoDiferencia = motivoDiferencia;
        this.idUsuario = idUsuario;
    }

    public static CerrarSesionCajaRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        for (String campo : CAMPOS_DERIVADOS) {
            if (request.get(campo) != null) {
                throw DomainException.throwException(400, "El campo '" + campo + "' lo calcula el sistema; no se envia");
            }
        }
        BigDecimal montoCierreDeclarado = BigDecimal.valueOf(
                Validador.numeroRequerido(request.get("montoCierreDeclarado"), "montoCierreDeclarado"));
        String motivoDiferencia = Validador.textoOpcional(request.get("motivoDiferencia"), "motivoDiferencia");
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");

        return new CerrarSesionCajaRequest(montoCierreDeclarado, motivoDiferencia, idUsuario);
    }

    public BigDecimal getMontoCierreDeclarado() {return montoCierreDeclarado;}
    public String getMotivoDiferencia() {return motivoDiferencia;}
    public String getIdUsuario() {return idUsuario;}
}
