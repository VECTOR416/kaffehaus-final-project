package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
 * Operacion interna de inventario: descuenta una cantidad de un producto. No tiene endpoint; la
 * invocan CrearProduccionUseCase (consumo de insumos) y CrearVentaUseCase (salida por venta),
 * dentro de su transaccion.
 *
 * Lo que se descuenta depende del producto (logica.md PRODUCTO-14, MOVIMIENTO-18):
 * - Producto base con presentaciones (harina): sale de sus existencias abiertas y no vencidas,
 *   las abiertas desde sus presentaciones, en orden FEFO, repartiendo el consumo entre varias si
 *   una sola no alcanza. Las existencias se bloquean antes de leerlas (INV-52). Una existencia que
 *   se vacia pasa al estado 'Agotada'. Registra un movimiento 'Produccion' por existencia tocada.
 * - Producto por unidades (pan, gaseosa, una bolsa cerrada): descuenta unidades de sus lineas de
 *   lote (LOTE-17), bloqueandolas. No registra movimiento.
 * - Bajo demanda (MTO): en una produccion se descuentan los ingredientes de su receta vigente
 *   escalados a la cantidad pedida, y un ciclo entre recetas se rechaza; en una venta no se
 *   descuenta nada (VENTA-23).
 *
 * Al terminar recalcula el stock de cada producto tocado. Nunca abre unidades cerradas (INV-34):
 * si no alcanza lanza 409 y, al estar dentro de la transaccion de quien la invoca, no queda nada
 * descontado a medias. Si hay bolsas cerradas que alcanzarian, el mensaje lo dice para que el
 * usuario abra una presentacion (LOTE-14) y reintente.
 */
@Service
@Transactional
public class RegistrarSalidaDeInventarioUseCase {
    // Fila de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String ESTADO_AGOTADA = "Agotada";

