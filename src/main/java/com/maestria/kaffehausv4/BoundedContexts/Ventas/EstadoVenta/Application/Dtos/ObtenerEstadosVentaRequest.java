package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerEstadosVentaRequest extends PaginationDTO {
    private ObtenerEstadosVentaRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerEstadosVentaRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerEstadosVentaRequest(skip, limit, search);
    }
}
