package com.lautaro.multimetrico.service.multimetrico;

import com.lautaro.multimetrico.dto.multimetrico.FilaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.Multimetrico;
import java.time.YearMonth;

public interface MultimetricoService {

    Multimetrico delEquipo(YearMonth periodo, String lider, String campania);

    FilaDeMultimetrico deUnAsesor(YearMonth periodo, String legajo, String campania);
}
