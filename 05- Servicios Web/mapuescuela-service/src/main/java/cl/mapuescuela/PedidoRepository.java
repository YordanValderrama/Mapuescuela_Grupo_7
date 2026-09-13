package cl.mapuescuela;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PedidoRepository {

    private static final String URL =
            "jdbc:h2:~/mapuescuela_data/mapuescuela";

    private static final String USUARIO = "sa";
    private static final String CLAVE = "";

    public PedidoRepository() {
        crearTabla();
    }

    private Connection conectar() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "No se encontró el controlador JDBC de H2.",
                    e
            );
        }

        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }

    private void crearTabla() {
        String sql =
                "CREATE TABLE IF NOT EXISTS pedidos (" +
                        "id_pedido VARCHAR(100) PRIMARY KEY," +
                        "nombre_cliente VARCHAR(200)," +
                        "correo_cliente VARCHAR(200)," +
                        "producto VARCHAR(200)," +
                        "cantidad INT," +
                        "estado_pago VARCHAR(100)," +
                        "modalidad_entrega VARCHAR(100)" +
                        ")";

        try (Connection conexion = conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo crear la tabla pedidos.", e
            );
        }
    }

    public void guardar(Pedido pedido) {
        String sql =
                "MERGE INTO pedidos (" +
                        "id_pedido, nombre_cliente, correo_cliente, producto, " +
                        "cantidad, estado_pago, modalidad_entrega" +
                        ") KEY (id_pedido) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conexion = conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setString(1, pedido.getIdPedido());
            sentencia.setString(2, pedido.getNombreCliente());
            sentencia.setString(3, pedido.getCorreoCliente());
            sentencia.setString(4, pedido.getProducto());
            sentencia.setObject(5, pedido.getCantidad());
            sentencia.setString(6, pedido.getEstadoPago());
            sentencia.setString(7, pedido.getModalidadEntrega());

            sentencia.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo guardar el pedido.", e
            );
        }
    }

    public Pedido buscarPorId(String idPedido) {
        String sql =
                "SELECT id_pedido, nombre_cliente, correo_cliente, producto, " +
                        "cantidad, estado_pago, modalidad_entrega " +
                        "FROM pedidos WHERE id_pedido = ?";

        try (Connection conexion = conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setString(1, idPedido);

            try (ResultSet resultado = sentencia.executeQuery()) {

                if (resultado.next()) {
                    Pedido pedido = new Pedido();

                    pedido.setIdPedido(
                            resultado.getString("id_pedido")
                    );

                    pedido.setNombreCliente(
                            resultado.getString("nombre_cliente")
                    );

                    pedido.setCorreoCliente(
                            resultado.getString("correo_cliente")
                    );

                    pedido.setProducto(
                            resultado.getString("producto")
                    );

                    pedido.setCantidad(
                            resultado.getInt("cantidad")
                    );

                    pedido.setEstadoPago(
                            resultado.getString("estado_pago")
                    );

                    pedido.setModalidadEntrega(
                            resultado.getString("modalidad_entrega")
                    );

                    return pedido;
                }

                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo buscar el pedido.", e
            );
        }
    }

    public void actualizarEstadoPago(
            String idPedido,
            String estadoPago
    ) {
        String sql =
                "UPDATE pedidos SET estado_pago = ? " +
                        "WHERE id_pedido = ?";

        try (Connection conexion = conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setString(1, estadoPago);
            sentencia.setString(2, idPedido);

            sentencia.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo actualizar el estado del pago.", e
            );
        }
    }
}