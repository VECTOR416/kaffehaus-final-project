package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.RegistrarAperturaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.RegistrarAperturaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DarDeBajaDetalleVencidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Da de baja las unidades cerradas de un detalle vencido (inventario.md INV-42). Para que la
 * perdida quede en el libro mayor, cada unidad se abre (movimiento 'Apertura') y su contenido
 * se da de baja de inmediato (movimiento 'Vencimiento'), dejando la existencia 'Agotada'.
 * Todo en una transaccion.
 */
@Service
@Transactional
public class DarDeBajaDetalleVencidoUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String ESTADO_AGOTADA = "Agotada";

    private final ILoteRepository loteRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IProductoRepository productoRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final RegistrarAperturaUseCase registrarAperturaUseCase;
    private final RecalcularStockUseCase recalcularStockUseCase;
    private final IUnidadRepository unidadRepository;

    public DarDeBajaDetalleVencidoUseCase(
            ILoteRepository loteRepository,
            IUsuarioRepository usuarioRepository,
            IProductoRepository productoRepository,
            IExistenciaRepository existenciaRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            RegistrarAperturaUseCase registrarAperturaUseCase,
            RecalcularStockUseCase recalcularStockUseCase,
            IUnidadRepository unidadRepository) {
        this.loteRepository = loteRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.existenciaRepository = existenciaRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.registrarAperturaUseCase = registrarAperturaUseCase;
        this.recalcularStockUseCase = recalcularStockUseCase;
        this.unidadRepository = unidadRepository;
    }

    public List<ExistenciaResponse> execute(String idDetalleLote, DarDeBajaDetalleVencidoRequest request) {
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + request.getIdUsuario() + "' no existe"));

        LocalDateTime ahora = LocalDateTime.now();

        Lote lote = loteRepository.obtenerPorDetalleIdParaActualizar(new DetalleLoteId(idDetalleLote))
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + idDetalleLote + "' no existe"));
        DetalleLote detalle = lote.obtenerDetalle(idDetalleLote);

        if (!detalle.estaVencido(ahora.toLocalDate())) {
            throw DomainException.throwException(409, "El detalle de lote con id '" + idDetalleLote + "' no esta vencido");
        }
        if (!detalle.tieneUnidadesCerradas()) {
            throw DomainException.throwException(409, "El detalle de lote con id '" + idDetalleLote + "' no tiene unidades cerradas");
        }

        TipoMovimiento merma = tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.MERMA)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento '" + TipoMovimiento.MERMA + "' no esta registrado"));
        EstadoExistencia agotada = estadoExistenciaRepository.obtenerPorNombre(ESTADO_AGOTADA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia '" + ESTADO_AGOTADA + "' no esta registrado"));
        String motivo = request.getMotivo() != null ? request.getMotivo() : "Vencimiento de unidades cerradas";

        // Solo unidades enteras: una fraccion de unidad no se puede abrir (INV-10).
        int unidades = detalle.getCantidadRestante().intValue();
        List<ExistenciaResponse> bajas = new ArrayList<>();
        for (int i = 0; i < unidades; i++) {
            ExistenciaResponse abierta = registrarAperturaUseCase.execute(
                    RegistrarAperturaRequest.crear(idDetalleLote, request.getIdUsuario(), true, ahora));

            Existencia existencia = existenciaRepository.obtenerPorIdParaActualizar(new ExistenciaId(abierta.getId()))
                    .orElseThrow(() -> DomainException.throwException(404, "La existencia recien abierta no existe"));
            BigDecimal cantidad = existencia.darDeBajaTodo(ahora, agotada.getId());
            existenciaRepository.actualizar(existencia.getId(), existencia);

            movimientoRepository.crear(Movimiento.crear(cantidad, ahora, motivo, existencia.getId(), merma.getId(), usuarioId, null));
            bajas.add(ExistenciaResponse.createFromAggregate(existencia, agotada, unidadRepository.obtenerPorId(existencia.getUnidadId()).orElse(null)));
        }

        Producto presentacion = productoRepository.obtenerPorId(detalle.getProductoId())
                .orElseThrow(() -> DomainException.throwException(404, "El producto del detalle de lote no existe"));
        recalcularStockUseCase.execute(presentacion.productoBaseId().getValue());

        return bajas;
    }
}
