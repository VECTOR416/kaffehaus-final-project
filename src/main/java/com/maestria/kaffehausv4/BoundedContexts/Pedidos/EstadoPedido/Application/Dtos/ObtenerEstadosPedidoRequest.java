package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginationDTO;

public class ObtenerEstadosPedidoRequest extends PaginationDTO {
    private ObtenerEstadosPedidoRequest(Integer skip, Integer limit, String search) {
        super(skip, limit, search);
    }

    public static ObtenerEstadosPedidoRequest createFromRequest(Map<String, Object> request) {
        Integer skip = (Integer) request.get("skip");
        Integer limit = (Integer) request.get("limit");
        String search = (String) request.get("search");

        return new ObtenerEstadosPedidoRequest(skip, limit, search);
    }
}
