package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos;
import java.util.Map;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerProveedoresRequest extends PaginationDTO {
    private ObtenerProveedoresRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerProveedoresRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerProveedoresRequest(skip, limit, search);
    }
}
