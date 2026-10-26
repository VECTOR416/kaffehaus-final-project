package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Operacion interna de inventario: descuenta todas las lineas de una produccion o de una venta
 * (logica.md MOVIMIENTO-19). No tiene endpoint; la invocan CrearProduccionUseCase y
 * CrearVentaUseCase dentro de su transaccion.
 *
 * 1. Plan de consumo (GENERAL-14), sin bloquear nada: explota recursivamente cada MTO por su
 *    receta vigente escalada a 'cantidad / rendimiento' (un MTO sin receta o un ciclo entre
 *    recetas responde 409) y agrega por producto, de modo que un insumo que usan dos lineas
 *    aparece una sola vez con la suma.
 * 2. Bloquea de una vez, en orden de id de lote, los lotes de los que va a descontar unidades
 *    (GENERAL-15, LOTE-27): los que, segun una lectura sin cargar, cubren cada producto por
 *    unidades en el orden de LOTE-17.
 * 3. Recorre el plan en orden de id de producto. Por cada entrada, si alcanza, la descuenta y
 *    recalcula el stock del producto:
 *    - Producto base con presentaciones (harina, leche): de sus existencias abiertas, con
 *      contenido y no vencidas a la fecha de la operacion, en orden FEFO, bloqueadas al leerlas.
 *      Una existencia que se vacia pasa a 'Agotada'.
 *    - Producto por unidades o presentacion (pan, una bolsa cerrada): de las lineas de los lotes
 *      ya bloqueados, en el orden de LOTE-17. Si otra operacion se llevo unidades antes del
 *      bloqueo, lo que falta se informa como faltante.
 *    Cada existencia o linea tocada registra un movimiento del tipo de la operacion ('Produccion'
 *    o 'Venta') con su documento (MOVIMIENTO-20, MOVIMIENTO-21).
 * 4. Si alguna entrada no alcanza, sigue evaluando las demas sin descontarlas y al final responde
 *    409 con todas las que faltan (VENTA-30): 'APERTURA_REQUERIDA' si todo se resuelve abriendo
 *    bolsas cerradas vigentes, con la bolsa sugerida de cada insumo; 'STOCK_INSUFICIENTE' en
 *    cualquier otro caso. Al estar dentro de la transaccion de quien la invoca, lo ya descontado
 *    se revierte. Nunca abre bolsas por su cuenta (LOTE-08).
 */
@Service
@Transactional
public class RegistrarSalidaDeInventarioUseCase {
    /** Codigos de los errores estructurados que lanza (logica.md VENTA-30, arquitectura.md 4.16). */
    public static final String APERTURA_REQUERIDA = "APERTURA_REQUERIDA";
    public static final String STOCK_INSUFICIENTE = "STOCK_INSUFICIENTE";

    // Fila de catalogo que el flujo resuelve por nombre (logica.md GENERAL-04).
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
    private final IUnidadRepository unidadRepository;
    private final IConversionRepository conversionRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public RegistrarSalidaDeInventarioUseCase(
            IProductoRepository productoRepository,
            IRecetaRepository recetaRepository,
            ILoteRepository loteRepository,
            IExistenciaRepository existenciaRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            IUnidadRepository unidadRepository,
            IConversionRepository conversionRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.productoRepository = productoRepository;
        this.recetaRepository = recetaRepository;
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.unidadRepository = unidadRepository;
        this.conversionRepository = conversionRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public SalidaDeInventarioResponse execute(RegistrarSalidaDeInventarioRequest request) {
        Salida salida = new Salida(request);

        for (RegistrarSalidaDeInventarioRequest.Linea linea : request.getLineas()) {
            planificar(new ProductoId(linea.getIdProducto()), linea.getCantidad(), salida);
        }
        bloquearLotes(salida);

        List<Faltante> faltantes = new ArrayList<>();
        for (Entrada entrada : salida.plan.values()) {
            // Tras el primer faltante ya no se descuenta nada: solo se evalua para informarlo todo.
            Optional<Faltante> faltante = entrada.porExistencias()
                    ? descontarExistencias(entrada, salida, faltantes.isEmpty())
                    : descontarUnidades(entrada, salida, faltantes.isEmpty());
            faltante.ifPresent(faltantes::add);
        }
        if (!faltantes.isEmpty()) {
            throw error(faltantes);
        }

        return SalidaDeInventarioResponse.create(salida.registrados);
    }

    // ------------------------------------------------------------------ plan de consumo

    /** Suma la cantidad al plan; un MTO se explota por su receta vigente (GENERAL-14). */
    private void planificar(ProductoId productoId, BigDecimal cantidad, Salida salida) {
        Producto producto = salida.producto(productoId);
        if (producto.isEsStockDirecto()) {
            salida.plan.computeIfAbsent(producto.getId().getValue(), id -> new Entrada(producto, presentacionesDe(producto)))
                    .sumar(cantidad);
            return;
        }

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
            planificar(ingrediente.getProductoId(), ingrediente.getCantidad().multiply(factor), salida);
        }
        salida.recetasEnCurso.remove(idProducto);
    }

