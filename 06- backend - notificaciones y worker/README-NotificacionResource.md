# README — NotificacionResource.java

## ¿Qué es este código?

`NotificacionResource` es un **recurso REST** implementado con JAX-RS (`jakarta.ws.rs`), similar en espíritu a `PedidoResource` del resto del proyecto, pero enfocado en exponer las notificaciones guardadas por `NotificacionWorkers`. Está anotado como `@Path("notificaciones")`, por lo que todos sus endpoints quedan bajo `/notificaciones`, y como `@Component` para que Spring lo administre e inyecte sus dependencias.

## ¿Qué problema resuelve?

Las notificaciones se generan "dentro del proceso" (cuando Flowable ejecuta un worker), pero alguien tiene que poder **consultarlas** después, por ejemplo desde una pantalla de "Mis notificaciones" del cliente o un panel del voluntario. Ese "alguien" (frontend, Postman, otra app) necesita una API REST, y ese es exactamente el rol de esta clase.

## Explicación de cada endpoint

### 1. `GET /notificaciones` → `listarNoLeidas()`
```java
@GET
public List<NotificacionEntity> listarNoLeidas() {
    return notificacionRepository.findByLeidaFalseOrderByFechaCreacionDesc();
}
```
Devuelve todas las notificaciones que aún no han sido marcadas como leídas, de la más reciente a la más antigua. Es el endpoint pensado para una "bandeja de entrada" o un contador de notificaciones pendientes.

### 2. `GET /notificaciones/pedido/{idPedido}` → `listarPorPedido(...)`
```java
@GET
@Path("pedido/{idPedido}")
public List<NotificacionEntity> listarPorPedido(@PathParam("idPedido") String idPedido) {
    return notificacionRepository.findByIdPedidoOrderByFechaCreacionDesc(idPedido);
}
```
Devuelve el historial completo de notificaciones asociadas a un pedido específico, usando `idPedido` como parámetro de ruta (`@PathParam`). Útil, por ejemplo, para mostrar toda la trazabilidad de un pedido puntual: pago pendiente → pago aprobado → envío despachado.

### 3. `POST /notificaciones/{id}/marcar-leida` → `marcarLeida(...)`
```java
@POST
@Path("{id}/marcar-leida")
public Response marcarLeida(@PathParam("id") Long id) {
    return notificacionRepository.findById(id)
            .map(n -> {
                n.setLeida(true);
                notificacionRepository.save(n);
                return Response.ok(n).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
}
```
Marca una notificación puntual como leída, identificándola por su `id`. Usa `Optional` (que retorna `findById`) junto con `.map()` y `.orElse()` para manejar de forma explícita ambos caminos posibles:
- Si la notificación existe: la actualiza (`leida = true`), la guarda y responde `200 OK` con la notificación actualizada.
- Si no existe: responde `404 Not Found`, en vez de lanzar una excepción sin control o devolver un cuerpo vacío ambiguo.

## Conceptos clave para explicar al docente

- **JAX-RS como estilo alternativo a Spring MVC** dentro del mismo proyecto: se usan anotaciones `@Path`, `@GET`, `@POST`, `@PathParam` (JAX-RS) en vez de `@RestController`, `@GetMapping`, etc. (Spring MVC), aunque la inyección de dependencias siga siendo de Spring (`@Autowired`).
- **Códigos de estado HTTP explícitos**: uso correcto de `200 OK` vs `404 Not Found` según exista o no el recurso solicitado.
- **Manejo funcional de `Optional`** (`.map(...).orElse(...)`) como alternativa más segura y legible a comprobar `if (resultado != null)`.
- **Diseño de API centrado en el consumidor**: los tres endpoints responden a necesidades reales de una interfaz de notificaciones (ver pendientes, ver historial por pedido, marcar como leída), no son simplemente un CRUD genérico.
