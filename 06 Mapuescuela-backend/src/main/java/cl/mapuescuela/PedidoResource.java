package cl.mapuescuela;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ACTUALIZADO (Entrega 3) a partir de la retroalimentación de Entrega 2:
 * "deben avanzar hacia la persistencia de la información utilizando una
 * base de datos real". Antes este recurso solo mutaba el DTO Pedido en
 * memoria; ahora todo se guarda en PedidoEntity via PedidoRepository.
 *
 * También se agrega el endpoint POST /pedidos (crearPedido), que es el
 * que el frontend (pantalla1.html) llama directamente al enviar el
 * formulario -- así se cierra el flujo interfaz -> Web Service ->
 * persistencia que pedía la retroalimentación, sin depender de que el
 * proceso BPMN ya esté corriendo para que exista un registro del pedido.
 */
@Path("pedidos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Component
public class PedidoResource {

    @Autowired
    private PedidoRepository pedidoRepository;

    /**
     * Crea el pedido apenas el cliente completa el formulario (pantalla1).
     * Genera el número de pedido acá mismo (mismo formato que
     * PedidoWorkers#generarNumeroPedido) para que exista un identificador
     * incluso si el proceso BPMN todavía no se ha iniciado.
     */
    @POST
    @Transactional
    public Response crearPedido(Pedido pedido) {
        if (pedido == null || pedido.getProducto() == null || pedido.getProducto().trim().isEmpty()) {
            return respuestaError("El campo 'producto' es obligatorio.");
        }
        if (pedido.getCantidad() <= 0) {
            pedido.setCantidad(1);
        }

        String idPedido = "PED-" + Instant.now().toEpochMilli();
        pedido.setIdPedido(idPedido);
        pedido.setEstadoPago("PENDIENTE_PAGO");

        PedidoEntity entidad = aEntidad(pedido);
        pedidoRepository.save(entidad);

        return Response.status(Response.Status.CREATED)
                .entity(crearRespuestaBase(pedido, "Pedido creado. Realice la transferencia y adjunte el comprobante."))
                .build();
    }

    @GET
    @Path("{idPedido}")
    public Response obtenerPedido(@PathParam("idPedido") String idPedido) {
        return pedidoRepository.findById(idPedido)
                .map(entidad -> Response.ok(entidad).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("aprobar-pago")
    @Transactional
    public Response aprobarPago(Pedido pedido) {
        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        PedidoEntity entidad = buscarOCrear(pedido);
        entidad.setEstadoPago("APROBADO");
        pedidoRepository.save(entidad);

        pedido.setEstadoPago("APROBADO");
        Map<String, Object> respuesta = crearRespuestaBase(
                pedido,
                "Pago aprobado e inventario actualizado correctamente."
        );
        respuesta.put("inventarioActualizado", true);

        return Response.ok(respuesta).build();
    }

    @POST
    @Path("rechazar-pago")
    @Transactional
    public Response rechazarPago(Pedido pedido) {
        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        PedidoEntity entidad = buscarOCrear(pedido);
        entidad.setEstadoPago("RECHAZADO");
        pedidoRepository.save(entidad);

        pedido.setEstadoPago("RECHAZADO");
        Map<String, Object> respuesta = crearRespuestaBase(pedido, "El pago fue rechazado.");

        return Response.ok(respuesta).build();
    }

    @POST
    @Path("liberar-stock")
    @Transactional
    public Response liberarStock(Pedido pedido) {
        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        PedidoEntity entidad = buscarOCrear(pedido);
        entidad.setEstadoPago("CANCELADO_POR_TIEMPO");
        pedidoRepository.save(entidad);

        pedido.setEstadoPago("CANCELADO_POR_TIEMPO");
        Map<String, Object> respuesta = crearRespuestaBase(
                pedido,
                "Pedido cancelado por superar las 24 horas sin comprobante."
        );
        respuesta.put("stockLiberado", true);
        respuesta.put("cantidadLiberada", pedido.getCantidad());

        return Response.ok(respuesta).build();
    }

    // ---------- Utilidades ----------

    private PedidoEntity buscarOCrear(Pedido pedido) {
        Optional<PedidoEntity> existente = pedidoRepository.findById(pedido.getIdPedido());
        return existente.orElseGet(() -> aEntidad(pedido));
    }

    private PedidoEntity aEntidad(Pedido pedido) {
        PedidoEntity entidad = new PedidoEntity();
        entidad.setIdPedido(pedido.getIdPedido());
        entidad.setNombreCliente(pedido.getNombreCliente());
        entidad.setCorreoCliente(pedido.getCorreoCliente());
        entidad.setProducto(pedido.getProducto());
        entidad.setCantidad(pedido.getCantidad() > 0 ? pedido.getCantidad() : 1);
        entidad.setEstadoPago(pedido.getEstadoPago());
        entidad.setModalidadEntrega(pedido.getModalidadEntrega());
        return entidad;
    }

    private boolean pedidoInvalido(Pedido pedido) {
        return pedido == null
                || pedido.getIdPedido() == null
                || pedido.getIdPedido().trim().isEmpty();
    }

    private Response respuestaError() {
        return respuestaError("El idPedido es obligatorio.");
    }

    private Response respuestaError(String mensaje) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("resultado", "ERROR");
        error.put("mensaje", mensaje);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .build();
    }

    private Map<String, Object> crearRespuestaBase(Pedido pedido, String mensaje) {
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
