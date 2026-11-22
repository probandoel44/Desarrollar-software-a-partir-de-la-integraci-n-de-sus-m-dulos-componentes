package com.appmiparqueo.appmiparqueo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.appmiparqueo.appmiparqueo.model.Usuarios;
import com.appmiparqueo.appmiparqueo.model.Vehiculo;
import com.appmiparqueo.appmiparqueo.repository.VehiculoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimuladorAccesoServiceTest {
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private ParqueaderoService parqueaderoService;
    @Mock
    private BarreraSimulada barreraSimulada;

    private SimuladorAccesoService service;

    @BeforeEach
    void setUp() {
        service = new SimuladorAccesoService(vehiculoRepository, parqueaderoService, barreraSimulada);
    }

    @Test
    void validQrRegistersEntryBeforeSimulatingBarrier() {
        Usuarios owner = new Usuarios();
        owner.setCorreo("cliente@example.com");
        Vehiculo vehicle = new Vehiculo();
        vehicle.setUsuario(owner);
        vehicle.setPlaca("ABC123");
        vehicle.setTipoVehiculo("automovil");
        when(vehiculoRepository.findByCodigoQrAndActivoTrue("MP-CODE")).thenReturn(Optional.of(vehicle));
        when(barreraSimulada.abrirYCerrar()).thenReturn("apertura-cierre-simulados");

        SimuladorAccesoService.Resultado result = service.procesarLectura("MP-CODE", "QR", "ENTRADA", null);

        assertEquals("ABC123", result.placa());
        assertEquals("apertura-cierre-simulados", result.estadoBarrera());
        verify(parqueaderoService).registrarIngreso("ABC123", "automovil", "cliente@example.com");
        verify(barreraSimulada).abrirYCerrar();
    }

    @Test
    void invalidPlateDoesNotActivateBarrier() {
        when(vehiculoRepository.findByPlacaIgnoreCaseAndActivoTrue("ABC123")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.procesarLectura("abc 123", "PLACA", "ENTRADA", null));

        verifyNoInteractions(parqueaderoService, barreraSimulada);
    }
}