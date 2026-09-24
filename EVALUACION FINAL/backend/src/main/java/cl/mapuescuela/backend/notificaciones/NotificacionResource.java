package cl.mapuescuela.backend.notificaciones;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;








@Path("notificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Component
public class NotificacionResource {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @GET
    public List<NotificacionEntity> listarNoLeidas() {
        return notificacionRepository.findByLeidaFalseOrderByFechaCreacionDesc();
    }

    @GET
    @Path("pedido/{idPedido}")
    public List<NotificacionEntity> listarPorPedido(@PathParam("idPedido") String idPedido) {
        return notificacionRepository.findByIdPedidoOrderByFechaCreacionDesc(idPedido);
    }

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
}
