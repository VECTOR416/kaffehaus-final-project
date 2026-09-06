package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerClientesRequest extends PaginationDTO {
    private ObtenerClientesRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerClientesRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerClientesRequest(skip, limit, search);
    }
}
