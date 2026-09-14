package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerMovimientosRequest extends PaginationDTO {
    private ObtenerMovimientosRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerMovimientosRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerMovimientosRequest(skip, limit, search);
    }
}
