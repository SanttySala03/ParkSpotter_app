# Sprint 04 — Reservas: contrato con el backend

> Implementado en **ParkSpotter-BackEnd**, rama `Sprint-04-Reservas` (guía oficial:
> `docs/API-Sprint-04-Reservas.md` del backend). Si la app habla con un backend que aún no tiene el
> módulo (`GET /api/v1/reservations/mine` responde **404**), trabaja en *modo demostración* y guarda
> las reservas en el celular.

## Modelo

| Enum | Valores |
|---|---|
| `ReservationStatus` | `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED` |
| `VehicleType` | el mismo de plazas: `CAR`, `MOTORCYCLE`, `BICYCLE`, `TRUCK`, `ELECTRIC`, `DISABLED` |

Una reserva **asigna una plaza** (`ParkingSpot`) libre del tipo pedido. Al confirmarse, la plaza
pasa a `RESERVED` (lo que ya dispara el evento SSE de disponibilidad del Sprint 3); al cancelarse
o completarse vuelve a `AVAILABLE`.

## Endpoints (todos requieren token)

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| POST | `/reservations` | Crear reserva | `201` + `ReservationResponse` |
| GET | `/reservations/mine?status=&page=&size=` | Reservas del usuario (más recientes primero) | `PageResponse<ReservationResponse>` |
| GET | `/reservations/{id}` | Detalle (solo dueño de la reserva, dueño del garaje o admin) | `ReservationResponse` |
| PATCH | `/reservations/{id}/cancel` | Cancelar (solo antes de `startTime`) | `ReservationResponse` |
| GET | `/garages/{garageId}/reservations?date=` | *(Opcional)* reservas de un garaje para su dueño | `PageResponse<ReservationResponse>` |

### `POST /reservations`

```json
{
  "garageId": "0e16c9b4-cc7c-44cd-ab06-0df3f9922998",
  "vehicleType": "CAR",
  "vehiclePlate": "KDZ482",
  "startTime": "2026-10-17T08:00:00-05:00",
  "endTime": "2026-10-17T10:00:00-05:00"
}
```

### `ReservationResponse`

```json
{
  "id": "…",
  "code": "PS-FE382E",
  "garageId": "…",
  "garageName": "Parqueadero Chapinero 60",
  "garageAddress": "Cra 7 # 60-15",
  "spotId": "…",
  "spotCode": "P-004",
  "vehicleType": "CAR",
  "vehiclePlate": "KDZ482",
  "startTime": "2026-10-17T08:00:00-05:00",
  "endTime": "2026-10-17T10:00:00-05:00",
  "status": "CONFIRMED",
  "pricePerHour": 5000,
  "totalPrice": 10000,
  "createdAt": "2026-10-16T23:10:00Z",
  "cancelledAt": null
}
```

## Reglas de negocio (la app ya las valida antes de enviar; el backend es la fuente de verdad)

1. `startTime` no puede estar en el pasado (tolerancia sugerida: 10 min) ni a más de **7 días**.
2. Duración entre **1 y 12 horas**. Se cobra cada fracción de hora completa:
   `totalPrice = pricePerHour × ceil(minutos / 60)`.
   **Tarifa plena:** si el garaje la tiene (`fullDayRate` y `fullDayAfterHours` en `/garages`, se envían
   juntos; `fullDayAfterHours: 0` en el PUT la quita), desde esa hora se cobra como máximo `fullDayRate`.
   Ej.: $4.000/h con plena de $15.000 desde 4 h → 3 h = $12.000, 5 h = $15.000.
3. Si el garaje no abre 24 h, el intervalo debe caber en su horario (soportar horarios nocturnos,
   p. ej. 20:00–06:00, igual que `OpeningHours`).
4. El garaje debe estar `ACTIVE` y tener una plaza libre del `vehicleType` **sin otra reserva que
   se cruce** en ese intervalo.
5. Un usuario no puede tener dos reservas activas que se crucen.
6. `vehiclePlate` obligatoria salvo `BICYCLE`; formato `^[A-Z]{3}[0-9]{2}[0-9A-Z]$` (carro ABC123, moto ABC12D).
7. **Concurrencia:** asignar la plaza con `SELECT … FOR UPDATE` (como ya hace `ParkingSpotService`)
   dentro de una transacción atómica, para que dos conductores no reserven la misma plaza.
8. **Cambio automático de estado:** tarea programada (`@Scheduled`) que pase a `COMPLETED` las
   reservas `CONFIRMED` cuyo `endTime` ya pasó y libere la plaza. (La app ya muestra como
   "Completada" una confirmada vencida, por si la tarea aún no corrió.)

## Errores (`ApiError`)

| Código | Cuándo | Mensaje que mostrará la app |
|---|---|---|
| 400 | Fechas inválidas, duración fuera de rango, placa inválida | `details` por campo (`startTime`, `endTime`, `vehiclePlate`) |
| 401 | Token vencido | La app renueva con `/auth/refresh` y reintenta |
| 403 | Ver/cancelar una reserva ajena | "No tienes permiso…" |
| 404 | Garaje o reserva inexistente | `message` |
| 409 | Sin plazas libres en ese horario, solapamiento, o cancelar una reserva ya iniciada | "Ya no hay cupo en ese horario: elige otro" |

## Dónde está en la app

- `data/remote/ReservaApi.kt` — interfaz Retrofit y DTOs de este contrato.
- `data/repository/ReservaRepository.kt` — detecta el 404 y alterna entre remoto y local.
- `util/ReglasReserva.kt` — reglas 1 a 6 (con pruebas en `ReglasReservaTest`).
