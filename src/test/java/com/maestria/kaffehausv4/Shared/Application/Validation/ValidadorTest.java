package com.maestria.kaffehausv4.Shared.Application.Validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas unitarias del validador de payloads: es la primera barrera de toda peticion. */
class ValidadorTest {

    private enum Color { ROJO, VERDE }

    private static DomainException errorDe(Runnable accion) {
        return assertThrows(DomainException.class, accion::run);
    }

    @Nested
    class Textos {

        @Test
        void textoRequeridoDevuelveElTextoSinEspaciosSobrantes() {
            // Arrange
            Object valor = "  cafe  ";

            // Act
            String resultado = Validador.textoRequerido(valor, "nombre");

            // Assert
            assertEquals("cafe", resultado);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t"})
        void textoRequeridoRechazaUnTextoVacio(String valor) {
            // Act
            DomainException error = errorDe(() -> Validador.textoRequerido(valor, "nombre"));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("'nombre'"));
        }

        @Test
        void textoRequeridoRechazaNuloYOtrosTipos() {
            // Act
            DomainException porNulo = errorDe(() -> Validador.textoRequerido(null, "nombre"));
            DomainException porNumero = errorDe(() -> Validador.textoRequerido(15, "nombre"));

            // Assert
            assertEquals(400, porNulo.getCodigo());
            assertEquals(400, porNumero.getCodigo());
        }

        @Test
        void textoOpcionalDevuelveNuloSiNoLlega() {
            // Act
            String resultado = Validador.textoOpcional(null, "motivo");

            // Assert
            assertNull(resultado);
        }

        @Test
        void textoOpcionalValidaElValorCuandoLlega() {
            // Act
            String valido = Validador.textoOpcional(" algo ", "motivo");
            DomainException error = errorDe(() -> Validador.textoOpcional("  ", "motivo"));

            // Assert
            assertEquals("algo", valido);
            assertEquals(400, error.getCodigo());
        }
    }

    @Nested
    class Numeros {

        @Test
        void numeroRequeridoAceptaNumerosYTextosNumericos() {
            // Act
            Double desdeEntero = Validador.numeroRequerido(7, "cantidad");
            Double desdeDecimal = Validador.numeroRequerido(2.5, "cantidad");
            Double desdeTexto = Validador.numeroRequerido(" 12.75 ", "cantidad");

            // Assert
            assertEquals(7.0, desdeEntero);
            assertEquals(2.5, desdeDecimal);
            assertEquals(12.75, desdeTexto);
        }

        @Test
        void numeroRequeridoRechazaNulo() {
            // Act
            DomainException error = errorDe(() -> Validador.numeroRequerido(null, "cantidad"));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("'cantidad'"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "", "12,5", "NaN", "Infinity"})
        void numeroRequeridoRechazaTextosQueNoSonNumerosFinitos(String valor) {
            // Act
            DomainException error = errorDe(() -> Validador.numeroRequerido(valor, "cantidad"));

            // Assert
            assertEquals(400, error.getCodigo());
        }

        @Test
        void numeroRequeridoRechazaNumerosNoFinitosYOtrosTipos() {
            // Act
            DomainException porNaN = errorDe(() -> Validador.numeroRequerido(Double.NaN, "cantidad"));
            DomainException porInfinito = errorDe(() -> Validador.numeroRequerido(Double.POSITIVE_INFINITY, "cantidad"));
            DomainException porBooleano = errorDe(() -> Validador.numeroRequerido(true, "cantidad"));

            // Assert
            assertEquals(400, porNaN.getCodigo());
            assertEquals(400, porInfinito.getCodigo());
            assertEquals(400, porBooleano.getCodigo());
        }

        @Test
        void numeroOpcionalTrataNuloYTextoVacioComoAusente() {
            // Act
            Double porNulo = Validador.numeroOpcional(null, "precio");
            Double porVacio = Validador.numeroOpcional("  ", "precio");
            Double conValor = Validador.numeroOpcional("3", "precio");

            // Assert
            assertNull(porNulo);
            assertNull(porVacio);
            assertEquals(3.0, conValor);
        }

