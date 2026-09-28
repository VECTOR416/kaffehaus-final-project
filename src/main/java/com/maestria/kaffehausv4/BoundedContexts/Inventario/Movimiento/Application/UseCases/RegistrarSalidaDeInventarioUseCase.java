package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.MovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarSalidaDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.SalidaDeInventarioResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Operacion interna de inventario: descuenta una cantidad de un producto y registra un
 * movimiento por cada existencia tocada. No tiene endpoint; la invocan CrearProduccionUseCase
 * (consumo de insumos) y CrearVentaUseCase (salida por venta), dentro de su transaccion.
 *
 * Implementa el algoritmo de consumo de inventario.md (INV-32). Lo que se descuenta depende
 * de la naturaleza del producto:
 * - Stock directo (MTS, reventa, insumos): sale de sus existencias abiertas y no vencidas, las
 *   suyas y las abiertas desde sus presentaciones, en orden FEFO, repartiendo el consumo entre
 *   varias si una sola no alcanza. Las existencias se bloquean antes de leerlas (INV-52). Una
 *   existencia que se vacia pasa al estado 'Agotada'. Al terminar recalcula 'producto.stock'.
 * - Bajo demanda (MTO): no tiene existencias propias. Se descuentan los ingredientes de su
 *   receta vigente escalados a la cantidad pedida (backflush). Si un ingrediente es a su vez
 *   bajo demanda se explota su receta, y un ciclo entre recetas se rechaza.
 *
 * Nunca abre unidades cerradas (INV-34): si el stock abierto no alcanza lanza 409 y, al estar
 * dentro de la transaccion del caso de uso que la invoca, no queda ni una existencia
 * descontada a medias. Si hay unidades cerradas que alcanzarian, el mensaje lo dice para que
 * el usuario abra una presentacion (INV-13, INV-14) y reintente.
 *
 * Solo se consideran las existencias en la unidad del producto ('unidad_actual'): la apertura
 * ya las dejo expresadas en ella (INV-10).
 */
@Service
@Transactional
public class RegistrarSalidaDeInventarioUseCase {
    // Fila de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String ESTADO_AGOTADA = "Agotada";

    /** Escala de NUMERIC(12, 4) en 'existencias' y 'movimientos'. */
    private static final int ESCALA_CANTIDAD = 4;
    private static final int ESCALA_FACTOR = 6;

