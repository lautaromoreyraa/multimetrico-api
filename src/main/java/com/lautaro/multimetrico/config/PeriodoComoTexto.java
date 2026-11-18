package com.lautaro.multimetrico.config;

import com.lautaro.multimetrico.util.PeriodoUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;

@Converter(autoApply = true)
public class PeriodoComoTexto implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth periodo) {
        return PeriodoUtil.aTexto(periodo);
    }

    @Override
    public YearMonth convertToEntityAttribute(String texto) {
        return PeriodoUtil.deTexto(texto);
    }
}
