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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Datasources.IVentaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ObtenerVentasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.PagoVentaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.PagoVentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.PagoVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

/**
 * Persistencia del agregado Venta completo: encabezado, lineas y pagos.
 *
 * Maneja los tres JpaRepository porque las tres tablas pertenecen al mismo agregado; esto no
 * contradice la regla de no acoplar datasources entre agregados, ya que detalle_venta y
 * pago_venta no son agregados. La atomicidad la aporta la @Transactional del caso de uso, que
 * envuelve todas las escrituras de este metodo en una sola transaccion de base de datos.
 *
 * Aqui no hay DELETE: el sistema no usa borrado fisico. Las bajas, tanto de una linea como de
 * la venta entera, son UPDATE del campo 'borrado'.
 */
@Repository
public class VentaDatasourceImpl implements IVentaDatasource {

    private final VentaJpaRepository ventaJpaRepository;
    private final DetalleVentaJpaRepository detalleVentaJpaRepository;
    private final PagoVentaJpaRepository pagoVentaJpaRepository;

    public VentaDatasourceImpl(
            VentaJpaRepository ventaJpaRepository,
            DetalleVentaJpaRepository detalleVentaJpaRepository,
            PagoVentaJpaRepository pagoVentaJpaRepository) {
        this.ventaJpaRepository = ventaJpaRepository;
        this.detalleVentaJpaRepository = detalleVentaJpaRepository;
        this.pagoVentaJpaRepository = pagoVentaJpaRepository;
    }

    /**
     * El encabezado se guarda con saveAndFlush: el INSERT tiene que llegar a la base para que
     * seq_numero_ticket asigne el numero de ticket y Hibernate lo relea (@Generated).
     */
    @Override
    public Venta crear(Venta venta) {
        VentaJpaModel modelGuardado = ventaJpaRepository.saveAndFlush(VentaJpaMapper.toPersistence(venta));

        List<DetalleVentaJpaModel> detalles = detalleVentaJpaRepository.saveAll(
                venta.getDetalles().stream()
                        .map(detalle -> DetalleVentaJpaMapper.toPersistence(detalle, modelGuardado.getId()))
                        .collect(Collectors.toList()));

        List<PagoVentaJpaModel> pagos = pagoVentaJpaRepository.saveAll(
                venta.getPagos().stream()
                        .map(pago -> PagoVentaJpaMapper.toPersistence(pago, modelGuardado.getId()))
                        .collect(Collectors.toList()));

        return VentaJpaMapper.toDomain(modelGuardado, detalles, pagos);
    }

