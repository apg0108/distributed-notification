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

**Reintentos**: scheduler propio de polling, `FailedNotificationRetryScheduler` corre cada `notification.retry.fixed-rate-ms` (30s por default), busca
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

Funcionalidad dejada afuera conscientemente, o implementada de forma más simple de lo ideal:

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

Este servicio está pensado como una aplicación Spring Boot autocontenida: trae su propio Tomcat, arma su propio pool de conexiones, crea sus propios
threads y decide sola cómo autenticar y cómo exponerse. Si hubiera que desplegarlo en un servidor de aplicaciones como WildFly, la premisa se
invierte: el contenedor pasa a ser el dueño del ciclo de vida, de los recursos y de la seguridad, y varias decisiones de diseño de este proyecto
habría que replanteárselas.

**El empaquetado.** Hoy el build produce un JAR ejecutable con Tomcat embebido. En WildFly eso no tiene sentido — el servidor ya trae su propio
listener HTTP — así que el proyecto pasaría a empaquetarse como WAR (`<packaging>war</packaging>`, con `spring-boot-starter-tomcat` en scope
`provided`) y la clase principal extendería `SpringBootServletInitializer`. Es un cambio chico en código pero grande en filosofía: la app deja de
arrancarse a sí misma y pasa a ser algo que el servidor despliega.

**La conexión a base de datos.** Ahora mismo Spring Boot autoconfigura HikariCP leyendo `application.properties`, y las credenciales viajan con la
app. En un app server lo natural es al revés: el datasource se define en la configuración de WildFly (`standalone.xml`), el servidor se encarga del
pooling, y la aplicación solo lo pide por nombre JNDI (algo como `java:/jdbc/NotificationDS`). La ganancia es operativa: las credenciales y el tuning
del pool los administra quien opera el servidor, sin recompilar ni tocar la app.

**El procesamiento asíncrono — el cambio más profundo.** El corazón de este diseño es un `@Async` sobre un `ThreadPoolTaskExecutor` propio
(`AsyncConfig`), y eso es justo lo que un servidor de aplicaciones no quiere: threads que el contenedor no conoce ni gestiona, algo que la
especificación de EE Concurrency desaconseja explícitamente. La versión mínima del cambio es reemplazar el executor por un `ManagedExecutorService`
inyectado con `@Resource`, para que los threads los preste el contenedor. Pero la versión más idiomática sería empujar la notificación a una cola JMS
y consumirla con un Message-Driven Bean — que es, en el fondo, la versión "de verdad" de lo que la fila `PENDING` en Postgres aproxima en este
ejercicio. Lo mismo corre para los schedulers de reintento y recuperación: `@Scheduled` crea threads propios, así que pasarían a ser timers del
contenedor (`@Schedule` de EJB).

**La seguridad.** El `ApiKeyAuthFilter` vive dentro de la cadena de filtros de Spring Security y deposita la autenticación en el
`SecurityContextHolder`. En Jakarta EE el equivalente es un `HttpAuthenticationMechanism` custom (de `jakarta.security.enterprise`) que valide la API
key contra un `IdentityStore`, integrándose con el realm de seguridad del propio servidor. La lógica de validación sería la misma; lo que cambia es
con quién se integra.

**La observabilidad.** Los endpoints de Actuator perderían protagonismo: en el mundo WildFly la superficie operativa habitual es la consola de
administración, el CLI y las MBeans de JMX. Las métricas custom (`notifications.sent` / `notifications.failed`) habría que exportarlas por esa vía, o
mantener un puente entre JMX y Micrometer en lugar de depender de `/actuator`.

En resumen: la lógica de negocio (el Strategy de canales, el modelo de estados, los reintentos como concepto) sobreviviría intacta. Lo que cambia es
todo lo que hoy la app hace por sí misma — servir HTTP, poolear conexiones, crear threads, autenticar, exponerse — porque en un app server esas
responsabilidades son del contenedor.
