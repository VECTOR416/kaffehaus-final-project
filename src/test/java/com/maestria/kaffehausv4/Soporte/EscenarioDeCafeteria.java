package com.maestria.kaffehausv4.Soporte;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.CrearLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.CrearLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.CrearProductoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.CrearProductoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.CrearProveedorRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases.CrearProveedorUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.AbrirSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.AbrirSesionCajaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.Shared.Application.Inicializacion.SembrarSistemaUseCase;

/**
 * Arma el escenario de las pruebas de persistencia con los mismos casos de uso que usa la API:
 * siembra la base y crea productos, proveedores, compras y la caja. Es un componente de Spring
 * solo en las pruebas; las unitarias no levantan contexto y no lo ven.
 */
@Component
public class EscenarioDeCafeteria {
    /** Contrasena del administrador que siembra el escenario (logica.md SIEMBRA-02). */
    public static final String CONTRASENA_ADMINISTRADOR = "admin-de-prueba";

    private final SembrarSistemaUseCase sembrarSistemaUseCase;
    private final CrearProductoUseCase crearProductoUseCase;
    private final CrearProveedorUseCase crearProveedorUseCase;
    private final CrearLoteUseCase crearLoteUseCase;
    private final AbrirSesionCajaUseCase abrirSesionCajaUseCase;
    private final IUsuarioRepository usuarioRepository;
    private final IUnidadRepository unidadRepository;
    private final ITipoRepository tipoRepository;
    private final ICategoriaRepository categoriaRepository;
    private final ITipoPagoRepository tipoPagoRepository;
    private final IProductoRepository productoRepository;

    private String idAdministrador;

    public EscenarioDeCafeteria(
            SembrarSistemaUseCase sembrarSistemaUseCase,
            CrearProductoUseCase crearProductoUseCase,
            CrearProveedorUseCase crearProveedorUseCase,
            CrearLoteUseCase crearLoteUseCase,
            AbrirSesionCajaUseCase abrirSesionCajaUseCase,
            IUsuarioRepository usuarioRepository,
            IUnidadRepository unidadRepository,
            ITipoRepository tipoRepository,
            ICategoriaRepository categoriaRepository,
            ITipoPagoRepository tipoPagoRepository,
            IProductoRepository productoRepository) {
        this.sembrarSistemaUseCase = sembrarSistemaUseCase;
        this.crearProductoUseCase = crearProductoUseCase;
        this.crearProveedorUseCase = crearProveedorUseCase;
        this.crearLoteUseCase = crearLoteUseCase;
        this.abrirSesionCajaUseCase = abrirSesionCajaUseCase;
        this.usuarioRepository = usuarioRepository;
        this.unidadRepository = unidadRepository;
        this.tipoRepository = tipoRepository;
        this.categoriaRepository = categoriaRepository;
        this.tipoPagoRepository = tipoPagoRepository;
        this.productoRepository = productoRepository;
    }

    /** Siembra la base y devuelve el id del administrador, que firma las operaciones. */
    public String sembrar() {
        sembrarSistemaUseCase.execute(CONTRASENA_ADMINISTRADOR);
        idAdministrador = usuarioRepository.obtenerPorCorreo(SembrarSistemaUseCase.CORREO_ADMINISTRADOR)
                .orElseThrow().getId().getValue();
        return idAdministrador;
    }

    public String usuario() {
        return idAdministrador;
    }

    public String idUnidad(String nombre) {
        return unidadRepository.obtenerPorNombre(nombre).orElseThrow().getId().getValue();
    }

    public String idTipoPago(String nombre) {
        return tipoPagoRepository.obtenerPorNombre(nombre).orElseThrow().getId().getValue();
    }

    /** Un producto de stock directo o bajo demanda; 'idPadre' lo vuelve presentacion. */
    public String producto(String nombre, String precio, boolean vendible, boolean esStockDirecto, String unidad, String idPadre) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("precio", precio);
        datos.put("stockMinimo", "0");
        datos.put("vendible", vendible);
        datos.put("esStockDirecto", esStockDirecto);
        datos.put("idUnidadActual", idUnidad(unidad));
        datos.put("idTipo", tipoRepository.obtenerPorNombre("Insumo").orElseThrow().getId().getValue());
        datos.put("idCategoria", categoriaRepository.obtenerPorNombre("Insumos").orElseThrow().getId().getValue());
        if (idPadre != null) {
            datos.put("idProductoPadre", idPadre);
        }
        return crearProductoUseCase.execute(CrearProductoRequest.createFromRequest(datos)).getId();
    }

    public String proveedor() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombres", "Molinos");
        datos.put("apellidos", "del Sur");
        datos.put("carnet", "PRV-1");
        return crearProveedorUseCase.execute(CrearProveedorRequest.createFromRequest(datos)).getId();
    }

    /** Una linea de compra: producto, cantidad, precio y vencimiento (puede ser nulo). */
    public static Map<String, Object> linea(String idProducto, String cantidad, String precio, String vencimiento) {
        Map<String, Object> linea = new HashMap<>();
        linea.put("idProducto", idProducto);
        linea.put("cantidadInicial", cantidad);
        linea.put("precioUnitario", precio);
        if (vencimiento != null) {
            linea.put("fechaVencimiento", vencimiento);
        }
        return linea;
    }

    @SafeVarargs
    public final LoteResponse comprar(String idProveedor, Map<String, Object>... lineas) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("idProveedor", idProveedor);
        datos.put("detalles", new ArrayList<>(List.of(lineas)));
        return crearLoteUseCase.execute(CrearLoteRequest.createFromRequest(datos, idAdministrador));
    }

    public String abrirCaja(String monto) {
        return abrirSesionCajaUseCase.execute(AbrirSesionCajaRequest.createFromRequest(Map.of("montoApertura", monto), idAdministrador)).getId();
    }

    public BigDecimal stock(String idProducto) {
        return productoRepository.obtenerPorId(new ProductoId(idProducto)).orElseThrow().getStock();
    }
}
