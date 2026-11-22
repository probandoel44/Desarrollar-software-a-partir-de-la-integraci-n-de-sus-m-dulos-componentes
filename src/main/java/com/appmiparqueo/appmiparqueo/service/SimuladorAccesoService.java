package com.appmiparqueo.appmiparqueo.service;

import com.appmiparqueo.appmiparqueo.model.Vehiculo;
import com.appmiparqueo.appmiparqueo.repository.VehiculoRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class SimuladorAccesoService {
    private final VehiculoRepository vehiculoRepository;
    private final ParqueaderoService parqueaderoService;
    private final BarreraSimulada barreraSimulada;

    public SimuladorAccesoService(VehiculoRepository vehiculoRepository, ParqueaderoService parqueaderoService,
            BarreraSimulada barreraSimulada) {
        this.vehiculoRepository = vehiculoRepository;
        this.parqueaderoService = parqueaderoService;
        this.barreraSimulada = barreraSimulada;
    }

    public Resultado procesarLectura(String lectura, String tipoLectura, String operacion, String metodoPago) {
        if (lectura == null || lectura.isBlank()) {
            throw new IllegalArgumentException("La lectura del vehículo es obligatoria.");
        }
        String tipo = tipoLectura == null ? "" : tipoLectura.trim().toUpperCase(Locale.ROOT);
        String accion = operacion == null ? "" : operacion.trim().toUpperCase(Locale.ROOT);
        Vehiculo vehiculo;
        if ("QR".equals(tipo)) {
            vehiculo = vehiculoRepository.findByCodigoQrAndActivoTrue(lectura.trim()).orElse(null);
        } else if ("PLACA".equals(tipo)) {
            String placa = lectura.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
            vehiculo = vehiculoRepository.findByPlacaIgnoreCaseAndActivoTrue(placa).orElse(null);
        } else {
            throw new IllegalArgumentException("El tipo de lectura debe ser placa o QR.");
        }
        if (vehiculo == null) {
            throw new IllegalArgumentException("No se identificó un vehículo activo con esa lectura.");
        }

        if ("ENTRADA".equals(accion)) {
            parqueaderoService.registrarIngreso(vehiculo.getPlaca(), vehiculo.getTipoVehiculo(),
                    vehiculo.getUsuario().getCorreo());
        } else if ("SALIDA".equals(accion)) {
            parqueaderoService.registrarSalida(vehiculo.getPlaca(), metodoPago);
        } else {
            throw new IllegalArgumentException("La operación debe ser entrada o salida.");
        }
        return new Resultado(vehiculo.getPlaca(), accion, barreraSimulada.abrirYCerrar());
    }

    public record Resultado(String placa, String operacion, String estadoBarrera) {
    }
}