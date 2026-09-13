package cl.mapuescuela;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("pedidos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PedidoResource {

    private final PedidoRepository repositorio =
            new PedidoRepository();

    @POST
    public Response crearPedido(Pedido pedido) {

        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        if (pedido.getEstadoPago() == null
                || pedido.getEstadoPago().trim().isEmpty()) {
            pedido.setEstadoPago("PENDIENTE");
        }

        repositorio.guardar(pedido);

        Map<String, Object> respuesta = crearRespuestaBase(
                pedido,
                "Pedido creado correctamente."
        );

        return Response.status(Response.Status.CREATED)
                .entity(respuesta)
                .build();
    }

    @GET
    @Path("/{idPedido}")
    public Response buscarPedido(
            @PathParam("idPedido") String idPedido
    ) {
        Pedido pedido = repositorio.buscarPorId(idPedido);

        if (pedido == null) {
            Map<String, Object> error = new LinkedHashMap<>();

            error.put("resultado", "ERROR");
            error.put("mensaje", "Pedido no encontrado.");

            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error)
                    .build();
        }

        return Response.ok(pedido).build();
    }

    @POST
    @Path("aprobar-pago")
    public Response aprobarPago(Pedido pedido) {

        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        pedido.setEstadoPago("APROBADO");
        repositorio.guardar(pedido);

        Map<String, Object> respuesta = crearRespuestaBase(
                pedido,
                "Pago aprobado e inventario actualizado correctamente."
        );

        respuesta.put("inventarioActualizado", true);

        return Response.ok(respuesta).build();
    }

    @POST
    @Path("rechazar-pago")
    public Response rechazarPago(Pedido pedido) {

        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        pedido.setEstadoPago("RECHAZADO");
        repositorio.guardar(pedido);

        Map<String, Object> respuesta = crearRespuestaBase(
                pedido,
                "El pago fue rechazado."
        );

        return Response.ok(respuesta).build();
    }

    @POST
    @Path("liberar-stock")
    public Response liberarStock(Pedido pedido) {

        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        pedido.setEstadoPago("CANCELADO_POR_TIEMPO");
        repositorio.guardar(pedido);

        Map<String, Object> respuesta = crearRespuestaBase(
                pedido,
                "Pedido cancelado por superar las 24 horas sin comprobante."
        );

        respuesta.put("stockLiberado", true);
        respuesta.put("cantidadLiberada", pedido.getCantidad());

        return Response.ok(respuesta).build();
    }

    private boolean pedidoInvalido(Pedido pedido) {
        return pedido == null
                || pedido.getIdPedido() == null
                || pedido.getIdPedido().trim().isEmpty();
    }

    private Response respuestaError() {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("resultado", "ERROR");
        error.put("mensaje", "El idPedido es obligatorio.");

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .build();
    }

    private Map<String, Object> crearRespuestaBase(
            Pedido pedido,
            String mensaje
    ) {
        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("resultado", "OK");
        respuesta.put("idPedido", pedido.getIdPedido());
        respuesta.put("estadoPago", pedido.getEstadoPago());
        respuesta.put("producto", pedido.getProducto());
        respuesta.put("cantidad", pedido.getCantidad());
        respuesta.put("mensaje", mensaje);

        return respuesta;
    }
}