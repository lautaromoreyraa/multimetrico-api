package com.lautaro.multimetrico.padron;

import com.lautaro.multimetrico.padron.domain.Padron;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class PadronEnMemoria implements PadronRepository {

    private final Map<YearMonth, Padron> padrones = new ConcurrentHashMap<>();

    @Override
    public Padron guardar(Padron padron) {
        padrones.put(padron.periodo(), padron);
        return padron;
    }

    @Override
    public Optional<Padron> buscar(YearMonth periodo) {
        return Optional.ofNullable(padrones.get(periodo));
    }

    @Override
    public List<YearMonth> periodos() {
        return padrones.keySet().stream().sorted(Comparator.reverseOrder()).toList();
    }
}
