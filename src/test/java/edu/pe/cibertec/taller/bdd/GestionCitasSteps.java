package edu.pe.cibertec.taller.bdd;

import edu.pe.cibertec.taller.excepcion.HorarioOcupadoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GestionCitasSteps {

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	private Mecanico mecanicoSeleccionado;
	private Cita citaExistente;
	private Cita resultado;
	private RuntimeException excepcion;

	private static final String PLACA = "GUE-984";

	private static final LocalDateTime FECHA_ACTUAL =
			LocalDateTime.of(2026, 9, 13, 8, 0);

	private static final LocalDateTime INICIO_CITA_EXISTENTE =
			LocalDateTime.of(2026, 9, 14, 10, 0);

	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);

		servicioCitas = new ServicioCitasImpl(
				repositorioMecanicos,
				repositorioCitas,
				proveedorFechaHora,
				servicioNotificaciones
		);

		mecanicoSeleccionado = null;
		citaExistente = null;
		resultado = null;
		excepcion = null;
	}

	@Given("existe una cita programada el dia 14 de setiembre de 2026 de 10:00 a 12:00")
	public void existeUnaCitaProgramada() {
		// Arrange
		Mecanico mecanicoZafiro = new Mecanico(
				1L,
				"Brayan Guevara",
				TipoServicio.MANTENIMIENTO_LIGERO
		);

		citaExistente = crearCitaProgramada(
				100L,
				mecanicoZafiro,
				INICIO_CITA_EXISTENTE
		);
	}

	@And("existe otro mecanico llamado {string} especializado en MANTENIMIENTO_LIGERO")
	public void existeOtroMecanico(String nombre) {
		// Arrange
		mecanicoSeleccionado = new Mecanico(
				2L,
				nombre,
				TipoServicio.MANTENIMIENTO_LIGERO
		);
	}

	@When("se agenda MANTENIMIENTO_LIGERO para la placa {string} con el otro mecanico a las 08:00")
	public void seAgendaConOtroMecanico(String placa) {
		// Arrange
		LocalDateTime horarioZafiro =
				LocalDateTime.of(2026, 9, 14, 8, 0);

		when(repositorioMecanicos.findById(mecanicoSeleccionado.getId()))
				.thenReturn(Optional.of(mecanicoSeleccionado));

		when(proveedorFechaHora.ahora())
				.thenReturn(FECHA_ACTUAL);

		when(repositorioCitas.findByMecanicoIdAndEstado(
				mecanicoSeleccionado.getId(),
				EstadoCita.PROGRAMADA
		)).thenReturn(List.of());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		resultado = servicioCitas.agendarCita(
				mecanicoSeleccionado.getId(),
				placa,
				TipoServicio.MANTENIMIENTO_LIGERO,
				horarioZafiro
		);

		// Assert
		assertEquals(PLACA, resultado.getPlacaVehiculo());
	}

	@Given("el mecanico {string} tiene una cita programada el dia 14 de setiembre de 2026 de 10:00 a 12:00")
	public void elMecanicoTieneUnaCitaProgramada(String nombre) {
		// Arrange
		Mecanico mecanicoZafiro = new Mecanico(
				1L,
				nombre,
				TipoServicio.MANTENIMIENTO_LIGERO
		);

		mecanicoSeleccionado = mecanicoZafiro;

		citaExistente = crearCitaProgramada(
				100L,
				mecanicoZafiro,
				INICIO_CITA_EXISTENTE
		);
	}

	@When("se intenta agendar MANTENIMIENTO_LIGERO para la placa {string} con el mismo mecanico a las 11:00")
	public void seIntentaAgendarALasOnce(String placa) {
		// Arrange
		LocalDateTime horarioZafiro =
				LocalDateTime.of(2026, 9, 14, 11, 0);

		prepararMecanicoConCitaExistente();

		// Act
		try {
			resultado = servicioCitas.agendarCita(
					mecanicoSeleccionado.getId(),
					placa,
					TipoServicio.MANTENIMIENTO_LIGERO,
					horarioZafiro
			);
		} catch (RuntimeException error) {
			excepcion = error;
		}

		// Assert
		assertEquals(null, resultado);
	}

	@When("se intenta agendar MANTENIMIENTO_LIGERO para la placa {string} con el mismo mecanico a las 12:00")
	public void seIntentaAgendarALasDoce(String placa) {
		// Arrange
		LocalDateTime horarioZafiro =
				LocalDateTime.of(2026, 9, 14, 12, 0);

		prepararMecanicoConCitaExistente();

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		try {
			resultado = servicioCitas.agendarCita(
					mecanicoSeleccionado.getId(),
					placa,
					TipoServicio.MANTENIMIENTO_LIGERO,
					horarioZafiro
			);
		} catch (RuntimeException error) {
			excepcion = error;
		}

		// Assert
		assertEquals(null, excepcion);
	}

	@Then("la cita queda PROGRAMADA")
	public void laCitaQuedaProgramada() {
		// Arrange
		EstadoCita estadoZafiro = EstadoCita.PROGRAMADA;

		// Act
		EstadoCita estadoObtenido = resultado.getEstado();

		// Assert
		assertEquals(null, excepcion);
		assertEquals(estadoZafiro, estadoObtenido);
		assertEquals(PLACA, resultado.getPlacaVehiculo());
		assertEquals(
				TipoServicio.MANTENIMIENTO_LIGERO,
				resultado.getTipoServicio()
		);
	}

	@And("se notifica el agendamiento")
	public void seNotificaElAgendamiento() {
		// Arrange
		int cantidadZafiro = 1;

		// Act
		int cantidadEsperada = cantidadZafiro;

		// Assert
		verify(repositorioCitas, times(cantidadEsperada))
				.save(any(Cita.class));

		verify(servicioNotificaciones, times(cantidadEsperada))
				.notificarCitaAgendada(any(Cita.class));
	}

	@Then("el agendamiento se rechaza por horario ocupado")
	public void elAgendamientoSeRechazaPorHorarioOcupado() {
		// Arrange
		Class<?> excepcionZafiro = HorarioOcupadoException.class;

		// Act
		boolean esHorarioOcupado =
				excepcionZafiro.isInstance(excepcion);

		// Assert
		assertTrue(esHorarioOcupado);
		assertEquals(null, resultado);

		verify(repositorioCitas, never())
				.save(any(Cita.class));

		verify(servicioNotificaciones, never())
				.notificarCitaAgendada(any(Cita.class));
	}

	private void prepararMecanicoConCitaExistente() {
		when(repositorioMecanicos.findById(mecanicoSeleccionado.getId()))
				.thenReturn(Optional.of(mecanicoSeleccionado));

		when(proveedorFechaHora.ahora())
				.thenReturn(FECHA_ACTUAL);

		when(repositorioCitas.findByMecanicoIdAndEstado(
				mecanicoSeleccionado.getId(),
				EstadoCita.PROGRAMADA
		)).thenReturn(List.of(citaExistente));
	}

	private Cita crearCitaProgramada(
			Long id,
			Mecanico mecanico,
			LocalDateTime inicio
	) {
		Cita cita = new Cita();
		cita.setId(id);
		cita.setMecanico(mecanico);
		cita.setPlacaVehiculo(PLACA);
		cita.setTipoServicio(TipoServicio.MANTENIMIENTO_LIGERO);
		cita.setFechaHoraInicio(inicio);
		cita.setDuracionHoras(
				TipoServicio.MANTENIMIENTO_LIGERO.getDuracionHoras()
		);
		cita.setEstado(EstadoCita.PROGRAMADA);
		return cita;
	}
}