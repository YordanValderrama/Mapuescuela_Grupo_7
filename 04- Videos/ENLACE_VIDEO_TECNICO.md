# Entrega 3 - Video de Explicación Técnica

## Enlace del Video (Google Drive)
* https://drive.google.com/file/d/1R9NZc_1337viJ_74ngV7OIx8hflzWENO/view?usp=sharing

---

## Justificación Técnica y Estado del Despliegue

Estimado profesor:

En este repositorio y en el video técnico adjunto se respalda la estructura completa de la Entrega 3:

1. **Frontend y Datos Dinámicos:** Selector interactivo de productos en `pantalla1.html` y cálculo automatizado del total en `pantalla2.html`.
2. **Servicios Web y Persistencia:** Endpoints REST para cambio de estado (`/pedidos/aprobar-pago`, `/pedidos/rechazar-pago`, `/pedidos/liberar-stock`) con persistencia en base de datos local H2.
3. **Modelado y Workers:** Modelo `Process_TO-BE.bpmn` y código de los External Workers desacoplados en Java dentro de la carpeta `06- backend`.

### Nota sobre el entorno de ejecución
Debido a incompatibilidades de librerías en el entorno local del servidor de aplicaciones tras la migración desde Flowable Cloud, se priorizó asegurar la persistencia de datos, los contratos de servicio REST y la modularidad del código fuente directamente en el repositorio.
