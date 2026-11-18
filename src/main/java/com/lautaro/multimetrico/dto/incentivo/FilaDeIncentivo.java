package com.lautaro.multimetrico.dto.incentivo;

import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import java.math.BigDecimal;

public record FilaDeIncentivo(AsesorResponse asesor, BigDecimal importe) {

    public boolean tieneImporte() {
        return importe != null;
    }
}
