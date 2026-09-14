package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.CrearLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearLoteUseCase {
    private final ILoteRepository loteRepository;
    private final IProveedorRepository proveedorRepository;
    private final IUsuarioRepository usuarioRepository;

    public CrearLoteUseCase(
            ILoteRepository loteRepository,
            IProveedorRepository proveedorRepository,
            IUsuarioRepository usuarioRepository) {
        this.loteRepository = loteRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public LoteResponse execute(CrearLoteRequest crearLoteRequest) {
        ProveedorId proveedorId = new ProveedorId(crearLoteRequest.getIdProveedor());
        proveedorRepository.obtenerPorId(proveedorId)
                .orElseThrow(() -> DomainException.throwException(404, "El proveedor con id '" + crearLoteRequest.getIdProveedor() + "' no existe"));
        UsuarioId usuarioId = new UsuarioId(crearLoteRequest.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + crearLoteRequest.getIdUsuario() + "' no existe"));

        Lote lote = Lote.crear(
                crearLoteRequest.getFechaCompra(),
                crearLoteRequest.getTotal(),
                proveedorId,
                usuarioId);

        Lote loteCreado = loteRepository.crear(lote);

        return LoteResponse.createFromAggregate(loteCreado);
    }
}
