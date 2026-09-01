package cl.mapuescuela.backend.notificaciones;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {

    List<NotificacionEntity> findByIdPedidoOrderByFechaCreacionDesc(String idPedido);

    List<NotificacionEntity> findByLeidaFalseOrderByFechaCreacionDesc();
}
