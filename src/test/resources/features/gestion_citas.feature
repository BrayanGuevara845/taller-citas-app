Feature: Gestion de citas del taller mecanico

  Scenario: Registrar mantenimiento ligero con otro mecanico
    Given existe una cita programada el dia 14 de setiembre de 2026 de 10:00 a 12:00
    And existe otro mecanico llamado "Brayan Guevara" especializado en MANTENIMIENTO_LIGERO
    When se agenda MANTENIMIENTO_LIGERO para la placa "GUE-984" con el otro mecanico a las 08:00
    Then la cita queda PROGRAMADA
    And se notifica el agendamiento

  Scenario: Rechazar cita que inicia a las 11:00 por horario ocupado
    Given el mecanico "Brayan Guevara" tiene una cita programada el dia 14 de setiembre de 2026 de 10:00 a 12:00
    When se intenta agendar MANTENIMIENTO_LIGERO para la placa "GUE-984" con el mismo mecanico a las 11:00
    Then el agendamiento se rechaza por horario ocupado

  Scenario: Registrar cita que inicia exactamente a las 12:00
    Given el mecanico "Brayan Guevara" tiene una cita programada el dia 14 de setiembre de 2026 de 10:00 a 12:00
    When se intenta agendar MANTENIMIENTO_LIGERO para la placa "GUE-984" con el mismo mecanico a las 12:00
    Then la cita queda PROGRAMADA
    And se notifica el agendamiento