        @Test
        void enteroRequeridoAceptaEnterosAunqueLleguenComoDecimalSinFraccion() {
            // Act
            Integer desdeEntero = Validador.enteroRequerido(4, "skip");
            Integer desdeDecimal = Validador.enteroRequerido(4.0, "skip");
            Integer desdeTexto = Validador.enteroRequerido("10", "skip");

            // Assert
            assertEquals(4, desdeEntero);
            assertEquals(4, desdeDecimal);
            assertEquals(10, desdeTexto);
        }

        @Test
        void enteroRequeridoRechazaNumerosConFraccion() {
            // Act
            DomainException error = errorDe(() -> Validador.enteroRequerido(4.5, "skip"));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("entero"));
        }

        @Test
        void enteroOpcionalTrataNuloYTextoVacioComoAusente() {
            // Act
            Integer porNulo = Validador.enteroOpcional(null, "limit");
            Integer porVacio = Validador.enteroOpcional("", "limit");
            Integer conValor = Validador.enteroOpcional(20, "limit");

            // Assert
            assertNull(porNulo);
            assertNull(porVacio);
            assertEquals(20, conValor);
        }
    }

    @Nested
    class Booleanos {

        @Test
        void booleanoRequeridoAceptaBooleanosYSuTextoSinImportarMayusculas() {
            // Act
            Boolean nativo = Validador.booleanoRequerido(Boolean.TRUE, "activo");
            Boolean textoVerdadero = Validador.booleanoRequerido(" TRUE ", "activo");
            Boolean textoFalso = Validador.booleanoRequerido("false", "activo");

            // Assert
            assertTrue(nativo);
            assertTrue(textoVerdadero);
            assertFalse(textoFalso);
        }

        @Test
        void booleanoRequeridoRechazaLoQueNoEsBooleano() {
            // Act
            DomainException porNulo = errorDe(() -> Validador.booleanoRequerido(null, "activo"));
            DomainException porTexto = errorDe(() -> Validador.booleanoRequerido("si", "activo"));
            DomainException porNumero = errorDe(() -> Validador.booleanoRequerido(1, "activo"));

            // Assert
            assertEquals(400, porNulo.getCodigo());
            assertEquals(400, porTexto.getCodigo());
            assertEquals(400, porNumero.getCodigo());
        }

        @Test
        void booleanoOpcionalTrataNuloYTextoVacioComoAusente() {
            // Act
            Boolean porNulo = Validador.booleanoOpcional(null, "activo");
            Boolean porVacio = Validador.booleanoOpcional(" ", "activo");
            Boolean conValor = Validador.booleanoOpcional("true", "activo");

            // Assert
            assertNull(porNulo);
            assertNull(porVacio);
            assertTrue(conValor);
        }
    }

    @Nested
    class Fechas {

        @Test
        void fechaRequeridaDevuelveTalCualUnLocalDateTime() {
            // Arrange
            LocalDateTime fecha = LocalDateTime.of(2026, 8, 22, 17, 14, 22);

            // Act
            LocalDateTime resultado = Validador.fechaRequerida(fecha, "fecha");

            // Assert
            assertSame(fecha, resultado);
        }

        @Test
        void fechaRequeridaLlevaUnLocalDateAlInicioDelDia() {
            // Act
            LocalDateTime resultado = Validador.fechaRequerida(LocalDate.of(2026, 8, 22), "fecha");

            // Assert
            assertEquals(LocalDateTime.of(2026, 8, 22, 0, 0), resultado);
        }

        @Test
        void fechaRequeridaConvierteSqlDateYMilisegundosALaZonaDelSistema() {
            // Arrange
            long milisegundos = 1_787_433_262_000L;
            LocalDateTime esperado = Instant.ofEpochMilli(milisegundos).atZone(ZoneId.systemDefault()).toLocalDateTime();

            // Act
            LocalDateTime desdeDate = Validador.fechaRequerida(new java.sql.Date(milisegundos), "fecha");
            LocalDateTime desdeNumero = Validador.fechaRequerida(milisegundos, "fecha");

            // Assert
            assertEquals(esperado, desdeDate);
            assertEquals(esperado, desdeNumero);
        }

        @Test
        void fechaRequeridaAceptaLosTresFormatosDeTexto() {
            // Arrange
            LocalDateTime esperadoConZona = Instant.parse("2026-08-22T21:14:22Z").atZone(ZoneId.systemDefault()).toLocalDateTime();

            // Act
            LocalDateTime conZona = Validador.fechaRequerida("2026-08-22T21:14:22Z", "fecha");
            LocalDateTime local = Validador.fechaRequerida(" 2026-08-22T17:14:22 ", "fecha");
            LocalDateTime soloFecha = Validador.fechaRequerida("2026-08-22", "fecha");

            // Assert
            assertEquals(esperadoConZona, conZona);
            assertEquals(LocalDateTime.of(2026, 8, 22, 17, 14, 22), local);
            assertEquals(LocalDateTime.of(2026, 8, 22, 0, 0), soloFecha);
        }

        @ParameterizedTest
        @ValueSource(strings = {"22/08/2026", "ayer", "", "2026-13-40"})
        void fechaRequeridaRechazaTextosQueNoSonFecha(String valor) {
            // Act
            DomainException error = errorDe(() -> Validador.fechaRequerida(valor, "fecha"));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("no es una fecha valida"));
        }

        @Test
        void fechaRequeridaRechazaNuloYTiposSinConversion() {
            // Act
            DomainException porNulo = errorDe(() -> Validador.fechaRequerida(null, "fecha"));
            DomainException porBooleano = errorDe(() -> Validador.fechaRequerida(true, "fecha"));

            // Assert
            assertEquals(400, porNulo.getCodigo());
            assertEquals(400, porBooleano.getCodigo());
        }

        @Test
        void fechaOpcionalTrataNuloYTextoVacioComoAusente() {
            // Act
            LocalDateTime porNulo = Validador.fechaOpcional(null, "fecha");
            LocalDateTime porVacio = Validador.fechaOpcional("  ", "fecha");
            LocalDateTime conValor = Validador.fechaOpcional("2026-08-22", "fecha");

            // Assert
            assertNull(porNulo);
            assertNull(porVacio);
            assertEquals(LocalDateTime.of(2026, 8, 22, 0, 0), conValor);
        }
    }

    @Nested
    class Colecciones {

        @Test
        void listaDeTextosRequeridaLimpiaCadaElemento() {
            // Act
            List<String> resultado = Validador.listaDeTextosRequerida(List.of(" a ", "b"), "ids");

            // Assert
            assertEquals(List.of("a", "b"), resultado);
        }

        @Test
        void listaDeTextosRequeridaAceptaUnaListaVacia() {
            // Act
            List<String> resultado = Validador.listaDeTextosRequerida(List.of(), "ids");

            // Assert
            assertTrue(resultado.isEmpty());
        }

        @Test
        void listaDeTextosRequeridaSenalaElElementoInvalidoPorSuIndice() {
            // Arrange
            List<Object> conUnInvalido = Arrays.asList("a", 5);

            // Act
            DomainException error = errorDe(() -> Validador.listaDeTextosRequerida(conUnInvalido, "ids"));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("ids[1]"));
        }

        @Test
        void listaDeTextosRequeridaRechazaLoQueNoEsUnaColeccion() {
            // Act
            DomainException porNulo = errorDe(() -> Validador.listaDeTextosRequerida(null, "ids"));
            DomainException porTexto = errorDe(() -> Validador.listaDeTextosRequerida("a,b", "ids"));

            // Assert
            assertEquals(400, porNulo.getCodigo());
            assertEquals(400, porTexto.getCodigo());
        }

        @Test
        void listaDeTextosOpcionalDevuelveNuloSiNoLlega() {
            // Act
            List<String> porNulo = Validador.listaDeTextosOpcional(null, "ids");
            List<String> conValor = Validador.listaDeTextosOpcional(List.of("x"), "ids");

            // Assert
            assertNull(porNulo);
            assertEquals(List.of("x"), conValor);
        }

        @Test
        void listaDeObjetosRequeridaDevuelveLosObjetosEnOrden() {
            // Arrange
            Map<String, Object> primero = Map.of("id", "1");
            Map<String, Object> segundo = Map.of("id", "2");

            // Act
            List<Map<String, Object>> resultado = Validador.listaDeObjetosRequerida(List.of(primero, segundo), "detalles");

            // Assert
            assertEquals(List.of(primero, segundo), resultado);
        }

        @Test
        void listaDeObjetosRequeridaExigeAlMenosUnElemento() {
            // Act
            DomainException error = errorDe(() -> Validador.listaDeObjetosRequerida(List.of(), "detalles"));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("al menos un elemento"));
        }

        @Test
        void listaDeObjetosRequeridaSenalaElElementoQueNoEsObjeto() {
            // Arrange
            List<Object> conUnInvalido = Arrays.asList(Map.of("id", "1"), "no-objeto");

            // Act
            DomainException error = errorDe(() -> Validador.listaDeObjetosRequerida(conUnInvalido, "detalles"));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("detalles[1]"));
        }

        @Test
        void listaDeObjetosRequeridaRechazaLoQueNoEsUnaColeccion() {
            // Act
            DomainException error = errorDe(() -> Validador.listaDeObjetosRequerida(Map.of(), "detalles"));

            // Assert
            assertEquals(400, error.getCodigo());
        }

        @Test
        void listaDeObjetosOpcionalDevuelveNuloSiNoLlegaPeroNoAceptaVacia() {
            // Act
            List<Map<String, Object>> porNulo = Validador.listaDeObjetosOpcional(null, "detalles");
            DomainException porVacia = errorDe(() -> Validador.listaDeObjetosOpcional(List.of(), "detalles"));

            // Assert
            assertNull(porNulo);
            assertEquals(400, porVacia.getCodigo());
        }

        @Test
        void objetoRequeridoDevuelveElMismoMapa() {
            // Arrange
            Map<String, Object> objeto = Map.of("clave", "valor");

            // Act
            Map<String, Object> resultado = Validador.objetoRequerido(objeto, "cliente");

            // Assert
            assertSame(objeto, resultado);
        }

        @Test
        void objetoRequeridoRechazaLoQueNoEsUnObjeto() {
            // Act
            DomainException porNulo = errorDe(() -> Validador.objetoRequerido(null, "cliente"));
            DomainException porLista = errorDe(() -> Validador.objetoRequerido(List.of(), "cliente"));

            // Assert
            assertEquals(400, porNulo.getCodigo());
            assertEquals(400, porLista.getCodigo());
        }

        @Test
        void objetoOpcionalDevuelveNuloSiNoLlega() {
            // Act
            Map<String, Object> resultado = Validador.objetoOpcional(null, "cliente");

            // Assert
            assertNull(resultado);
        }
    }

    @Nested
    class Enumerados {

        @Test
        void valorDeEnumRequeridoResuelveLaConstantePorNombre() {
            // Act
            Color resultado = Validador.valorDeEnumRequerido(" VERDE ", "color", Color.class);

            // Assert
            assertEquals(Color.VERDE, resultado);
        }

        @Test
        void valorDeEnumRequeridoListaLosValoresPermitidosAlFallar() {
            // Act
            DomainException error = errorDe(() -> Validador.valorDeEnumRequerido("AZUL", "color", Color.class));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("ROJO, VERDE"));
        }

        @Test
        void valorDeEnumRequeridoDistingueMayusculas() {
            // Act
            DomainException error = errorDe(() -> Validador.valorDeEnumRequerido("rojo", "color", Color.class));

            // Assert
            assertEquals(400, error.getCodigo());
        }

        @Test
        void valorDeEnumOpcionalTrataNuloYTextoVacioComoAusente() {
            // Act
            Color porNulo = Validador.valorDeEnumOpcional(null, "color", Color.class);
            Color porVacio = Validador.valorDeEnumOpcional(" ", "color", Color.class);
            Color conValor = Validador.valorDeEnumOpcional("ROJO", "color", Color.class);

            // Assert
            assertNull(porNulo);
            assertNull(porVacio);
            assertEquals(Color.ROJO, conValor);
        }
    }
}
