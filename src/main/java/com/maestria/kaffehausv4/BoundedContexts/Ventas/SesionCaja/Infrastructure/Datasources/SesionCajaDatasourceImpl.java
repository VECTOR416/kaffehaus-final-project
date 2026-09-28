package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Datasources;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Datasources.ISesionCajaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ObtenerSesionesCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.MovimientoCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.MovimientoCajaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.MovimientoCajaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.MovimientoCajaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.SesionCajaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.SesionCajaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.SesionCajaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

/**
 * Persistencia del agregado SesionCaja completo: encabezado mas movimientos de efectivo.
 * Maneja los dos JpaRepository porque ambas tablas pertenecen al mismo agregado.
 */
@Repository
public class SesionCajaDatasourceImpl implements ISesionCajaDatasource {

    private final SesionCajaJpaRepository sesionCajaJpaRepository;
    private final MovimientoCajaJpaRepository movimientoCajaJpaRepository;

    public SesionCajaDatasourceImpl(
            SesionCajaJpaRepository sesionCajaJpaRepository,
            MovimientoCajaJpaRepository movimientoCajaJpaRepository) {
        this.sesionCajaJpaRepository = sesionCajaJpaRepository;
        this.movimientoCajaJpaRepository = movimientoCajaJpaRepository;
    }

    @Override
    public SesionCaja crear(SesionCaja sesionCaja) {
        SesionCajaJpaModel modelGuardado = sesionCajaJpaRepository.save(SesionCajaJpaMapper.toPersistence(sesionCaja));

        List<MovimientoCajaJpaModel> movimientos = movimientoCajaJpaRepository.saveAll(
                sesionCaja.getMovimientos().stream()
                        .map(movimiento -> MovimientoCajaJpaMapper.toPersistence(movimiento, modelGuardado.getId()))
                        .collect(Collectors.toList()));

        return SesionCajaJpaMapper.toDomain(modelGuardado, movimientos);
    }

    @Override
    public SesionCaja actualizar(SesionCajaId sesionCajaId, SesionCaja sesionCaja) {
        UUID id = aUuid(sesionCajaId.getValue());

        SesionCajaJpaModel model = sesionCajaJpaRepository.findByIdAndBorradoFalse(id)
                .orElseThrow(() -> DomainException.throwException(404, "La sesion de caja con id '" + sesionCajaId.getValue() + "' no existe"));

        SesionCajaJpaMapper.copiarCambios(sesionCaja, model);
        SesionCajaJpaModel modelGuardado = sesionCajaJpaRepository.save(model);

        movimientoCajaJpaRepository.saveAll(
                sesionCaja.getMovimientos().stream()
                        .map(movimiento -> conciliar(movimiento, id))
                        .collect(Collectors.toList()));
        // Se releen para devolver la fecha que fija la persistencia a los movimientos nuevos.
        movimientoCajaJpaRepository.flush();

        return SesionCajaJpaMapper.toDomain(modelGuardado, movimientoCajaJpaRepository.findByIdSesionCajaAndBorradoFalseOrderByFecCAsc(id));
    }

    /** Movimiento con identidad: se recupera su fila. Sin ella: fila nueva. */
    private MovimientoCajaJpaModel conciliar(MovimientoCaja movimiento, UUID idSesionCaja) {
        if (movimiento.getId() == null) {
            return MovimientoCajaJpaMapper.toPersistence(movimiento, idSesionCaja);
        }

        MovimientoCajaJpaModel model = movimientoCajaJpaRepository.findById(aUuid(movimiento.getId().getValue()))
                .orElseThrow(() -> DomainException.throwException(404,
                        "El movimiento de caja con id '" + movimiento.getId().getValue() + "' no existe"));

        MovimientoCajaJpaMapper.copiarCambios(movimiento, model);

        return model;
    }

    @Override
    public Optional<SesionCaja> obtenerPorId(SesionCajaId sesionCajaId) {
        return sesionCajaJpaRepository.findByIdAndBorradoFalse(aUuid(sesionCajaId.getValue()))
                .map(this::conMovimientos);
    }

    @Override
    public PaginatedResponse<List<SesionCaja>> obtenerTodos(ObtenerSesionesCajaRequest obtenerSesionesCajaRequest) {
        int skip = obtenerSesionesCajaRequest.getSkip();
        int limit = obtenerSesionesCajaRequest.getLimit();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        // Sin campo de texto buscable: 'search' se ignora. La mas reciente primero.
        Page<SesionCajaJpaModel> pagina = sesionCajaJpaRepository.findByBorradoFalseOrderByFechaAperturaDesc(pageable);

        Map<UUID, List<MovimientoCajaJpaModel>> movimientosPorSesion = movimientosDe(pagina.getContent());

        List<SesionCaja> datos = pagina.getContent().stream()
                .map(model -> SesionCajaJpaMapper.toDomain(
                        model,
                        movimientosPorSesion.getOrDefault(model.getId(), Collections.emptyList())))
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
    public Optional<SesionCaja> obtenerAbierta() {
        return sesionCajaJpaRepository.findFirstByEstadoAndBorradoFalse(SesionCaja.ABIERTA)
                .map(this::conMovimientos);
    }

    @Override
    public Optional<SesionCaja> obtenerAbiertaParaActualizar() {
        return sesionCajaJpaRepository.readFirstByEstadoAndBorradoFalse(SesionCaja.ABIERTA)
                .map(this::conMovimientos);
    }

    @Override
    public Optional<SesionCaja> obtenerAbiertaParaVenta() {
        return sesionCajaJpaRepository.getFirstByEstadoAndBorradoFalse(SesionCaja.ABIERTA)
                .map(this::conMovimientos);
    }

    private SesionCaja conMovimientos(SesionCajaJpaModel model) {
        return SesionCajaJpaMapper.toDomain(model, movimientoCajaJpaRepository.findByIdSesionCajaAndBorradoFalseOrderByFecCAsc(model.getId()));
    }

    /** Resuelve los movimientos de una pagina completa en una sola consulta. */
    private Map<UUID, List<MovimientoCajaJpaModel>> movimientosDe(List<SesionCajaJpaModel> sesiones) {
        if (sesiones.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> ids = sesiones.stream()
                .map(SesionCajaJpaModel::getId)
                .collect(Collectors.toList());

        return movimientoCajaJpaRepository.findByIdSesionCajaInAndBorradoFalse(ids).stream()
                .collect(Collectors.groupingBy(MovimientoCajaJpaModel::getIdSesionCaja));
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
