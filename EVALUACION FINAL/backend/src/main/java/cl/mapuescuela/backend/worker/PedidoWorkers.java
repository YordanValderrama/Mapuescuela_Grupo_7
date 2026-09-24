package cl.mapuescuela.backend.worker;

import cl.mapuescuela.Pedido;
import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.WorkerResult;
import org.flowable.external.worker.WorkerResultBuilder;
import org.flowable.external.worker.annotation.FlowableWorker;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;











@Component
public class PedidoWorkers extends BaseWorker {




    @FlowableWorker(topic = "generar-numero-pedido")
    public WorkerResult generarNumeroPedido(AcquiredExternalWorkerJob job, WorkerResultBuilder resultBuilder) {
        Map<String, Object> vars = job.getVariables();








        Object existente = vars.get("nPedido");
        String numeroPedido = (existente != null && !existente.toString().trim().isEmpty())
                ? existente.toString()
                : "PED-" + Instant.now().toEpochMilli();

        log.info("Pedido generado: {} (cliente: {})", numeroPedido, vars.get("nombreCompleto"));

        return resultBuilder.success().variable("nPedido", numeroPedido);
    }









    @FlowableWorker(topic = "Registrar_comprobante")
    public void registrarPago(AcquiredExternalWorkerJob job) {
        Pedido pedido = construirPedido(job.getVariables());

        Map<String, Object> respuesta = post("/pedidos/aprobar-pago", pedido);
        log.info("Pago registrado/aprobado en DB para pedido {}: {}", pedido.getIdPedido(), respuesta);
    }




    @FlowableWorker(topic = "cancelar-pedido-vencido")
    public void cancelarPedidoVencido(AcquiredExternalWorkerJob job) {
        Pedido pedido = construirPedido(job.getVariables());

        Map<String, Object> respuesta = post("/pedidos/liberar-stock", pedido);
        log.info("Stock liberado para pedido {}: {}", pedido.getIdPedido(), respuesta);

        liberarInventario(pedido);
    }




    @FlowableWorker(topic = "anular-pedido-rechazado")
    public void anularPedidoRechazado(AcquiredExternalWorkerJob job) {
        Pedido pedido = construirPedido(job.getVariables());

        Map<String, Object> respuesta = post("/pedidos/rechazar-pago", pedido);
        log.info("Pago rechazado para pedido {}: {}", pedido.getIdPedido(), respuesta);

        liberarInventario(pedido);
    }










    private void liberarInventario(Pedido pedido) {
        Map<String, Object> body = Map.of(
                "idPedido", pedido.getIdPedido(),
                "producto", pedido.getProducto() != null ? pedido.getProducto() : "PRODUCTO_PRUEBA",
                "cantidad", pedido.getCantidad() > 0 ? pedido.getCantidad() : 1
        );

        Map<String, Object> respuestaInventario = post("/inventario/liberar", body);
        log.info("Inventario liberado para pedido {}: {}", pedido.getIdPedido(), respuestaInventario);
    }
}