    private List<Producto> presentacionesDe(Producto producto) {
        return productoRepository.obtenerPresentaciones(producto.getId());
    }

    /**
     * Bloquea por su raiz, en una sola lectura y en orden de id, los lotes de los que el plan va a
     * descontar unidades (GENERAL-15, LOTE-27). Para elegirlos lee sin cargar (arquitectura.md
     * 5.7) las lineas de cada producto por unidades en el orden de LOTE-17 y toma las que cubren
     * lo requerido; si no alcanzan, todas, para que el faltante se informe con lo que hay.
     */
    private void bloquearLotes(Salida salida) {
        Set<String> idsLote = new TreeSet<>();
        for (Entrada entrada : salida.plan.values()) {
            if (entrada.porExistencias()) {
                continue;
            }
            BigDecimal requerido = entrada.cantidad.setScale(ESCALA_UNIDADES, RoundingMode.HALF_UP);
            List<Lote> lotes = loteRepository.obtenerPorProductosSinCargar(List.of(entrada.producto.getId()));
            BigDecimal cubierto = BigDecimal.ZERO;
            for (DetalleLote linea : Lote.conUnidadesEnOrden(lotes, entrada.producto.getId(), salida.fecha())) {
                if (cubierto.compareTo(requerido) >= 0) {
                    break;
                }
                idsLote.add(Lote.dueno(lotes, linea).getId().getValue());
                cubierto = cubierto.add(linea.getCantidadRestante());
            }
        }
        salida.lotesBloqueados.addAll(loteRepository.obtenerPorIdsParaActualizar(
                idsLote.stream().map(LoteId::new).collect(Collectors.toList())));
    }

    // ------------------------------------------------------------------ productos por unidades

    /**
     * Descuenta unidades de las lineas de lote del producto (logica.md LOTE-17): primero la que
     * vence antes, solo de los lotes ya bloqueados, con un movimiento por linea.
     */
    private Optional<Faltante> descontarUnidades(Entrada entrada, Salida salida, boolean descontar) {
        Producto producto = entrada.producto;
        BigDecimal requerido = entrada.cantidad.setScale(ESCALA_UNIDADES, RoundingMode.HALF_UP);
        if (requerido.signum() <= 0) {
            return Optional.empty();
        }

        // Leidas con el bloqueo tomado: otra transaccion pudo descontar antes de bloquear.
        List<DetalleLote> lineas = Lote.conUnidadesEnOrden(salida.lotesBloqueados, producto.getId(), salida.fecha());
        BigDecimal disponible = sumarRestantes(lineas);
        if (disponible.compareTo(requerido) < 0) {
            return Optional.of(Faltante.sinApertura(producto, requerido, disponible));
        }
        if (!descontar) {
            return Optional.empty();
        }

        TipoMovimiento tipo = salida.tipo();
        BigDecimal pendiente = requerido;
        for (DetalleLote linea : lineas) {
            if (pendiente.signum() == 0) {
                break;
            }
            String idDetalle = linea.getId().getValue();
            Lote lote = Lote.dueno(salida.lotesBloqueados, linea);
            BigDecimal tomado = pendiente.min(linea.getCantidadRestante());
            lote.descontarUnidades(idDetalle, tomado);
            loteRepository.actualizar(lote.getId(), lote);
            registrar(Movimiento.registrarSobreLinea(
                    tomado, salida.request.getFecha(), salida.request.getMotivo(), linea.getId(),
                    tipo.getId(), salida.usuarioId, salida.produccionId, salida.ventaId), salida);
            pendiente = pendiente.subtract(tomado);
        }

        recalcularStockUseCase.execute(producto.getId().getValue());
        return Optional.empty();
    }

    // ------------------------------------------------------------------ productos base

