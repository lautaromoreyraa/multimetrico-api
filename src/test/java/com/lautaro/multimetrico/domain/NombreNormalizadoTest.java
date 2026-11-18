package com.lautaro.multimetrico.domain;

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

    @Test
    void elCorteAMitadDePalabraTambienEsPrefijo() {
        assertThat(NombreNormalizado.de("AGUIRRE FLAVIA Y")
                .esPrefijoDe(NombreNormalizado.de("AGUIRRE FLAVIA YAMILA")))
                .isTrue();
    }

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
