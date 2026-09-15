# README — InventarioWorker.java

## ¿Qué es este código?

`InventarioWorker` es otro External Worker de Flowable, encargado del paso del proceso BPMN relacionado con la actualización de inventario (topic `actualizar-inventario`). Igual que `PedidoWorkers`, hereda de `BaseWorker`, aunque en este caso **todavía no usa** sus métodos de llamada HTTP, porque el endpoint correspondiente no existe.

## ¿Qué hace realmente hoy?

```java
@FlowableWorker(topic = "actualizar-inventario")
public void actualizarInventario(AcquiredExternalWorkerJob job) {
    log.warn("actualizar-inventario: sin endpoint de inventario todavía (MVP sin BD)...");
}
```

El worker se registra correctamente en el motor de Flowable y **reclama** los jobs del topic `actualizar-inventario` para que el proceso BPMN no quede bloqueado esperando una tarea que nadie atiende. Pero, en lugar de descontar stock de verdad, solo deja un `log.warn(...)` indicando que el descuento real es un `TODO` pendiente.

Esto es una decisión de diseño explícita, no un olvido: el proyecto está en etapa de MVP y todavía no existe ni una base de datos de inventario ni un `InventarioResource` (el equivalente a `PedidoResource` pero para stock).

## Historial de corrección

El comentario de la clase documenta que antes se había agregado por error una llamada a `RuntimeService.startProcessInstanceByKey("actualizarInventarioProcess", ...)`. Esto estaba mal por dos razones:

1. Conceptualmente: esta tarea es un paso *dentro* del proceso de venta que ya está en ejecución, no el disparador de un proceso nuevo.
2. Prácticamente: el proceso `"actualizarInventarioProcess"` **ni siquiera existe** definido en el archivo BPMN, así que esa llamada habría fallado en tiempo de ejecución.

También se corrigió un detalle menor: el log usaba `job.getProcessInstanceId()` cuando lo correcto era `job.getId()` (el identificador del job específico, no de la instancia completa del proceso).

## Conceptos clave para explicar al docente

- **Job placeholder / stub**: un worker puede registrarse y "completar" un job sin ejecutar la lógica de negocio real todavía, para no dejar el proceso BPMN bloqueado mientras se desarrolla el resto del sistema.
- **TODO documentado como decisión consciente**: es una buena práctica dejar explícito en el código y en comentarios qué falta y por qué, en vez de simular una funcionalidad que no existe.
- **Importancia de que el nombre del proceso citado exista en el BPMN**: llamar a un proceso inexistente por nombre solo falla en tiempo de ejecución, no en compilación — por eso es un error fácil de introducir y difícil de detectar sin pruebas.
- **Uso correcto de identificadores del job** (`job.getId()`) versus identificadores de la instancia del proceso (`job.getProcessInstanceId()`) al momento de loguear o depurar.
