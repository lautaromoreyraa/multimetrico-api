package com.lautaro.multimetrico.enumeration;

public enum Rol {

    ASESOR,

    SUPERVISOR,

    ADMIN;

    public String autoridad() {
        return "ROLE_" + name();
    }
}
