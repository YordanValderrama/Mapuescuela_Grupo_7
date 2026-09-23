package cl.mapuescuela.backend.inventario;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CatalogoInicial implements CommandLineRunner {
    private final InventarioRepository repository;

    public CatalogoInicial(InventarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        crearSiFalta("Cuaderno Mapuche Artesanal", 4500);
        crearSiFalta("Juego Didáctico Mapudungun", 8990);
        crearSiFalta("Set de Lápices Ilustrados", 3200);
        crearSiFalta("Mochila Étnica Infantil", 12500);
    }

    private void crearSiFalta(String nombre, int precio) {
        if (repository.findByProducto(nombre).isPresent()) return;
        InventarioEntity producto = new InventarioEntity(nombre, 10);
        producto.setPrecio(precio);
        producto.setDescripcion("Producto Mapuescuela");
        repository.save(producto);
    }
}
