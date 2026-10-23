package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.InventarioProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Situacion de inventario y disponibilidad de un producto (inventario.md INV-30, INV-31,
 * INV-46). Es una lectura: no bloquea filas. La validacion definitiva de una venta sigue
 * siendo la del algoritmo de consumo, dentro de su transaccion (INV-54).
 *
 * - MTS con presentaciones: saldos del producto base y de sus presentaciones. 'disponible'
 *   excluye lo vencido.
 * - MTS por unidades: sus unidades disponibles en lotes (logica.md PRODUCTO-17).
 * - En un MTS, el stock fisico se separa en vigente y vencido a hoy (VENCIMIENTO-02).
 * - MTO: min(floor(saldo de cada insumo / lo que consume una unidad)), explotando
 *   recursivamente los insumos que son a su vez MTO. Sin receta viva, disponibilidad 0; un
 *   ciclo entre recetas es un error de datos (409).
 */
@Service
@Transactional(readOnly = true)
public class ObtenerInventarioProductoUseCase {
    private static final int ESCALA_CANTIDAD = 4;
    private static final int ESCALA_FACTOR = 10;

    private final IProductoRepository productoRepository;
    private final ILoteRepository loteRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IConversionRepository conversionRepository;
    private final IRecetaRepository recetaRepository;

    public ObtenerInventarioProductoUseCase(
            IProductoRepository productoRepository,
            ILoteRepository loteRepository,
            IExistenciaRepository existenciaRepository,
            IConversionRepository conversionRepository,
            IRecetaRepository recetaRepository) {
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
        this.conversionRepository = conversionRepository;
        this.recetaRepository = recetaRepository;
    }

