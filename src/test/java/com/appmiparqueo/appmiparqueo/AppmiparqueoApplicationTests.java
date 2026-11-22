package com.appmiparqueo.appmiparqueo;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.appmiparqueo.appmiparqueo.model.Movimiento;
import com.appmiparqueo.appmiparqueo.model.Alerta;
import com.appmiparqueo.appmiparqueo.model.Pago;
import com.appmiparqueo.appmiparqueo.model.Fidelizacion;
import com.appmiparqueo.appmiparqueo.model.Usuarios;
import com.appmiparqueo.appmiparqueo.model.Reserva;
import com.appmiparqueo.appmiparqueo.model.UbicacionVehiculo;
import com.appmiparqueo.appmiparqueo.model.Vehiculo;
import com.appmiparqueo.appmiparqueo.model.Puesto;
import com.appmiparqueo.appmiparqueo.repository.AlertaRepository;
import com.appmiparqueo.appmiparqueo.repository.FidelizacionRepository;
import com.appmiparqueo.appmiparqueo.repository.MovimientoRepository;
import com.appmiparqueo.appmiparqueo.repository.PagoRepository;
import com.appmiparqueo.appmiparqueo.repository.PuestoRepository;
import com.appmiparqueo.appmiparqueo.repository.ReservaRepository;
import com.appmiparqueo.appmiparqueo.repository.UbicacionVehiculoRepository;
import com.appmiparqueo.appmiparqueo.repository.UsuariosRepository;
import com.appmiparqueo.appmiparqueo.repository.VehiculoRepository;
import com.appmiparqueo.appmiparqueo.service.ParqueaderoService;
import com.appmiparqueo.appmiparqueo.service.UsuariosAdminService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AppmiparqueoApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ParqueaderoService parqueaderoService;

	@Autowired
	private UsuariosRepository usuariosRepository;

	@Autowired
	private MovimientoRepository movimientoRepository;

	@Autowired
	private PagoRepository pagoRepository;

	@Autowired
	private PuestoRepository puestoRepository;

	@Autowired
	private ReservaRepository reservaRepository;

	@Autowired
	private VehiculoRepository vehiculoRepository;

	@Autowired
	private UbicacionVehiculoRepository ubicacionRepository;

	@Autowired
	private AlertaRepository alertaRepository;

	@Autowired
	private FidelizacionRepository fidelizacionRepository;

	@Autowired
	private UsuariosAdminService usuariosAdminService;

	@Value("${app.admin.email}")
	private String adminEmail;

	@Test
	void reservationCanBeExtendedCancelledAndRemainsInHistory() {
		Usuarios cliente = usuariosRepository.findByCorreo("cliente@miparqueo.local").orElseThrow();
		String plate = "E" + Integer.toString((int) (System.nanoTime() % 1000000000L), 36).toUpperCase();
		Vehiculo vehiculo = parqueaderoService.registrarVehiculo(cliente, plate, "automovil", "Prueba", "Extensión", "Verde");
		Puesto disponible = puestoRepository.findFirstByEstadoOrderByCodigoAsc("disponible").orElseThrow();
		LocalDateTime inicio = LocalDateTime.now().plusHours(1);
		Reserva reserva = parqueaderoService.reservarEspacio(cliente, vehiculo.getId(), disponible.getId(),
				inicio, inicio.plusHours(1));

		parqueaderoService.extenderReserva(cliente, reserva.getId(), inicio.plusHours(2));
		Assertions.assertEquals(inicio.plusHours(2), reservaRepository.findById(reserva.getId()).orElseThrow().getHoraFinEstimada());
		Assertions.assertTrue(reservaRepository.findByUsuarioOrderByHoraInicioDesc(cliente).stream()
				.anyMatch(item -> item.getId().equals(reserva.getId())));
		Assertions.assertTrue(alertaRepository.findByUsuarioAndNivelStartingWithOrderByFechaDesc(cliente, "RESERVA_")
				.stream().anyMatch(item -> "RESERVA_EXTENDIDA".equals(item.getNivel())));

		parqueaderoService.cancelarReserva(cliente, reserva.getId());
		Assertions.assertEquals("cancelada", reservaRepository.findById(reserva.getId()).orElseThrow().getEstado());
		Assertions.assertTrue(parqueaderoService.consultarDisponibilidad(inicio, inicio.plusHours(2)).stream()
				.anyMatch(item -> item.getId().equals(disponible.getId())));
		Assertions.assertTrue(alertaRepository.findByUsuarioAndNivelStartingWithOrderByFechaDesc(cliente, "RESERVA_")
				.stream().anyMatch(item -> "RESERVA_CANCELADA".equals(item.getNivel())));
	}

	@Test
	void expiredReservationReleasesItsSpaceAndPreservesHistory() {
		Usuarios cliente = usuariosRepository.findByCorreo("cliente@miparqueo.local").orElseThrow();
		String plate = "V" + Integer.toString((int) (System.nanoTime() % 1000000000L), 36).toUpperCase();
		Vehiculo vehiculo = parqueaderoService.registrarVehiculo(cliente, plate, "automovil", "Prueba", "Vencimiento", "Gris");
		Puesto disponible = puestoRepository.findFirstByEstadoOrderByCodigoAsc("disponible").orElseThrow();
		LocalDateTime fin = LocalDateTime.now().minusMinutes(1);
		Reserva reserva = parqueaderoService.reservarEspacio(cliente, vehiculo.getId(), disponible.getId(),
				fin.minusHours(1), fin);

		parqueaderoService.expirarReservas(LocalDateTime.now());

		Assertions.assertEquals("vencida", reservaRepository.findById(reserva.getId()).orElseThrow().getEstado());
		Assertions.assertEquals("disponible", puestoRepository.findById(disponible.getId()).orElseThrow().getEstado());
	}

	@Test
	void adjacentReservationsCanShareASpaceButAnExtensionCannotOverlap() {
		Usuarios firstClient = crearUsuarioPrueba("extension-a-" + System.nanoTime() + "@example.com");
		Usuarios secondClient = crearUsuarioPrueba("extension-b-" + System.nanoTime() + "@example.com");
		String suffix = Integer.toString((int) (System.nanoTime() % 1000000000L), 36).toUpperCase();
		Vehiculo firstVehicle = parqueaderoService.registrarVehiculo(firstClient, "F" + suffix, "automovil", "Prueba", "Uno", "Negro");
		Vehiculo secondVehicle = parqueaderoService.registrarVehiculo(secondClient, "G" + suffix, "automovil", "Prueba", "Dos", "Blanco");
		Puesto available = puestoRepository.findFirstByEstadoOrderByCodigoAsc("disponible").orElseThrow();
		LocalDateTime startTime = LocalDateTime.now().plusHours(1);
		Reserva firstReservation = parqueaderoService.reservarEspacio(firstClient, firstVehicle.getId(), available.getId(),
				startTime, startTime.plusHours(1));
		parqueaderoService.reservarEspacio(secondClient, secondVehicle.getId(), available.getId(),
				startTime.plusHours(1), startTime.plusHours(2));

		Assertions.assertThrows(IllegalArgumentException.class,
				() -> parqueaderoService.extenderReserva(firstClient, firstReservation.getId(), startTime.plusHours(2)));
		Assertions.assertEquals(startTime.plusHours(1),
				reservaRepository.findById(firstReservation.getId()).orElseThrow().getHoraFinEstimada());
	}

	@Test
	void invalidSimulatedVehicleReadIsReportedAndStoredAsIncident() throws Exception {
		Usuarios admin = usuariosRepository.findByCorreo(adminEmail).orElseThrow();
		MockHttpSession adminSession = new MockHttpSession();
		adminSession.setAttribute("authenticatedUser", admin);
		long alertsBefore = alertaRepository.countByResueltaFalseAndNivelIn(List.of("INFO", "ADVERTENCIA", "CRITICA"));

		mockMvc.perform(post("/movimientos/acceso-simulado").session(adminSession)
				.param("lectura", "INVALID-" + System.nanoTime())
				.param("tipoLectura", "PLACA")
				.param("operacion", "ENTRADA"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/movimientos"));

		Assertions.assertEquals(alertsBefore + 1,
				alertaRepository.countByResueltaFalseAndNivelIn(List.of("INFO", "ADVERTENCIA", "CRITICA")));
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void concurrentRequestsCannotReserveOverlappingPeriodsForOneSpace() throws Exception {
		String suffix = Integer.toString((int) (System.nanoTime() % 1000000000L), 36).toUpperCase();
		Usuarios firstClient = crearUsuarioPrueba("concurrent-a-" + suffix + "@example.com");
		Usuarios secondClient = crearUsuarioPrueba("concurrent-b-" + suffix + "@example.com");
		Vehiculo firstVehicle = parqueaderoService.registrarVehiculo(firstClient, "A" + suffix, "automovil", "Prueba", "A", "Rojo");
		Vehiculo secondVehicle = parqueaderoService.registrarVehiculo(secondClient, "B" + suffix, "automovil", "Prueba", "B", "Azul");
		Puesto available = puestoRepository.findFirstByEstadoOrderByCodigoAsc("disponible").orElseThrow();
		LocalDateTime startTime = LocalDateTime.now().plusHours(1);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		Callable<Boolean> firstRequest = reservationRequest(ready, start, firstClient, firstVehicle, available, startTime);
		Callable<Boolean> secondRequest = reservationRequest(ready, start, secondClient, secondVehicle, available, startTime);

		try {
			Future<Boolean> firstResult = executor.submit(firstRequest);
			Future<Boolean> secondResult = executor.submit(secondRequest);
			Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();
			int accepted = (firstResult.get(10, TimeUnit.SECONDS) ? 1 : 0)
					+ (secondResult.get(10, TimeUnit.SECONDS) ? 1 : 0);
			Assertions.assertEquals(1, accepted);
		} finally {
			executor.shutdownNow();
			usuariosAdminService.eliminarUsuario(firstClient.getId_usuario());
			usuariosAdminService.eliminarUsuario(secondClient.getId_usuario());
		}
	}

	private Callable<Boolean> reservationRequest(CountDownLatch ready, CountDownLatch start, Usuarios client,
			Vehiculo vehicle, Puesto space, LocalDateTime startTime) {
		return () -> {
			ready.countDown();
			if (!start.await(5, TimeUnit.SECONDS)) return false;
			try {
				parqueaderoService.reservarEspacio(client, vehicle.getId(), space.getId(),
						startTime, startTime.plusHours(1));
				return true;
			} catch (IllegalArgumentException ex) {
				return false;
			}
		};
	}

	@Test
	void contextLoads() {
	}

	@Test
	void clientAndAdminPanelsRenderDatabaseSpaces() throws Exception {
		Puesto disponible = puestoRepository.findFirstByEstadoOrderByCodigoAsc("disponible").orElseThrow();
		Usuarios cliente = usuariosRepository.findByCorreo("cliente@miparqueo.local").orElseThrow();
		MockHttpSession clientSession = new MockHttpSession();
		clientSession.setAttribute("authenticatedUser", cliente);

		mockMvc.perform(get("/view/cliente").session(clientSession))
				.andExpect(status().isOk())
				.andExpect(view().name("cliente"))
				.andExpect(content().string(containsString(disponible.getCodigo())));

		Usuarios admin = usuariosRepository.findByCorreo(adminEmail).orElseThrow();
		MockHttpSession adminSession = new MockHttpSession();
		adminSession.setAttribute("authenticatedUser", admin);

		mockMvc.perform(get("/ocupacion").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(view().name("ocupacion"))
				.andExpect(content().string(containsString(disponible.getCodigo())));
	}

	@Test
	void adminPanelRoutesRenderWithAdminSession() throws Exception {
		Usuarios admin = usuariosRepository.findByCorreo(adminEmail).orElseThrow();
		MockHttpSession adminSession = new MockHttpSession();
		adminSession.setAttribute("authenticatedUser", admin);

		for (String path : List.of("/dashboard", "/movimientos", "/alertas", "/pagos",
				"/ubicacion-vehiculos", "/fidelizacion", "/view/usuarios/list")) {
			mockMvc.perform(get(path).session(adminSession)).andExpect(status().isOk());
		}
	}

	@Test
	void registeredClientVehicleAppearsOnAdminVehiclesPage() throws Exception {
		Usuarios cliente = usuariosRepository.findByCorreo("cliente@miparqueo.local").orElseThrow();
		Usuarios admin = usuariosRepository.findByCorreo(adminEmail).orElseThrow();
		String plate = "R" + Integer.toString((int) (System.nanoTime() % 1000000000L), 36).toUpperCase();
		parqueaderoService.registrarVehiculo(cliente, plate, "automovil", "Prueba", "Panel", "Azul");
		MockHttpSession adminSession = new MockHttpSession();
		adminSession.setAttribute("authenticatedUser", admin);

		mockMvc.perform(get("/ubicacion-vehiculos").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(plate)))
				.andExpect(content().string(containsString(cliente.getCorreo())));
	}

	@Test
	void deactivatingUserPreservesExistingReservations() throws Exception {
		Usuarios cliente = new Usuarios();
		cliente.setNombre("Prueba");
		cliente.setApellido("Desactivacion");
		cliente.setCorreo("deactivation-" + System.nanoTime() + "@example.com");
		cliente.setTelefono("0000000000");
		cliente.setContrasenaHash("test-hash");
		cliente.setEstado("activo");
		cliente = usuariosRepository.save(cliente);

		String plate = "D" + Integer.toString((int) (System.nanoTime() % 1000000000L), 36).toUpperCase();
		Vehiculo vehiculo = parqueaderoService.registrarVehiculo(cliente, plate, "automovil", "Prueba", "Reserva", "Gris");
		Puesto disponible = puestoRepository.findFirstByEstadoOrderByCodigoAsc("disponible").orElseThrow();
		Reserva reserva = parqueaderoService.reservarEspacio(cliente, vehiculo.getId(), disponible.getId(),
				java.time.LocalDateTime.now().plusHours(1), java.time.LocalDateTime.now().plusHours(3));
		Usuarios admin = usuariosRepository.findByCorreo(adminEmail).orElseThrow();
		MockHttpSession adminSession = new MockHttpSession();
		adminSession.setAttribute("authenticatedUser", admin);

		mockMvc.perform(post("/view/usuarios/deactivate/{id}", cliente.getId_usuario()).session(adminSession))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/view/usuarios/list"));

		Assertions.assertEquals("inactivo", usuariosRepository.findById(cliente.getId_usuario()).orElseThrow().getEstado());
		Assertions.assertTrue(reservaRepository.findByUsuarioOrderByHoraInicioDesc(cliente).stream()
				.anyMatch(savedReserva -> savedReserva.getId().equals(reserva.getId())));
	}

	@Test
	void adminCanDeleteUserAndAllRelatedRecords() throws Exception {
		Usuarios usuario = crearUsuarioPrueba("delete-" + System.nanoTime() + "@example.com");
		String plate = "X" + Integer.toString((int) (System.nanoTime() % 1000000000L), 36).toUpperCase();
		Vehiculo vehiculo = parqueaderoService.registrarVehiculo(usuario, plate, "automovil", "Prueba", "Borrado", "Negro");
		Puesto disponible = puestoRepository.findFirstByEstadoOrderByCodigoAsc("disponible").orElseThrow();
		Reserva reserva = parqueaderoService.reservarEspacio(usuario, vehiculo.getId(), disponible.getId(),
				java.time.LocalDateTime.now().minusMinutes(1), java.time.LocalDateTime.now().plusHours(2));
		Movimiento movimiento = parqueaderoService.registrarIngreso(plate, "Automóvil", usuario.getCorreo());
		Pago pago = parqueaderoService.registrarSalida(plate, "Efectivo");
		UbicacionVehiculo ubicacion = ubicacionRepository.findByMovimientoOrderByFechaRegistroDesc(movimiento)
				.stream().findFirst().orElseThrow();
		Fidelizacion fidelizacion = new Fidelizacion();
		fidelizacion.setUsuario(usuario);
		fidelizacion.setPuntosAcumulados(10);
		fidelizacion.setNivel("basico");
		fidelizacion = fidelizacionRepository.save(fidelizacion);
		Alerta alerta = new Alerta();
		alerta.setUsuario(usuario);
		alerta.setTitulo("Prueba");
		alerta.setMensaje("Registro para eliminación");
		alerta.setNivel("INFO");
		alerta.setResuelta(false);
		alerta = alertaRepository.save(alerta);

		Usuarios admin = usuariosRepository.findByCorreo(adminEmail).orElseThrow();
		MockHttpSession adminSession = new MockHttpSession();
		adminSession.setAttribute("authenticatedUser", admin);
		mockMvc.perform(post("/view/usuarios/delete/{id}", usuario.getId_usuario()).session(adminSession))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/view/usuarios/list"));

		Assertions.assertFalse(usuariosRepository.existsById(usuario.getId_usuario()));
		Assertions.assertFalse(vehiculoRepository.existsById(vehiculo.getId()));
		Assertions.assertFalse(reservaRepository.existsById(reserva.getId()));
		Assertions.assertFalse(movimientoRepository.existsById(movimiento.getId()));
		Assertions.assertFalse(pagoRepository.existsById(pago.getId()));
		Assertions.assertFalse(ubicacionRepository.existsById(ubicacion.getId()));
		Assertions.assertFalse(fidelizacionRepository.existsById(fidelizacion.getId()));
		Assertions.assertFalse(alertaRepository.existsById(alerta.getId()));
		Assertions.assertEquals("disponible", puestoRepository.findById(disponible.getId()).orElseThrow().getEstado());
	}

	@Test
	void deletingUserWithVehicleInsideIsRejected() {
		Usuarios usuario = crearUsuarioPrueba("active-" + System.nanoTime() + "@example.com");
		String plate = "Y" + Integer.toString((int) (System.nanoTime() % 1000000000L), 36).toUpperCase();
		parqueaderoService.registrarVehiculo(usuario, plate, "automovil", "Prueba", "Activo", "Blanco");
		parqueaderoService.registrarIngreso(plate, "Automóvil", usuario.getCorreo());

		Assertions.assertThrows(IllegalArgumentException.class,
				() -> usuariosAdminService.eliminarUsuario(usuario.getId_usuario()));
		Assertions.assertTrue(usuariosRepository.existsById(usuario.getId_usuario()));
	}

	@Test
	void adminAccountCannotBeDeleted() {
		Usuarios admin = usuariosRepository.findByCorreo(adminEmail).orElseThrow();
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> usuariosAdminService.eliminarUsuario(admin.getId_usuario()));
		Assertions.assertTrue(usuariosRepository.existsById(admin.getId_usuario()));
	}

	private Usuarios crearUsuarioPrueba(String correo) {
		Usuarios usuario = new Usuarios();
		usuario.setNombre("Prueba");
		usuario.setApellido("Eliminacion");
		usuario.setCorreo(correo);
		usuario.setTelefono("0000000000");
		usuario.setContrasenaHash("test-hash");
		usuario.setEstado("activo");
		return usuariosRepository.save(usuario);
	}

	@Test
	void parkingLifecycleUpdatesRelatedRecords() {
		Usuarios cliente = usuariosRepository.findByCorreo("cliente@miparqueo.local").orElseThrow();
		long occupiedBefore = puestoRepository.countByEstado("ocupado");
		int pointsBefore = fidelizacionRepository.findByUsuario(cliente).orElseThrow().getPuntosAcumulados();
		String plate = "T" + Integer.toString((int) (System.nanoTime() % 1000000000L), 36).toUpperCase();
		Vehiculo vehiculo = parqueaderoService.registrarVehiculo(cliente, plate, "automovil", "Prueba", "Modelo", "Negro");
		long reservedBefore = puestoRepository.countByEstado("reservado");
		Puesto puestoDisponible = puestoRepository.findFirstByEstadoOrderByCodigoAsc("disponible").orElseThrow();
		Reserva reserva = parqueaderoService.reservarEspacio(cliente, vehiculo.getId(), puestoDisponible.getId(),
				java.time.LocalDateTime.now().minusMinutes(1), java.time.LocalDateTime.now().plusHours(2));
		Assertions.assertEquals(puestoDisponible.getId(), reserva.getEspacio().getId());
		Assertions.assertEquals(reservedBefore + 1, puestoRepository.countByEstado("reservado"));

		Movimiento movimiento = parqueaderoService.registrarIngreso(plate, "Automóvil", cliente.getCorreo());
		Assertions.assertEquals(reserva.getId(), movimiento.getReserva().getId());
		Assertions.assertEquals(occupiedBefore + 1, puestoRepository.countByEstado("ocupado"));
		Assertions.assertEquals("ACTIVO", movimientoRepository.findById(movimiento.getId()).orElseThrow().getEstado());

		Pago pago = parqueaderoService.registrarSalida(plate, "Efectivo");
		Assertions.assertEquals("pendiente", pagoRepository.findById(pago.getId()).orElseThrow().getEstadoPago());
		Assertions.assertEquals("disponible", movimiento.getReserva().getEspacio().getEstado());

		parqueaderoService.marcarPagoComoPagado(pago.getId(), "Tarjeta");
		Fidelizacion puntosActualizados = fidelizacionRepository.findByUsuario(cliente).orElseThrow();
		Assertions.assertEquals(pointsBefore + 4, puntosActualizados.getPuntosAcumulados());
		Assertions.assertEquals(1, movimientoRepository.countByReserva_Usuario_CorreoAndFechaSalidaIsNotNull(cliente.getCorreo()));

		var alerta = parqueaderoService.crearAlerta("Prueba transaccional", "Alerta de validación", "INFO");
		Assertions.assertFalse(alertaRepository.findById(alerta.getId()).orElseThrow().isResuelta());
		parqueaderoService.resolverAlerta(alerta.getId());
		Assertions.assertTrue(alertaRepository.findById(alerta.getId()).orElseThrow().isResuelta());
	}

}
