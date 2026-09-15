package cl.mapuescuela.backend.inventario;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa el stock disponible de un producto.
 *
 * Cierra el TODO explícito que dejó InventarioWorker: hasta ahora el
 * worker "actualizar-inventario" solo dejaba un log.warn(...) porque no
 * existía ni tabla de inventario ni endpoint equivalente a PedidoResource
 * para el stock. Esta clase sigue exactamente el mismo patrón que
 * NotificacionEntity (misma convención de anotaciones y de paquete
 * cl.mapuescuela.backend.<dominio>).
 */
@Entity
@Table(name = "inventario")
public class InventarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto", unique = true)
    private String producto;

    @Column(name = "stock_disponible")
    private int stockDisponible;

    public InventarioEntity() {
    }

    public InventarioEntity(String producto, int stockDisponible) {
        this.producto = producto;
        this.stockDisponible = stockDisponible;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public int getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(int stockDisponible) {
        this.stockDisponible = stockDisponible;
    }
}
