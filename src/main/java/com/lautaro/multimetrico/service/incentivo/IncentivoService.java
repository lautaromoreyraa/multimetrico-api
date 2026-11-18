package com.lautaro.multimetrico.service.incentivo;

import com.lautaro.multimetrico.dto.incentivo.CargaDeIncentivos;
import com.lautaro.multimetrico.dto.incentivo.IncentivosResponse;
import com.lautaro.multimetrico.dto.perfil.PerfilDeIncentivos;
import java.time.YearMonth;

public interface IncentivoService {

    CargaDeIncentivos cargar(YearMonth periodo, PerfilDeIncentivos perfil, byte[] archivo, String origen);

    IncentivosResponse delEquipo(YearMonth periodo, String lider);
}
