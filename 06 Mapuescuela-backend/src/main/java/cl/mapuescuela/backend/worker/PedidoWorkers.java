package cl.mapuescuela.backend.worker;

import cl.mapuescuela.Pedido;
import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.WorkerResult;
import org.flowable.external.worker.WorkerResultBuilder;
import org.flowable.external.worker.annotation.FlowableWorker;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Worker relacionados al ciclo de vida del pedido.
 *
 * ACTUALIZACIÓN (Entrega 3): ahora que existe InventarioResource
 * (POST /inventario/liberar), cancelarPedidoVencido y
 * anularPedidoRechazado además de marcar el pedido en PedidoResource
 * liberan el stock reservado en el inventario real — antes solo se
 * cambiaba el estado del pedido, sin tocar el stock, que era el mismo
 * hueco que dejó pendiente InventarioWorker.
 */
@Component
public class PedidoWorkers extends BaseWorker {

    // =====================================================================
    // 1. Generar número de pedido (local; no depende de un endpoint externo)
    // =====================================================================
    @FlowableWorker(topic = "generar-numero-pedido")
    public WorkerResult generarNumeroPedido(AcquiredExternalWorkerJob job, WorkerResultBuilder resultBuilder) {
        Map<String, Object> vars = job.getVariables();

        String numeroPedido = "PED-" + Instant.now().toEpochMilli();

        log.info("Pedido generado: {} (cliente: {})", numeroPedido, vars.get("nombreCompleto"));

        return resultBuilder.success().variable("nPedido", numeroPedido);
    }

    // =====================================================================
    // 2. Registrar pago -> guarda en la DB que el pago quedó aprobado
    // =====================================================================
    @FlowableWorker(topic = "registrar-pago")
    public void registrarPago(AcquiredExternalWorkerJob job) {
        Pedido pedido = construirPedido(job.getVariables());

        Map<String, Object> respuesta = post("/pedidos/aprobar-pago", pedido);
        log.info("Pago registrado/aprobado en DB para pedido {}: {}", pedido.getIdPedido(), respuesta);
    }

    // =====================================================================
    // 3. Cancelar pedido vencido (timer 24h) -> libera el stock reservado
    // =====================================================================
    @FlowableWorker(topic = "cancelar-pedido-vencido")
    public void cancelarPedidoVencido(AcquiredExternalWorkerJob job) {
        Pedido pedido = construirPedido(job.getVariables());

        Map<String, Object> respuesta = post("/pedidos/liberar-stock", pedido);
        log.info("Stock liberado para pedido {}: {}", pedido.getIdPedido(), respuesta);

        liberarInventario(pedido);
    }

    // =====================================================================
    // 4. Anular pedido rechazado -> marca el pago como rechazado
    // =====================================================================
    @FlowableWorker(topic = "anular-pedido-rechazado")
    public void anularPedidoRechazado(AcquiredExternalWorkerJob job) {
        Pedido pedido = construirPedido(job.getVariables());

        Map<String, Object> respuesta = post("/pedidos/rechazar-pago", pedido);
        log.info("Pago rechazado para pedido {}: {}", pedido.getIdPedido(), respuesta);

        liberarInventario(pedido);
    }

    // ---------- Utilidad ----------

    /**
     * Devuelve al inventario el stock reservado para un pedido que no
     * llegó a concretarse (vencido o rechazado). El nombre del producto
     * real aún no viene de un carrito de compras (limitación de MVP
     * documentada en BaseWorker#construirPedido), así que se usa el
     * mismo valor de prueba con el que se descontó.
     */
    private void liberarInventario(Pedido pedido) {
        Map<String, Object> body = Map.of(
                "producto", pedido.getProducto() != null ? pedido.getProducto() : "PRODUCTO_PRUEBA",
                "cantidad", pedido.getCantidad() > 0 ? pedido.getCantidad() : 1
        );

        Map<String, Object> respuestaInventario = post("/inventario/liberar", body);
        log.info("Inventario liberado para pedido {}: {}", pedido.getIdPedido(), respuestaInventario);
    }
}
