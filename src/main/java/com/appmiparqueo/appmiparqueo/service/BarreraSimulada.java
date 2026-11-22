package com.appmiparqueo.appmiparqueo.service;

import org.springframework.stereotype.Component;

@Component
public class BarreraSimulada {
    private Estado estado = Estado.CERRADA;
    private long ciclosCompletados;

    public synchronized String abrirYCerrar() {
        estado = Estado.ABIERTA;
        ciclosCompletados++;
        estado = Estado.CERRADA;
        return "Ciclo simulado " + ciclosCompletados + ": apertura y cierre completados; no hay hardware conectado.";
    }

    public synchronized Estado getEstado() {
        return estado;
    }

    public synchronized long getCiclosCompletados() {
        return ciclosCompletados;
    }

    public enum Estado {
        ABIERTA,
        CERRADA
    }
}