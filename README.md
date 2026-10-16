# ParkSpotter — App Android

**Encuentra tu espacio.** Aplicación Android (Kotlin + Jetpack Compose) para encontrar, reservar y
publicar parqueaderos —incluidos garajes en desuso— en **Bogotá, Colombia**.

Proyecto de la **Práctica IV de Ingeniería de Sistemas — Universidad Central**.
Backend: [ParkSpotter-BackEnd](https://github.com/Circunvalar/ParkSpotter-BackEnd) (Spring Boot, rama `develop`).

---

## Funcionalidades por sprint

| Sprint | Objetivo | Estado en la app |
|---|---|---|
| 1 — Autenticación y roles | Registro, login, sesión persistente, perfil | ✅ JWT + refresh automático, tokens cifrados, cambio de contraseña, modo conductor/propietario |
| 2 — Gestión de garajes | Publicar y administrar garajes | ✅ Crear/editar/eliminar, publicar/ocultar, ocupar/liberar plazas |
| 3 — Búsqueda y mapa en tiempo real | Encontrar parqueadero cerca | ✅ Mapa con carga por área visible, lista, filtros, detalle con plazas por piso, disponibilidad en vivo (SSE) |
| 4 — Reservas | Reservar, confirmar, cancelar, historial | ✅ Flujo en 3 pasos, validaciones, plaza asignada, historial y cancelación contra `/api/v1/reservations` (backend rama `Sprint-04-Reservas`, [contrato](docs/API-Sprint-04-Reservas.md)) |
| 5 — Pagos y notificaciones | — | Pendiente |
| 6 — Calidad y calificaciones | — | Pendiente |

## Tecnologías

Kotlin · Jetpack Compose (Material 3) · Navigation Compose · ViewModel + StateFlow · Retrofit/OkHttp
· OkHttp SSE · MapLibre (OpenFreeMap) · Play Services Location · EncryptedSharedPreferences
· JUnit + Robolectric.

## Cómo correrla

1. **Backend** (`develop`): ver su README. Para desarrollo sin tocar la base compartida se puede
   levantar con H2 en memoria:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.useTestClasspath=true "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:mem:parkspotter_dev;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect --spring.jpa.hibernate.ddl-auto=create-drop"
   ```
2. **App**: abrir en Android Studio y ejecutar.
   - En el **emulador** la app usa `http://10.0.2.2:8080/api/v1/` (el localhost del PC).
   - En un **celular físico** agregar en `local.properties` (no se sube a Git):
     ```properties
     parkspotter.apiUrl=http://192.168.1.15:8080/api/v1/
     ```
     y esa IP en `app/src/main/res/xml/network_security_config.xml`.

## Pruebas

```bash
./gradlew :app:testDebugUnitTest
```

- `ReglasReservaTest`, `MapeosTest`: reglas de negocio y traducción del JSON del backend.
- `ContratoBackendTest`: recorre el flujo completo contra un backend real (registro → garaje →
  búsqueda → mapa → detalle → evento SSE → edición). Se activa con
  `PARKSPOTTER_API=http://localhost:8080/api/v1/`.
- `CapturasPantallasTest`: renderiza las pantallas reales con Robolectric y guarda PNG (sin
  emulador). Se activa con `PARKSPOTTER_API` y `PARKSPOTTER_SHOTS=<carpeta>`.

## Estructura

```text
app/src/main/java/com/example/parkspotter/
├── MainActivity.kt
├── data/
│   ├── AppContainer.kt        # dependencias compartidas
│   ├── remote/                # Retrofit (Auth, Garaje, Reserva), DTOs, mapeos, SSE
│   ├── local/                 # perfil y reservas guardadas en el dispositivo
│   ├── repository/            # Auth, Garaje, Reserva (remoto/local)
│   └── session/               # tokens cifrados
├── model/                     # Garaje, Plaza, Reserva, FiltrosBusqueda…
├── viewmodel/                 # Auth, Busqueda, DetalleGaraje, Garaje, Reservar, MisReservas
├── screens/                   # Splash, Login, Registro, Mapa, Detalle, Reservar, MisReservas, MisGarajes, RegistrarGaraje, Perfil
├── navigation/NavGraph.kt
├── ui/components/             # Logo y marca, píldoras de estado, barra inferior…
├── ui/theme/                  # Paleta e identidad visual
└── util/                      # Reglas de reserva, validaciones, formatos, ubicación
```

## Identidad visual

- Azul `#0B2D5B` (confianza, seguridad, tecnología) como color principal.
- Verde `#00C896` (disponibilidad, movilidad) como acento; `#00875F` para texto y botones con
  texto blanco (contraste WCAG AA).
- Gris claro `#F4F7FA` para fondos.
- Símbolo: **P + vehículo + plaza de parqueo** (`ui/components/Marca.kt` y el ícono adaptativo).
- Tipografía sans serif geométrica (Montserrat en el manual; ver nota en `ui/theme/Type.kt`).

## Equipo

| Integrante | Rol |
|---|---|
| Daniel Alejandro Andrade | Backend (API) |
| David Santiago Carrillo Salamanca | App Android |
| Erika Mailyn Quitiano Andrade | Plataforma web y diseño |

**Docente:** Angie Lorena Aldana Padilla · Universidad Central · Bogotá, Colombia
