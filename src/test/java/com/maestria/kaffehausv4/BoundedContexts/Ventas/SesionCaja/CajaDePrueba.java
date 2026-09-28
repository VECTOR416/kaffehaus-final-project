package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.MovimientoCajaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.SesionCajaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.SesionCajaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Infrastructure.Persistence.Jpa.TipoPagoJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Inicializacion.SembrarSistemaUseCase;

/**
 * Apoyo de los tests de integracion que registran ventas: desde que la venta exige una caja
 * abierta y pagos que sumen el total (arquitectura.md 4.11, CAJ-01, CAJ-11), todos necesitan lo mismo.
 *
 * - prepararCaja siembra el sistema (idempotente, asi el test no depende de una base sembrada
 *   a mano) y garantiza una caja abierta. Si ya habia una la reutiliza y devuelve null: solo
 *   hay una caja abierta por local, y la de otro no es suya para borrarla.
 * - retirarCaja borra la sesion que abrio la prueba. Va despues de borrar las ventas de la
 *   prueba (venta.id_sesion_caja es RESTRICT) y antes de borrar al usuario titular.
 */
@Component
public class CajaDePrueba {

    private final SembrarSistemaUseCase sembrarSistemaUseCase;
    private final SesionCajaJpaRepository sesionCajaJpaRepository;
    private final MovimientoCajaJpaRepository movimientoCajaJpaRepository;
    private final TipoPagoJpaRepository tipoPagoJpaRepository;

    public CajaDePrueba(
            SembrarSistemaUseCase sembrarSistemaUseCase,
            SesionCajaJpaRepository sesionCajaJpaRepository,
            MovimientoCajaJpaRepository movimientoCajaJpaRepository,
            TipoPagoJpaRepository tipoPagoJpaRepository) {
        this.sembrarSistemaUseCase = sembrarSistemaUseCase;
        this.sesionCajaJpaRepository = sesionCajaJpaRepository;
        this.movimientoCajaJpaRepository = movimientoCajaJpaRepository;
        this.tipoPagoJpaRepository = tipoPagoJpaRepository;
    }

    /** La sesion que abrio esta llamada, o null si reutilizo una que ya estaba abierta. */
    public UUID prepararCaja(UUID idUsuario) {
        sembrarSistemaUseCase.execute();
        if (sesionCajaJpaRepository.findFirstByEstadoAndBorradoFalse(SesionCaja.ABIERTA).isPresent()) {
            return null;
        }
        UUID id = UUID.randomUUID();
        SesionCajaJpaModel sesion = new SesionCajaJpaModel();
        sesion.setId(id);
        sesion.setIdUsuario(idUsuario);
        sesion.setFechaApertura(LocalDateTime.now());
        sesion.setMontoApertura(new BigDecimal("100.00"));
        sesion.setEstado(SesionCaja.ABIERTA);
        sesion.setFecC(OffsetDateTime.now());
        sesion.setBorrado(false);
        sesionCajaJpaRepository.save(sesion);
        return id;
    }

    public void retirarCaja(UUID idSesionCaja) {
        if (idSesionCaja == null) {
            return;
        }
        movimientoCajaJpaRepository.deleteAll(movimientoCajaJpaRepository.findByIdSesionCajaAndBorradoFalseOrderByFecCAsc(idSesionCaja));
        sesionCajaJpaRepository.deleteById(idSesionCaja);
    }

    public UUID idTipoPago(String nombre) {
        return tipoPagoJpaRepository.findFirstByNombreIgnoreCaseAndBorradoFalse(nombre).orElseThrow().getId();
    }

    /** Un solo pago exacto en efectivo por 'monto'. */
    public List<Object> enEfectivo(String monto) {
        return List.of(Map.of("idTipoPago", idTipoPago("Efectivo").toString(), "monto", monto));
    }
}
