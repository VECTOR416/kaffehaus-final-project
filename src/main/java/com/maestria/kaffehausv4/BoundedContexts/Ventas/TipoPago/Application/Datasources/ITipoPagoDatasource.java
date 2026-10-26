package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.ObtenerTiposPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ITipoPagoDatasource {
    TipoPago crear(TipoPago tipoPago);
    TipoPago actualizar(TipoPagoId tipoPagoId, TipoPago tipoPago);
    Optional<TipoPago> obtenerPorId(TipoPagoId tipoPagoId);
    PaginatedResponse<List<TipoPago>> obtenerTodos(ObtenerTiposPagoRequest obtenerTiposPagoRequest);
    void eliminar(TipoPagoId tipoPagoId);

    /**
     * Busca la fila viva por nombre, sin distinguir mayusculas. La venta y el arqueo de caja
     * reconocen el efectivo por su nombre, igual que la seguridad resuelve los roles: el
     * catalogo sigue siendo datos, no un enum.
     */
    Optional<TipoPago> obtenerPorNombre(String nombre);

    /** Todos los tipos vivos: el arqueo desglosa lo recaudado por cada uno, aunque sea cero. */
    List<TipoPago> obtenerVivos();
}
