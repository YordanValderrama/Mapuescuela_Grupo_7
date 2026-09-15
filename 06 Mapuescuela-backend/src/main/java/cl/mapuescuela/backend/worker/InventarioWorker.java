package cl.mapuescuela.backend.worker;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.annotation.FlowableWorker;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Worker de inventario.
 *
 * Actualizado: ya existe InventarioResource (POST /inventario/descontar),
 * así que este worker deja de ser un stub y llama al endpoint real,
 * reutilizando post(...) heredado de BaseWorker. El producto y la
 * cantidad se toman de las variables del proceso; si no vienen definidas
 * (MVP sin modelo de carrito real todavía), se usa "PRODUCTO_PRUEBA" y
 * cantidad 1, igual que hace BaseWorker#construirPedido.
 */
@Component
public class InventarioWorker extends BaseWorker {

    @FlowableWorker(topic = "actualizar-inventario")
    public void actualizarInventario(AcquiredExternalWorkerJob job) {
        Map<String, Object> vars = job.getVariables();

        String producto = str(vars.get("producto"));
        if (producto == null || producto.trim().isEmpty()) {
            producto = "PRODUCTO_PRUEBA";
        }

        Object cantidadVar = vars.get("cantidad");
        int cantidad = 1;
        if (cantidadVar instanceof Number) {
            cantidad = ((Number) cantidadVar).intValue();
        }

        Map<String, Object> body = Map.of(
                "producto", producto,
                "cantidad", cantidad
        );

        Map<String, Object> respuesta = post("/inventario/descontar", body);
        log.info("Inventario actualizado para pedido {}: {}", vars.get("nPedido"), respuesta);
    }
}
