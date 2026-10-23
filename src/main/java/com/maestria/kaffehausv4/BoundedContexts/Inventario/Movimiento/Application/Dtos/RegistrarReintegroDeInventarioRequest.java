package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entrada de RegistrarReintegroDeInventarioUseCase. No viaja por HTTP: la arma la anulacion de
 * una venta, por eso su factoria es crear(...).
 *
 * Lleva lo que desconto cada linea de la venta: el producto y su cantidad (logica.md VENTA-22).
 */
public class RegistrarReintegroDeInventarioRequest {
    private List<Linea> lineas;

    private RegistrarReintegroDeInventarioRequest(List<Linea> lineas) {
        this.lineas = lineas;
    }

    public static RegistrarReintegroDeInventarioRequest crear() {
        return new RegistrarReintegroDeInventarioRequest(new ArrayList<>());
    }

    /** Agrega la cantidad de un producto a devolver. */
    public RegistrarReintegroDeInventarioRequest con(String idProducto, BigDecimal cantidad) {
        this.lineas.add(new Linea(idProducto, cantidad));
        return this;
    }

    public List<Linea> getLineas() {return Collections.unmodifiableList(lineas);}

    /** Un producto y la cantidad que vuelve a sus lineas de lote. */
    public static final class Linea {
        private final String idProducto;
        private final BigDecimal cantidad;

        private Linea(String idProducto, BigDecimal cantidad) {
            this.idProducto = idProducto;
            this.cantidad = cantidad;
        }

        public String getIdProducto() {return idProducto;}
        public BigDecimal getCantidad() {return cantidad;}
    }
}
