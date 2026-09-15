package cl.mapuescuela.backend.inventario;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Equivalente a PedidoResource pero para el dominio de inventario.
 * Cierra el TODO documentado en InventarioWorker / README-InventarioWorker.md:
 * ya existe un endpoint real que el worker "actualizar-inventario" puede
 * invocar en vez de solo loguear una advertencia.
 *
 * Igual que PedidoResource, no usa @ApplicationPath: queda expuesto en la
 * raíz (http://localhost:<puerto>/inventario/...).
 */
@Path("inventario")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Component
public class InventarioResource {

    @Autowired
    private InventarioRepository inventarioRepository;

    @GET
    @Path("{producto}")
    public Response consultarStock(@PathParam("producto") String producto) {
        return inventarioRepository.findByProducto(producto)
                .map(inv -> Response.ok(inv).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Descuenta stock de un producto (paso "Actualizar inventario" del
     * BPMN, topic actualizar-inventario). Si el producto no existe todavía
     * en la tabla, se crea con stock 0 y queda un registro explícito de
     * que se intentó descontar sin stock inicial cargado (MVP: la carga
     * inicial de stock no está definida todavía).
     */
    @POST
    @Path("descontar")
    @Transactional
    public Response descontarStock(Map<String, Object> body) {
        String producto = str(body.get("producto"));
        int cantidad = intVal(body.get("cantidad"), 1);

        if (producto == null || producto.trim().isEmpty()) {
            return respuestaError("El campo 'producto' es obligatorio.");
        }

        InventarioEntity inventario = inventarioRepository.findByProducto(producto)
                .orElseGet(() -> new InventarioEntity(producto, 0));

        int nuevoStock = inventario.getStockDisponible() - cantidad;
        inventario.setStockDisponible(nuevoStock);
        inventarioRepository.save(inventario);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("resultado", "OK");
        respuesta.put("producto", producto);
        respuesta.put("cantidadDescontada", cantidad);
        respuesta.put("stockDisponible", nuevoStock);
        respuesta.put("mensaje", nuevoStock < 0
                ? "Stock descontado, pero quedó negativo: revisar carga inicial de inventario."
                : "Stock descontado correctamente.");

        return Response.ok(respuesta).build();
    }

    /**
     * Libera stock previamente reservado (usado cuando se cancela un
     * pedido por vencimiento de las 24h, o se rechaza un pago después de
     * haber descontado inventario).
     */
    @POST
    @Path("liberar")
    @Transactional
    public Response liberarStock(Map<String, Object> body) {
        String producto = str(body.get("producto"));
        int cantidad = intVal(body.get("cantidad"), 1);

        if (producto == null || producto.trim().isEmpty()) {
            return respuestaError("El campo 'producto' es obligatorio.");
        }

        InventarioEntity inventario = inventarioRepository.findByProducto(producto)
                .orElseGet(() -> new InventarioEntity(producto, 0));

        int nuevoStock = inventario.getStockDisponible() + cantidad;
        inventario.setStockDisponible(nuevoStock);
        inventarioRepository.save(inventario);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("resultado", "OK");
        respuesta.put("producto", producto);
        respuesta.put("cantidadLiberada", cantidad);
        respuesta.put("stockDisponible", nuevoStock);
        respuesta.put("mensaje", "Stock liberado correctamente.");

        return Response.ok(respuesta).build();
    }

    private Response respuestaError(String mensaje) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("resultado", "ERROR");
        error.put("mensaje", mensaje);
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }

    private String str(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private int intVal(Object valor, int porDefecto) {
        if (valor == null) return porDefecto;
        if (valor instanceof Number) return ((Number) valor).intValue();
        try {
            return Integer.parseInt(valor.toString());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
