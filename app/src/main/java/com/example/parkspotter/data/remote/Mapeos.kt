package com.example.parkspotter.data.remote

import com.example.parkspotter.model.Disponibilidad
import com.example.parkspotter.model.DisponibilidadGaraje
import com.example.parkspotter.model.DisponibilidadPorTipo
import com.example.parkspotter.model.EstadoGaraje
import com.example.parkspotter.model.EstadoPlaza
import com.example.parkspotter.model.EstadoReserva
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.model.GarajeDetalle
import com.example.parkspotter.model.Plaza
import com.example.parkspotter.model.Reserva
import com.example.parkspotter.model.TipoVehiculo
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// Traducción entre el JSON del backend (inglés) y el modelo de la app (español).

private val ZONA = ZoneId.of(Config.ZONA_HORARIA)
private val HORA = DateTimeFormatter.ofPattern("HH:mm")

internal fun String?.aHora(): LocalTime? = this?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

internal fun LocalTime.aTexto(): String = format(HORA)

/** Acepta "2026-10-17T08:00:00-05:00", "2026-10-17T13:00:00Z" o con milisegundos. */
internal fun String?.aFechaHora(): ZonedDateTime? = this?.let {
    runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZONA) }.getOrNull()
}

internal fun ZonedDateTime.aIso(): String = toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

private fun Double?.aPesos(): Int = this?.roundToInt() ?: 0

fun GarageResponse.aModelo() = Garaje(
    id = id,
    nombre = name.orEmpty(),
    descripcion = description.orEmpty(),
    telefono = phone.orEmpty(),
    direccion = addressLine.orEmpty(),
    ciudad = city.orEmpty(),
    departamento = state.orEmpty(),
    pais = country.orEmpty(),
    codigoPostal = postalCode.orEmpty(),
    lat = latitude,
    lng = longitude,
    precioPorHora = pricePerHour.aPesos(),
    espaciosTotales = totalSpots ?: 0,
    espaciosDisponibles = availableSpots ?: 0,
    espaciosOcupados = occupiedSpots ?: 0,
    espaciosReservados = reservedSpots ?: 0,
    abierto24h = open24Hours ?: false,
    horaApertura = openingTime.aHora(),
    horaCierre = closingTime.aHora(),
    estado = EstadoGaraje.de(status),
    disponibilidad = Disponibilidad.de(availability),
    tarifaPlena = fullDayRate?.roundToInt(),
    tarifaPlenaDesdeHoras = fullDayAfterHours
)

fun GarageSummaryResponse.aModelo() = Garaje(
    id = id,
    nombre = name.orEmpty(),
    direccion = addressLine.orEmpty(),
    ciudad = city.orEmpty(),
    lat = latitude,
    lng = longitude,
    precioPorHora = pricePerHour.aPesos(),
    espaciosTotales = totalSpots ?: 0,
    espaciosDisponibles = availableSpots ?: 0,
    espaciosOcupados = occupiedSpots ?: 0,
    espaciosReservados = reservedSpots ?: 0,
    abierto24h = open24Hours ?: false,
    horaApertura = openingTime.aHora(),
    horaCierre = closingTime.aHora(),
    // /search solo devuelve garajes activos
    estado = EstadoGaraje.ACTIVE,
    disponibilidad = Disponibilidad.de(availability),
    abiertoAhora = openNow,
    distanciaKm = distanceKm
)

fun GarageMarkerResponse.aModelo() = Garaje(
    id = id,
    nombre = name.orEmpty(),
    lat = latitude,
    lng = longitude,
    precioPorHora = pricePerHour.aPesos(),
    espaciosTotales = totalSpots ?: 0,
    espaciosDisponibles = availableSpots ?: 0,
    estado = EstadoGaraje.ACTIVE,
    disponibilidad = Disponibilidad.de(availability),
    abiertoAhora = openNow
)

fun GarageAvailabilityResponse.aModelo() = DisponibilidadGaraje(
    garajeId = garageId,
    estado = EstadoGaraje.de(status),
    disponibilidad = Disponibilidad.de(availability),
    abiertoAhora = openNow ?: false,
    total = totalSpots ?: 0,
    disponibles = availableSpots ?: 0,
    ocupadas = occupiedSpots ?: 0,
    reservadas = reservedSpots ?: 0,
    fueraDeServicio = outOfServiceSpots ?: 0
)

fun ParkingSpotResponse.aModelo() = Plaza(
    id = id,
    codigo = code.orEmpty(),
    piso = floor ?: 1,
    tipo = TipoVehiculo.de(vehicleType),
    estado = EstadoPlaza.de(status)
)

