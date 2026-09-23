package cl.mapuescuela.backend.inventario;

import jakarta.persistence.*;

@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventarioEntity {
    @Id
    @Column(name = "id_pedido")
    private String idPedido;
    private String producto;
    private int cantidad;
    private boolean liberado;

    public MovimientoInventarioEntity() {}
    public MovimientoInventarioEntity(String idPedido, String producto, int cantidad) {
        this.idPedido = idPedido;
        this.producto = producto;
        this.cantidad = cantidad;
    }
    public String getIdPedido() { return idPedido; }
    public String getProducto() { return producto; }
    public int getCantidad() { return cantidad; }
    public boolean isLiberado() { return liberado; }
    public void setLiberado(boolean liberado) { this.liberado = liberado; }
}
