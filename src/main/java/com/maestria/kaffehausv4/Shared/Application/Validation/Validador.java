package com.maestria.kaffehausv4.Shared.Application.Validation;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

public final class Validador {

    private Validador() {}

    public static String textoRequerido(Object valor, String campo) {
        if (!(valor instanceof String) || ((String) valor).trim().isEmpty()) {
            throw DomainException.throwException(400, "El campo '" + campo + "' es obligatorio y debe ser un texto no vacio");
        }
        return ((String) valor).trim();
    }

    public static String textoOpcional(Object valor, String campo) {
        if (valor == null) return null;
        return textoRequerido(valor, campo);
    }

    public static Double numeroRequerido(Object valor, String campo) {
        if (valor == null) {
            throw DomainException.throwException(400, "El campo '" + campo + "' es obligatorio y debe ser un numero");
        }

        if (valor instanceof Number) {
            double num = ((Number) valor).doubleValue();
            if (Double.isInfinite(num) || Double.isNaN(num)) {
                throw DomainException.throwException(400, "El campo '" + campo + "' debe ser un numero valido");
            }
            return num;
        }

        if (valor instanceof String) {
            try {
                double num = Double.parseDouble(((String) valor).trim());
                if (Double.isInfinite(num) || Double.isNaN(num)) {
                    throw DomainException.throwException(400, "El campo '" + campo + "' debe ser un numero valido");
                }
                return num;
            } catch (NumberFormatException e) {
                throw DomainException.throwException(400, "El campo '" + campo + "' debe ser un numero valido");
            }
        }

        throw DomainException.throwException(400, "El campo '" + campo + "' es obligatorio y debe ser un numero");
    }

    public static Double numeroOpcional(Object valor, String campo) {
        if (valor == null || (valor instanceof String && ((String) valor).trim().isEmpty())) return null;
        return numeroRequerido(valor, campo);
    }

    public static Integer enteroRequerido(Object valor, String campo) {
        Double numero = numeroRequerido(valor, campo);
        if (numero % 1 != 0) {
            throw DomainException.throwException(400, "El campo '" + campo + "' debe ser un numero entero");
        }
        return numero.intValue();
    }

    public static Integer enteroOpcional(Object valor, String campo) {
        if (valor == null || (valor instanceof String && ((String) valor).trim().isEmpty())) return null;
        return enteroRequerido(valor, campo);
    }

    public static Boolean booleanoRequerido(Object valor, String campo) {
        if (valor instanceof Boolean) return (Boolean) valor;
        if (valor instanceof String) {
            String str = ((String) valor).trim();
            if ("true".equalsIgnoreCase(str)) return true;
            if ("false".equalsIgnoreCase(str)) return false;
        }
        throw DomainException.throwException(400, "El campo '" + campo + "' es obligatorio y debe ser un booleano");
    }

    public static Boolean booleanoOpcional(Object valor, String campo) {
        if (valor == null || (valor instanceof String && ((String) valor).trim().isEmpty())) return null;
        return booleanoRequerido(valor, campo);
    }

    public static LocalDateTime fechaRequerida(Object valor, String campo) {
        if (valor == null) {
            throw DomainException.throwException(400, "El campo '" + campo + "' es obligatorio y debe ser una fecha");
        }

        if (valor instanceof LocalDateTime) {
            return (LocalDateTime) valor;
        }

        if (valor instanceof LocalDate) {
            return ((LocalDate) valor).atStartOfDay();
        }

        if (valor instanceof Date) {
            return Instant.ofEpochMilli(((Date) valor).getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        if (valor instanceof Number) {
            return Instant.ofEpochMilli(((Number) valor).longValue())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        if (valor instanceof String) {
            String str = ((String) valor).trim();
            try {
                // Soporta ISO-8601 con zona (ej. 2026-08-22T21:14:22Z)
                return Instant.parse(str).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (DateTimeParseException ignored) {}

            try {
                // Soporta ISO local (ej. 2026-08-22T17:14:22)
                return LocalDateTime.parse(str);
            } catch (DateTimeParseException ignored) {}

            try {
                // Soporta solo fecha (ej. 2026-08-22)
                return LocalDate.parse(str).atStartOfDay();
            } catch (DateTimeParseException ignored) {}

            throw DomainException.throwException(400, "El campo '" + campo + "' no es una fecha valida");
        }

        throw DomainException.throwException(400, "El campo '" + campo + "' es obligatorio y debe ser una fecha");
    }

    public static LocalDateTime fechaOpcional(Object valor, String campo) {
        if (valor == null || (valor instanceof String && ((String) valor).trim().isEmpty())) return null;
        return fechaRequerida(valor, campo);
    }

    public static List<String> listaDeTextosRequerida(Object valor, String campo) {
        if (!(valor instanceof Collection<?>)) {
            throw DomainException.throwException(400, "El campo '" + campo + "' es obligatorio y debe ser una lista de textos");
        }

        List<?> lista = new ArrayList<>((Collection<?>) valor);
        List<String> resultado = new ArrayList<>();

        for (int i = 0; i < lista.size(); i++) {
            resultado.add(textoRequerido(lista.get(i), campo + "[" + i + "]"));
        }

        return resultado;
    }

    public static List<String> listaDeTextosOpcional(Object valor, String campo) {
        if (valor == null) return null;
        return listaDeTextosRequerida(valor, campo);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> objetoRequerido(Object valor, String campo) {
        if (!(valor instanceof Map)) {
            throw DomainException.throwException(400, "El campo '" + campo + "' es obligatorio y debe ser un objeto");
        }
        return (Map<String, Object>) valor;
    }

    public static Map<String, Object> objetoOpcional(Object valor, String campo) {
        if (valor == null) return null;
        return objetoRequerido(valor, campo);
    }

    public static <E extends Enum<E>> E valorDeEnumRequerido(Object valor, String campo, Class<E> enumClass) {
        String texto = textoRequerido(valor, campo);
        try {
            return Enum.valueOf(enumClass, texto);
        } catch (IllegalArgumentException e) {
            String permitidos = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw DomainException.throwException(400,
                    "El campo '" + campo + "' debe ser uno de: " + permitidos);
        }
    }

    public static <E extends Enum<E>> E valorDeEnumOpcional(Object valor, String campo, Class<E> enumClass) {
        if (valor == null || (valor instanceof String && ((String) valor).trim().isEmpty())) return null;
        return valorDeEnumRequerido(valor, campo, enumClass);
    }
}
