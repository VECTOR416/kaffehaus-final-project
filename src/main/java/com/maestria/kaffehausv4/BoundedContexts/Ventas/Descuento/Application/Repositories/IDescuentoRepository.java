package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ObtenerDescuentosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects.DescuentoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IDescuentoRepository {
    Descuento crear(Descuento descuento);
    Descuento actualizar(DescuentoId descuentoId, Descuento descuento);
    Optional<Descuento> obtenerPorId(DescuentoId descuentoId);
    PaginatedResponse<List<Descuento>> obtenerTodos(ObtenerDescuentosRequest obtenerDescuentosRequest);
    void eliminar(DescuentoId descuentoId);
}
