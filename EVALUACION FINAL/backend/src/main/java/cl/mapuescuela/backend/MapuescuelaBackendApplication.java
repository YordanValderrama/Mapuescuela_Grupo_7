package cl.mapuescuela.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Punto de entrada unico de la aplicacion. Al arrancar, Spring Boot:
 *  - levanta el servidor embebido en el puerto de server.port,
 *  - publica los recursos REST (PedidoResource, NotificacionResource,
 *    InventarioResource) via Jersey (ver JerseyConfig),
 *  - conecta el cliente de External Workers de Flowable usando las
 *    propiedades flowable.external.worker.* de application.properties,
 *  - y registra PedidoWorkers, NotificacionWorkers e InventarioWorker
 *    como beans Spring, listos para reclamar jobs por topic.
 *
 * IMPORTANTE: @EntityScan y @EnableJpaRepositories con basePackages
 * "cl.mapuescuela" son necesarios porque PedidoEntity/PedidoRepository
 * viven en el paquete raiz cl.mapuescuela (un nivel arriba de
 * cl.mapuescuela.backend, donde esta esta clase). Sin esto, Spring Boot
 * solo escanea automaticamente el paquete de esta clase y sus
 * subpaquetes, y nunca encuentra PedidoRepository -- eso causaba el
 * error "No qualifying bean of type 'cl.mapuescuela.PedidoRepository'"
 * al llamar POST /pedidos desde el frontend.
 */
@SpringBootApplication(scanBasePackages = "cl.mapuescuela")
@EntityScan(basePackages = "cl.mapuescuela")
@EnableJpaRepositories(basePackages = "cl.mapuescuela")
@EnableScheduling
public class MapuescuelaBackendApplication {

    public static void main(String[] args) {
        // NUEVO: el driver de SQLite crea el archivo .db solo si falta,
        // pero NO crea la carpeta que lo contiene ("./data"). Si alguien
        // del equipo clona el repo en una carpeta nueva (sin ese "./data"
        // de una corrida anterior), Hibernate fallaba al arrancar con
        // "path to './data/mapuescuela.db': ... does not exist". Se crea
        // acá, antes de que Spring Boot levante el datasource.
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("No se pudo crear la carpeta ./data: " + e.getMessage());
        }

        SpringApplication.run(MapuescuelaBackendApplication.class, args);
    }
}
