package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.ValueObjects.ConversionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;

public class Conversion {

    /** Saltos maximos al encadenar conversiones (inventario.md INV-09). */
    private static final int SALTOS_MAXIMOS = 4;
    /** Escala de calculo de los factores inversos y encadenados; se redondea al final. */
    private static final int ESCALA_CALCULO = 10;
    private ConversionId id;
    private BigDecimal factorConversion;
    private boolean esBorrable;
    private UnidadId unidadOrigenId;
    private UnidadId unidadDestinoId;
    private boolean borrado;

    private Conversion(
        ConversionId id,
        BigDecimal factorConversion,
        boolean esBorrable,
        UnidadId unidadOrigenId,
        UnidadId unidadDestinoId,
        boolean borrado
    ){
        this.id = id;
        this.factorConversion = factorConversion;
        this.esBorrable = esBorrable;
        this.unidadOrigenId = unidadOrigenId;
        this.unidadDestinoId = unidadDestinoId;
        this.borrado = borrado;
    }

    public static Conversion restaurar(
        String id,
        BigDecimal factorConversion,
        boolean esBorrable,
        UnidadId unidadOrigenId,
        UnidadId unidadDestinoId,
        boolean borrado
    ){
        return new Conversion(new ConversionId(id), factorConversion, esBorrable, unidadOrigenId, unidadDestinoId, borrado);
    }

    public static Conversion crear(
        BigDecimal factorConversion,
        boolean esBorrable,
        UnidadId unidadOrigenId,
        UnidadId unidadDestinoId
    ){
        return new Conversion(null, factorConversion, esBorrable, unidadOrigenId, unidadDestinoId, false);
    }

    public void actualizar(
        BigDecimal factorConversion,
        Boolean esBorrable,
        UnidadId unidadOrigenId,
        UnidadId unidadDestinoId
    ){
        this.factorConversion = factorConversion != null ? factorConversion : this.factorConversion;
        this.esBorrable = esBorrable != null ? esBorrable : this.esBorrable;
        this.unidadOrigenId = unidadOrigenId != null ? unidadOrigenId : this.unidadOrigenId;
        this.unidadDestinoId = unidadDestinoId != null ? unidadDestinoId : this.unidadDestinoId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    /**
     * Factor para expresar una cantidad de 'origen' en 'destino' (inventario.md INV-09):
     * misma unidad = 1; fila directa = su factor; fila inversa = 1 / factor; si no, el camino
     * mas corto de hasta cuatro saltos por el grafo de conversiones vivas, en ambos sentidos,
     * multiplicando los factores. Vacio si no hay camino.
     *
     * Es una funcion pura sobre la tabla completa de conversiones: la tabla es pequena y
     * resolverla en memoria evita una consulta por salto.
     */
    public static Optional<BigDecimal> factorEntre(UnidadId origen, UnidadId destino, List<Conversion> conversiones) {
        String desde = origen.getValue();
        String hasta = destino.getValue();
        if (desde.equals(hasta)) {
            return Optional.of(BigDecimal.ONE);
        }

        Map<String, List<Arista>> grafo = new HashMap<>();
        for (Conversion conversion : conversiones) {
            if (conversion.isBorrado() || conversion.getFactorConversion() == null || conversion.getFactorConversion().signum() <= 0) {
                continue;
            }
            String a = conversion.getUnidadOrigenId().getValue();
            String b = conversion.getUnidadDestinoId().getValue();
            BigDecimal factor = conversion.getFactorConversion();
            grafo.computeIfAbsent(a, clave -> new ArrayList<>()).add(new Arista(b, factor));
            grafo.computeIfAbsent(b, clave -> new ArrayList<>())
                    .add(new Arista(a, BigDecimal.ONE.divide(factor, ESCALA_CALCULO, RoundingMode.HALF_UP)));
        }

        // Busqueda en anchura: el primer camino que llega es el mas corto. La arista directa
        // se prefiere a la inversa porque se inserta antes en la lista del nodo origen.
        Map<String, BigDecimal> alcanzados = new HashMap<>();
        alcanzados.put(desde, BigDecimal.ONE);
        Deque<String> frontera = new ArrayDeque<>();
        frontera.add(desde);

        for (int salto = 0; salto < SALTOS_MAXIMOS && !frontera.isEmpty(); salto++) {
            Deque<String> siguiente = new ArrayDeque<>();
            for (String nodo : frontera) {
                for (Arista arista : grafo.getOrDefault(nodo, List.of())) {
                    if (alcanzados.containsKey(arista.destino)) {
                        continue;
                    }
                    BigDecimal acumulado = alcanzados.get(nodo).multiply(arista.factor);
                    if (arista.destino.equals(hasta)) {
                        return Optional.of(acumulado.setScale(ESCALA_CALCULO, RoundingMode.HALF_UP).stripTrailingZeros());
                    }
                    alcanzados.put(arista.destino, acumulado);
                    siguiente.add(arista.destino);
                }
            }
            frontera = siguiente;
        }
        return Optional.empty();
    }

    private static final class Arista {
        private final String destino;
        private final BigDecimal factor;

        private Arista(String destino, BigDecimal factor) {
            this.destino = destino;
            this.factor = factor;
        }
    }

    public ConversionId getId() {return id;}
    public BigDecimal getFactorConversion() {return factorConversion;}
    public boolean isEsBorrable() {return esBorrable;}
    public UnidadId getUnidadOrigenId() {return unidadOrigenId;}
    public UnidadId getUnidadDestinoId() {return unidadDestinoId;}
    public boolean isBorrado() {return borrado;}
}
