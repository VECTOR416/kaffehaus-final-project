package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ActualizarLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarLoteUseCase {
    private final ILoteRepository loteRepository;
    private final IProveedorRepository proveedorRepository;
    private final IUsuarioRepository usuarioRepository;

    public ActualizarLoteUseCase(
            ILoteRepository loteRepository,
            IProveedorRepository proveedorRepository,
            IUsuarioRepository usuarioRepository) {
        this.loteRepository = loteRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public LoteResponse execute(String loteId, ActualizarLoteRequest actualizarLoteRequest) {
        LoteId id = new LoteId(loteId);

        Lote lote = loteRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId + "' no existe"));

        ProveedorId proveedorId = null;
        if (actualizarLoteRequest.getIdProveedor() != null) {
            proveedorId = new ProveedorId(actualizarLoteRequest.getIdProveedor());
            proveedorRepository.obtenerPorId(proveedorId)
                    .orElseThrow(() -> DomainException.throwException(404, "El proveedor con id '" + actualizarLoteRequest.getIdProveedor() + "' no existe"));
        }
        UsuarioId usuarioId = null;
        if (actualizarLoteRequest.getIdUsuario() != null) {
            usuarioId = new UsuarioId(actualizarLoteRequest.getIdUsuario());
            usuarioRepository.obtenerPorId(usuarioId)
                    .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + actualizarLoteRequest.getIdUsuario() + "' no existe"));
        }

        lote.actualizar(
                actualizarLoteRequest.getFechaCompra(),
                actualizarLoteRequest.getTotal(),
                proveedorId,
                usuarioId);

        Lote loteActualizado = loteRepository.actualizar(id, lote);

        return LoteResponse.createFromAggregate(loteActualizado);
    }
}