    private final IProductoRepository productoRepository;
    private final IRecetaRepository recetaRepository;
    private final ILoteRepository loteRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public RegistrarSalidaDeInventarioUseCase(
            IProductoRepository productoRepository,
            IRecetaRepository recetaRepository,
            ILoteRepository loteRepository,
            IExistenciaRepository existenciaRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.productoRepository = productoRepository;
        this.recetaRepository = recetaRepository;
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public SalidaDeInventarioResponse execute(RegistrarSalidaDeInventarioRequest request) {
        EstadoExistencia agotada = estadoExistenciaRepository.obtenerPorNombre(ESTADO_AGOTADA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia '" + ESTADO_AGOTADA + "' no esta registrado"));

        Salida salida = new Salida(
                request,
                tipoMovimiento(request.getNombreTipoMovimientoDirecto()),
                tipoMovimiento(request.getNombreTipoMovimientoPorReceta()),
                agotada);

        BigDecimal costo = descontar(new ProductoId(request.getIdProducto()), request.getCantidad(), salida, false);

        return SalidaDeInventarioResponse.create(costo.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP), salida.registrados);
    }

    private TipoMovimiento tipoMovimiento(String nombre) {
        return tipoMovimientoRepository.obtenerPorNombre(nombre)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento '" + nombre + "' no esta registrado"));
    }

    /** 'porReceta' indica que la cantidad viene de explotar la receta de un producto MTO. */
    private BigDecimal descontar(ProductoId productoId, BigDecimal cantidad, Salida salida, boolean porReceta) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + productoId.getValue() + "' no existe"));

        if (producto.isEsStockDirecto()) {
            return descontarExistencias(producto, cantidad, salida, porReceta ? salida.tipoPorReceta : salida.tipoDirecto);
        }
        return descontarReceta(producto, cantidad, salida);
    }

    /** Backflush: descuenta los ingredientes de la receta escalados a la cantidad pedida. */
    private BigDecimal descontarReceta(Producto producto, BigDecimal cantidad, Salida salida) {
        String idProducto = producto.getId().getValue();
        if (!salida.recetasEnCurso.add(idProducto)) {
            throw DomainException.throwException(409,
                    "La receta del producto '" + producto.getNombre() + "' se contiene a si misma");
        }

        Receta receta = recetaRepository.obtenerPorProductoObtenido(producto.getId())
                .orElseThrow(() -> DomainException.throwException(409,
                        "El producto '" + producto.getNombre() + "' se prepara bajo demanda y no tiene receta"));

        BigDecimal factor = cantidad.divide(receta.getCantidad(), ESCALA_FACTOR, RoundingMode.HALF_UP);

        BigDecimal costo = BigDecimal.ZERO;
        for (IngredienteReceta ingrediente : receta.getIngredientesActivos()) {
            costo = costo.add(descontar(ingrediente.getProductoId(), ingrediente.getCantidad().multiply(factor), salida, true));
        }

        salida.recetasEnCurso.remove(idProducto);
        return costo;
    }

    /** Consumo FEFO sobre las existencias abiertas del producto. Devuelve su costo. */
    private BigDecimal descontarExistencias(Producto producto, BigDecimal cantidad, Salida salida, TipoMovimiento tipoMovimiento) {
        BigDecimal requerido = cantidad.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
        if (requerido.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        // El insumo base y sus presentaciones: las existencias abiertas desde cualquiera de ellas
        // contienen al insumo base (INV-08).
        List<ProductoId> fuentes = new ArrayList<>();
        fuentes.add(producto.getId());
        productoRepository.obtenerPresentaciones(producto.getId()).forEach(presentacion -> fuentes.add(presentacion.getId()));
        Set<String> idsFuente = fuentes.stream().map(ProductoId::getValue).collect(Collectors.toSet());

        LocalDate hoy = salida.request.getFecha().toLocalDate();
        Map<String, Lote> lotePorDetalle = new HashMap<>();
        List<DetalleLoteId> detallesDelProducto = new ArrayList<>();
        boolean hayUnidadesCerradas = false;
        for (Lote lote : loteRepository.obtenerPorProductos(fuentes)) {
            for (DetalleLote detalle : lote.getDetallesActivos()) {
                if (idsFuente.contains(detalle.getProductoId().getValue())) {
                    lotePorDetalle.put(detalle.getId().getValue(), lote);
                    detallesDelProducto.add(detalle.getId());
                    hayUnidadesCerradas = hayUnidadesCerradas || (detalle.tieneUnidadesCerradas() && !detalle.estaVencido(hoy));
                }
            }
        }

        String unidadDelProducto = producto.getUnidadActualId().getValue();
        List<Existencia> disponibles = existenciaRepository.obtenerDisponiblesParaConsumo(detallesDelProducto).stream()
                .filter(existencia -> existencia.getUnidadId().getValue().equals(unidadDelProducto))
                .filter(existencia -> !existencia.estaVencida(hoy))
                .collect(Collectors.toList());

        BigDecimal totalDisponible = disponibles.stream()
                .map(Existencia::getContenidoActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDisponible.compareTo(requerido) < 0) {
            String cantidades = ": se requieren " + requerido.stripTrailingZeros().toPlainString()
                    + " y hay " + totalDisponible.stripTrailingZeros().toPlainString() + " abiertas y vigentes";
            if (hayUnidadesCerradas) {
                throw DomainException.throwException(409,
                        "Stock abierto insuficiente de '" + producto.getNombre() + "' (idProducto '" + producto.getId().getValue()
                        + "')" + cantidades + ": abre una presentacion para continuar");
            }
            throw DomainException.throwException(409, "Stock insuficiente de '" + producto.getNombre() + "'" + cantidades);
        }

        BigDecimal pendiente = requerido;
        BigDecimal costo = BigDecimal.ZERO;
        for (Existencia existencia : disponibles) {
            if (pendiente.signum() == 0) {
                break;
            }

            BigDecimal consumo = pendiente.min(existencia.getContenidoActual());
            existencia.consumir(consumo, salida.request.getFecha(), salida.agotada.getId());
            existenciaRepository.actualizar(existencia.getId(), existencia);

            Lote lote = lotePorDetalle.get(existencia.getDetalleLoteId().getValue());
            costo = costo.add(consumo.multiply(
                    lote.costoPorUnidadDeContenido(existencia.getDetalleLoteId(), existencia.getContenidoInicial())));

            Movimiento movimiento = movimientoRepository.crear(Movimiento.crear(
                    consumo,
                    salida.request.getFecha(),
                    salida.request.getMotivo(),
                    existencia.getId(),
                    tipoMovimiento.getId(),
                    salida.usuarioId,
                    salida.produccionId));
            salida.registrados.add(MovimientoResponse.createFromAggregate(movimiento, tipoMovimiento));

            pendiente = pendiente.subtract(consumo);
        }

        recalcularStockUseCase.execute(producto.getId().getValue());
        return costo;
    }

    /** Estado de una ejecucion: lo que ya se resolvio una vez y lo que se va acumulando. */
    private static final class Salida {
        private final RegistrarSalidaDeInventarioRequest request;
        private final TipoMovimiento tipoDirecto;
        private final TipoMovimiento tipoPorReceta;
        private final EstadoExistencia agotada;
        private final UsuarioId usuarioId;
        private final ProduccionId produccionId;
        private final List<MovimientoResponse> registrados = new ArrayList<>();
        private final Set<String> recetasEnCurso = new HashSet<>();

        private Salida(
                RegistrarSalidaDeInventarioRequest request,
                TipoMovimiento tipoDirecto,
                TipoMovimiento tipoPorReceta,
                EstadoExistencia agotada) {
            this.request = request;
            this.tipoDirecto = tipoDirecto;
            this.tipoPorReceta = tipoPorReceta;
            this.agotada = agotada;
            this.usuarioId = new UsuarioId(request.getIdUsuario());
            this.produccionId = request.getIdProduccion() != null ? new ProduccionId(request.getIdProduccion()) : null;
        }
    }
}
