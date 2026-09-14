package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.CrearLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

@Service
@Transactional
public class CrearLoteUseCase {
    private final ILoteRepository loteRepository;

    public CrearLoteUseCase(
            ILoteRepository loteRepository
        ) {
        this.loteRepository = loteRepository;
    }

    public LoteResponse execute(CrearLoteRequest crearLoteRequest) {
        ProveedorId proveedorId = new ProveedorId(crearLoteRequest.getIdProveedor());
        UsuarioId usuarioId = new UsuarioId(crearLoteRequest.getIdUsuario());

        Lote lote = Lote.crear(
                crearLoteRequest.getFechaCompra(),
                crearLoteRequest.getTotal(),
                proveedorId,
                usuarioId);

        Lote loteCreado = loteRepository.crear(lote);

        return LoteResponse.createFromAggregate(loteCreado);
    }
}
