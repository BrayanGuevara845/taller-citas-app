package edu.pe.cibertec.taller.servicio;

import edu.pe.cibertec.taller.excepcion.EspecialidadIncorrectaException;
import edu.pe.cibertec.taller.excepcion.MecanicoNoEncontradoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;
	private Mecanico mecanico;

	private static final Long ID_MECANICO = 1L;
	private static final String PLACA = "GUE-984";
	private static final LocalDateTime FECHA_CITA =
			LocalDateTime.of(2026, 9, 14, 10, 0);
	private static final LocalDateTime FECHA_ACTUAL =
			LocalDateTime.of(2026, 9, 13, 8, 0);

	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(
				repositorioMecanicos,
				repositorioCitas,
				proveedorFechaHora,
				servicioNotificaciones
		);

		mecanico = new Mecanico(
				ID_MECANICO,
				"Brayan Guevara",
				TipoServicio.CAMBIO_ACEITE
		);
	}

	@Test
	@DisplayName("Agendar una cita valida la guarda, notifica y la retorna en estado PROGRAMADA")
	void agendarCitaExitosa() {
		// Arrange
		String placaZafiro = PLACA;

		when(repositorioMecanicos.findById(ID_MECANICO))
				.thenReturn(Optional.of(mecanico));

		when(proveedorFechaHora.ahora())
				.thenReturn(FECHA_ACTUAL);

		when(repositorioCitas.findByMecanicoIdAndEstado(
				ID_MECANICO,
				EstadoCita.PROGRAMADA
		)).thenReturn(List.of());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		Cita resultado = servicioCitas.agendarCita(
				ID_MECANICO,
				placaZafiro,
				TipoServicio.CAMBIO_ACEITE,
				FECHA_CITA
		);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
		assertEquals(
				TipoServicio.CAMBIO_ACEITE.getDuracionHoras(),
				resultado.getDuracionHoras()
		);
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1))
				.notificarCitaAgendada(any(Cita.class));
	}

	@Test
	@DisplayName("Agendar con un mecanico inexistente lanza MecanicoNoEncontradoException")
	void agendarConMecanicoInexistente() {
		// Arrange
		Long idMecanicoZafiro = 99L;

		when(repositorioMecanicos.findById(idMecanicoZafiro))
				.thenReturn(Optional.empty());

		// Act
		Executable accion = () -> servicioCitas.agendarCita(
				idMecanicoZafiro,
				PLACA,
				TipoServicio.CAMBIO_ACEITE,
				FECHA_CITA
		);

		// Assert
		assertThrows(MecanicoNoEncontradoException.class, accion);
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("Agendar cuando la especialidad no coincide lanza EspecialidadIncorrectaException")
	void agendarConEspecialidadIncorrecta() {
		// Arrange
		TipoServicio servicioZafiro = TipoServicio.REPARACION_MOTOR;

		when(repositorioMecanicos.findById(ID_MECANICO))
				.thenReturn(Optional.of(mecanico));

		// Act
		Executable accion = () -> servicioCitas.agendarCita(
				ID_MECANICO,
				PLACA,
				servicioZafiro,
				FECHA_CITA
		);

		// Assert
		assertThrows(EspecialidadIncorrectaException.class, accion);
		verify(repositorioCitas, never()).save(any(Cita.class));
	}
}