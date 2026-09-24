package cl.mapuescuela.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;




















@SpringBootApplication(scanBasePackages = "cl.mapuescuela")
@EntityScan(basePackages = "cl.mapuescuela")
@EnableJpaRepositories(basePackages = "cl.mapuescuela")
@EnableScheduling
public class MapuescuelaBackendApplication {

    public static void main(String[] args) {






        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("No se pudo crear la carpeta ./data: " + e.getMessage());
        }

        SpringApplication.run(MapuescuelaBackendApplication.class, args);
    }
}
