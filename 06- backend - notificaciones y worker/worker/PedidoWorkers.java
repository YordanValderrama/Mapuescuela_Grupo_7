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
 * Workers relacionados al ciclo de vida del pedido.
 *
 * NOTA: esta clase había sido migrada por error a un patrón basado en
 * RuntimeService.startProcessInstanceByKey(...), lo cual rompía la
 * integración: las Service Tasks del BPMN son External Worker Tasks
 * (flowable:type="external-worker"), es decir, jobs que un cliente
 * externo debe reclamar por topic — no puntos donde tenga sentido
 * arrancar un proceso nuevo. Se restaura el patrón @FlowableWorker.
 *
 * También el archivo declaraba "class PedidoWorker" (sin "s"), lo cual
 * no compila: el nombre de la clase pública debe coincidir exactamente
 * con el nombre del archivo (PedidoWorkers.java -> class PedidoWorkers).
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

        log.info("Pedido generado: {} (cliente: {})", numeroPedido, vars.get("Nombrecompleto"));

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
    }

    // =====================================================================
    // 4. Anular pedido rechazado -> marca el pago como rechazado
    // =====================================================================
    @FlowableWorker(topic = "anular-pedido-rechazado")
    public void anularPedidoRechazado(AcquiredExternalWorkerJob job) {
        Pedido pedido = construirPedido(job.getVariables());

        Map<String, Object> respuesta = post("/pedidos/rechazar-pago", pedido);
        log.info("Pago rechazado para pedido {}: {}", pedido.getIdPedido(), respuesta);
    }
}
