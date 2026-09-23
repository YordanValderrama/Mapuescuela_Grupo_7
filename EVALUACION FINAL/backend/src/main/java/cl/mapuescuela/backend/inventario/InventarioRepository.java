package cl.mapuescuela.backend.inventario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventarioRepository extends JpaRepository<InventarioEntity, Long> {

    Optional<InventarioEntity> findByProducto(String producto);
}
