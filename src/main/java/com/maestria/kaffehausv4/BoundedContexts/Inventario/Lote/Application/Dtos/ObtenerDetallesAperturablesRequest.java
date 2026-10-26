package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** Consulta de detalles aperturables de un producto base (inventario.md INV-13). */
public class ObtenerDetallesAperturablesRequest {
    private String idProducto;

    private ObtenerDetallesAperturablesRequest(String idProducto) {
        this.idProducto = idProducto;
    }

    public static ObtenerDetallesAperturablesRequest createFromRequest(Map<String, Object> request) {
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");
        return new ObtenerDetallesAperturablesRequest(idProducto);
    }

    public String getIdProducto() {return idProducto;}
}
