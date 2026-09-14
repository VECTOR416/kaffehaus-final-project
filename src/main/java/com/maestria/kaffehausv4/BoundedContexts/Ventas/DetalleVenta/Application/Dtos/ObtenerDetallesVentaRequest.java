package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerDetallesVentaRequest extends PaginationDTO {
    private ObtenerDetallesVentaRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerDetallesVentaRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerDetallesVentaRequest(skip, limit, search);
    }
}
