package cl.mapuescuela.backend.worker;

import cl.mapuescuela.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Base común para los External Workers de Flowable.
 * Centraliza la llamada HTTP hacia el web service REST (PedidoResource)
 * para que PedidoWorkers, InventarioWorker y NotificacionWorkers no
 * repitan el boilerplate de RestTemplate.
 *
 * IMPORTANTE: PedidoResource no usa @ApplicationPath (queda expuesto en
 * la raíz vía JerseyConfig), así que baseUrl() apunta directo a
 * http://localhost:<puerto>, sin prefijo "/api/rest".
 */
public abstract class BaseWorker {

    protected static final Logger log = LoggerFactory.getLogger(BaseWorker.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${server.port}")
    protected String puertoLocal;

    protected String baseUrl() {
        return "http://localhost:" + puertoLocal;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        String url = baseUrl() + path;
        Map<?, ?> respuesta = restTemplate.postForObject(url, request, Map.class);
        return (Map<String, Object>) respuesta;
    }

    /**
     * Arma el DTO real Pedido a partir de las variables del proceso.
     * producto/cantidad quedan con un valor de prueba fijo hasta definir
     * cómo se modela el carrito real.
     */
    protected Pedido construirPedido(Map<String, Object> vars) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(str(vars.get("nPedido")));
        pedido.setNombreCliente(str(vars.get("Nombrecompleto")));
        pedido.setCorreoCliente(str(vars.get("correoElectronico")));
        pedido.setModalidadEntrega(mapModoReparto(vars.get("seleccioneModalidadDeEntrega")));
        pedido.setProducto("PRODUCTO_PRUEBA");
        pedido.setCantidad(1);
        return pedido;
    }

    protected String mapModoReparto(Object valorFormulario) {
        if (valorFormulario == null) return "RETIRO_TIENDA";
        return "reparto_domicilio".equals(valorFormulario.toString()) ? "DOMICILIO" : "RETIRO_TIENDA";
    }

    protected String str(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
