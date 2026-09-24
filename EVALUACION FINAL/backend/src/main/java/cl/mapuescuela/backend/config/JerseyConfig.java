package cl.mapuescuela.backend.config;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

















@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        packages("cl.mapuescuela");
        register(MultiPartFeature.class);
    }
}
