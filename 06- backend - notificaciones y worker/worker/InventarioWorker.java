package cl.mapuescuela.backend.worker;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.annotation.FlowableWorker;
import org.springframework.stereotype.Component;

/**
 * Worker de inventario.
 *
 * MVP: todavía no existe base de datos de inventario ni un endpoint
 * "/inventario/descontar". Este worker queda registrado (topic
 * "actualizar-inventario") pero el descuento real de stock sigue como
 * TODO explícito.
 *
 * NOTA: se quitó el uso de RuntimeService.startProcessInstanceByKey(...)
 * que se había agregado por error — esta tarea es un paso DENTRO del
 * proceso de venta que ya está corriendo, no un disparador para arrancar
 * un proceso nuevo llamado "actualizarInventarioProcess" (que además no
 * existe en el BPMN).
 */
@Component
public class InventarioWorker extends BaseWorker {

    @FlowableWorker(topic = "actualizar-inventario")
    public void actualizarInventario(AcquiredExternalWorkerJob job) {
        // TODO: reemplazar por una llamada real cuando exista
        // InventarioResource + persistencia de stock.
        log.warn("actualizar-inventario: sin endpoint de inventario todavía (MVP sin BD). "
                + "Job {} completado sin descuento real de stock.",
                job.getId()); // corregido: antes era job.getProcessInstanceId()
    }
}

