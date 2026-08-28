package com.lautaro.multimetrico.config;

import com.lautaro.multimetrico.util.PeriodoUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;

/**
 * {@link YearMonth} en la base, como el texto {@code aaaa-mm}.
 *
 * <p>SQL no tiene un tipo "mes". Las tres salidas posibles eran un entero
 * {@code 202608}, un {@code DATE} en el día 1 del mes, o el texto que ya usa la
 * API. Se eligió el texto por tres motivos, en este orden:
 *
 * <ul>
 *   <li><b>Ordena bien.</b> {@code '2026-08' < '2026-09' < '2027-01'}
 *       alfabéticamente, así que un {@code ORDER BY periodo} sale cronológico
 *       sin conversiones ni funciones sobre la columna que maten el índice.</li>
 *   <li><b>Se lee sin traducir.</b> Un {@code SELECT} de urgencia en producción
 *       muestra {@code 2026-08}, no {@code 202608} ni {@code 2026-08-01}.</li>
 *   <li><b>No inventa un día.</b> Un {@code DATE} obliga a elegir uno, y ese día
 *       falso después se cuela en comparaciones que no lo esperaban.</li>
 * </ul>
 *
 * <p>La regla del proyecto queda: lo que la base entiende va nativo
 * ({@code LocalDate} en {@code DATE}, {@code Instant} en {@code DATETIME(6)});
 * lo que no entiende va como el mismo texto que expone la API. Que sea
 * <em>el mismo</em> texto lo garantiza {@link PeriodoUtil}, que es el único lugar
 * donde está escrito el formato.
 *
 * <p>{@code autoApply = true}: se aplica a todo {@link YearMonth} de cualquier
 * entidad sin que haya que acordarse de anotarlo. Son cinco tablas y olvidarse en
 * una sola daría una columna con otra forma.
 */
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
