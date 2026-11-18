package com.lautaro.multimetrico.service.multimetrico;

import com.lautaro.multimetrico.dto.multimetrico.FilaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.FilaResponse;
import com.lautaro.multimetrico.dto.multimetrico.Multimetrico;
import com.lautaro.multimetrico.dto.multimetrico.MultimetricoResponse;
import java.time.YearMonth;

public interface MultimetricoService {

    Multimetrico delEquipo(YearMonth periodo, String lider, String campania);

    FilaDeMultimetrico deUnAsesor(YearMonth periodo, String legajo, String campania);

    MultimetricoResponse tablaDelEquipo(YearMonth periodo, String lider, String campania);

    FilaResponse filaDeUnAsesor(YearMonth periodo, String legajo, String campania);
}
