package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ObtenerSesionesCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ISesionCajaRepository {
    SesionCaja crear(SesionCaja sesionCaja);
    SesionCaja actualizar(SesionCajaId sesionCajaId, SesionCaja sesionCaja);
    Optional<SesionCaja> obtenerPorId(SesionCajaId sesionCajaId);
    PaginatedResponse<List<SesionCaja>> obtenerTodos(ObtenerSesionesCajaRequest obtenerSesionesCajaRequest);

    /** La sesion abierta del local, si la hay, sin bloquearla: para consultar el estado. */
    Optional<SesionCaja> obtenerAbierta();

    /** La sesion abierta bloqueada en exclusiva (FOR UPDATE): para cerrarla o moverle efectivo. */
    Optional<SesionCaja> obtenerAbiertaParaActualizar();

    /**
     * La sesion abierta con bloqueo compartido (FOR SHARE): para registrar una venta en ella.
     * Varias ventas la comparten; un cierre espera a que terminen.
     */
    Optional<SesionCaja> obtenerAbiertaParaVenta();
}
