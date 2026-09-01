package cl.mapuescuela.backend.worker;

import cl.mapuescuela.backend.notificaciones.NotificacionEntity;
import cl.mapuescuela.backend.notificaciones.NotificacionRepository;
import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.annotation.FlowableWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Workers de notificación al cliente. Ninguno toca el estado del pedido
 * (eso es de PedidoWorkers) — acá solo se registra el evento como una
 * notificación "dentro de la app" (tabla notificaciones, consultable vía
 * NotificacionResource: GET /notificaciones y
 * GET /notificaciones/pedido/{idPedido}), en vez de enviar un correo.
 * Se eliminó la dependencia de correo SMTP (Gmail) por la fragilidad de
 * red observada (timeouts, puertos bloqueados).
 */
@Component
public class NotificacionWorkers extends BaseWorker {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @FlowableWorker(topic = "notificar-rechazo")
    public void notificarRechazo(AcquiredExternalWorkerJob job) {
        Map<String, Object> vars = job.getVariables();
        String numeroPedido = str(vars.get("nPedido"));
        String motivo = str(vars.get("estadoDelPago"));

        String mensaje = "Lamentamos informarte que tu pago fue rechazado."
                + (motivo != null ? " Motivo: " + motivo : "");

        guardarNotificacion(numeroPedido, "PAGO_RECHAZADO", str(vars.get("correoElectronico")),
                "Tu pago no pudo ser procesado - Pedido " + numeroPedido, mensaje);
    }

    @FlowableWorker(topic = "notificar-pago-aprobado")
    public void notificarPagoAprobado(AcquiredExternalWorkerJob job) {
        Map<String, Object> vars = job.getVariables();
        String numeroPedido = str(vars.get("nPedido"));

        String mensaje = "Gracias por su compra, su pago ha sido aprobado. N° pedido: " + numeroPedido;

        guardarNotificacion(numeroPedido, "PAGO_APROBADO", str(vars.get("correoElectronico")),
                "Tu pago fue aprobado - Pedido " + numeroPedido, mensaje);
    }

    @FlowableWorker(topic = "notificar-pago-pendiente")
    public void notificarPendiente(AcquiredExternalWorkerJob job) {
        Map<String, Object> vars = job.getVariables();
        String numeroPedido = str(vars.get("nPedido"));
        String motivo = str(vars.get("estadoDelPago"));

        String mensaje = "Tu comprobante no pudo ser validado, por favor reenvíalo."
                + (motivo != null ? " Detalle: " + motivo : "");

        guardarNotificacion(numeroPedido, "PAGO_PENDIENTE", str(vars.get("correoElectronico")),
                "Necesitamos que reenvíes tu comprobante - Pedido " + numeroPedido, mensaje);
    }

    @FlowableWorker(topic = "notificar-envio-cliente")
    public void notificarEnvio(AcquiredExternalWorkerJob job) {
        Map<String, Object> vars = job.getVariables();
        String numeroPedido = str(vars.get("nPedido"));
        String empresa = str(vars.get("empresa"));
        String seguimiento = str(vars.get("nDeSeguimiento"));

        String mensaje = "Tu pedido fue despachado con " + empresa + ". N° de seguimiento: " + seguimiento;

        guardarNotificacion(numeroPedido, "ENVIO_DESPACHADO", str(vars.get("correoElectronico")),
                "Tu pedido fue despachado - Pedido " + numeroPedido, mensaje);
    }

    // ---------- Utilidad ----------

    private void guardarNotificacion(String idPedido, String tipo, String destinatario,
                                      String asunto, String mensaje) {
        try {
            NotificacionEntity notificacion = new NotificacionEntity();
            notificacion.setIdPedido(idPedido);
            notificacion.setTipo(tipo);
            notificacion.setDestinatario(destinatario);
            notificacion.setAsunto(asunto);
            notificacion.setMensaje(mensaje);
            notificacion.setFechaCreacion(LocalDateTime.now());

            notificacionRepository.save(notificacion);
            log.info("Notificación [{}] guardada para pedido {}: {}", tipo, idPedido, asunto);
        } catch (Exception e) {
            // No relanzamos: un fallo al guardar la notificación no
            // debería marcar el job como fallido y bloquear el resto
            // del proceso (igual criterio que teníamos con el correo).
            log.error("No se pudo guardar la notificación [{}] para pedido {}: {}",
                    tipo, idPedido, e.getMessage(), e);
        }
    }
}
