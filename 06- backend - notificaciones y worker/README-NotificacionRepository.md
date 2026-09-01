# README — NotificacionRepository.java

## ¿Qué es este código?

`NotificacionRepository` es una **interfaz de repositorio de Spring Data JPA**. No contiene ninguna implementación escrita a mano: Spring genera automáticamente, en tiempo de ejecución, una clase que implementa esta interfaz y sabe cómo ejecutar las consultas SQL correspondientes.

```java
public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {
    List<NotificacionEntity> findByIdPedidoOrderByFechaCreacionDesc(String idPedido);
    List<NotificacionEntity> findByLeidaFalseOrderByFechaCreacionDesc();
}
```

## ¿Qué gana el proyecto con esto?

Al extender `JpaRepository<NotificacionEntity, Long>`, la interfaz **ya hereda gratis** operaciones CRUD completas sobre `NotificacionEntity` (guardar, buscar por id, listar todo, eliminar, etc.), sin que nadie tenga que escribirlas. El `Long` indica el tipo de dato de la llave primaria (`id`).

## Explicación de los métodos declarados

### `findByIdPedidoOrderByFechaCreacionDesc(String idPedido)`
Spring Data interpreta el **nombre del método** y construye la consulta automáticamente, sin que se escriba SQL ni JPQL: "buscar todas las notificaciones cuyo campo `idPedido` coincida con el parámetro, ordenadas por `fechaCreacion` de forma descendente (más recientes primero)". Este método es el que usa `NotificacionResource` para responder al endpoint `GET /notificaciones/pedido/{idPedido}`.

### `findByLeidaFalseOrderByFechaCreacionDesc()`
De la misma forma, genera la consulta "buscar todas las notificaciones donde `leida` sea `false`, ordenadas por fecha de creación descendente". Es lo que usa `NotificacionResource` para responder al endpoint `GET /notificaciones` (bandeja de no leídas).

## Conceptos clave para explicar al docente

- **Query methods de Spring Data JPA**: cómo el propio nombre del método (siguiendo una convención: `findBy` + nombre de campo + condiciones + `OrderBy...`) le indica al framework qué consulta generar, sin escribir SQL.
- **Herencia de interfaz (`extends JpaRepository<...>`)**: patrón que evita reimplementar operaciones CRUD básicas en cada entidad del sistema.
- **Separación de capas**: el repositorio solo se preocupa del acceso a datos; no sabe nada de HTTP ni de Flowable — esa responsabilidad la tienen `NotificacionResource` y `NotificacionWorkers`, que lo consumen.
