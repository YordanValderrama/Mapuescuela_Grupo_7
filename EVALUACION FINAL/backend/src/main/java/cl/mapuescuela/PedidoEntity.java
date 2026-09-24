package cl.mapuescuela;

import jakarta.persistence.*;













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

    private String rutCliente;
    private String telefonoCliente;
    private String direccion;
    private String region;
    private String comuna;

    @Column(name = "producto")
    private String producto;

    @Column(name = "cantidad")
    private int cantidad;

    @Column(name = "estado_pago")
    private String estadoPago;

    @Column(name = "modalidad_entrega")
    private String modalidadEntrega;

    @Column(name = "nombre_archivo_comprobante")
    private String nombreArchivoComprobante;




    @Column(name = "process_instance_id")
    private String processInstanceId;

    public String getRutCliente() { return rutCliente; }
    public void setRutCliente(String v) { rutCliente = v; }
    public String getTelefonoCliente() { return telefonoCliente; }
    public void setTelefonoCliente(String v) { telefonoCliente = v; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String v) { direccion = v; }
    public String getRegion() { return region; }
    public void setRegion(String v) { region = v; }
    public String getComuna() { return comuna; }
    public void setComuna(String v) { comuna = v; }

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

    public String getNombreArchivoComprobante() {
        return nombreArchivoComprobante;
    }

    public void setNombreArchivoComprobante(String nombreArchivoComprobante) {
        this.nombreArchivoComprobante = nombreArchivoComprobante;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
}
