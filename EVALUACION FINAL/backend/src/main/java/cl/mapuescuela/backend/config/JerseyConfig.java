package cl.mapuescuela.backend.config;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

/**
 * Registra todos los recursos JAX-RS (@Path) del proyecto en un unico
 * lugar. Escanear el paquete raiz "cl.mapuescuela" alcanza a:
 *   - cl.mapuescuela.MyResource, Pedido, PedidoResource
 *   - cl.mapuescuela.backend.notificaciones.NotificacionResource
 *   - cl.mapuescuela.backend.inventario.InventarioResource
 * porque Jersey escanea subpaquetes automaticamente.
 *
 * Esto es lo que el comentario de BaseWorker daba por hecho al decir
 * que PedidoResource "queda expuesto en la raiz via JerseyConfig": antes
 * esta clase no existia en el repositorio, asi que ese supuesto quedaba
 * sin cumplirse.
 *
 * MultiPartFeature es necesario para que PedidoResource pueda recibir
 * el comprobante como multipart/form-data (POST /pedidos/{id}/comprobante).
 */
@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        packages("cl.mapuescuela");
        register(MultiPartFeature.class);
    }
}
