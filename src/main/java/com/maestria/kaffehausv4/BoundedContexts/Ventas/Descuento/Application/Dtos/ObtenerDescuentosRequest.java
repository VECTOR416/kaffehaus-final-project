package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerDescuentosRequest extends PaginationDTO {
    private ObtenerDescuentosRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerDescuentosRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerDescuentosRequest(skip, limit, search);
    }
}
