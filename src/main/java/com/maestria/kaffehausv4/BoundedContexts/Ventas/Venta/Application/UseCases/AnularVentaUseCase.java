package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.AnularVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.RegistrarAnulacionDeVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;

/**
 * Anulacion de una venta a pedido de un Administrador (ventas.md VEN-40). El endpoint exige el
 * rol; la regla vive en RegistrarAnulacionDeVentaUseCase, que tambien usa la cancelacion de
 * un pedido.
 */
@Service
@Transactional
public class AnularVentaUseCase {
    private final RegistrarAnulacionDeVentaUseCase registrarAnulacionDeVentaUseCase;

    public AnularVentaUseCase(RegistrarAnulacionDeVentaUseCase registrarAnulacionDeVentaUseCase) {
        this.registrarAnulacionDeVentaUseCase = registrarAnulacionDeVentaUseCase;
    }

    public VentaResponse execute(String ventaId, AnularVentaRequest request) {
        return registrarAnulacionDeVentaUseCase.execute(RegistrarAnulacionDeVentaRequest.crear(
                ventaId, request.getIdUsuario(), request.getMotivo(), request.isReintegrar(), LocalDateTime.now()));
    }
}
