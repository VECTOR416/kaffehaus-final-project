package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos;

import java.util.Map;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerUsuariosRequest extends PaginationDTO {
    private ObtenerUsuariosRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerUsuariosRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerUsuariosRequest(skip, limit, search);
    }
}
