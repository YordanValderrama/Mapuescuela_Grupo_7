# README — BaseWorker.java

## ¿Qué es este código?

`BaseWorker` es una clase **abstracta** que actúa como la base común de todos los External Workers del proyecto (`PedidoWorkers`, `InventarioWorker` y `NotificacionWorkers`). No implementa ningún topic de Flowable por sí misma: su función es evitar que las demás clases repitan código.

## ¿Por qué existe?

Los tres workers reales necesitan, una y otra vez, dos cosas:

1. Llamar por HTTP al propio servicio REST (`PedidoResource`) para registrar cambios de estado del pedido.
2. Convertir las variables del proceso BPMN (que llegan como un `Map<String, Object>` genérico) en un objeto `Pedido` tipado.

En vez de escribir ese código en cada worker, se sube a `BaseWorker` y las demás clases lo heredan con `extends BaseWorker`. Esto sigue el principio de **no repetir código (DRY)**.

## Explicación por partes

### 1. `baseUrl()`
```java
protected String baseUrl() {
    return "http://localhost:" + puertoLocal;
}
```
Arma la URL base del propio servicio, usando el puerto (`server.port`) que Spring inyecta automáticamente con `@Value`. Es clave notar que `PedidoResource` **no** usa `@ApplicationPath`, por lo tanto queda expuesto en la raíz (`http://localhost:<puerto>/pedidos/...`) y no bajo un prefijo como `/api/rest`. Si alguien agrega ese prefijo por error, todas las llamadas fallan con 404.

### 2. `post(String path, Object body)`
```java
protected Map<String, Object> post(String path, Object body) {
    ...
    Map<?, ?> respuesta = restTemplate.postForObject(url, request, Map.class);
    return (Map<String, Object>) respuesta;
}
```
Encapsula una petición `POST` genérica usando `RestTemplate` de Spring. Arma los headers con `Content-Type: application/json`, envía el `body` (normalmente un `Pedido`) y devuelve la respuesta como un `Map`. Todos los workers que necesitan hablar con `PedidoResource` usan este método en vez de instanciar su propio `RestTemplate`.

### 3. `construirPedido(Map<String, Object> vars)`
Traduce las variables que llegan desde el proceso BPMN (formulario de Flowable) al DTO real `Pedido`:

| Variable del proceso | Campo de `Pedido` |
|---|---|
| `nPedido` | `idPedido` |
| `Nombrecompleto` | `nombreCliente` |
| `correoElectronico` | `correoCliente` |
| `seleccioneModalidadDeEntrega` | `modalidadEntrega` (mapeado) |

`producto` y `cantidad` quedan con un valor fijo de prueba (`PRODUCTO_PRUEBA`, `1`) porque el modelo de carrito de compras real todavía no está definido — es una limitación reconocida del MVP, no un error.

### 4. `mapModoReparto(Object valorFormulario)`
Traduce el valor textual que llega del formulario (`"reparto_domicilio"`) a uno de los dos valores válidos del dominio: `DOMICILIO` o `RETIRO_TIENDA`. Si el valor es `null`, asume `RETIRO_TIENDA` por defecto.

### 5. `str(Object valor)`
Método de utilidad para convertir cualquier valor a `String` de forma segura, evitando `NullPointerException` cuando el valor es `null`.

## Conceptos clave para explicar al docente

- **Clase abstracta como base de herencia**: `BaseWorker` nunca se instancia directamente; solo sirve para que otras clases hereden comportamiento común.
- **Reutilización de código (DRY)**: centraliza la lógica de comunicación HTTP y de mapeo de datos.
- **Acoplamiento con la configuración de Spring**: usa `@Value("${server.port}")`, es decir, depende de `application.properties` para saber en qué puerto está corriendo el propio servicio.
- **Adaptador/Mapper**: `construirPedido` cumple el rol de traducir entre dos representaciones de datos distintas (variables de proceso ↔ DTO de dominio).
