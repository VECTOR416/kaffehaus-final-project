package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Datasources.IProductoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ObtenerProductosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class ProductoDatasourceImpl implements IProductoDatasource {

    private final ProductoJpaRepository productoJpaRepository;

    public ProductoDatasourceImpl(ProductoJpaRepository productoJpaRepository) {
        this.productoJpaRepository = productoJpaRepository;
    }

    @Override
    public Producto crear(Producto producto) {
        ProductoJpaModel model = ProductoJpaMapper.toPersistence(producto);
        return ProductoJpaMapper.toDomain(productoJpaRepository.save(model));
    }

    @Override
    public Producto actualizar(ProductoId productoId, Producto producto) {
        ProductoJpaModel model = productoJpaRepository.findByIdAndBorradoFalse(aUuid(productoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + productoId.getValue() + "' no existe"));

        ProductoJpaMapper.copiarCambios(producto, model);

        return ProductoJpaMapper.toDomain(productoJpaRepository.save(model));
    }

    @Override
    public Optional<Producto> obtenerPorId(ProductoId productoId) {
        return productoJpaRepository.findByIdAndBorradoFalse(aUuid(productoId.getValue()))
                .map(ProductoJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Producto>> obtenerTodos(ObtenerProductosRequest obtenerProductosRequest) {
        int skip = obtenerProductosRequest.getSkip();
        int limit = obtenerProductosRequest.getLimit();
        String search = obtenerProductosRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<ProductoJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? productoJpaRepository.findByBorradoFalse(pageable)
                : productoJpaRepository.findByBorradoFalseAndNombreContainingIgnoreCase(search.trim(), pageable);

        List<Producto> datos = pagina.getContent().stream()
                .map(ProductoJpaMapper::toDomain)
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
    public void eliminar(ProductoId productoId) {
        ProductoJpaModel model = productoJpaRepository.findByIdAndBorradoFalse(aUuid(productoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + productoId.getValue() + "' no existe"));

        model.setBorrado(true);
        model.setFecU(OffsetDateTime.now());
        productoJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
