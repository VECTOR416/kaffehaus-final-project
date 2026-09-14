package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Datasources.IVentaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ObtenerVentasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class VentaDatasourceImpl implements IVentaDatasource {

    private final VentaJpaRepository ventaJpaRepository;

    public VentaDatasourceImpl(VentaJpaRepository ventaJpaRepository) {
        this.ventaJpaRepository = ventaJpaRepository;
    }

    @Override
    public Venta crear(Venta venta) {
        VentaJpaModel model = VentaJpaMapper.toPersistence(venta);
        return VentaJpaMapper.toDomain(ventaJpaRepository.save(model));
    }

    @Override
    public Venta actualizar(VentaId ventaId, Venta venta) {
        VentaJpaModel model = ventaJpaRepository.findById(aUuid(ventaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + ventaId.getValue() + "' no existe"));

        VentaJpaMapper.copiarCambios(venta, model);

        return VentaJpaMapper.toDomain(ventaJpaRepository.save(model));
    }

    @Override
    public Optional<Venta> obtenerPorId(VentaId ventaId) {
        return ventaJpaRepository.findById(aUuid(ventaId.getValue()))
                .map(VentaJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Venta>> obtenerTodos(ObtenerVentasRequest obtenerVentasRequest) {
        int skip = obtenerVentasRequest.getSkip();
        int limit = obtenerVentasRequest.getLimit();
        String search = obtenerVentasRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<VentaJpaModel> pagina = ventaJpaRepository.findAll(pageable);

        List<Venta> datos = pagina.getContent().stream()
                .map(VentaJpaMapper::toDomain)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                datos,
                skip,
                limit,
                pagina.getTotalElements(),
                skip + datos.size() < pagina.getTotalElements()
        );
    }

    @Override
    public void eliminar(VentaId ventaId) {
        UUID id = aUuid(ventaId.getValue());

        if (!ventaJpaRepository.existsById(id)) {
            throw DomainException.throwException(404, "La venta con id '" + ventaId.getValue() + "' no existe");
        }

        ventaJpaRepository.deleteById(id);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
