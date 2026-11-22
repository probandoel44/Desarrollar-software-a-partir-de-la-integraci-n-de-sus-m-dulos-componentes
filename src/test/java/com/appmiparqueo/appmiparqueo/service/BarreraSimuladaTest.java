package com.appmiparqueo.appmiparqueo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BarreraSimuladaTest {
    @Test
    void validAccessCompletesOneOpenCloseCycle() {
        BarreraSimulada barrera = new BarreraSimulada();

        barrera.abrirYCerrar();

        assertEquals(BarreraSimulada.Estado.CERRADA, barrera.getEstado());
        assertEquals(1, barrera.getCiclosCompletados());
    }
}