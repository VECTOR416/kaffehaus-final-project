package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerEntidadesRequest extends PaginationDTO {
    private ObtenerEntidadesRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerEntidadesRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerEntidadesRequest(skip, limit, search);
    }
}
