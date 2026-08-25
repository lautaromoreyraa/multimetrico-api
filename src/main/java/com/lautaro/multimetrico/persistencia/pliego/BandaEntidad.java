package com.lautaro.multimetrico.persistencia.pliego;

import com.lautaro.multimetrico.persistencia.Decimales;
import com.lautaro.multimetrico.pliego.domain.Banda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "banda")
public class BandaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escala_id", nullable = false)
    private EscalaEntidad escala;

    /**
     * {@code null} es el extremo abierto, tal cual el dominio.
     *
     * <p>No se reemplaza por un centinela ni por el mínimo del tipo: la banda
     * {@code [-∞, 50)} existe justamente para que un valor absurdo caiga en algún
     * lado, y un {@code -99999} guardado convertiría "sin piso" en "piso muy
     * bajo", que no es lo mismo cuando alguien lee la tabla.
     */
    @Column(name = "desde", precision = 18, scale = 6)
    private BigDecimal desde;

    @Column(name = "hasta", precision = 18, scale = 6)
    private BigDecimal hasta;

    @Column(name = "resultado", nullable = false, precision = 18, scale = 6)
    private BigDecimal resultado;

    @Column(name = "orden", nullable = false)
    private int orden;

    protected BandaEntidad() {
    }

    static BandaEntidad de(EscalaEntidad escala, Banda banda, int orden) {
        BandaEntidad entidad = new BandaEntidad();
        entidad.escala = escala;
        entidad.desde = banda.desde();
        entidad.hasta = banda.hasta();
        entidad.resultado = banda.resultado();
        entidad.orden = orden;
        return entidad;
    }

    Banda aDominio() {
        return new Banda(
                Decimales.comoEntro(desde),
                Decimales.comoEntro(hasta),
                Decimales.comoEntro(resultado));
    }
}
