package cl.mapuescuela.backend.worker;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.annotation.FlowableWorker;
import org.springframework.stereotype.Component;

import java.util.Map;











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
                "idPedido", str(vars.get("nPedido")),
                "producto", producto,
                "cantidad", cantidad
        );

        Map<String, Object> respuesta = post("/inventario/descontar", body);
        log.info("Inventario actualizado para pedido {}: {}", vars.get("nPedido"), respuesta);
    }
}
