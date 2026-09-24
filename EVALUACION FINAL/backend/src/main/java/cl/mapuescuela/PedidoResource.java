package cl.mapuescuela;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import cl.mapuescuela.backend.flowable.FlowableProcessClient;
import cl.mapuescuela.backend.inventario.InventarioRepository;
import cl.mapuescuela.backend.inventario.InventarioEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;













@Path("pedidos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Component
public class PedidoResource {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private EntregaRepository entregaRepository;

    @Autowired
    private FlowableProcessClient flowableProcessClient;

    @Value("${mapuescuela.uploads.dir:./data/uploads}")
    private String uploadsDir;







    @POST
    @Transactional
    public Response crearPedido(Pedido pedido) {
        if (pedido == null || pedido.getProducto() == null || pedido.getProducto().trim().isEmpty()) {
            return respuestaError("El campo 'producto' es obligatorio.");
        }
        if (pedido.getCantidad() <= 0) {
            return respuestaError("La cantidad debe ser positiva.");
        }
        if (pedido.getNombreCliente() == null || pedido.getNombreCliente().isBlank()
                || pedido.getCorreoCliente() == null || pedido.getCorreoCliente().isBlank()
                || pedido.getRutCliente() == null || pedido.getRutCliente().isBlank()
                || pedido.getTelefonoCliente() == null || pedido.getTelefonoCliente().isBlank()) {
            return respuestaError("Completa nombre, correo, RUT y teléfono del cliente.");
        }
        if ("reparto_domicilio".equals(pedido.getModalidadEntrega())
                && (pedido.getDireccion() == null || pedido.getDireccion().isBlank()
                || pedido.getRegion() == null || pedido.getRegion().isBlank()
                || pedido.getComuna() == null || pedido.getComuna().isBlank())) {
            return respuestaError("Para despacho completa dirección, región y comuna.");
        }

        InventarioEntity disponible = inventarioRepository.findByProducto(pedido.getProducto()).orElse(null);
        if (disponible == null || !disponible.isActivo() || disponible.getPrecio() <= 0
                || disponible.getStockDisponible() < pedido.getCantidad()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("mensaje", "Producto no disponible o stock insuficiente.")).build();
        }

        String idPedido = "PED-" + Instant.now().toEpochMilli();
        pedido.setIdPedido(idPedido);
        pedido.setEstadoPago("PENDIENTE_PAGO");

        PedidoEntity entidad = aEntidad(pedido);
        pedidoRepository.save(entidad);








        Map<String, Object> variablesProceso = new LinkedHashMap<>();
        variablesProceso.put("nPedido", idPedido);
        variablesProceso.put("nombreCompleto", pedido.getNombreCliente());
        variablesProceso.put("correoElectronico", pedido.getCorreoCliente());
        variablesProceso.put("rUT", pedido.getRutCliente());
        variablesProceso.put("telefono", pedido.getTelefonoCliente());
        variablesProceso.put("direccion", pedido.getDireccion() == null ? "" : pedido.getDireccion());
        variablesProceso.put("region", pedido.getRegion() == null ? "" : pedido.getRegion());
        variablesProceso.put("comuna", pedido.getComuna() == null ? "" : pedido.getComuna());
        variablesProceso.put("producto", pedido.getProducto());
        variablesProceso.put("cantidad", pedido.getCantidad());
        variablesProceso.put("seleccioneModalidadDeEntrega", pedido.getModalidadEntrega());

        String processInstanceId = flowableProcessClient.iniciarProceso(idPedido, variablesProceso);
        if (processInstanceId != null) {
            entidad.setProcessInstanceId(processInstanceId);
            pedidoRepository.save(entidad);
        }

        return Response.status(Response.Status.CREATED)
                .entity(crearRespuestaBase(pedido, "Pedido creado. Realice la transferencia y adjunte el comprobante."))
                .build();
    }

    @GET
    @Path("{idPedido}")
    public Response obtenerPedido(@PathParam("idPedido") String idPedido) {
        return pedidoRepository.findById(idPedido)
                .map(entidad -> Response.ok(entidad).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("{idPedido}/entrega")
    public Response obtenerEntrega(@PathParam("idPedido") String idPedido) {
        return entregaRepository.findById(idPedido)
                .map(e -> Response.ok(e).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("{idPedido}/entrega")
    @Transactional
    public Response registrarEntrega(@PathParam("idPedido") String idPedido, Map<String, Object> body) {
        PedidoEntity pedido = pedidoRepository.findById(idPedido).orElse(null);
        if (pedido == null) return Response.status(Response.Status.NOT_FOUND).build();
        if (!"APROBADO".equals(pedido.getEstadoPago())) return respuestaError("El pago debe estar aprobado.");
        if (entregaRepository.existsById(idPedido)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("mensaje", "La entrega ya fue registrada.")).build();
        }
        if (body == null) return respuestaError("Faltan datos de entrega.");
        String tipo = String.valueOf(body.getOrDefault("tipo", ""));
        String modalidad = pedido.getModalidadEntrega();
        if (!("RETIRO".equals(tipo) && "retiro_tienda".equals(modalidad))
                && !("DESPACHO".equals(tipo) && "reparto_domicilio".equals(modalidad))) {
            return respuestaError("La entrega no coincide con la modalidad del pedido.");
        }
        String responsable = String.valueOf(body.getOrDefault("voluntario", "")).trim();
        String empresa = String.valueOf(body.getOrDefault("empresa", "")).trim();
        if ("RETIRO".equals(tipo) && responsable.isBlank()) return respuestaError("Indica el voluntario que entrega.");
        if ("DESPACHO".equals(tipo) && empresa.isBlank()) return respuestaError("Indica la empresa de transporte.");
        EntregaEntity entrega = new EntregaEntity();
        entrega.setIdPedido(idPedido);
        entrega.setTipo(tipo);
        entrega.setVoluntario(responsable);
        entrega.setRutVoluntario(String.valueOf(body.getOrDefault("rutVoluntario", "")));
        entrega.setObservaciones(String.valueOf(body.getOrDefault("observaciones", "")));
        entrega.setEmpresa(empresa);
        entrega.setNumeroSeguimiento(String.valueOf(body.getOrDefault("numeroSeguimiento", "")));
        entrega.setFechaRegistro(LocalDateTime.now());
        return Response.status(Response.Status.CREATED).entity(entregaRepository.save(entrega)).build();
    }


    @POST
    @Path("{idPedido}/entrega/completar")
    public Response completarEntrega(@PathParam("idPedido") String idPedido) {
        PedidoEntity pedido = pedidoRepository.findById(idPedido).orElse(null);
        EntregaEntity entrega = entregaRepository.findById(idPedido).orElse(null);
        if (pedido == null || entrega == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("mensaje", "Primero debes guardar el registro de entrega del pedido.")).build();
        }
        if (!"APROBADO".equals(pedido.getEstadoPago())) return respuestaError("El pago debe estar aprobado.");
        String tipo = entrega.getTipo();
        if (!("RETIRO".equals(tipo) && "retiro_tienda".equals(pedido.getModalidadEntrega()))
                && !("DESPACHO".equals(tipo) && "reparto_domicilio".equals(pedido.getModalidadEntrega()))) {
            return respuestaError("La modalidad de entrega no coincide con el pedido.");
        }
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("nPedido", idPedido);
        if ("RETIRO".equals(tipo)) {
            valores.put("voluntarioQueEntregaPedido", entrega.getVoluntario());
            valores.put("rutVoluntario", entrega.getRutVoluntario());
            valores.put("observacionesDelRetiro", entrega.getObservaciones());

            valores.put("fechaYHoraDeEntrega", entrega.getFechaRegistro().toLocalDate().toString());
        } else {
            valores.put("empresa", entrega.getEmpresa());
            valores.put("nDeSeguimiento", entrega.getNumeroSeguimiento());
        }
        try {
            flowableProcessClient.completarEntrega(pedido.getProcessInstanceId(), tipo, valores);
            return Response.ok(Map.of("resultado", "OK", "mensaje", "Registro guardado y tarea completada en Flowable Work.")).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("resultado", "PENDIENTE_FLOWABLE", "mensaje", e.getMessage())).build();
        }
    }


    @POST
    @Path("{idPedido}/revision")
    public Response revisarComprobante(@PathParam("idPedido") String idPedido, Map<String, Object> body) {
        PedidoEntity entidad = pedidoRepository.findById(idPedido).orElse(null);
        if (entidad == null) return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("mensaje", "No existe ese pedido.")).build();
        if (!"EN_REVISION".equals(entidad.getEstadoPago()) || entidad.getNombreArchivoComprobante() == null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("mensaje", "El pedido no tiene un comprobante pendiente de revisión.")).build();
        }
        String decision = body == null ? "" : String.valueOf(body.getOrDefault("decision", ""));
        if (!"pago_aprobado".equals(decision) && !"pago_rechazado".equals(decision)
                && !"error_comprobante".equals(decision)) return respuestaError("Decisión de pago no válida.");
        String observaciones = body == null ? "" : String.valueOf(body.getOrDefault("observaciones", "")).trim();
        if (observaciones.length() > 500) return respuestaError("Las observaciones no pueden superar 500 caracteres.");
        if (!"pago_aprobado".equals(decision) && observaciones.isBlank()) {
            return respuestaError("Indica el motivo del rechazo o del error de comprobante.");
        }
        try {
            flowableProcessClient.completarRevision(entidad.getProcessInstanceId(), decision, observaciones);
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("mensaje", e.getMessage())).build();
        }


        entidad.setEstadoPago(switch (decision) {
            case "pago_aprobado" -> "APROBADO";
            case "pago_rechazado" -> "RECHAZADO";
            default -> "ERROR_COMPROBANTE";
        });
        pedidoRepository.save(entidad);
        return Response.ok(Map.of("resultado", "OK", "estadoPago", entidad.getEstadoPago(),
                "mensaje", "Decisión guardada y tarea de revisión completada en Flowable.")).build();
    }

    @GET
    @Path("{idPedido}/etapa-flowable")
    public Response etapaFlowable(@PathParam("idPedido") String idPedido) {
        PedidoEntity pedido = pedidoRepository.findById(idPedido).orElse(null);
        if (pedido == null) return Response.status(Response.Status.NOT_FOUND).build();
        if (pedido.getProcessInstanceId() == null || pedido.getProcessInstanceId().isBlank()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("mensaje", "Este pedido no tiene proceso Flowable asociado.")).build();
        }
        try {
            Map<String, Object> tarea = flowableProcessClient.tareaActiva(pedido.getProcessInstanceId());
            String clave = tarea == null ? "PROCESANDO" : String.valueOf(tarea.get("taskDefinitionKey"));
            return Response.ok(Map.of("etapa", clave,
                    "revisionDisponible", "ut_revisar_comprobante".equals(clave))).build();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("mensaje", "No se pudo consultar la etapa en Flowable.")).build();
        }
    }










    @POST
    @Path("{idPedido}/comprobante")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response subirComprobante(@PathParam("idPedido") String idPedido,
                                      @FormDataParam("archivo") InputStream archivo,
                                      @FormDataParam("archivo") FormDataContentDisposition detalleArchivo) {
        Optional<PedidoEntity> existente = pedidoRepository.findById(idPedido);
        if (existente.isEmpty()) {
            return respuestaError("No existe un pedido con id " + idPedido);
        }
        if (archivo == null || detalleArchivo == null) {
            return respuestaError("Falta el archivo del comprobante (campo 'archivo').");
        }

        try {
            java.nio.file.Path carpeta = Paths.get(uploadsDir);
            Files.createDirectories(carpeta);

            String nombreOriginal = detalleArchivo.getFileName();
            String extension = "";
            int punto = nombreOriginal != null ? nombreOriginal.lastIndexOf('.') : -1;
            if (punto >= 0) {
                extension = nombreOriginal.substring(punto);
            }

            String nombreGuardado = idPedido + extension;
            java.nio.file.Path destino = carpeta.resolve(nombreGuardado);
            Files.copy(archivo, destino, StandardCopyOption.REPLACE_EXISTING);

            PedidoEntity entidad = existente.get();
            entidad.setNombreArchivoComprobante(nombreGuardado);
            entidad.setEstadoPago("EN_REVISION");
            pedidoRepository.save(entidad);

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("resultado", "OK");
            respuesta.put("idPedido", idPedido);
            respuesta.put("estadoPago", "EN_REVISION");
            respuesta.put("archivo", nombreGuardado);
            respuesta.put("mensaje", "Comprobante recibido, en espera de revisión.");
            return Response.ok(respuesta).build();

        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("resultado", "ERROR", "mensaje", "No se pudo guardar el archivo: " + e.getMessage()))
                    .build();
        }
    }






    @GET
    @Path("{idPedido}/comprobante")
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, "image/*", "application/pdf" })
    public Response descargarComprobante(@PathParam("idPedido") String idPedido) {
        Optional<PedidoEntity> existente = pedidoRepository.findById(idPedido);
        if (existente.isEmpty() || existente.get().getNombreArchivoComprobante() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            java.nio.file.Path archivo = Paths.get(uploadsDir).resolve(existente.get().getNombreArchivoComprobante());
            if (!Files.exists(archivo)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            byte[] contenido = Files.readAllBytes(archivo);
            String tipoContenido = Files.probeContentType(archivo);
            if (tipoContenido == null) {
                tipoContenido = MediaType.APPLICATION_OCTET_STREAM;
            }

            return Response.ok(contenido)
                    .type(tipoContenido)
                    .header("Content-Disposition", "inline; filename=\"" + archivo.getFileName() + "\"")
                    .build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("aprobar-pago")
    @Transactional
    public Response aprobarPago(Pedido pedido) {
        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        PedidoEntity entidad = buscarOCrear(pedido);
        entidad.setEstadoPago("APROBADO");
        pedidoRepository.save(entidad);

        pedido.setEstadoPago("APROBADO");
        Map<String, Object> respuesta = crearRespuestaBase(
                pedido,
                "Pago aprobado. El inventario se descuenta cuando Flowable ejecuta la tarea actualizar-inventario."
        );
        respuesta.put("inventarioActualizado", false);

        return Response.ok(respuesta).build();
    }

    @POST
    @Path("rechazar-pago")
    @Transactional
    public Response rechazarPago(Pedido pedido) {
        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        PedidoEntity entidad = buscarOCrear(pedido);
        entidad.setEstadoPago("RECHAZADO");
        pedidoRepository.save(entidad);

        pedido.setEstadoPago("RECHAZADO");
        Map<String, Object> respuesta = crearRespuestaBase(pedido, "El pago fue rechazado.");

        return Response.ok(respuesta).build();
    }

    @POST
    @Path("liberar-stock")
    @Transactional
    public Response liberarStock(Pedido pedido) {
        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        PedidoEntity entidad = buscarOCrear(pedido);
        entidad.setEstadoPago("CANCELADO_POR_TIEMPO");
        pedidoRepository.save(entidad);

        pedido.setEstadoPago("CANCELADO_POR_TIEMPO");
        Map<String, Object> respuesta = crearRespuestaBase(
                pedido,
                "Pedido cancelado por superar las 24 horas sin comprobante."
        );
        respuesta.put("stockLiberado", true);
        respuesta.put("cantidadLiberada", pedido.getCantidad());

        return Response.ok(respuesta).build();
    }








    @POST
    @Path("marcar-error-comprobante")
    @Transactional
    public Response marcarErrorComprobante(Pedido pedido) {
        if (pedidoInvalido(pedido)) {
            return respuestaError();
        }

        PedidoEntity entidad = buscarOCrear(pedido);
        entidad.setEstadoPago("ERROR_COMPROBANTE");
        pedidoRepository.save(entidad);

        pedido.setEstadoPago("ERROR_COMPROBANTE");
        Map<String, Object> respuesta = crearRespuestaBase(
                pedido,
                "El comprobante no pudo procesarse; se solicitó al cliente que lo reenvíe."
        );

        return Response.ok(respuesta).build();
    }



    private PedidoEntity buscarOCrear(Pedido pedido) {
        Optional<PedidoEntity> existente = pedidoRepository.findById(pedido.getIdPedido());
        return existente.orElseGet(() -> aEntidad(pedido));
    }

    private PedidoEntity aEntidad(Pedido pedido) {
        PedidoEntity entidad = new PedidoEntity();
        entidad.setIdPedido(pedido.getIdPedido());
        entidad.setNombreCliente(pedido.getNombreCliente());
        entidad.setCorreoCliente(pedido.getCorreoCliente());
        entidad.setRutCliente(pedido.getRutCliente());
        entidad.setTelefonoCliente(pedido.getTelefonoCliente());
        entidad.setDireccion(pedido.getDireccion());
        entidad.setRegion(pedido.getRegion());
        entidad.setComuna(pedido.getComuna());
        entidad.setProducto(pedido.getProducto());
        entidad.setCantidad(pedido.getCantidad() > 0 ? pedido.getCantidad() : 1);
        entidad.setEstadoPago(pedido.getEstadoPago());
        entidad.setModalidadEntrega(pedido.getModalidadEntrega());
        return entidad;
    }

    private boolean pedidoInvalido(Pedido pedido) {
        return pedido == null
                || pedido.getIdPedido() == null
                || pedido.getIdPedido().trim().isEmpty();
    }

    private Response respuestaError() {
        return respuestaError("El idPedido es obligatorio.");
    }

    private Response respuestaError(String mensaje) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("resultado", "ERROR");
        error.put("mensaje", mensaje);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .build();
    }

    private Map<String, Object> crearRespuestaBase(Pedido pedido, String mensaje) {
        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("resultado", "OK");
        respuesta.put("idPedido", pedido.getIdPedido());
        respuesta.put("estadoPago", pedido.getEstadoPago());
        respuesta.put("producto", pedido.getProducto());
        respuesta.put("cantidad", pedido.getCantidad());
        respuesta.put("mensaje", mensaje);

        return respuesta;
    }
}
