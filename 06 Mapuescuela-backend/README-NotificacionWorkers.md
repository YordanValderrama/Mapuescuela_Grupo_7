# README — NotificacionWorkers.java

## ¿Qué es este código?

`NotificacionWorkers` agrupa los External Workers responsables de **notificar al cliente** en distintos puntos del proceso de venta (pago rechazado, pago aprobado, pago pendiente de validar, y despacho del pedido). A diferencia de `PedidoWorkers`, ninguno de estos workers modifica el estado del pedido; solo dejan un registro de la notificación.

## Decisión de diseño importante: no se envía correo

El comentario de la clase explica que se eliminó la dependencia de envío de correo por SMTP (Gmail), por la fragilidad de red observada (timeouts, puertos bloqueados). En su lugar, cada notificación se guarda como una fila en la tabla `notificaciones` mediante `NotificacionRepository`, y queda disponible para consultarse a través de `NotificacionResource` (`GET /notificaciones` y `GET /notificaciones/pedido/{idPedido}`). Es decir, la "notificación" hoy es un registro interno de la aplicación, no un correo real enviado al cliente.

## Explicación de cada worker (topic)

### 1. `notificarRechazo` — topic `notificar-rechazo`
Arma un mensaje informando que el pago fue rechazado, incluyendo el motivo (`estadoDelPago`) si está disponible, y lo guarda como notificación de tipo `PAGO_RECHAZADO`.

### 2. `notificarPagoAprobado` — topic `notificar-pago-aprobado`
Genera un mensaje de agradecimiento confirmando que el pago fue aprobado, y lo guarda como notificación de tipo `PAGO_APROBADO`.

### 3. `notificarPendiente` — topic `notificar-pago-pendiente`
Se usa cuando el comprobante de pago no pudo validarse; pide al cliente que lo reenvíe, incluyendo el detalle del motivo si existe. Se guarda como tipo `PAGO_PENDIENTE`.

### 4. `notificarEnvio` — topic `notificar-envio-cliente`
Informa que el pedido fue despachado, incluyendo la empresa de transporte (`empresa`) y el número de seguimiento (`nDeSeguimiento`). Se guarda como tipo `ENVIO_DESPACHADO`.

### 5. `guardarNotificacion(...)` — método privado de utilidad
Todos los métodos anteriores terminan llamando a este método común, que arma una `NotificacionEntity`, la completa con `idPedido`, `tipo`, `destinatario`, `asunto`, `mensaje` y la fecha actual, y la persiste con `notificacionRepository.save(...)`.

```java
try {
    ...
    notificacionRepository.save(notificacion);
} catch (Exception e) {
    log.error("No se pudo guardar la notificación [{}]...", tipo, idPedido, e.getMessage(), e);
}
```

El `try/catch` es intencional: si falla el guardado de la notificación, **no se relanza la excepción**. La razón (documentada en el comentario) es que un fallo al notificar no debería marcar el job de Flowable como fallido ni bloquear el resto del proceso de venta — se mantiene el mismo criterio que se aplicaba antes cuando se intentaba enviar el correo.

## Conceptos clave para explicar al docente

- **Separación de responsabilidades**: `NotificacionWorkers` solo informa/registra eventos; no toca el estado del pedido (eso es responsabilidad exclusiva de `PedidoWorkers`).
- **Tolerancia a fallos en tareas secundarias**: capturar la excepción en vez de propagarla evita que un problema al notificar detenga un proceso de negocio más importante (la venta).
- **Persistencia con Spring Data**: uso de `@Autowired NotificacionRepository` para guardar entidades sin escribir SQL manualmente.
- **Trazabilidad**: al guardar cada notificación en base de datos en vez de solo enviarla y olvidarla, queda un historial consultable vía API REST.
