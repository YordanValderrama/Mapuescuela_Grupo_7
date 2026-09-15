package cl.mapuescuela;

import jakarta.persistence.*;

/**
 * Entidad JPA que persiste el pedido en la base de datos.
 *
 * Antes, PedidoResource solo recibía un Pedido (DTO), lo modificaba en
 * memoria y devolvía la respuesta -- nada quedaba guardado. Es exactamente
 * lo que señaló la retroalimentación de Entrega 2: "deben avanzar hacia
 * la persistencia de la información utilizando una base de datos real".
 *
 * Se usa idPedido (String, ej. "PED-1735489201234") como clave primaria
 * en vez de un id autogenerado, porque ese es el identificador de negocio
 * que ya circula entre el frontend, el proceso BPMN y los workers.
 */
@Entity
@Table(name = "pedidos")
public class PedidoEntity {

    @Id
    @Column(name = "id_pedido")
    private String idPedido;

    @Column(name = "nombre_cliente")
    private String nombreCliente;

    @Column(name = "correo_cliente")
    private String correoCliente;

    @Column(name = "producto")
    private String producto;

    @Column(name = "cantidad")
    private int cantidad;

    @Column(name = "estado_pago")
    private String estadoPago;

    @Column(name = "modalidad_entrega")
    private String modalidadEntrega;

    public PedidoEntity() {
    }

    public String getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(String idPedido) {
        this.idPedido = idPedido;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getCorreoCliente() {
        return correoCliente;
    }

    public void setCorreoCliente(String correoCliente) {
        this.correoCliente = correoCliente;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public String getModalidadEntrega() {
        return modalidadEntrega;
    }

    public void setModalidadEntrega(String modalidadEntrega) {
        this.modalidadEntrega = modalidadEntrega;
    }
}
