package com.tallermecanico.api.common;

import java.util.Locale;

/**
 * Normaliza los términos ingresados por el usuario antes de compararlos en las búsquedas.
 * La placa se trata aparte porque los registros históricos pueden conservar guiones.
 */
public final class SearchNormalizer {
    private SearchNormalizer() {
    }

    public static String text(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String plate(String value) {
        return text(value).replace("-", "").replace(" ", "");
    }
}
