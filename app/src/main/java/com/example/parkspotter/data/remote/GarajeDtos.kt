package com.example.parkspotter.data.remote

// Contrato JSON de /api/v1/garages (backend rama develop, Sprint 02 + 03).
// Los campos que el backend puede omitir o enviar null se declaran nullables: Gson no
// respeta la nulabilidad de Kotlin y un null en un campo no-null revienta lejos del origen.
// Horas (LocalTime) llegan como "06:00:00"; fechas (Instant) en ISO-8601; precios como número.

/** Alta de garaje: POST /garages. */
data class GarageRequest(
    val name: String,
    val description: String?,
    val phone: String?,
    val addressLine: String,
    val city: String,
    val state: String,
    val country: String,
    val postalCode: String?,
    val latitude: Double,
    val longitude: Double,
    val totalSpots: Int,
    val pricePerHour: Double,
    val open24Hours: Boolean,
    val openingTime: String?,
    val closingTime: String?,
    val fullDayRate: Double? = null,
    val fullDayAfterHours: Int? = null,
    /** Cuántas de las totalSpots plazas son para moto (el resto, para carro). */
    val motorcycleSpots: Int? = null
)

/** Edición parcial: PUT /garages/{id}. Gson omite los null, así solo viaja lo que cambió. */
data class GarageUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val phone: String? = null,
    val addressLine: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postalCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val totalSpots: Int? = null,
    val pricePerHour: Double? = null,
    val open24Hours: Boolean? = null,
    val openingTime: String? = null,
    val closingTime: String? = null,
    val fullDayRate: Double? = null,
    val fullDayAfterHours: Int? = null
)

data class GarageResponse(
    val id: String,
    val name: String?,
    val description: String?,
    val phone: String?,
    val ownerId: String?,
    val ownerEmail: String?,
    val addressLine: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val postalCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val totalSpots: Int?,
    val availableSpots: Int?,
    val occupiedSpots: Int?,
    val reservedSpots: Int?,
    val outOfServiceSpots: Int?,
    val pricePerHour: Double?,
    val open24Hours: Boolean?,
    val openingTime: String?,
    val closingTime: String?,
    val status: String?,
    val availability: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val fullDayRate: Double? = null,
    val fullDayAfterHours: Int? = null
)

/** Elemento de GET /garages/search. distanceKm solo llega si se envió lat/lng. */
data class GarageSummaryResponse(
    val id: String,
    val name: String?,
    val addressLine: String?,
    val city: String?,
    val latitude: Double?,
    val longitude: Double?,
    val pricePerHour: Double?,
    val totalSpots: Int?,
    val availableSpots: Int?,
    val occupiedSpots: Int?,
    val reservedSpots: Int?,
    val availability: String?,
    val open24Hours: Boolean?,
    val openingTime: String?,
    val closingTime: String?,
    val openNow: Boolean?,
    val distanceKm: Double?
)

data class PageResponse<T>(
    val content: List<T>?,
    val page: Int?,
    val size: Int?,
    val totalElements: Long?,
    val totalPages: Int?,
    val first: Boolean?,
    val last: Boolean?
)

/** GET /garages/map: marcadores livianos del área visible. */
data class GarageMapResponse(
    val markers: List<GarageMarkerResponse>?,
    val count: Int?,
    val truncated: Boolean?
)

data class GarageMarkerResponse(
    val id: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val pricePerHour: Double?,
    val availableSpots: Int?,
    val totalSpots: Int?,
    val availability: String?,
    val openNow: Boolean?
)

/** Foto de disponibilidad: GET /garages/{id}/availability y cada evento SSE "availability". */
data class GarageAvailabilityResponse(
    val garageId: String,
    val status: String?,
    val availability: String?,
    val openNow: Boolean?,
    val totalSpots: Int?,
    val availableSpots: Int?,
    val occupiedSpots: Int?,
    val reservedSpots: Int?,
    val outOfServiceSpots: Int?,
    val timestamp: String?
)

data class VehicleTypeAvailabilityResponse(
    val vehicleType: String?,
    val totalSpots: Int?,
    val availableSpots: Int?,
    val occupiedSpots: Int?,
    val reservedSpots: Int?,
    val outOfServiceSpots: Int?
)

data class ParkingSpotResponse(
    val id: String,
    val garageId: String?,
    val code: String?,
    val floor: Int?,
    val vehicleType: String?,
    val status: String?,
    val updatedAt: String?
)

/** GET /garages/{id}/detail?includeSpots=true */
data class GarageDetailResponse(
    val id: String,
    val name: String?,
    val description: String?,
    val phone: String?,
    val addressLine: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val postalCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val pricePerHour: Double?,
    val open24Hours: Boolean?,
    val openingTime: String?,
    val closingTime: String?,
    val availability: GarageAvailabilityResponse?,
    val availabilityByVehicleType: List<VehicleTypeAvailabilityResponse>?,
    val floors: List<Int>?,
    val spots: List<ParkingSpotResponse>?,
    val createdAt: String?,
    val updatedAt: String?,
    val fullDayRate: Double? = null,
    val fullDayAfterHours: Int? = null
)
