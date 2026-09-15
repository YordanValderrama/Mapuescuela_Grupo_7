# mapuescuela-backend (proyecto Spring Boot unificado)

Este es el `application.properties` (y el resto del proyecto que lo rodea)
que no existía en el repositorio. Antes había dos partes sueltas que no
podían compilarse juntas:

- `05- Servicios Web/mapuescuela-service`: un WAR de Jersey puro (sin
  Spring), con `PedidoResource`.
- `06- backend - notificaciones y worker`: clases sueltas (`.java`) que
  usaban anotaciones de Spring (`@Component`, `@Autowired`, `@Value`) y
  del cliente de Flowable, pero sin `pom.xml`, sin clase `main`, sin
  `application.properties` — no era un proyecto ejecutable todavía.

El propio comentario de `BaseWorker` ya asumía que `PedidoResource`
"queda expuesto en la raíz vía JerseyConfig", es decir, que en algún
momento todo iba a vivir junto en una sola aplicación. Este proyecto es
exactamente eso: une ambas partes en **un solo Spring Boot app**.

## Qué contiene

```
mapuescuela-backend/
├── pom.xml
└── src/main/
    ├── java/cl/mapuescuela/
    │   ├── MyResource.java            (sin cambios)
    │   ├── Pedido.java                (sin cambios)
    │   ├── PedidoResource.java        (sin cambios)
    │   └── backend/
    │       ├── MapuescuelaBackendApplication.java   (nuevo)
    │       ├── config/JerseyConfig.java             (nuevo)
    │       ├── notificaciones/  (Entity, Repository, Resource -- sin cambios)
    │       ├── inventario/      (Entity, Repository, Resource -- nuevo, cierra
    │       │                     el TODO que dejó InventarioWorker)
    │       └── worker/
    │           ├── BaseWorker.java           (sin cambios)
    │           ├── NotificacionWorkers.java  (sin cambios)
    │           ├── InventarioWorker.java     (actualizado: ya llama al
    │           │                              endpoint real de inventario)
    │           └── PedidoWorkers.java        (actualizado: al cancelar o
    │                                          rechazar un pedido, también
    │                                          libera el stock reservado)
    └── resources/application.properties      (nuevo)
```

## Cómo correrlo

Requisitos: Java 17 y Maven (o el wrapper `mvnw` si lo agregan).

```bash
cd mapuescuela-backend
mvn spring-boot:run
```

Al levantar, va a exponer en `http://localhost:8081`:

- `GET  /myresource`
- `POST /pedidos/aprobar-pago`, `/pedidos/rechazar-pago`, `/pedidos/liberar-stock`
- `GET  /notificaciones`, `GET /notificaciones/pedido/{idPedido}`
- `GET  /inventario/{producto}`, `POST /inventario/descontar`, `POST /inventario/liberar`
- Consola de la base de datos H2 en `http://localhost:8081/h2-console`
  (JDBC URL: `jdbc:h2:file:./data/mapuescuela`, usuario `sa`, sin clave)

La base de datos es un archivo H2 local (`./data/mapuescuela.mv.db`): no
hay que instalar MySQL/PostgreSQL para la demo. Las tablas
`notificaciones` e `inventario` se crean solas la primera vez que corre
(`spring.jpa.hibernate.ddl-auto=update`).

## Lo único que tienen que completar ustedes: la conexión a Flowable

