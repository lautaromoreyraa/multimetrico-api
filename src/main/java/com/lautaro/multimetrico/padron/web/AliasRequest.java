package com.lautaro.multimetrico.padron.web;

import jakarta.validation.constraints.NotBlank;

public record AliasRequest(
        @NotBlank(message = "el texto es obligatorio") String texto,
        @NotBlank(message = "el legajo es obligatorio") String legajo) {
}
