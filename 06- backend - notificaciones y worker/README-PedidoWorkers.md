# README — PedidoWorkers.java

## ¿Qué es este código?

`PedidoWorkers` es un **External Worker de Flowable**: una clase Spring (`@Component`) que se conecta al motor de procesos BPMN y "reclama" (claim) trabajos pendientes identificados por un **topic**. Cada método anotado con `@FlowableWorker(topic = "...")` corresponde a una Service Task del diagrama BPMN de Mapuescuela cuyo tipo es `flowable:type="external-worker"`.

## ¿Qué problema resuelve?

En el proceso de venta hay pasos que el motor de Flowable no puede ejecutar por sí solo (por ejemplo: generar un número de pedido, o llamar a la API REST para guardar en la base de datos que un pago fue aprobado). Para esos pasos, el BPMN no ejecuta código Java directamente; en cambio, publica un "job" a un topic y espera que un **cliente externo** lo reclame, lo procese y le devuelva el resultado. `PedidoWorkers` es ese cliente externo.

## Historial de corrección (importante para la exposición)

El comentario de la clase deja constancia de dos errores previos que se corrigieron:

1. Alguien había migrado por error la lógica a `RuntimeService.startProcessInstanceByKey(...)`, como si cada Service Task debiera **arrancar un proceso nuevo**. Eso es conceptualmente incorrecto: una External Worker Task es un paso *dentro* de un proceso que ya está corriendo, no un disparador de un proceso distinto.
2. El archivo declaraba `class PedidoWorker` (sin la "s" final), lo cual no compila en Java, porque el nombre de la clase pública debe coincidir exactamente con el nombre del archivo (`PedidoWorkers.java` → `class PedidoWorkers`).

## Explicación de cada worker (topic)

### 1. `generarNumeroPedido` — topic `generar-numero-pedido`
```java
String numeroPedido = "PED-" + Instant.now().toEpochMilli();
return resultBuilder.success().variable("nPedido", numeroPedido);
```
No llama a ningún servicio externo: genera localmente un número de pedido único usando el timestamp actual (`Instant.now().toEpochMilli()`), y lo devuelve como variable de proceso `nPedido` usando `WorkerResultBuilder`. Esta variable queda disponible para el resto del proceso (por ejemplo, para las notificaciones).

### 2. `registrarPago` — topic `registrar-pago`
Arma un `Pedido` con `construirPedido(...)` (heredado de `BaseWorker`) y hace `POST /pedidos/aprobar-pago`. Corresponde al paso del proceso donde, una vez validado el pago, se persiste en la base de datos que el pedido quedó pagado/aprobado.

### 3. `cancelarPedidoVencido` — topic `cancelar-pedido-vencido`
Se dispara cuando un **timer de 24 horas** vence sin que el cliente complete el pago. Llama a `POST /pedidos/liberar-stock` para devolver al inventario el stock que se había reservado para ese pedido.

### 4. `anularPedidoRechazado` — topic `anular-pedido-rechazado`
Se ejecuta cuando el pago es explícitamente rechazado. Llama a `POST /pedidos/rechazar-pago` para marcar en la base de datos que el pago fue rechazado.

## Conceptos clave para explicar al docente

- **Patrón External Worker (Flowable)**: el motor BPMN delega la ejecución real a un cliente externo que se suscribe a un topic (desacopla el motor de procesos del código de negocio).
- **Diferencia entre "iniciar un proceso" y "avanzar un paso de un proceso en curso"**: error conceptual que quedó documentado en el propio código como lección aprendida.
- **Convención de nombres en Java**: el nombre de la clase pública debe ser idéntico al nombre del archivo `.java`.
- **Variables de proceso**: cómo un worker puede tanto leer variables (`job.getVariables()`) como escribir nuevas variables de vuelta al proceso (`resultBuilder.variable(...)`).
- **Reutilización mediante herencia**: todos los métodos usan `construirPedido(...)` y `post(...)` heredados de `BaseWorker`, evitando duplicar la lógica de mapeo y de llamada HTTP.
