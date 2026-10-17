package com.example.parkspotter

import com.example.parkspotter.data.remote.GarageDetailResponse
import com.example.parkspotter.data.remote.GarageSummaryResponse
import com.example.parkspotter.data.remote.PageResponse
import com.example.parkspotter.data.remote.ReservationResponse
import com.example.parkspotter.data.remote.aModelo
import com.example.parkspotter.data.remote.aRequest
import com.example.parkspotter.model.AreaMapa
import com.example.parkspotter.model.Disponibilidad
import com.example.parkspotter.model.EstadoPlaza
import com.example.parkspotter.model.EstadoReserva
import com.example.parkspotter.model.FiltrosBusqueda
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.model.OrdenBusqueda
import com.example.parkspotter.model.TipoVehiculo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** El JSON de ejemplo es el de ParkSpotter-BackEnd/docs/API-Sprint-03-Busqueda-Garajes.md. */
class MapeosTest {

    private val gson = Gson()

    @Test fun `resultado de busqueda del backend`() {
        val json = """
            {"content":[{"id":"a1","name":"Garaje Centro","addressLine":"Cra 7 # 12-30","city":"Bogotá",
              "latitude":4.710989,"longitude":-74.072092,"pricePerHour":3500.00,
              "totalSpots":50,"availableSpots":49,"occupiedSpots":1,"reservedSpots":0,
              "availability":"AVAILABLE","open24Hours":false,"openingTime":"06:00:00","closingTime":"22:00:00",
              "openNow":true,"distanceKm":0.35}],
             "page":0,"size":20,"totalElements":1,"totalPages":1,"first":true,"last":true}
        """.trimIndent()
        val tipo = object : TypeToken<PageResponse<GarageSummaryResponse>>() {}.type
        val pagina: PageResponse<GarageSummaryResponse> = gson.fromJson(json, tipo)
        val g = pagina.content!!.first().aModelo()
        assertEquals("Garaje Centro", g.nombre)
        assertEquals(3500, g.precioPorHora)
        assertEquals(49, g.espaciosDisponibles)
        assertEquals(Disponibilidad.AVAILABLE, g.disponibilidad)
        assertEquals(LocalTime.of(6, 0), g.horaApertura)
        assertEquals(0.35, g.distanciaKm!!, 0.0001)
        assertTrue(g.reservable)
    }

    @Test fun `valores desconocidos o nulos no rompen la app`() {
        val json = """{"id":"x","availability":"NUEVO_ESTADO","openNow":false}"""
        val g = gson.fromJson(json, GarageSummaryResponse::class.java).aModelo()
        assertEquals(Disponibilidad.CLOSED, g.disponibilidad)
        assertEquals("", g.nombre)
        assertFalse(g.reservable)
    }

    @Test fun `detalle con plazas y disponibilidad por tipo`() {
        val json = """
            {"id":"g","name":"Parqueadero Chapinero","pricePerHour":5000,"open24Hours":true,
             "availability":{"garageId":"g","status":"ACTIVE","availability":"FEW_SPOTS","openNow":true,
               "totalSpots":21,"availableSpots":3,"occupiedSpots":17,"reservedSpots":1,"outOfServiceSpots":0},
             "availabilityByVehicleType":[{"vehicleType":"CAR","totalSpots":20,"availableSpots":2},
                                          {"vehicleType":"MOTORCYCLE","totalSpots":1,"availableSpots":1}],
             "floors":[2,1],
             "spots":[{"id":"s1","code":"P-001","floor":1,"vehicleType":"CAR","status":"RESERVED"}]}
        """.trimIndent()
        val d = gson.fromJson(json, GarageDetailResponse::class.java).aModelo()
        assertEquals(Disponibilidad.FEW_SPOTS, d.garaje.disponibilidad)
        assertEquals(3, d.garaje.espaciosDisponibles)
        assertEquals(listOf(1, 2), d.pisos)
        assertEquals(TipoVehiculo.MOTORCYCLE, d.porTipo[1].tipo)
        assertEquals(EstadoPlaza.RESERVED, d.plazas.first().estado)
    }

    @Test fun `alta de garaje envia lo que exige el backend`() {
        val g = Garaje(
            nombre = " Garaje Norte ", direccion = "Calle 100 # 15-20", precioPorHora = 4000, espaciosTotales = 10,
            abierto24h = false, horaApertura = LocalTime.of(6, 0), horaCierre = LocalTime.of(22, 30),
            lat = 4.68, lng = -74.05
        )
        val r = g.aRequest()
        assertEquals("Garaje Norte", r.name)
        assertEquals("Bogotá", r.city)
        assertEquals("Colombia", r.country)
        assertEquals("06:00", r.openingTime)
        assertEquals("22:30", r.closingTime)
        assertNull(r.description)
        assertEquals(0, r.motorcycleSpots)
        assertEquals(3, g.copy(cuposMoto = 3).aRequest().motorcycleSpots)
        val r24 = g.copy(abierto24h = true).aRequest()
        assertNull(r24.openingTime)
    }

    @Test fun `filtros se traducen a query params del backend`() {
        val f = FiltrosBusqueda(precioMax = 5000, soloDisponibles = true, abiertoAhora = true,
            tipoVehiculo = TipoVehiculo.MOTORCYCLE, radioKm = 3.0)
        val p = f.aParametrosBusqueda(4.71, -74.07)
        assertEquals("5000", p["maxPrice"])
        assertEquals("true", p["onlyAvailable"])
        assertEquals("true", p["openNow"])
        assertEquals("MOTORCYCLE", p["vehicleType"])
        assertEquals("3.0", p["radiusKm"])
        assertEquals("DISTANCE", p["sort"])
        assertNull(p["q"])
        // Con texto se amplía el radio para no esconder el garaje buscado
        assertEquals("50.0", f.copy(texto = "chapinero").aParametrosBusqueda(4.71, -74.07)["radiusKm"])
        // Sin ubicación no se puede ordenar por distancia
        assertEquals(OrdenBusqueda.NAME.name, FiltrosBusqueda().aParametrosBusqueda(null, null)["sort"])
        assertEquals(5, f.activos)
    }

    @Test fun `area del mapa`() {
        val p = AreaMapa(4.6, -74.2, 4.8, -74.0).aParametros()
        assertEquals("4.6", p["minLat"])
        assertEquals("-74.0", p["maxLng"])
        assertEquals("200", p["limit"])
    }

    @Test fun `reserva con zona horaria`() {
        val json = """
            {"id":"r","code":"PS-AB12CD","garageId":"g","garageName":"Centro","startTime":"2026-10-17T13:00:00Z",
             "endTime":"2026-10-17T10:00:00-05:00","status":"CONFIRMED","totalPrice":7000}
        """.trimIndent()
        val r = gson.fromJson(json, ReservationResponse::class.java).aModelo()!!
        assertEquals(8, r.inicio.hour) // 13:00 UTC = 8:00 en Bogotá
        assertEquals(10, r.fin.hour)
        assertEquals(EstadoReserva.CONFIRMED, r.estado)
        assertEquals(7000, r.total)
    }
}