    @Override
    public Venta actualizar(VentaId ventaId, Venta venta) {
        UUID id = aUuid(ventaId.getValue());

        VentaJpaModel model = ventaJpaRepository.findByIdAndBorradoFalse(id)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + ventaId.getValue() + "' no existe"));

        VentaJpaMapper.copiarCambios(venta, model);
        VentaJpaModel modelGuardado = ventaJpaRepository.save(model);

        List<DetalleVentaJpaModel> detalles = detalleVentaJpaRepository.saveAll(
                venta.getDetalles().stream()
                        .map(detalle -> conciliar(detalle, id))
                        .collect(Collectors.toList()));

        List<PagoVentaJpaModel> pagos = pagoVentaJpaRepository.saveAll(
                venta.getPagos().stream()
                        .map(pago -> conciliar(pago, id))
                        .collect(Collectors.toList()));

        return VentaJpaMapper.toDomain(modelGuardado, detalles, pagos);
    }

    /**
     * Traduce una linea del agregado a su fila: si ya tenia identidad se recupera la fila y se
     * le copian los cambios, conservando su id; si no, se crea una nueva. Sin borrado fisico,
     * reemplazar la coleccion entera dejaria una fila muerta por cada modificacion.
     */
    private DetalleVentaJpaModel conciliar(DetalleVenta detalle, UUID idVenta) {
        if (detalle.getId() == null) {
            return DetalleVentaJpaMapper.toPersistence(detalle, idVenta);
        }

        UUID idDetalle = aUuid(detalle.getId().getValue());
        DetalleVentaJpaModel model = detalleVentaJpaRepository.findById(idDetalle)
                .orElseThrow(() -> DomainException.throwException(404,
                        "El detalle de venta con id '" + detalle.getId().getValue() + "' no existe"));

        DetalleVentaJpaMapper.copiarCambios(detalle, model);

        return model;
    }

    /** Misma politica que las lineas, para los pagos. */
    private PagoVentaJpaModel conciliar(PagoVenta pago, UUID idVenta) {
        if (pago.getId() == null) {
            return PagoVentaJpaMapper.toPersistence(pago, idVenta);
        }

        PagoVentaJpaModel model = pagoVentaJpaRepository.findById(aUuid(pago.getId().getValue()))
                .orElseThrow(() -> DomainException.throwException(404,
                        "El pago de venta con id '" + pago.getId().getValue() + "' no existe"));

        PagoVentaJpaMapper.copiarCambios(pago, model);

        return model;
    }

    @Override
    public Optional<Venta> obtenerPorId(VentaId ventaId) {
        return ventaJpaRepository.findByIdAndBorradoFalse(aUuid(ventaId.getValue()))
                .map(this::completa);
    }

    @Override
    public PaginatedResponse<List<Venta>> obtenerTodos(ObtenerVentasRequest obtenerVentasRequest) {
        int skip = obtenerVentasRequest.getSkip();
        int limit = obtenerVentasRequest.getLimit();
        //String search = obtenerVentasRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<VentaJpaModel> pagina = ventaJpaRepository.findByBorradoFalse(pageable);

        List<Venta> datos = completas(pagina.getContent());

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

        VentaJpaModel model = ventaJpaRepository.findByIdAndBorradoFalse(id)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + ventaId.getValue() + "' no existe"));

        // La baja la decide el agregado: se reconstruye entero, se le pide eliminar y se
        // escriben tanto el encabezado como las lineas que el marco.
        Venta venta = completa(model);
        venta.eliminar();

        VentaJpaMapper.copiarCambios(venta, model);
        ventaJpaRepository.save(model);

        detalleVentaJpaRepository.saveAll(
                venta.getDetalles().stream()
                        .map(detalle -> conciliar(detalle, id))
                        .collect(Collectors.toList()));
    }

    @Override
    public Optional<Venta> obtenerPorClaveIdempotencia(String claveIdempotencia) {
        return ventaJpaRepository.findByClaveIdempotenciaAndBorradoFalse(claveIdempotencia)
                .map(this::completa);
    }

    @Override
    public Optional<Venta> obtenerPorNumeroTicket(Long numeroTicket) {
        return ventaJpaRepository.findByNumeroTicketAndBorradoFalse(numeroTicket)
                .map(this::completa);
    }

    @Override
    public List<Venta> obtenerPorSesionCaja(SesionCajaId sesionCajaId) {
        return completas(ventaJpaRepository.findByIdSesionCajaAndBorradoFalse(aUuid(sesionCajaId.getValue())));
    }

    private Venta completa(VentaJpaModel model) {
        return VentaJpaMapper.toDomain(
                model,
                detalleVentaJpaRepository.findByIdVentaAndBorradoFalse(model.getId()),
                pagoVentaJpaRepository.findByIdVentaAndBorradoFalse(model.getId()));
    }

    /** Resuelve lineas y pagos de varias ventas en dos consultas, no en dos por venta. */
    private List<Venta> completas(List<VentaJpaModel> ventas) {
        if (ventas.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> idsVenta = ventas.stream()
                .map(VentaJpaModel::getId)
                .collect(Collectors.toList());

        Map<UUID, List<DetalleVentaJpaModel>> detallesPorVenta = detalleVentaJpaRepository.findByIdVentaInAndBorradoFalse(idsVenta).stream()
                .collect(Collectors.groupingBy(DetalleVentaJpaModel::getIdVenta));
        Map<UUID, List<PagoVentaJpaModel>> pagosPorVenta = pagoVentaJpaRepository.findByIdVentaInAndBorradoFalse(idsVenta).stream()
                .collect(Collectors.groupingBy(PagoVentaJpaModel::getIdVenta));

        return ventas.stream()
                .map(model -> VentaJpaMapper.toDomain(
                        model,
                        detallesPorVenta.getOrDefault(model.getId(), Collections.emptyList()),
                        pagosPorVenta.getOrDefault(model.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
