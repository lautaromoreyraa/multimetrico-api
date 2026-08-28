package com.lautaro.multimetrico.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NombreNormalizadoTest {

    @ParameterizedTest(name = "\"{0}\" queda como \"{1}\"")
    @CsvSource({
            "'aguirre flavia',      'AGUIRRE FLAVIA'",
            "'  GOMEZ   ANA  ',     'GOMEZ ANA'",
            "'MIÑO HECTOR',         'MINO HECTOR'",
            "'GARCÍA JOSÉ',         'GARCIA JOSE'",
            "'PEREZ, JUAN.',        'PEREZ JUAN'"
    })
    void llevaTodoALaMismaForma(String crudo, String esperado) {
        assertThat(NombreNormalizado.de(crudo).valor()).isEqualTo(esperado);
    }

    /**
     * Cuando una planilla se guarda con la codificación equivocada, la eñe queda
     * como el carácter de reemplazo. Se lo saca en vez de cambiarlo por un
     * espacio: partir el nombre en dos sería peor que acortarlo.
     */
    @Test
    void sacaElCaracterDeReemplazoSinPartirElNombre() {
        assertThat(NombreNormalizado.de("MI\uFFFDO HECTOR").valor()).isEqualTo("MIO HECTOR");
    }

    @Test
    void reconoceUnNombreCortadoComoPrefijo() {
        NombreNormalizado cortado = NombreNormalizado.de("AGUIRRE FLAVIA");
        NombreNormalizado completo = NombreNormalizado.de("AGUIRRE FLAVIA YAMILA");

        assertThat(cortado.esPrefijoDe(completo)).isTrue();
        assertThat(completo.esPrefijoDe(cortado)).isFalse();
    }

    /**
     * El truncado real corta a mitad de palabra: en los archivos el nombre llega
     * como "AGUIRRE FLAVIA Y", no "AGUIRRE FLAVIA". Exigir que el corte caiga en
     * un espacio dejaría afuera justo el caso que hay que resolver.
     */
    @Test
    void elCorteAMitadDePalabraTambienEsPrefijo() {
        assertThat(NombreNormalizado.de("AGUIRRE FLAVIA Y")
                .esPrefijoDe(NombreNormalizado.de("AGUIRRE FLAVIA YAMILA")))
                .isTrue();
    }

    /**
     * La contracara: un prefijo puede apuntar a dos personas. Acá eso es cierto y
     * está bien que lo sea — quien decide que no alcanza es la unicidad, que se
     * verifica en el resolutor, no esta comparación.
     */
    @Test
    void unPrefijoPuedeApuntarADosPersonas() {
        NombreNormalizado cortado = NombreNormalizado.de("SOSA ANA");

        assertThat(cortado.esPrefijoDe(NombreNormalizado.de("SOSA ANA MARIA"))).isTrue();
        assertThat(cortado.esPrefijoDe(NombreNormalizado.de("SOSA ANABELA ROSA"))).isTrue();
    }

    @Test
    void unTextoSinLetrasNoEsUnNombre() {
        assertThatThrownBy(() -> NombreNormalizado.de("--- ///"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
