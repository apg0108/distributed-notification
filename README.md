# distributed-notification

Servicio de notificaciones distribuido — prueba técnica Java Senior (Sagant).

## Cómo levantar el proyecto

Desde la raíz del repo:

```bash
docker compose up --build
```

Esto construye la imagen de la app (build multistage con Maven + JDK 17, runtime en JRE 17 Alpine) y levanta tres servicios:

- **`db`** — PostgreSQL, con un healthcheck (`pg_isready`) que la app espera antes de arrancar.
- **`mailhog`** — servidor SMTP de prueba que intercepta los correos del canal `EMAIL`. Su UI web queda en `http://localhost:8025` para ver los emails
  recibidos.
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

Para el canal `EMAIL`, el correo se puede ver en la UI de Mailhog (`http://localhost:8025`):

```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "X-API-KEY: api-key-notifications" \
  -H "Content-Type: application/json" \
  -d '{"recipient":"cliente@example.com","channel":"EMAIL","subject":"Bienvenido","body":"Gracias por registrarte"}'
```

También se puede probar la API desde el navegador con Swagger UI, sin usar `curl`:

```
http://localhost:8080/api/v1/swagger-ui/index.html
```

Ahí mismo hay un botón **Authorize** para cargar el `X-API-KEY` una sola vez y que se aplique a los "Try it out" que se hagan después.

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

## Decisiones de diseño

**Procesamiento asíncrono sin broker.** La creación de una notificación (`POST /notifications`) persiste la fila con `status=PENDING` de forma
síncrona y responde inmediatamente — esa fila en Postgres es la cola. El despacho real ocurre después, publicando un `NotificationCreatedEvent`
que un `@TransactionalEventListener(phase = AFTER_COMMIT)` consume en un `ThreadPoolTaskExecutor`. Se descartó RabbitMQ/Kafka: un broker da
durabilidad y escalado entre procesos que esta prueba no exige a esta escala, y agrega complejidad (contenedor propio, DLQs, consumer groups)
para una ventana de 7 días. Como el evento que dispara el despacho vive solo en memoria, una caída del proceso entre el `save()` y que el listener
llegue a correr dejaría la notificación en `PENDING` para siempre — por eso existe `StuckNotificationRecoveryScheduler`: corre cada
`notification.recovery.fixed-rate-ms` (60s por default) y reencola cualquier notificación en `PENDING` cuyo `created_at` sea más viejo que
`notification.recovery.timeout-ms` (2 minutos por default), usando el mismo camino de despacho (`NotificationProcessingListener.attemptDispatch`).

**Autenticación: API Key estática por header (`X-API-KEY`)**, validada por un `ApiKeyAuthFilter` (`OncePerRequestFilter`) con comparación en tiempo
constante (`MessageDigest.isEqual`). Se descartó JWT/OAuth2/Basic Auth: el endpoint es un punto de integración service-to-service sin identidad de
usuario, sesión, ni necesidad de expiración/refresh.

**Canales de despacho con Strategy pattern: LOG + EMAIL, sin `SERVICE`.** `NotificationSender` es la interfaz de la estrategia;
`LogNotificationSender`
serializa la notificación a JSON y la loguea (con `logstash-logback-encoder` eso se muestra como una línea de log estructurada);
`EmailNotificationSender`
manda un correo real por SMTP (`JavaMailSender`) contra un Mailhog descartable en Docker Compose. Un `NotificationSenderResolver` arma en el
constructor un `Map<NotificationChannel, NotificationSender>` a partir de **todos** los beans `NotificationSender` que Spring detecta — agregar un
canal nuevo, sin tocar el listener que despacha.

**Reintentos: scheduler propio de polling, `FailedNotificationRetryScheduler` corre cada `notification.retry.fixed-rate-ms` (30s por default), busca
notificaciones `FAILED` con
`retry_count < notification.retry.max-attempts` (1 por default), incrementa el contador y vuelve a invocar el mismo camino de despacho. Se prefirió
esto sobre `@Retryable`/`@Recover` porque separa con claridad "el intento inicial" (parte del flujo de creación, corre en el executor async) de "la
recuperación" (un proceso independiente y observable en el tiempo, con su propio intervalo configurable), sin necesitar que Spring AOP proxyee el
método de envío. El costo: se pierde el backoff exponencial y las políticas de reintento más ricas que Spring Retry da.

**Flyway en vez de `ddl-auto=update`.** Migraciones versionadas y revisables (`V1__create_notifications_table.sql`), y deja un schema determinístico.