    /** Escala de NUMERIC(12, 4) en 'existencias' y 'movimientos'. */
    private static final int ESCALA_CANTIDAD = 4;
    /** Escala de NUMERIC(12, 2) en 'detalle_lote'. */
    private static final int ESCALA_UNIDADES = 2;
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
        Salida salida = new Salida(request);
        descontar(new ProductoId(request.getIdProducto()), request.getCantidad(), salida);
        return SalidaDeInventarioResponse.create(salida.registrados);
    }

    private void descontar(ProductoId productoId, BigDecimal cantidad, Salida salida) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + productoId.getValue() + "' no existe"));

        if (!producto.isEsStockDirecto()) {
            if (salida.request.isExplotarBajoDemanda()) {
                descontarReceta(producto, cantidad, salida);
            }
            return;
        }

        List<Producto> presentaciones = productoRepository.obtenerPresentaciones(producto.getId());
        if (presentaciones.isEmpty()) {
            descontarUnidades(producto, cantidad, salida);
        } else {
            descontarExistencias(producto, presentaciones, cantidad, salida);
        }
    }

    /** Backflush: descuenta los ingredientes de la receta escalados a la cantidad pedida. */
    private void descontarReceta(Producto producto, BigDecimal cantidad, Salida salida) {
        String idProducto = producto.getId().getValue();
        if (!salida.recetasEnCurso.add(idProducto)) {
            throw DomainException.throwException(409,
                    "La receta del producto '" + producto.getNombre() + "' se contiene a si misma");
        }

        Receta receta = recetaRepository.obtenerPorProductoObtenido(producto.getId())
                .orElseThrow(() -> DomainException.throwException(409,
                        "El producto '" + producto.getNombre() + "' se prepara bajo demanda y no tiene receta"));

        BigDecimal factor = cantidad.divide(receta.getCantidad(), ESCALA_FACTOR, RoundingMode.HALF_UP);
        for (IngredienteReceta ingrediente : receta.getIngredientesActivos()) {
            descontar(ingrediente.getProductoId(), ingrediente.getCantidad().multiply(factor), salida);
        }

        salida.recetasEnCurso.remove(idProducto);
    }

    /**
     * Descuenta unidades de las lineas de lote del producto (logica.md LOTE-17): primero la que
     * vence antes, bloqueando cada linea antes de descontarla.
     */
    private void descontarUnidades(Producto producto, BigDecimal cantidad, Salida salida) {
        BigDecimal requerido = cantidad.setScale(ESCALA_UNIDADES, RoundingMode.HALF_UP);
        if (requerido.signum() <= 0) {
            return;
        }

        LocalDate hoy = salida.request.getFecha().toLocalDate();
        List<DetalleLote> lineas = Lote.conUnidadesEnOrden(
                loteRepository.obtenerPorProductos(List.of(producto.getId())), producto.getId(), hoy);
        BigDecimal disponible = lineas.stream()
                .map(DetalleLote::getCantidadRestante)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (disponible.compareTo(requerido) < 0) {
            throw DomainException.throwException(409, "Stock insuficiente de '" + producto.getNombre() + "': se requieren "
                    + requerido.stripTrailingZeros().toPlainString() + " y hay " + disponible.stripTrailingZeros().toPlainString()
                    + " unidades vigentes");
        }

        BigDecimal pendiente = requerido;
        for (DetalleLote linea : lineas) {
            if (pendiente.signum() == 0) {
                break;
            }
            String idDetalle = linea.getId().getValue();
            Lote lote = loteRepository.obtenerPorDetalleIdParaActualizar(linea.getId())
                    .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + idDetalle + "' no existe"));
            // Releida con el bloqueo tomado: otra transaccion pudo descontar antes.
            BigDecimal tomado = pendiente.min(lote.obtenerDetalle(idDetalle).getCantidadRestante());
            if (tomado.signum() <= 0) {
                continue;
            }
            lote.descontarUnidades(idDetalle, tomado);
            loteRepository.actualizar(lote.getId(), lote);
            pendiente = pendiente.subtract(tomado);
        }

        if (pendiente.signum() > 0) {
            throw DomainException.throwException(409,
                    "Stock insuficiente de '" + producto.getNombre() + "': otra operacion desconto unidades a la vez; reintenta");
        }
        recalcularStockUseCase.execute(producto.getId().getValue());
    }

    /** Consumo FEFO sobre las existencias abiertas de un producto base con presentaciones. */
    private void descontarExistencias(Producto producto, List<Producto> presentaciones, BigDecimal cantidad, Salida salida) {
        BigDecimal requerido = cantidad.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
        if (requerido.signum() <= 0) {
            return;
        }

        // El insumo base y sus presentaciones: las existencias abiertas desde cualquiera de ellas
        // contienen al insumo base (INV-08).
        List<ProductoId> fuentes = new ArrayList<>();
        fuentes.add(producto.getId());
        presentaciones.forEach(presentacion -> fuentes.add(presentacion.getId()));
        Set<String> idsFuente = fuentes.stream().map(ProductoId::getValue).collect(Collectors.toSet());

        LocalDate hoy = salida.request.getFecha().toLocalDate();
        List<DetalleLoteId> detallesDelProducto = new ArrayList<>();
        boolean hayUnidadesCerradas = false;
        for (Lote lote : loteRepository.obtenerPorProductos(fuentes)) {
            for (DetalleLote detalle : lote.getDetallesActivos()) {
                if (idsFuente.contains(detalle.getProductoId().getValue())) {
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

        TipoMovimiento tipoProduccion = salida.tipoProduccion();
        EstadoExistencia agotada = salida.agotada();
        BigDecimal pendiente = requerido;
        for (Existencia existencia : disponibles) {
            if (pendiente.signum() == 0) {
                break;
            }
            BigDecimal consumo = pendiente.min(existencia.getContenidoActual());
            existencia.consumir(consumo, salida.request.getFecha(), agotada.getId());
            existenciaRepository.actualizar(existencia.getId(), existencia);

            Movimiento movimiento = movimientoRepository.crear(Movimiento.crear(
                    consumo,
                    salida.request.getFecha(),
                    salida.request.getMotivo(),
                    existencia.getId(),
                    tipoProduccion.getId(),
                    salida.usuarioId,
                    salida.produccionId));
            salida.registrados.add(MovimientoResponse.createFromAggregate(movimiento, tipoProduccion));

            pendiente = pendiente.subtract(consumo);
        }

        recalcularStockUseCase.execute(producto.getId().getValue());
    }

    /**
     * Estado de una ejecucion: lo que ya se resolvio una vez y lo que se va acumulando. Las filas
     * de catalogo se resuelven solo si se tocan existencias: una venta de unidades no las necesita.
     */
    private final class Salida {
        private final RegistrarSalidaDeInventarioRequest request;
        private final UsuarioId usuarioId;
        private final ProduccionId produccionId;
        private final List<MovimientoResponse> registrados = new ArrayList<>();
        private final Set<String> recetasEnCurso = new HashSet<>();
        private TipoMovimiento tipoProduccion;
        private EstadoExistencia agotada;

        private Salida(RegistrarSalidaDeInventarioRequest request) {
            this.request = request;
            this.usuarioId = new UsuarioId(request.getIdUsuario());
            this.produccionId = request.getIdProduccion() != null ? new ProduccionId(request.getIdProduccion()) : null;
        }

        private TipoMovimiento tipoProduccion() {
            if (tipoProduccion == null) {
                tipoProduccion = tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.PRODUCCION)
                        .orElseThrow(() -> DomainException.throwException(404,
                                "El tipo de movimiento '" + TipoMovimiento.PRODUCCION + "' no esta registrado"));
            }
            return tipoProduccion;
        }

        private EstadoExistencia agotada() {
            if (agotada == null) {
                agotada = estadoExistenciaRepository.obtenerPorNombre(ESTADO_AGOTADA)
                        .orElseThrow(() -> DomainException.throwException(404,
                                "El estado de existencia '" + ESTADO_AGOTADA + "' no esta registrado"));
            }
            return agotada;
        }
    }
}
