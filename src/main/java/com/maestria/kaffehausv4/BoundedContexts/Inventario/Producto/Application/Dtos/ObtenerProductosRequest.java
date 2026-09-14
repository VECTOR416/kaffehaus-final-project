package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerProductosRequest extends PaginationDTO {
    private ObtenerProductosRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerProductosRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerProductosRequest(skip, limit, search);
    }
}
