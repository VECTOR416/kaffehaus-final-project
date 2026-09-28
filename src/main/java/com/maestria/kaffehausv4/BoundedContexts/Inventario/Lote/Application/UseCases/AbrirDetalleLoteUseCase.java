package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.RegistrarAperturaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.RegistrarAperturaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.AbrirDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Abre la unidad cerrada que eligio el usuario (inventario.md INV-14). El usuario elige la
 * presentacion enviando un detalle de la consulta de aperturables; el sistema exige que sea
 * el que toca abrir de esa presentacion, el mas proximo a vencer, y lo comprueba ya con la
 * linea bloqueada: si otro usuario abrio antes la ultima unidad, o el cliente manda un
 * detalle arbitrario, responde 409 nombrando el que corresponde. Nunca abre otro en silencio.
 */
@Service
@Transactional
public class AbrirDetalleLoteUseCase {
    private final ILoteRepository loteRepository;
    private final IUsuarioRepository usuarioRepository;
    private final RegistrarAperturaUseCase registrarAperturaUseCase;

    public AbrirDetalleLoteUseCase(
            ILoteRepository loteRepository,
            IUsuarioRepository usuarioRepository,
            RegistrarAperturaUseCase registrarAperturaUseCase) {
        this.loteRepository = loteRepository;
        this.usuarioRepository = usuarioRepository;
        this.registrarAperturaUseCase = registrarAperturaUseCase;
    }

    public ExistenciaResponse execute(String idDetalleLote, AbrirDetalleLoteRequest request) {
        usuarioRepository.obtenerPorId(new UsuarioId(request.getIdUsuario()))
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + request.getIdUsuario() + "' no existe"));

        LocalDateTime ahora = LocalDateTime.now();

        Lote lote = loteRepository.obtenerPorDetalleIdParaActualizar(new DetalleLoteId(idDetalleLote))
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + idDetalleLote + "' no existe"));
        DetalleLote detalle = lote.obtenerDetalle(idDetalleLote);

        if (detalle.estaVencido(ahora.toLocalDate())) {
            throw DomainException.throwException(409,
                    "El detalle de lote con id '" + idDetalleLote + "' esta vencido; registra su vencimiento en lugar de abrirlo");
        }
        if (!detalle.tieneUnidadesCerradas()) {
            throw DomainException.throwException(409, "El detalle de lote con id '" + idDetalleLote + "' no tiene unidades cerradas para abrir");
        }

        List<DetalleLote> enOrden = Lote.aperturablesEnOrden(
                loteRepository.obtenerPorProductos(List.of(detalle.getProductoId())),
                detalle.getProductoId(),
                ahora.toLocalDate());
        DetalleLote queToca = enOrden.get(0);
        if (!queToca.getId().getValue().equals(idDetalleLote)) {
            throw DomainException.throwException(409,
                    "Debe abrirse primero el detalle '" + queToca.getId().getValue() + "'"
                    + (queToca.getFechaVencimiento() != null ? " que vence el " + queToca.getFechaVencimiento() : ""));
        }

        return registrarAperturaUseCase.execute(RegistrarAperturaRequest.crear(idDetalleLote, request.getIdUsuario(), false, ahora));
    }
}
