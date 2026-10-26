package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerTiposPagoRequest extends PaginationDTO {
    private ObtenerTiposPagoRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerTiposPagoRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerTiposPagoRequest(skip, limit, search);
    }
}
