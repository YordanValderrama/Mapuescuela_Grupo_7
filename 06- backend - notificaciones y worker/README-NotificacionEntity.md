# README — NotificacionEntity.java

## ¿Qué es este código?

`NotificacionEntity` es una **entidad JPA**: una clase Java anotada con `@Entity` que Spring Data JPA mapea automáticamente a una tabla de la base de datos llamada `notificaciones` (definida con `@Table(name = "notificaciones")`). Cada instancia de esta clase representa una fila de esa tabla.

## ¿Para qué sirve dentro del proyecto?

Es el modelo de datos que soporta la decisión de diseño explicada en `NotificacionWorkers`: en vez de enviar un correo real al cliente, cada evento del proceso de venta (pago aprobado, pago rechazado, pago pendiente, envío despachado) se guarda como un registro de este tipo en la base de datos.

## Explicación de los campos

| Campo | Anotación | Propósito |
|---|---|---|
| `id` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | Llave primaria autogenerada por la base de datos (autoincremental). |
| `idPedido` | `@Column(name = "id_pedido")` | Relaciona la notificación con el pedido que la originó (`nPedido` del proceso BPMN). |
| `tipo` | `@Column(name = "tipo")` | Clasifica el evento: `PAGO_APROBADO`, `PAGO_RECHAZADO`, `PAGO_PENDIENTE` o `ENVIO_DESPACHADO`. |
| `destinatario` | `@Column(name = "destinatario")` | Guarda el correo del cliente **solo como referencia** — el comentario deja explícito que no se usa para enviar nada. |
| `asunto` | `@Column(name = "asunto")` | Título corto de la notificación, similar al "asunto" que tendría un correo. |
| `mensaje` | `@Column(name = "mensaje", length = 2000)` | Cuerpo del mensaje. Se limita explícitamente a 2000 caracteres para evitar que la columna crezca sin control. |
| `leida` | `@Column(name = "leida")`, valor por defecto `false` | Permite implementar una bandeja de notificaciones tipo "leídas / no leídas". |
| `fechaCreacion` | `@Column(name = "fecha_creacion")` | Marca temporal de cuándo se generó la notificación. |

Además de los atributos, la clase incluye un constructor vacío (obligatorio para que JPA pueda instanciarla por reflexión) y los métodos `getters`/`setters` estándar de un JavaBean, que JPA usa para leer y escribir cada columna.

## Conceptos clave para explicar al docente

- **Mapeo objeto-relacional (ORM)**: cómo una clase Java se traduce en una tabla SQL sin escribir sentencias `CREATE TABLE` ni `INSERT` a mano.
- **`@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`**: delega en la base de datos la generación del identificador único (equivalente a un `AUTO_INCREMENT`).
- **Uso de un campo booleano como estado simple** (`leida`) en vez de modelar un enum de estados más complejo, adecuado para el alcance de un MVP.
- **Documentar decisiones en el propio modelo**: el comentario sobre `destinatario` dice explícitamente que ese dato no dispara ningún envío, evitando que otro desarrollador asuma que existe una integración de correo.
