package cl.mapuescuela.backend.flowable;

import cl.mapuescuela.PedidoEntity;
import cl.mapuescuela.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;


@Component
public class SincronizadorPedidos {
    private static final Logger log = LoggerFactory.getLogger(SincronizadorPedidos.class);
    private final PedidoRepository pedidos;
    private final FlowableProcessClient flowable;

    public SincronizadorPedidos(PedidoRepository pedidos, FlowableProcessClient flowable) {
        this.pedidos = pedidos;
        this.flowable = flowable;
    }


    @Scheduled(fixedDelay = 12000, initialDelay = 6000)
    public void sincronizarPendientes() {
        for (PedidoEntity pedido : pedidos.findAll()) {
            if (pedido.getProcessInstanceId() == null || pedido.getProcessInstanceId().isBlank()) continue;
            if (!"PENDIENTE_PAGO".equals(pedido.getEstadoPago())
                    && !"EN_REVISION".equals(pedido.getEstadoPago())) continue;
            try {
                sincronizar(pedido);
            } catch (Exception e) {
                log.warn("No se pudo avanzar el pedido {} en Flowable: {}", pedido.getIdPedido(), e.getMessage());
            }
        }
    }

    private void sincronizar(PedidoEntity pedido) {
        Map<String, Object> tarea = flowable.tareaActiva(pedido.getProcessInstanceId());
        if (tarea == null) return; 
        String clave = String.valueOf(tarea.get("taskDefinitionKey"));
        if ("ut_datos_personales_modalidad_entrega".equals(clave)) {
            if (vacio(pedido.getRutCliente()) || vacio(pedido.getTelefonoCliente())) return;
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("nPedido", pedido.getIdPedido());
            datos.put("nombreCompleto", pedido.getNombreCliente());
            datos.put("correoElectronico", pedido.getCorreoCliente());
            datos.put("rUT", pedido.getRutCliente());
            datos.put("telefono", pedido.getTelefonoCliente());
            datos.put("direccion", texto(pedido.getDireccion()));
            datos.put("region", texto(pedido.getRegion()));
            datos.put("comuna", texto(pedido.getComuna()));
            datos.put("seleccioneModalidadDeEntrega", pedido.getModalidadEntrega());
            datos.put("producto", pedido.getProducto());
            datos.put("cantidad", pedido.getCantidad());
            flowable.completarEtapaCliente(pedido.getProcessInstanceId(), clave, datos);
            log.info("Datos del cliente confirmados en Flowable para {}", pedido.getIdPedido());
        } else if (("ut_cargar_comprobante".equals(clave) || "reenviar_comprobante".equals(clave))
                && "EN_REVISION".equals(pedido.getEstadoPago())
                && !vacio(pedido.getNombreArchivoComprobante())) {
            flowable.completarEtapaCliente(pedido.getProcessInstanceId(), clave,
                    Map.of("nPedido", pedido.getIdPedido(),
                            "nombreArchivoComprobante", pedido.getNombreArchivoComprobante()));
            log.info("Carga de comprobante confirmada en Flowable para {}", pedido.getIdPedido());
        }

    }

    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }
    private String texto(String valor) { return valor == null ? "" : valor; }
}
