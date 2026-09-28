package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ObtenerSesionesCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.SesionCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

/** Historial de turnos, el mas reciente primero. */
@Service
@Transactional(readOnly = true)
public class ObtenerSesionesCajaUseCase {
    private final ISesionCajaRepository sesionCajaRepository;

    public ObtenerSesionesCajaUseCase(ISesionCajaRepository sesionCajaRepository) {
        this.sesionCajaRepository = sesionCajaRepository;
    }

    public PaginatedResponse<List<SesionCajaResponse>> execute(ObtenerSesionesCajaRequest request) {
        PaginatedResponse<List<SesionCaja>> resultado = sesionCajaRepository.obtenerTodos(request);

        List<SesionCajaResponse> sesiones = resultado.getData().stream()
                .map(SesionCajaResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                sesiones,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