Buena noticia: como usan **Flowable Trial**, es más simple de lo que parecía.
Según la documentación oficial de Flowable ([External Clients](https://documentation.flowable.com/latest/develop/external-clients) y
[Security — Flowable Access Tokens](https://documentation.flowable.com/latest/develop/be/security#flowable-access-tokens)),
los clientes de External Worker **ya vienen preconfigurados por defecto
para conectarse a Flowable Trial** — no hace falta poner la URL. Lo
único que falta es generar un **Personal Access Token**:

1. Entra a [trial.flowable.com](https://trial.flowable.com) con tu cuenta.
2. Click en tu usuario (abajo a la izquierda) → **Settings**.
3. Pestaña **Access Tokens** → botón **"New token"**.
4. Ponle un nombre (ej. `mapuescuela-backend`) y elige la validez.
5. Copia el valor del token — **solo se muestra una vez**, en ese momento.
6. Pégalo en `application.properties`:

```properties
flowable.external.worker.rest.authentication.bearer.token=<tu-token>
```

Nada más. No hay que tocar `base-url` a menos que en algún momento
cambien a un Flowable local/self-hosted (ahí sí, ver el bloque
comentado en el mismo archivo con usuario/clave).

## Actualización: flujo completo interfaz → Web Service → persistencia

A partir de la retroalimentación de Entrega 2 ("todavía falta avanzar
significativamente en la integración... el usuario interactúa con la
interfaz, esta utiliza los Web Services correspondientes, la información
se persiste..."), se agregó:

- **`PedidoEntity` / `PedidoRepository`**: antes `PedidoResource` solo
  mutaba el DTO `Pedido` en memoria y no quedaba nada guardado. Ahora
  cada pedido se persiste en la tabla `pedidos` (misma base H2 que
  `notificaciones` e `inventario`).
- **`POST /pedidos`** (nuevo): lo llama directamente `pantalla1.html`
  al enviar el formulario. Crea el pedido, le asigna un número
  (`PED-<timestamp>`) y lo persiste con estado `PENDIENTE_PAGO`.
- **`GET /pedidos/{idPedido}`** (nuevo): consulta un pedido persistido.
- **Selección de producto**: se agregó un `<select>` simple (4 productos
  de ejemplo) en `pantalla1.html`, tal como pedía la retroalimentación
  ("una selección mediante un select puede ser suficiente").
- **`CorsFilter`** (nuevo): sin esto, el navegador bloquea las llamadas
  de `pantalla1.html` al backend por política CORS si se abre el HTML
  directo desde el disco o desde otro puerto.
- **`pantalla2.html`** ya no muestra el número de pedido fijo
  `MAP-84920`: lo lee de la URL (que le pasa `pantalla1.html` después de
  crear el pedido real) y también muestra el producto/cantidad elegidos.

El archivo `mapuescuela-frontend-actualizado.zip` (aparte de este) trae
`pantalla1.html` y `pantalla2.html` ya actualizados — reemplacen los que
tienen en `mapuescuela-frontend/frontend/` por estos.

### Cómo probarlo de punta a punta

1. `mvn spring-boot:run` en `mapuescuela-backend` (puerto 8081).
2. Abrir `pantalla1.html` directo en el navegador (doble clic al
   archivo alcanza, gracias al `CorsFilter`).
3. Completar el formulario, elegir un producto y enviar: debería
   redirigir a `pantalla2.html?nPedido=PED-...` con el número real.
4. Confirmar en `http://localhost:8081/h2-console` que apareció la fila
   en la tabla `PEDIDOS`.

### Lo que queda pendiente (y no me arriesgué a inventar)

- **Conectar este pedido con el proceso BPMN real**: falta decidir cómo
  se correlaciona el `idPedido` generado acá con la instancia de
  Flowable que se inicia (manualmente, vía Flowable Work, o llamando a
  la REST API de procesos de Flowable) — necesita las mismas
  credenciales de Flowable que ya quedaron pendientes en el bloque
  `flowable.external.worker.*`.
- **`pantalla3.html`** (panel interno de revisión) sigue sin conectar al
  backend; no la toqué porque no vi en la retroalimentación un pedido
  explícito sobre ella y no quise adivinar cómo debería listar los
  pedidos pendientes sin arriesgar tiempo de ustedes revisando algo que
  no pidieron.
- **Carga real del comprobante** (archivo): sigue siendo solo un mensaje
  de confirmación visual en `pantalla2.html`, como ya estaba.

## Si el build/arranque falla (`exit code: 1` sin más detalle)

Ya corregí una causa probable: la dependencia de Flowable estaba fijada
en la versión `2.0.0` (del 27 de febrero de 2026), que fue publicada
apenas 2 días después de un cambio interno para pasarla a **Spring Boot
4 / Jackson 3**, mientras que este proyecto usa `spring-boot-starter-parent`
**3.3.5** (Spring Boot 3.x). Esa mezcla puede tirar la aplicación abajo
justo al arrancar, con un mensaje final tan genérico como el que suele
salir (`Process terminated with exit code: 1`) sin explicar la causa
real. Ya bajé la dependencia a la versión **`1.0.0`**, que sí corresponde
a esta generación de Spring Boot.

Si vuelve a fallar después de este cambio, el resumen final de Maven
(el bloque `BUILD FAILURE`) casi nunca trae la causa real — está más
arriba, en un bloque que suele decir `APPLICATION FAILED TO START` o
tiene un stack trace de Java con líneas `Caused by:`. Para verlo:

```bash
mvn spring-boot:run > salida.txt 2>&1
grep -n -A 30 "APPLICATION FAILED TO START\|Caused by\|ERROR" salida.txt | head -60
```

y compartan ese bloque si necesitan más ayuda con un error puntual.

## Qué no pude verificar

No tengo acceso a Maven Central desde este entorno, así que no pude
correr `mvn clean install` para confirmar que compila. Revisé la
dependencia y las propiedades de conexión contra la documentación
oficial de Flowable (`flowable/flowable-external-client-java`), pero
igual les recomiendo compilarlo apenas lo bajen para detectar cualquier
detalle de versión antes de la entrega.
