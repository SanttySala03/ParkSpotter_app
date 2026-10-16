package com.example.parkspotter

import com.example.parkspotter.model.EstadoGaraje
import com.example.parkspotter.model.EstadoReserva
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.model.Reserva
import com.example.parkspotter.model.SolicitudReserva
import com.example.parkspotter.model.TipoVehiculo
import com.example.parkspotter.util.ReglasReserva
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Sprint 4 — reglas del flujo de reserva (validación previa al envío). */
class ReglasReservaTest {

    private val zona = ZoneId.of("America/Bogota")
    private val ahora = ZonedDateTime.of(2026, 10, 16, 14, 10, 0, 0, zona)
    private val garaje24h = Garaje(id = "g1", nombre = "Centro", precioPorHora = 3500, espaciosDisponibles = 5)
    private val garajeDiurno = garaje24h.copy(abierto24h = false, horaApertura = LocalTime.of(6, 0), horaCierre = LocalTime.of(22, 0))
    private val garajeNocturno = garaje24h.copy(abierto24h = false, horaApertura = LocalTime.of(20, 0), horaCierre = LocalTime.of(6, 0))

    private fun solicitud(
        inicio: ZonedDateTime = ahora.plusHours(1),
        horas: Long = 2,
        garaje: Garaje = garaje24h,
        placa: String = "ABC123",
        tipo: TipoVehiculo = TipoVehiculo.CAR
    ) = SolicitudReserva(garaje, tipo, placa, inicio, inicio.plusHours(horas))

    private fun reserva(inicio: ZonedDateTime, fin: ZonedDateTime, estado: EstadoReserva = EstadoReserva.CONFIRMED) =
        Reserva("r1", "PS-1", "g1", "Centro", "", null, TipoVehiculo.CAR, "ABC123", inicio, fin, estado, 3500, 7000, null, null)

    @Test fun `solicitud valida no tiene errores`() {
        assertNull(ReglasReserva.validar(solicitud(), ahora, emptyList(), cuposDelTipo = 3))
    }

    @Test fun `rechaza inicio en el pasado`() {
        assertNotNull(ReglasReserva.validar(solicitud(inicio = ahora.minusHours(1)), ahora, emptyList(), 3))
    }

    @Test fun `tolera unos minutos de atraso al reservar ahora`() {
        assertNull(ReglasReserva.validar(solicitud(inicio = ahora.minusMinutes(5)), ahora, emptyList(), 3))
    }

    @Test fun `rechaza mas de 7 dias de anticipacion`() {
        assertNotNull(ReglasReserva.validar(solicitud(inicio = ahora.plusDays(8)), ahora, emptyList(), 3))
    }

    @Test fun `duracion entre 1 y 12 horas`() {
        assertNotNull(ReglasReserva.validar(solicitud(horas = 13), ahora, emptyList(), 3))
        assertNull(ReglasReserva.validar(solicitud(horas = 12), ahora, emptyList(), 3))
    }

    @Test fun `sin cupos del tipo elegido`() {
        assertNotNull(ReglasReserva.validar(solicitud(), ahora, emptyList(), cuposDelTipo = 0))
    }

    @Test fun `garaje inactivo no recibe reservas`() {
        val inactivo = garaje24h.copy(estado = EstadoGaraje.INACTIVE)
        assertNotNull(ReglasReserva.validar(solicitud(garaje = inactivo), ahora, emptyList(), 3))
    }

    @Test fun `horario diurno`() {
        val hoy = ahora.toLocalDate()
        assertTrue(ReglasReserva.dentroDeHorario(garajeDiurno, hoy.atTime(8, 0).atZone(zona), hoy.atTime(10, 0).atZone(zona)))
        assertFalse(ReglasReserva.dentroDeHorario(garajeDiurno, hoy.atTime(21, 0).atZone(zona), hoy.atTime(23, 0).atZone(zona)))
        assertFalse(ReglasReserva.dentroDeHorario(garajeDiurno, hoy.atTime(5, 0).atZone(zona), hoy.atTime(7, 0).atZone(zona)))
    }

    @Test fun `horario nocturno que cruza la medianoche`() {
        val hoy = ahora.toLocalDate()
        assertTrue(ReglasReserva.dentroDeHorario(garajeNocturno, hoy.atTime(23, 0).atZone(zona), hoy.plusDays(1).atTime(2, 0).atZone(zona)))
        assertTrue(ReglasReserva.dentroDeHorario(garajeNocturno, hoy.atTime(1, 0).atZone(zona), hoy.atTime(5, 0).atZone(zona)))
        assertFalse(ReglasReserva.dentroDeHorario(garajeNocturno, hoy.atTime(12, 0).atZone(zona), hoy.atTime(14, 0).atZone(zona)))
    }

    @Test fun `rechaza reserva que se cruza con otra activa`() {
        val existente = reserva(ahora.plusHours(2), ahora.plusHours(4))
        assertNotNull(ReglasReserva.validar(solicitud(inicio = ahora.plusHours(3)), ahora, listOf(existente), 3))
        // Una cancelada no bloquea el horario
        val cancelada = existente.copy(estado = EstadoReserva.CANCELLED)
        assertNull(ReglasReserva.validar(solicitud(inicio = ahora.plusHours(3)), ahora, listOf(cancelada), 3))
    }

    @Test fun `reservas contiguas no se solapan`() {
        val a = ahora.plusHours(1)
        assertFalse(ReglasReserva.seSolapan(a, a.plusHours(2), a.plusHours(2), a.plusHours(3)))
    }

    @Test fun `cada fraccion de hora se cobra completa`() {
        val ini = ahora
        assertEquals(1, ReglasReserva.horasCobradas(ini, ini.plusMinutes(60)))
        assertEquals(2, ReglasReserva.horasCobradas(ini, ini.plusMinutes(61)))
        assertEquals(7000, ReglasReserva.costo(3500, ini, ini.plusHours(2)))
    }

    @Test fun `placas colombianas`() {
        assertNull(ReglasReserva.errorPlaca("abc-123", TipoVehiculo.CAR))
        assertNull(ReglasReserva.errorPlaca("XYZ12D", TipoVehiculo.MOTORCYCLE))
        assertNotNull(ReglasReserva.errorPlaca("12ABC3", TipoVehiculo.CAR))
        assertNotNull(ReglasReserva.errorPlaca("", TipoVehiculo.CAR))
        assertNull(ReglasReserva.errorPlaca("", TipoVehiculo.BICYCLE))
    }

    @Test fun `inicio sugerido redondea a la proxima media hora`() {
        val sugerido = ReglasReserva.primerInicioPosible(garaje24h, ahora)
        assertEquals(LocalTime.of(14, 30), sugerido.toLocalTime())
    }

    @Test fun `inicio sugerido salta a la siguiente apertura si esta cerrado`() {
        val noche = ahora.withHour(23)
        val sugerido = ReglasReserva.primerInicioPosible(garajeDiurno, noche)
        assertEquals(noche.toLocalDate().plusDays(1), sugerido.toLocalDate())
        assertEquals(LocalTime.of(6, 0), sugerido.toLocalTime())
    }

    @Test fun `una confirmada que ya termino se ve completada`() {
        val pasada = reserva(ahora.minusHours(3), ahora.minusHours(1))
        assertEquals(EstadoReserva.COMPLETED, pasada.estadoVisible(ahora))
        assertFalse(pasada.cancelable(ahora))
        val enCurso = reserva(ahora.minusHours(1), ahora.plusHours(1))
        assertTrue(enCurso.enCurso(ahora))
        assertFalse(enCurso.cancelable(ahora))
    }
}
