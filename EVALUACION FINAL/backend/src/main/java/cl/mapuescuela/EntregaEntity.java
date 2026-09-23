package cl.mapuescuela;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entregas")
public class EntregaEntity {
    @Id
    @Column(name = "id_pedido")
    private String idPedido;
    private String tipo;
    private String voluntario;
    private String rutVoluntario;
    private String observaciones;
    private String empresa;
    private String numeroSeguimiento;
    private LocalDateTime fechaRegistro;

    public String getIdPedido() { return idPedido; }
    public void setIdPedido(String v) { idPedido = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = v; }
    public String getVoluntario() { return voluntario; }
    public void setVoluntario(String v) { voluntario = v; }
    public String getRutVoluntario() { return rutVoluntario; }
    public void setRutVoluntario(String v) { rutVoluntario = v; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String v) { observaciones = v; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String v) { empresa = v; }
    public String getNumeroSeguimiento() { return numeroSeguimiento; }
    public void setNumeroSeguimiento(String v) { numeroSeguimiento = v; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime v) { fechaRegistro = v; }
}
