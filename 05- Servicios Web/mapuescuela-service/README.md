# Web Services REST — Mapuescuela

## Descripción

Servicio REST desarrollado en Java con Jersey y Maven para apoyar la automatización del proceso de pedidos de Mapuescuela.

Los servicios permiten procesar tres situaciones del proceso BPMN:

* Aprobar un pago y actualizar el inventario.
* Rechazar un pago.
* Cancelar un pedido y liberar el stock después de 24 horas sin comprobante.

## Tecnologías utilizadas

* Java
* Maven
* Jersey REST
* JSON
* Apache Tomcat 10.1
* PowerShell para las pruebas

## Compilación

Desde la carpeta `mapuescuela-service`, ejecutar:

```powershell
mvn clean package
```

El archivo generado queda en:

```text
target/mapuescuela-service.war
```

## Despliegue en Tomcat

Copiar el archivo WAR:

```powershell
Copy-Item ".\target\mapuescuela-service.war" `
"C:\apache-tomcat-10.1.29\webapps\mapuescuela-service.war" -Force
```

Iniciar Tomcat:

```powershell
& "C:\apache-tomcat-10.1.29\bin\startup.bat"
```

## Dirección base

```text
http://localhost:8080/mapuescuela-service/webapi
```

## Datos JSON de prueba

```json
{
  "idPedido": "PED-001",
  "nombreCliente": "Cliente Prueba",
  "correoCliente": "cliente@ejemplo.cl",
  "producto": "Cuaderno",
  "cantidad": 2,
  "estadoPago": "PENDIENTE",
  "modalidadEntrega": "DESPACHO"
}
```

## Endpoints

### 1. Servicio inicial

```http
GET /myresource
```

Respuesta:

```text
Got it!
```

### 2. Aprobar pago

```http
POST /pedidos/aprobar-pago
```

Este servicio cambia el pago a `APROBADO` y devuelve la confirmación de actualización del inventario.

Respuesta esperada:

```json
{
  "resultado": "OK",
  "idPedido": "PED-001",
  "estadoPago": "APROBADO",
  "producto": "Cuaderno",
  "cantidad": 2,
  "mensaje": "Pago aprobado e inventario actualizado correctamente.",
  "inventarioActualizado": true
}
```

### 3. Rechazar pago

```http
POST /pedidos/rechazar-pago
```

Este servicio cambia el estado del pago a `RECHAZADO`.

Respuesta esperada:

```json
{
  "resultado": "OK",
  "idPedido": "PED-001",
  "estadoPago": "RECHAZADO",
  "producto": "Cuaderno",
  "cantidad": 2,
  "mensaje": "El pago fue rechazado."
}
```

### 4. Liberar stock

```http
POST /pedidos/liberar-stock
```

Este servicio cancela el pedido cuando pasan 24 horas sin comprobante y libera la cantidad reservada.

Respuesta esperada:

```json
{
  "resultado": "OK",
  "idPedido": "PED-001",
  "estadoPago": "CANCELADO_POR_TIEMPO",
  "producto": "Cuaderno",
  "cantidad": 2,
  "mensaje": "Pedido cancelado por superar las 24 horas sin comprobante.",
  "stockLiberado": true,
  "cantidadLiberada": 2
}
```

## Evidencias

Las capturas de las pruebas se encuentran en:

```text
05- Servicios Web/Evidencias
```

## Integración con Flowable

Los endpoints serán consumidos mediante External Worker Tasks para ejecutar automáticamente las acciones definidas en el proceso BPMN.

Esta versión corresponde al MVP y no utiliza todavía una base de datos de inventario.
