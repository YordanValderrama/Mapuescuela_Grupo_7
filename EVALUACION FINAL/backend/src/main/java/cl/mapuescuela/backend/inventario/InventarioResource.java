package cl.mapuescuela.backend.inventario;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;










@Path("inventario")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Component
public class InventarioResource {

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    @GET
    @Path("productos")
    public List<InventarioEntity> listarProductos() {
        return inventarioRepository.findAll();
    }

    @POST
    @Path("productos")
    @Transactional
    public Response crearProducto(Map<String, Object> body) {
        String nombre = str(body.get("producto"));
        int precio = intVal(body.get("precio"), -1);
        int stock = intVal(body.get("stockDisponible"), -1);
        if (nombre == null || nombre.isBlank() || precio <= 0 || stock < 0) {
            return respuestaError("Indica nombre, precio positivo y stock no negativo.");
        }
        nombre = nombre.trim();
        if (inventarioRepository.findByProducto(nombre).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("mensaje", "Ya existe un producto con ese nombre.")).build();
        }
        InventarioEntity producto = new InventarioEntity(nombre, stock);
        producto.setPrecio(precio);
        producto.setDescripcion(str(body.get("descripcion")));
        producto.setImagenUrl(str(body.get("imagenUrl")));
        producto.setActivo(true);
        return Response.status(Response.Status.CREATED).entity(inventarioRepository.save(producto)).build();
    }

    @PUT
    @Path("productos/{id}")
    @Transactional
    public Response editarProducto(@PathParam("id") Long id, Map<String, Object> body) {
        InventarioEntity producto = inventarioRepository.findById(id).orElse(null);
        if (producto == null) return Response.status(Response.Status.NOT_FOUND).build();
        int precio = intVal(body.get("precio"), -1);
        int stock = intVal(body.get("stockDisponible"), -1);
        if (precio <= 0 || stock < 0) return respuestaError("Precio o stock inválido.");
        producto.setPrecio(precio);
        producto.setStockDisponible(stock);
        producto.setDescripcion(str(body.get("descripcion")));
        producto.setImagenUrl(str(body.get("imagenUrl")));
        producto.setActivo(Boolean.TRUE.equals(body.get("activo")));
        return Response.ok(inventarioRepository.save(producto)).build();
    }

    @GET
    @Path("{producto}")
    public Response consultarStock(@PathParam("producto") String producto) {
        return inventarioRepository.findByProducto(producto)
                .map(inv -> Response.ok(inv).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }








    @POST
    @Path("descontar")
    @Transactional
    public Response descontarStock(Map<String, Object> body) {
        String producto = str(body.get("producto"));
        int cantidad = intVal(body.get("cantidad"), 1);
        String idPedido = str(body.get("idPedido"));

        if (idPedido == null || idPedido.isBlank()) return respuestaError("El idPedido es obligatorio.");
        if (movimientoRepository.existsById(idPedido)) {
            return Response.ok(Map.of("resultado", "OK", "mensaje", "Pedido ya descontado; no se repite el movimiento.")).build();
        }

        if (producto == null || producto.trim().isEmpty()) {
            return respuestaError("El campo 'producto' es obligatorio.");
        }

        InventarioEntity inventario = inventarioRepository.findByProducto(producto).orElse(null);
        if (inventario == null) return respuestaError("Producto sin stock cargado.");

        if (cantidad <= 0 || inventario.getStockDisponible() < cantidad) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("mensaje", "Stock insuficiente o cantidad inválida.")).build();
        }
        int nuevoStock = inventario.getStockDisponible() - cantidad;
        inventario.setStockDisponible(nuevoStock);
        inventarioRepository.save(inventario);
        movimientoRepository.save(new MovimientoInventarioEntity(idPedido, producto, cantidad));

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






    @POST
    @Path("liberar")
    @Transactional
    public Response liberarStock(Map<String, Object> body) {
        String idPedido = str(body.get("idPedido"));
        if (idPedido == null || idPedido.isBlank()) return respuestaError("El idPedido es obligatorio.");
        MovimientoInventarioEntity movimiento = movimientoRepository.findById(idPedido).orElse(null);
        if (movimiento == null || movimiento.isLiberado()) {
            return Response.ok(Map.of("resultado", "OK", "mensaje", "No hay stock reservado pendiente de liberar.")).build();
        }
        String producto = movimiento.getProducto();
        int cantidad = movimiento.getCantidad();

        if (producto == null || producto.trim().isEmpty()) {
            return respuestaError("El campo 'producto' es obligatorio.");
        }

        InventarioEntity inventario = inventarioRepository.findByProducto(producto)
                .orElseGet(() -> new InventarioEntity(producto, 0));

        int nuevoStock = inventario.getStockDisponible() + cantidad;
        inventario.setStockDisponible(nuevoStock);
        inventarioRepository.save(inventario);
        movimiento.setLiberado(true);
        movimientoRepository.save(movimiento);

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