**Logging estructurado con correlación por MDC.** `logback-spring.xml` usa `LogstashEncoder` para que **toda** la aplicación loguee JSON, no solo el
canal LOG. Al arrancar cada intento de despacho — tanto el disparo inicial en `NotificationProcessingListener` como cada reintento en
`FailedNotificationRetryScheduler` — se hace `MDC.put("notificationId", id)` y se limpia en un `finally`. Esto permite reconstruir el ciclo de vida
completo de una notificación puntual filtrando por ese campo en cualquier herramienta de logs.

**Observabilidad con Actuator.** `/actuator/health`, `/actuator/info` y `/actuator/metrics` están expuestos. Además de las métricas que Spring Boot
registra solo (JVM, pool de Hikari, executor, HTTP), `NotificationProcessingListener` registra dos contadores propios vía `MeterRegistry`:
`notifications.sent` y `notifications.failed`, ambos tageados por `channel` — así se puede ver en `/actuator/metrics/notifications.sent` cuántas
notificaciones se mandaron por `LOG` vs `EMAIL`, sin depender de parsear logs. El trade-off de exponer esa información sin protección queda anotado
abajo.

## Trade-offs y limitaciones

Funcionalidad dejada afuera conscientemente, o implementada de forma más simple de lo ideal, listada sin vueltas:

- **El reintento es un scheduler de polling propio, no Spring Retry.** Intervalo fijo, sin backoff exponencial, un solo reintento por default.
  Suficiente para el alcance de esta prueba, pero menos flexible que una política declarativa si el caso de uso creciera.
- **`EmailNotificationSender` está testeado con `JavaMailSender` mockeado**, no contra GreenMail (SMTP embebido). La verificación de que el correo
  realmente sale con el contenido correcto se hizo a mano contra Mailhog.
- **Sin tabla de API keys en base de datos.** Una sola key estática por variable de entorno alcanza para esta prueba; no hay rotación, no hay keys
  por-cliente, no hay expiración.
- **Canal `SERVICE` (webhook HTTP saliente) fuera de alcance** — LOG + EMAIL ya cubren los escenarios de éxito y de fallo de integración externa que
  se querían probar con la lógica de reintento.
- **Actuator completamente público** (`health`, `info` y `metrics` sin API key). Expone información operativa real (memoria y threads de la JVM, pool
  de conexiones de Hikari, nombres de la cadena de filtros de Spring Security) sin ninguna autenticación. Aceptable para esta prueba; en un despliegue
  real, como mínimo se separaría el puerto de management o se exigiría autenticación para todo lo que no sea `health`.

## Consideración sobre Jakarta EE

- **Packaging**: el JAR autocontenido con Tomcat embebido pasaría a ser un WAR (`<packaging>war</packaging>`, `spring-boot-starter-tomcat` en
  `provided`) extendiendo `SpringBootServletInitializer`, desplegado en un servidor como WildFly — la app deja de ser dueña del ciclo de vida de su
  propio listener HTTP.
- **Datasource**: en vez de que Spring Boot autoconfigure HikariCP desde `application.properties`, el datasource lo definiría y poolearía el servidor
  de aplicaciones (config propia en `standalone.xml`), y la app lo buscaría por JNDI (`java:/jdbc/NotificationDS`) — el connection pooling, las
  credenciales pasan a ser responsabilidad operativa del app server, no de la aplicación.
- **Ejecución asíncrona**: `@Async` con `ThreadPoolTaskExecutor` (usado hoy en `AsyncConfig`/`NotificationProcessingListener`) crea threads no
  gestionados por el contenedor, algo que la especificación EE Concurrency desaconseja explícitamente en un app server. Se reemplazaría por un
  `ManagedExecutorService` inyectado con `@Resource`, o, de forma más idiomática a EE, empujando la notificación a una cola JMS consumida por un
  Message-Driven Bean — el equivalente EE genuino de lo que la fila en Postgres aproxima en este ejercicio. Lo mismo aplica al `@Scheduled` de
  `FailedNotificationRetryScheduler`, que pasaría a ser un timer EE (`@Schedule` de EJB) o un job disparado por el propio contenedor.
- **Seguridad**: el `ApiKeyAuthFilter` (`OncePerRequestFilter`) + cadena de Spring Security se reemplazaría por Jakarta EE Security
  (`jakarta.security.enterprise`) con un `HttpAuthenticationMechanism` custom validando la API key, respaldado por un `IdentityStore`, integrando la
  autenticación con el realm de seguridad del app server en vez de con el `SecurityContextHolder` de Spring.
- **Observabilidad**: los endpoints HTTP de Actuator perderían protagonismo; la consola de administración/CLI del app server y las MBeans JMX pasarían
  a ser la superficie operativa principal, con métricas exportadas vía el subsistema de monitoreo propio del servidor o un puente JMX-a-Micrometer en
  vez de `/actuator`.
