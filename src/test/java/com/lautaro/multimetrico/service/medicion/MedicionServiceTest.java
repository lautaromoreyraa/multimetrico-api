package com.lautaro.multimetrico.service.medicion;

import static com.lautaro.multimetrico.fixture.MedicionesDeEjemplo.medicion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.MedicionVigente;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * El servicio de mediciones contra la base.
 *
 * <p>Antes corría contra un doble en memoria. Con el repositorio siendo una
 * interfaz de Spring Data eso ya no se puede escribir a mano, y tampoco
 * convendría: <b>el orden de carga es parte del contrato</b> —el desempate entre
 * dos mediciones del mismo corte lo gana la última— y ese orden lo produce el
 * {@code ORDER BY id} de la consulta. Un doble en memoria lo daba por sentado en
 * vez de verificarlo.
 *
 * <p>Cada test usa su propio período, como el resto de la suite: el contexto de
 * Spring y la base se comparten entre clases, y las mediciones se acumulan
 * porque nunca se pisan.
 */
@SpringBootTest
class MedicionServiceTest {

    @Autowired
    private MedicionService servicio;

    /**
     * El defecto que este módulo corrige: en la planilla la carga del lunes pisa
     * la del lunes anterior y el número viejo deja de existir.
     */
    @Test
    void cadaCargaSemanalAgregaEnVezDePisarLaAnterior() {
        YearMonth periodo = YearMonth.of(2040, 1);

        servicio.registrar(List.of(medicion(periodo, "s100", Metrica.FCR, "70.5", "2040-01-08")));
        servicio.registrar(List.of(medicion(periodo, "s100", Metrica.FCR, "74.1", "2040-01-15")));
        servicio.registrar(List.of(medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-01-22")));

        assertThat(servicio.historia(periodo, "s100", Metrica.FCR))
                .extracting(Medicion::valor)
                .containsExactly(new BigDecimal("70.5"), new BigDecimal("74.1"), new BigDecimal("78.2"));

        assertThat(servicio.vigentesDe(periodo, "s100").de(Metrica.FCR)).get()
                .extracting(MedicionVigente::valor)
                .isEqualTo(new BigDecimal("78.2"));
    }

    @Test
    void laHistoriaSaleEnOrdenDeCargaYNoOrdenadaPorCorte() {
        YearMonth periodo = YearMonth.of(2040, 2);

        servicio.registrar(List.of(
                medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-02-22"),
                medicion(periodo, "s100", Metrica.FCR, "74.1", "2040-02-15")));

        assertThat(servicio.historia(periodo, "s100", Metrica.FCR))
                .extracting(Medicion::valor)
                .containsExactly(new BigDecimal("78.2"), new BigDecimal("74.1"));
    }

    @Test
    void laHistoriaSeFiltraPorAsesorYPorMetrica() {
        YearMonth periodo = YearMonth.of(2040, 3);

        servicio.registrar(List.of(
                medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-03-22"),
                medicion(periodo, "s100", Metrica.TMO, "612", "2040-03-22"),
                medicion(periodo, "s200", Metrica.FCR, "61.0", "2040-03-22")));

        assertThat(servicio.historia(periodo, null, null)).hasSize(3);
        assertThat(servicio.historia(periodo, "s100", null)).hasSize(2);
        assertThat(servicio.historia(periodo, null, Metrica.FCR)).hasSize(2);
        assertThat(servicio.historia(periodo, "s100", Metrica.FCR)).hasSize(1);
    }

    /** Los archivos de origen escriben el mismo legajo de las dos formas. */
    @Test
    void elLegajoSeBuscaSinDistinguirMayusculas() {
        YearMonth periodo = YearMonth.of(2040, 4);

        servicio.registrar(List.of(medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-04-22")));

        assertThat(servicio.historia(periodo, "S100", null)).hasSize(1);
        assertThat(servicio.vigentesDe(periodo, "S100").vacia()).isFalse();
    }

    @Test
    void cadaPeriodoTieneSuPropiaHistoria() {
        YearMonth periodo = YearMonth.of(2040, 5);

        servicio.registrar(List.of(medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-05-22")));

        assertThat(servicio.historia(YearMonth.of(2040, 6), null, null)).isEmpty();
    }

    /**
     * Los valores se comparan con {@code compareTo} y no con {@code equals}.
     *
     * <p>No es un detalle de estilo del test: {@code BigDecimal.equals} mira la
     * escala, y un {@code 61.0} vuelve de la base como {@code 61} porque el
     * accesor le saca los ceros de más —ver {@code DecimalUtil}, que existe para
     * que los mensajes digan "sobran 12" y no "sobran 12.000000"—. Comparar por
     * igualdad acá era afirmar algo sobre la escala de la columna, que es
     * justamente lo que a nadie le importa.
     */
    @Test
    void agrupaLasVigentesDeTodaLaOperacionEnUnaSolaPasada() {
        YearMonth periodo = YearMonth.of(2040, 7);

        servicio.registrar(List.of(
                medicion(periodo, "s100", Metrica.FCR, "70.5", "2040-07-15"),
                medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-07-22"),
                medicion(periodo, "s200", Metrica.FCR, "61.0", "2040-07-15")));

        Map<String, MedicionesVigentes> vigentes = servicio.vigentesDelPeriodo(periodo);

        assertThat(vigentes).containsOnlyKeys("S100", "S200");
        assertThat(vigentes.get("S100").valores().get(Metrica.FCR)).isEqualByComparingTo("78.2");
        assertThat(vigentes.get("S200").valores().get(Metrica.FCR)).isEqualByComparingTo("61.0");
    }

    /**
     * Marcar la vigente por id y no por valor.
     *
     * <p>Dos cargas del mismo corte con el mismo número tienen todos los campos
     * iguales: comparándolas por valor, las dos quedarían marcadas como vigentes
     * y la pantalla mostraría dos filas subrayadas para una sola métrica.
     */
    @Test
    void marcaComoVigenteUnaSolaFilaPorAsesorYMetrica() {
        YearMonth periodo = YearMonth.of(2040, 8);

        servicio.registrar(List.of(
                medicion(periodo, "s100", Metrica.TMO, "612", "2040-08-22"),
                medicion(periodo, "s100", Metrica.TMO, "612", "2040-08-22")));

        List<Medicion> historia = servicio.historia(periodo, "s100", Metrica.TMO);

        assertThat(historia).hasSize(2);
        assertThat(servicio.vigentesEn(historia)).containsExactly(historia.getLast().id());
    }

    @Test
    void unLoteVacioNoEsUnAltaSilenciosa() {
        assertThatThrownBy(() -> servicio.registrar(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ninguna medición");
    }
}