    public InventarioProductoResponse execute(String idProducto) {
        Producto producto = productoRepository.obtenerPorId(new ProductoId(idProducto))
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + idProducto + "' no existe"));

        Consulta consulta = new Consulta(conversionRepository.obtenerVivas(), LocalDate.now());

        if (producto.isEsStockDirecto()) {
            Saldos saldos = saldosDe(producto, consulta);
            return InventarioProductoResponse.deStockDirecto(
                    producto,
                    saldos.abierto,
                    saldos.abiertoVigente,
                    saldos.cerrado,
                    saldos.valor.setScale(2, RoundingMode.HALF_UP),
                    saldos.abiertoVigente,
                    saldos.abiertoVigente.add(saldos.cerradoVigente));
        }

        return InventarioProductoResponse.deBajoDemanda(
                producto,
                disponibilidadBajoDemanda(producto, false, consulta, new HashSet<>()),
                disponibilidadBajoDemanda(producto, true, consulta, new HashSet<>()));
    }

    /** Disponibilidad de un MTO (INV-31): unidades enteras que alcanzan todos sus insumos. */
    private BigDecimal disponibilidadBajoDemanda(Producto producto, boolean incluirCerrado, Consulta consulta, Set<String> enCurso) {
        if (!enCurso.add(producto.getId().getValue())) {
            throw DomainException.throwException(409, "La receta del producto '" + producto.getNombre() + "' se contiene a si misma");
        }

        Optional<Receta> receta = recetaRepository.obtenerPorProductoObtenido(producto.getId());
        if (receta.isEmpty() || receta.get().getIngredientesActivos().isEmpty()) {
            enCurso.remove(producto.getId().getValue());
            return BigDecimal.ZERO;
        }

        BigDecimal minimo = null;
        for (IngredienteReceta ingrediente : receta.get().getIngredientesActivos()) {
            Producto insumo = productoRepository.obtenerPorId(ingrediente.getProductoId())
                    .orElseThrow(() -> DomainException.throwException(404, "Un ingrediente de la receta de '" + producto.getNombre() + "' no existe"));

            BigDecimal saldo;
            if (insumo.isEsStockDirecto()) {
                Saldos saldos = saldosDe(insumo, consulta);
                saldo = incluirCerrado ? saldos.abiertoVigente.add(saldos.cerradoVigente) : saldos.abiertoVigente;
            } else {
                saldo = disponibilidadBajoDemanda(insumo, incluirCerrado, consulta, enCurso);
            }

            BigDecimal porUnidad = ingrediente.getCantidad().divide(receta.get().getCantidad(), ESCALA_FACTOR, RoundingMode.HALF_UP);
            BigDecimal alcanza = saldo.divide(porUnidad, 0, RoundingMode.FLOOR);
            minimo = minimo == null ? alcanza : minimo.min(alcanza);
        }

        enCurso.remove(producto.getId().getValue());
        return minimo.max(BigDecimal.ZERO);
    }

    /**
     * Saldos de un producto base y de sus presentaciones, en su unidad (INV-08, INV-46). Las
     * unidades cerradas se expresan con el contenido que daria abrirlas (INV-09); las que no
     * tienen conversion posible no se cuentan.
     */
    private Saldos saldosDe(Producto base, Consulta consulta) {
        Saldos enCache = consulta.saldos.get(base.getId().getValue());
        if (enCache != null) {
            return enCache;
        }

        List<Producto> presentaciones = productoRepository.obtenerPresentaciones(base.getId());
        if (presentaciones.isEmpty()) {
            Saldos saldos = saldosDeUnidades(base, consulta);
            consulta.saldos.put(base.getId().getValue(), saldos);
            return saldos;
        }

        List<Producto> fuentes = new ArrayList<>();
        fuentes.add(base);
        fuentes.addAll(presentaciones);
        Map<String, Producto> fuentePorId = new HashMap<>();
        fuentes.forEach(fuente -> fuentePorId.put(fuente.getId().getValue(), fuente));

        Map<String, Lote> lotePorDetalle = new HashMap<>();
        List<DetalleLoteId> detalles = new ArrayList<>();
        Saldos saldos = new Saldos();
        for (Lote lote : loteRepository.obtenerPorProductos(fuentes.stream().map(Producto::getId).toList())) {
            for (DetalleLote detalle : lote.getDetallesActivos()) {
                Producto fuente = fuentePorId.get(detalle.getProductoId().getValue());
                if (fuente == null) {
                    continue;
                }
                lotePorDetalle.put(detalle.getId().getValue(), lote);
                detalles.add(detalle.getId());

                Optional<BigDecimal> contenido = Conversion.factorEntre(fuente.getUnidadActualId(), base.getUnidadActualId(), consulta.conversiones);
                if (contenido.isPresent() && detalle.getCantidadRestante().signum() > 0) {
                    BigDecimal cerrado = detalle.getCantidadRestante().multiply(contenido.get());
                    saldos.cerrado = saldos.cerrado.add(cerrado);
                    if (!detalle.estaVencido(consulta.hoy)) {
                        saldos.cerradoVigente = saldos.cerradoVigente.add(cerrado);
                    }
                }
            }
        }

        String unidad = base.getUnidadActualId().getValue();
        for (Existencia existencia : existenciaRepository.obtenerDisponiblesPorDetallesLote(detalles)) {
            if (!existencia.getUnidadId().getValue().equals(unidad)) {
                continue;
            }
            saldos.abierto = saldos.abierto.add(existencia.getContenidoActual());
            if (!existencia.estaVencida(consulta.hoy)) {
                saldos.abiertoVigente = saldos.abiertoVigente.add(existencia.getContenidoActual());
            }
            Lote lote = lotePorDetalle.get(existencia.getDetalleLoteId().getValue());
            saldos.valor = saldos.valor.add(existencia.getContenidoActual().multiply(
                    lote.costoPorUnidadDeContenido(existencia.getDetalleLoteId(), existencia.getContenidoInicial())));
        }

        saldos.aEscala();
        consulta.saldos.put(base.getId().getValue(), saldos);
        return saldos;
    }

    /**
     * Saldos de un producto por unidades (logica.md PRODUCTO-14, PRODUCTO-17): sus unidades
     * disponibles en lineas de lote vivas son su saldo abierto, sin nada cerrado que abrir.
     * 'valor' es lo que costaron esas unidades al comprarlas; lo producido no tiene costo.
     */
    private Saldos saldosDeUnidades(Producto producto, Consulta consulta) {
        String idProducto = producto.getId().getValue();
        Saldos saldos = new Saldos();
        for (Lote lote : loteRepository.obtenerPorProductos(List.of(producto.getId()))) {
            if (lote.isBorrado()) {
                continue;
            }
            for (DetalleLote detalle : lote.getDetallesActivos()) {
                if (!idProducto.equals(detalle.getProductoId().getValue())) {
                    continue;
                }
                saldos.abierto = saldos.abierto.add(detalle.getCantidadRestante());
                if (!detalle.estaVencido(consulta.hoy)) {
                    saldos.abiertoVigente = saldos.abiertoVigente.add(detalle.getCantidadRestante());
                }
                saldos.valor = saldos.valor.add(detalle.getCantidadRestante().multiply(detalle.getPrecioUnitario()));
            }
        }
        saldos.aEscala();
        return saldos;
    }

    private static final class Consulta {
        private final List<Conversion> conversiones;
        private final LocalDate hoy;
        private final Map<String, Saldos> saldos = new HashMap<>();

        private Consulta(List<Conversion> conversiones, LocalDate hoy) {
            this.conversiones = conversiones;
            this.hoy = hoy;
        }
    }

    private static final class Saldos {
        private BigDecimal abierto = BigDecimal.ZERO;
        private BigDecimal abiertoVigente = BigDecimal.ZERO;
        private BigDecimal cerrado = BigDecimal.ZERO;
        private BigDecimal cerradoVigente = BigDecimal.ZERO;
        private BigDecimal valor = BigDecimal.ZERO;

        private void aEscala() {
            abierto = abierto.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
            abiertoVigente = abiertoVigente.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
            cerrado = cerrado.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
            cerradoVigente = cerradoVigente.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
        }
    }
}