fun GarageDetailResponse.aModelo(): GarajeDetalle {
    val disp = availability
    val garaje = Garaje(
        id = id,
        nombre = name.orEmpty(),
        descripcion = description.orEmpty(),
        telefono = phone.orEmpty(),
        direccion = addressLine.orEmpty(),
        ciudad = city.orEmpty(),
        departamento = state.orEmpty(),
        pais = country.orEmpty(),
        codigoPostal = postalCode.orEmpty(),
        lat = latitude,
        lng = longitude,
        precioPorHora = pricePerHour.aPesos(),
        espaciosTotales = disp?.totalSpots ?: 0,
        espaciosDisponibles = disp?.availableSpots ?: 0,
        espaciosOcupados = disp?.occupiedSpots ?: 0,
        espaciosReservados = disp?.reservedSpots ?: 0,
        abierto24h = open24Hours ?: false,
        horaApertura = openingTime.aHora(),
        horaCierre = closingTime.aHora(),
        estado = EstadoGaraje.de(disp?.status),
        disponibilidad = Disponibilidad.de(disp?.availability),
        abiertoAhora = disp?.openNow,
        tarifaPlena = fullDayRate?.roundToInt(),
        tarifaPlenaDesdeHoras = fullDayAfterHours
    )
    return GarajeDetalle(
        garaje = garaje,
        porTipo = availabilityByVehicleType.orEmpty().map {
            DisponibilidadPorTipo(
                tipo = TipoVehiculo.de(it.vehicleType),
                total = it.totalSpots ?: 0,
                disponibles = it.availableSpots ?: 0,
                ocupadas = it.occupiedSpots ?: 0,
                reservadas = it.reservedSpots ?: 0
            )
        },
        pisos = floors.orEmpty().sorted(),
        plazas = spots.orEmpty().map { it.aModelo() },
        fueraDeServicio = disp?.outOfServiceSpots ?: 0
    )
}

/** Alta: el backend exige ciudad, departamento, país, ubicación y horario. */
fun Garaje.aRequest() = GarageRequest(
    name = nombre.trim(),
    description = descripcion.trim().ifBlank { null },
    phone = telefono.trim().ifBlank { null },
    addressLine = direccion.trim(),
    city = ciudad.trim(),
    state = departamento.trim(),
    country = pais.trim(),
    postalCode = codigoPostal.trim().ifBlank { null },
    latitude = requireNotNull(lat) { "El garaje no tiene ubicación" },
    longitude = requireNotNull(lng) { "El garaje no tiene ubicación" },
    totalSpots = espaciosTotales,
    pricePerHour = precioPorHora.toDouble(),
    open24Hours = abierto24h,
    openingTime = if (abierto24h) null else horaApertura?.aTexto(),
    closingTime = if (abierto24h) null else horaCierre?.aTexto(),
    fullDayRate = tarifaPlena?.toDouble(),
    fullDayAfterHours = tarifaPlenaDesdeHoras,
    motorcycleSpots = cuposMoto
)

/** Edición: se envían todos los campos editables (el backend ignora los que llegan null). */
fun Garaje.aUpdateRequest() = GarageUpdateRequest(
    name = nombre.trim(),
    description = descripcion.trim(),
    phone = telefono.trim().ifBlank { null },
    addressLine = direccion.trim(),
    city = ciudad.trim(),
    state = departamento.trim(),
    country = pais.trim(),
    postalCode = codigoPostal.trim().ifBlank { null },
    latitude = lat,
    longitude = lng,
    totalSpots = espaciosTotales,
    pricePerHour = precioPorHora.toDouble(),
    open24Hours = abierto24h,
    openingTime = if (abierto24h) null else horaApertura?.aTexto(),
    closingTime = if (abierto24h) null else horaCierre?.aTexto(),
    fullDayRate = tarifaPlena?.toDouble(),
    // 0 = quitar la tarifa plena (contrato del backend)
    fullDayAfterHours = if (tarifaPlena == null) 0 else tarifaPlenaDesdeHoras
)

fun ReservationResponse.aModelo(): Reserva? {
    val ini = startTime.aFechaHora() ?: return null
    val fin = endTime.aFechaHora() ?: return null
    return Reserva(
        id = id,
        codigo = code ?: id.take(8).uppercase(),
        garajeId = garageId,
        garajeNombre = garageName.orEmpty(),
        garajeDireccion = garageAddress.orEmpty(),
        plazaCodigo = spotCode,
        tipoVehiculo = TipoVehiculo.de(vehicleType),
        placa = vehiclePlate,
        inicio = ini,
        fin = fin,
        estado = EstadoReserva.de(status),
        precioPorHora = pricePerHour.aPesos(),
        total = totalPrice.aPesos(),
        creadaEn = createdAt.aFechaHora(),
        canceladaEn = cancelledAt.aFechaHora()
    )
}
