package com.lautaro.multimetrico.dto.padron;

import jakarta.validation.constraints.NotBlank;

/**
 * La corrección del nombre o el equipo de alguien.
 *
 * <p><b>Sin el legajo</b>, y no por olvido: el legajo va en la URL porque no se
 * puede cambiar. Es la identidad con la que se guardaron sus mediciones e
 * incentivos, y moverlo dejaría todos esos hechos apuntando a alguien que ya no
 * existe. Tenerlo también en el cuerpo invitaría a mandar uno distinto y a que
 * alguien tuviera que decidir cuál gana.
 */
public record CorreccionDeAsesorRequest(
        @NotBlank(message = "el nombre es obligatorio") String nombre,
        @NotBlank(message = "el líder es obligatorio") String lider) {
}
