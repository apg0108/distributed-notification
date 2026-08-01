# distributed-notification

Servicio de notificaciones distribuido — prueba técnica Java Senior (Sagant).

## Cómo levantar el proyecto

Desde la raíz del repo:

```bash
docker compose up --build
```

Esto construye la imagen de la app (build multistage con Maven + JDK 17, runtime en JRE 17 Alpine) y levanta dos servicios:

- **`db`** — PostgreSQL, con un healthcheck (`pg_isready`) que la app espera antes de arrancar.
- **`app`** — el servicio Spring Boot. Corre las migraciones de Flyway automáticamente al iniciar (crea la tabla `notifications`).

Cuando el contenedor `app` termina de arrancar, la API queda disponible en:

```
http://localhost:8080/api/v1
```

Probarla:

```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "X-API-KEY: api-key-notifications" \
  -H "Content-Type: application/json" \
  -d '{"recipient":"user@example.com","channel":"LOG","body":"Log1"}'
```

## Precondiciones

Para levantar el proyecto vía Docker Compose (la forma soportada), solo hace falta:

| Herramienta    | Versión mínima                                                                    |
|----------------|-----------------------------------------------------------------------------------|
| Docker Engine  | 24.x o superior                                                                   |
| Docker Compose | v2 (viene integrado con Docker Desktop — `docker compose version` para confirmar) |

## Credenciales de prueba

El endpoint de creación de notificaciones está protegido por API Key. Para probarlo hace falta el header:

```
X-API-KEY: api-key-notifications
```

Ese valor es el default de desarrollo, definido en `docker-compose.yml` (variable de entorno
`API_KEY`) y en `application.properties` (`notification.security.api-key`). **Es solo para esta prueba/demo** — en un entorno real no se hardcodearía
así, sino que vendría de un secreto por-cliente gestionado externamente (ver la sección de Decisiones de diseño).

No se necesitan credenciales de base de datos para usar la API — la app se conecta a Postgres internamente. Si igual querés inspeccionar la base con
un cliente externo, las credenciales son `notifications` / `notifications` contra
`localhost:5434` (con el stack de Docker Compose levantado).
