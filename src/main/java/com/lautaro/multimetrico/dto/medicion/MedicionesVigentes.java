package com.lautaro.multimetrico.dto.medicion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public record MedicionesVigentes(Map<Metrica, MedicionVigente> porMetrica) {

    private static final MedicionesVigentes NINGUNA = new MedicionesVigentes(Collections.emptyMap());

    public MedicionesVigentes {
        porMetrica = porMetrica == null || porMetrica.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new EnumMap<>(porMetrica));
    }

    public static MedicionesVigentes ninguna() {
        return NINGUNA;
    }

    public Optional<MedicionVigente> de(Metrica metrica) {
        return Optional.ofNullable(porMetrica.get(metrica));
    }

    public Optional<LocalDate> corteDe(Metrica metrica) {
        return de(metrica).map(MedicionVigente::fechaDeCorte);
    }

    public Map<Metrica, BigDecimal> valores() {
        Map<Metrica, BigDecimal> valores = new EnumMap<>(Metrica.class);
        porMetrica.forEach((metrica, vigente) -> valores.put(metrica, vigente.valor()));
        return valores;
    }

    public boolean vacia() {
        return porMetrica.isEmpty();
    }
}
