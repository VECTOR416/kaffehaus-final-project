package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerExistenciasRequest extends PaginationDTO {
    private ObtenerExistenciasRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerExistenciasRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerExistenciasRequest(skip, limit, search);
    }
}
