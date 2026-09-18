package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Datasources;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Datasources.IVentaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ObtenerVentasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

/**
 * Persistencia del agregado Venta completo: encabezado mas lineas.
 *
 * Maneja los dos JpaRepository porque ambas tablas pertenecen al mismo agregado; esto no
 * contradice la regla de no acoplar datasources entre agregados, ya que detalle_venta dejo
 * de ser uno. La atomicidad la aporta la @Transactional del caso de uso, que envuelve todas
 * las escrituras de este metodo en una sola transaccion de base de datos.
 */
@Repository
public class VentaDatasourceImpl implements IVentaDatasource {

    private final VentaJpaRepository ventaJpaRepository;
    private final DetalleVentaJpaRepository detalleVentaJpaRepository;

    public VentaDatasourceImpl(
            VentaJpaRepository ventaJpaRepository,
            DetalleVentaJpaRepository detalleVentaJpaRepository) {
        this.ventaJpaRepository = ventaJpaRepository;
        this.detalleVentaJpaRepository = detalleVentaJpaRepository;
    }

    @Override
    public Venta crear(Venta venta) {
        VentaJpaModel model = VentaJpaMapper.toPersistence(venta);
        VentaJpaModel modelGuardado = ventaJpaRepository.save(model);

        List<DetalleVentaJpaModel> detalles = guardarDetalles(modelGuardado.getId(), venta.getDetalles());

        return VentaJpaMapper.toDomain(modelGuardado, detalles);
    }

    @Override
    public Venta actualizar(VentaId ventaId, Venta venta) {
        UUID id = aUuid(ventaId.getValue());

        VentaJpaModel model = ventaJpaRepository.findById(id)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + ventaId.getValue() + "' no existe"));

        VentaJpaMapper.copiarCambios(venta, model);
        VentaJpaModel modelGuardado = ventaJpaRepository.save(model);

        // Las lineas se reemplazan enteras. El flush fuerza a que los DELETE lleguen a la base
        // antes que los INSERT, porque Hibernate no garantiza ese orden por si solo.
        detalleVentaJpaRepository.deleteByIdVenta(id);
        detalleVentaJpaRepository.flush();

        List<DetalleVentaJpaModel> detalles = guardarDetalles(id, venta.getDetalles());

        return VentaJpaMapper.toDomain(modelGuardado, detalles);
    }

    @Override
    public Optional<Venta> obtenerPorId(VentaId ventaId) {
        UUID id = aUuid(ventaId.getValue());

        return ventaJpaRepository.findById(id)
                .map(model -> VentaJpaMapper.toDomain(model, detalleVentaJpaRepository.findByIdVenta(id)));
    }

    @Override
    public PaginatedResponse<List<Venta>> obtenerTodos(ObtenerVentasRequest obtenerVentasRequest) {
        int skip = obtenerVentasRequest.getSkip();
        int limit = obtenerVentasRequest.getLimit();
        //String search = obtenerVentasRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<VentaJpaModel> pagina = ventaJpaRepository.findAll(pageable);

        Map<UUID, List<DetalleVentaJpaModel>> detallesPorVenta = detallesDe(pagina.getContent());

        List<Venta> datos = pagina.getContent().stream()
                .map(model -> VentaJpaMapper.toDomain(
                        model,
                        detallesPorVenta.getOrDefault(model.getId(), Collections.emptyList())))
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

        // El agregado cae entero: primero las lineas y despues el encabezado.
        detalleVentaJpaRepository.deleteByIdVenta(id);
        ventaJpaRepository.deleteById(id);
    }

    private List<DetalleVentaJpaModel> guardarDetalles(UUID idVenta, List<DetalleVenta> detalles) {
        List<DetalleVentaJpaModel> modelos = detalles.stream()
                .map(detalle -> DetalleVentaJpaMapper.toPersistence(detalle, idVenta))
                .collect(Collectors.toList());

        return detalleVentaJpaRepository.saveAll(modelos);
    }

    /** Resuelve los detalles de una pagina completa en una sola consulta, no una por venta. */
    private Map<UUID, List<DetalleVentaJpaModel>> detallesDe(List<VentaJpaModel> ventas) {
        if (ventas.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> idsVenta = ventas.stream()
                .map(VentaJpaModel::getId)
                .collect(Collectors.toList());

        return detalleVentaJpaRepository.findByIdVentaIn(idsVenta).stream()
                .collect(Collectors.groupingBy(DetalleVentaJpaModel::getIdVenta));
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