    /** Consumo FEFO sobre las existencias abiertas de un producto base con presentaciones. */
    private Optional<Faltante> descontarExistencias(Entrada entrada, Salida salida, boolean descontar) {
        Producto producto = entrada.producto;
        BigDecimal requerido = entrada.cantidad.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
        if (requerido.signum() <= 0) {
            return Optional.empty();
        }

        // El insumo base y sus presentaciones: las existencias abiertas desde cualquiera de ellas
        // contienen al insumo base (PRODUCTO-03).
        List<ProductoId> fuentes = new ArrayList<>();
        fuentes.add(producto.getId());
        entrada.presentaciones.forEach(presentacion -> fuentes.add(presentacion.getId()));
        Set<String> idsFuente = fuentes.stream().map(ProductoId::getValue).collect(Collectors.toSet());

        // Sin cargar las lineas: otra entrada del plan puede bloquear despues la de una presentacion.
        List<Lote> lotes = loteRepository.obtenerPorProductosSinCargar(fuentes);
        List<DetalleLoteId> detallesDelProducto = lotes.stream()
                .flatMap(lote -> lote.getDetallesActivos().stream())
                .filter(detalle -> idsFuente.contains(detalle.getProductoId().getValue()))
                .map(DetalleLote::getId)
                .collect(Collectors.toList());

        // Las existencias se leen bloqueadas desde el principio (EXISTENCIA-14): la lectura con
        // bloqueo es su primera carga en la transaccion y trae su contenido vigente.
        String unidadDelProducto = producto.getUnidadActualId().getValue();
        LocalDate fecha = salida.fecha();
        List<Existencia> disponibles = existenciaRepository.obtenerDisponiblesParaConsumo(detallesDelProducto).stream()
                .filter(existencia -> existencia.getUnidadId().getValue().equals(unidadDelProducto))
                .filter(existencia -> !existencia.estaVencida(fecha))
                .collect(Collectors.toList());
        BigDecimal abierto = disponibles.stream()
                .map(Existencia::getContenidoActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (abierto.compareTo(requerido) < 0) {
            return Optional.of(faltanteDeBase(entrada, lotes, requerido, abierto, salida));
        }
        if (!descontar) {
            return Optional.empty();
        }

        TipoMovimiento tipo = salida.tipo();
        EstadoExistencia agotada = salida.agotada();
        BigDecimal pendiente = requerido;
        for (Existencia existencia : disponibles) {
            if (pendiente.signum() == 0) {
                break;
            }
            BigDecimal consumo = pendiente.min(existencia.getContenidoActual());
            existencia.consumir(consumo, salida.request.getFecha(), agotada.getId());
            existenciaRepository.actualizar(existencia.getId(), existencia);
            registrar(Movimiento.registrarSobreExistencia(
                    consumo, salida.request.getFecha(), salida.request.getMotivo(), existencia.getId(),
                    tipo.getId(), salida.usuarioId, salida.produccionId, salida.ventaId), salida);
            pendiente = pendiente.subtract(consumo);
        }

        recalcularStockUseCase.execute(producto.getId().getValue());
        return Optional.empty();
    }

    /**
     * Lo que falta de un producto base. Si su contenido abierto mas el de sus bolsas cerradas
     * vigentes alcanza, se resuelve abriendo: sugiere la bolsa que toca abrir segun FEFO entre
     * todas sus presentaciones (la primera fila de LOTE-14) y cuantas de esa presentacion hacen
     * falta. Las presentaciones sin conversion posible a la unidad del base no cuentan.
     */
    private Faltante faltanteDeBase(Entrada entrada, List<Lote> lotes, BigDecimal requerido, BigDecimal abierto, Salida salida) {
        Producto base = entrada.producto;
        BigDecimal cerrado = BigDecimal.ZERO;
        Bolsa sugerida = null;
        for (Producto presentacion : entrada.presentaciones) {
            List<DetalleLote> aperturables = Lote.aperturablesEnOrden(lotes, presentacion.getId(), salida.fecha());
            Optional<BigDecimal> contenido = Conversion.factorEntre(
                    presentacion.getUnidadActualId(), base.getUnidadActualId(), salida.conversiones());
            if (aperturables.isEmpty() || contenido.isEmpty() || contenido.get().signum() <= 0) {
                continue;
            }
            BigDecimal bolsasCerradas = aperturables.stream()
                    .map(detalle -> detalle.getCantidadRestante().setScale(0, RoundingMode.FLOOR))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            cerrado = cerrado.add(bolsasCerradas.multiply(contenido.get()));

            DetalleLote siguiente = aperturables.get(0);
            Bolsa candidata = new Bolsa(Lote.dueno(lotes, siguiente), siguiente, presentacion, contenido.get());
            if (sugerida == null || Bolsa.ORDEN_FEFO.compare(candidata, sugerida) < 0) {
                sugerida = candidata;
            }
        }

        if (sugerida != null && abierto.add(cerrado).compareTo(requerido) >= 0) {
            BigDecimal bolsas = requerido.subtract(abierto).divide(sugerida.contenido, 0, RoundingMode.CEILING);
            return Faltante.conApertura(base, requerido, abierto, sugerida, bolsas.intValueExact());
        }
        return Faltante.sinApertura(base, requerido, abierto.add(cerrado));
    }

    // ------------------------------------------------------------------ error estructurado

    /**
     * Un solo 409 con todos los faltantes, en orden de id de producto (logica.md VENTA-30,
     * arquitectura.md 4.16).
     */
    private DomainException error(List<Faltante> faltantes) {
        boolean apertura = faltantes.stream().allMatch(Faltante::seResuelveAbriendo);
        List<Map<String, Object>> detalle = faltantes.stream()
                .map(faltante -> faltante.aDetalle(apertura, nombreDeUnidad(faltante.producto)))
                .collect(Collectors.toList());
        String nombres = faltantes.stream()
                .map(faltante -> "'" + faltante.producto.getNombre() + "'")
                .collect(Collectors.joining(", "));

        String mensaje;
        if (apertura) {
            mensaje = "Stock abierto insuficiente de " + nombres + ": abre una presentacion para continuar";
        } else {
            mensaje = "Stock insuficiente de " + faltantes.stream()
                    .map(faltante -> "'" + faltante.producto.getNombre() + "' (se requieren " + texto(faltante.requerido)
                            + " y hay " + texto(faltante.disponible) + " vigentes)")
                    .collect(Collectors.joining(", "));
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("faltantes", detalle);
        return DomainException.throwException(409, apertura ? APERTURA_REQUERIDA : STOCK_INSUFICIENTE, mensaje, cuerpo);
    }

    private String nombreDeUnidad(Producto producto) {
        return unidadRepository.obtenerPorId(producto.getUnidadActualId()).map(Unidad::getUnidad).orElse(null);
    }

    private static String texto(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal sumarRestantes(List<DetalleLote> lineas) {
        return lineas.stream().map(DetalleLote::getCantidadRestante).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void registrar(Movimiento movimiento, Salida salida) {
        salida.registrados.add(MovimientoResponse.createFromAggregate(movimientoRepository.crear(movimiento), salida.tipo()));
    }

    // ------------------------------------------------------------------ estado de una ejecucion

    /** Una entrada del plan: un producto MTS y la suma de lo que se descuenta de el. */
    private static final class Entrada {
        private final Producto producto;
        private final List<Producto> presentaciones;
        private BigDecimal cantidad = BigDecimal.ZERO;

        private Entrada(Producto producto, List<Producto> presentaciones) {
            this.producto = producto;
            this.presentaciones = presentaciones;
        }

        private void sumar(BigDecimal otra) {
            this.cantidad = this.cantidad.add(otra);
        }

        /** Solo un producto base con presentaciones tiene existencias (PRODUCTO-14). */
        private boolean porExistencias() {
            return !presentaciones.isEmpty();
        }
    }

    /** La bolsa cerrada que conviene abrir de una presentacion, con el contenido que da al abrirla. */
    private static final class Bolsa {
        /** Mismo orden que la lista de aperturables (LOTE-09, LOTE-14). */
        private static final Comparator<Bolsa> ORDEN_FEFO = Comparator
                .comparing((Bolsa bolsa) -> bolsa.detalle.getFechaVencimiento(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(bolsa -> bolsa.lote.getFechaCompra(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(bolsa -> bolsa.detalle.getId().getValue());

        private final Lote lote;
        private final DetalleLote detalle;
        private final Producto presentacion;
        private final BigDecimal contenido;

        private Bolsa(Lote lote, DetalleLote detalle, Producto presentacion, BigDecimal contenido) {
            this.lote = lote;
            this.detalle = detalle;
            this.presentacion = presentacion;
            this.contenido = contenido;
        }
    }

    /** Lo que falta de una entrada del plan y, si se resuelve abriendo, la bolsa sugerida. */
    private static final class Faltante {
        private final Producto producto;
        private final BigDecimal requerido;
        /** Lo que hay: el contenido abierto si se resuelve abriendo; todo lo vigente si no. */
        private final BigDecimal disponible;
        private final Bolsa sugerida;
        private final int bolsasNecesarias;

        private Faltante(Producto producto, BigDecimal requerido, BigDecimal disponible, Bolsa sugerida, int bolsasNecesarias) {
            this.producto = producto;
            this.requerido = requerido;
            this.disponible = disponible;
            this.sugerida = sugerida;
            this.bolsasNecesarias = bolsasNecesarias;
        }

        private static Faltante sinApertura(Producto producto, BigDecimal requerido, BigDecimal disponible) {
            return new Faltante(producto, requerido, disponible, null, 0);
        }

        private static Faltante conApertura(Producto producto, BigDecimal requerido, BigDecimal abierto, Bolsa sugerida, int bolsas) {
            return new Faltante(producto, requerido, abierto, sugerida, bolsas);
        }

        private boolean seResuelveAbriendo() {
            return sugerida != null;
        }

        /** Una entrada de 'detalle.faltantes' con la forma de su codigo (arquitectura.md 4.16). */
        private Map<String, Object> aDetalle(boolean apertura, String unidad) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idProducto", producto.getId().getValue());
            fila.put("producto", producto.getNombre());
            fila.put("requerido", requerido.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP));
            fila.put(apertura ? "disponibleAbierto" : "disponible", disponible.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP));
            fila.put("unidad", unidad);
            if (apertura) {
                Map<String, Object> bolsa = new LinkedHashMap<>();
                bolsa.put("idDetalleLote", sugerida.detalle.getId().getValue());
                bolsa.put("idProductoPresentacion", sugerida.presentacion.getId().getValue());
                bolsa.put("presentacion", sugerida.presentacion.getNombre());
                bolsa.put("fechaVencimiento", sugerida.detalle.getFechaVencimiento());
                bolsa.put("contenido", sugerida.contenido.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP));
                fila.put("bolsasNecesarias", bolsasNecesarias);
                fila.put("bolsaSugerida", bolsa);
            }
            return fila;
        }
    }

    /**
     * Estado de una ejecucion: el plan (ordenado por id de producto, GENERAL-15), lo ya leido y lo
     * que se va registrando. Las filas de catalogo se resuelven solo si hacen falta.
     */
    private final class Salida {
        private final RegistrarSalidaDeInventarioRequest request;
        private final UsuarioId usuarioId;
        private final ProduccionId produccionId;
        private final VentaId ventaId;
        private final TreeMap<String, Entrada> plan = new TreeMap<>();
        private final Map<String, Producto> productos = new HashMap<>();
        private final Set<String> recetasEnCurso = new HashSet<>();
        private final List<MovimientoResponse> registrados = new ArrayList<>();
        private final List<Lote> lotesBloqueados = new ArrayList<>();
        private TipoMovimiento tipo;
        private EstadoExistencia agotada;
        private List<Conversion> conversiones;

        private Salida(RegistrarSalidaDeInventarioRequest request) {
            this.request = request;
            this.usuarioId = new UsuarioId(request.getIdUsuario());
            this.produccionId = request.getIdProduccion() != null ? new ProduccionId(request.getIdProduccion()) : null;
            this.ventaId = request.getIdVenta() != null ? new VentaId(request.getIdVenta()) : null;
        }

        /** Lo vencido se juzga a la fecha de la operacion (VENTA-28). */
        private LocalDate fecha() {
            return request.getFecha().toLocalDate();
        }

        private Producto producto(ProductoId productoId) {
            return productos.computeIfAbsent(productoId.getValue(), id -> productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + id + "' no existe")));
        }

        private TipoMovimiento tipo() {
            if (tipo == null) {
                String nombre = request.getTipoMovimiento();
                tipo = tipoMovimientoRepository.obtenerPorNombre(nombre)
                        .orElseThrow(() -> DomainException.throwException(404,
                                "El tipo de movimiento '" + nombre + "' no esta registrado"));
            }
            return tipo;
        }

        private EstadoExistencia agotada() {
            if (agotada == null) {
                agotada = estadoExistenciaRepository.obtenerPorNombre(ESTADO_AGOTADA)
                        .orElseThrow(() -> DomainException.throwException(404,
                                "El estado de existencia '" + ESTADO_AGOTADA + "' no esta registrado"));
            }
            return agotada;
        }

        private List<Conversion> conversiones() {
            if (conversiones == null) {
                conversiones = conversionRepository.obtenerVivas();
            }
            return conversiones;
        }
    }
}
