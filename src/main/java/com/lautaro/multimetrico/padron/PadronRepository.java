package com.lautaro.multimetrico.padron;

import com.lautaro.multimetrico.padron.domain.Padron;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface PadronRepository {

    Padron guardar(Padron padron);

    Optional<Padron> buscar(YearMonth periodo);

    List<YearMonth> periodos();
}
