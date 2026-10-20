package com.example.parkspotter

import com.example.parkspotter.data.remote.PaymentResponse
import com.example.parkspotter.data.remote.aModelo
import com.example.parkspotter.model.EstadoPago
import com.example.parkspotter.util.Tarjeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/** Sprint 5 — validación de tarjeta en el celular, tokenización simulada y lectura del pago. */
class TarjetaTest {

    private val hoy = YearMonth.of(2026, 10)

    @Test fun `luhn y franquicias`() {
        assertTrue(Tarjeta.luhnValido("4242 4242 4242 4242"))
        assertFalse(Tarjeta.luhnValido("4242 4242 4242 4241"))
        assertFalse(Tarjeta.luhnValido("4242"))
        assertEquals("VISA", Tarjeta.franquicia("4242424242424242"))
        assertEquals("MASTERCARD", Tarjeta.franquicia("5555555555554444"))
        assertEquals("MASTERCARD", Tarjeta.franquicia("2223003122003222"))
        assertEquals("AMEX", Tarjeta.franquicia("378282246310005"))
        assertNull(Tarjeta.franquicia("6011111111111117"))
    }

    @Test fun `errores del formulario`() {
        assertNull(Tarjeta.error("4242424242424242", "12/29", "123", "ANA PEREZ", hoy))
        assertNull(Tarjeta.error("378282246310005", "10/26", "1234", "ANA PEREZ", hoy))
        assertNotNull(Tarjeta.error("4242424242424242", "12/29", "123", " ", hoy))
        assertNotNull(Tarjeta.error("4242424242424241", "12/29", "123", "ANA", hoy))
        assertNotNull(Tarjeta.error("4242424242424242", "09/26", "123", "ANA", hoy))
        assertNotNull(Tarjeta.error("4242424242424242", "13/29", "123", "ANA", hoy))
        assertNotNull(Tarjeta.error("4242424242424242", "1229", "123", "ANA", hoy))
        assertNotNull(Tarjeta.error("378282246310005", "12/29", "123", "ANA", hoy))
    }

    @Test fun `el token solo lleva el resultado de prueba y los ultimos 4`() {
        assertEquals("tok_test_approved_4242", Tarjeta.tokenizar("4242 4242 4242 4242"))
        assertEquals("tok_test_declined_0002", Tarjeta.tokenizar(Tarjeta.PRUEBA_RECHAZADA))
    }

    @Test fun `pago aprobado del backend`() {
        val p = PaymentResponse(
            id = "p1", reference = "PAY-ABCD1234", reservationId = "r1", reservationCode = "PS-XYZ123",
            garageName = "Centro", garageAddress = "Cra 7", vehiclePlate = "AGH430",
            startTime = "2026-10-20T15:00:00Z", endTime = "2026-10-20T17:00:00Z", amount = 8000.0,
            status = "APPROVED", cardBrand = "VISA", cardLast4 = "4242", gatewayTransactionId = "SIM-1",
            receiptNumber = "CP-20261019-ABCD1234", declineReason = null, createdAt = "2026-10-19T20:00:00Z",
            approvedAt = "2026-10-19T20:00:02Z", refundedAt = null
        ).aModelo()
        assertEquals(EstadoPago.APPROVED, p.estado)
        assertEquals(8000, p.monto)
        assertEquals(10, p.inicio!!.hour) // 15:00 UTC = 10:00 en Bogotá
        assertEquals("CP-20261019-ABCD1234", p.comprobante)
    }
}
