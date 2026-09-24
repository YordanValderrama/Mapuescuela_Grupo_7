package cl.mapuescuela.backend.flowable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Component
public class FlowableProcessClient {

    private static final Logger log = LoggerFactory.getLogger(FlowableProcessClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${flowable.process.rest.base-url}")
    private String baseUrl;

    @Value("${flowable.process.rest.context-path}")
    private String contextPath;

    @Value("${flowable.process.rest.definition-key}")
    private String processDefinitionKey;

    @Value("${flowable.process.rest.username}")
    private String username;

    @Value("${flowable.process.rest.password}")
    private String password;

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        return headers;
    }


    public Map<String, Object> tareaActiva(String processInstanceId) {
        HttpHeaders headers = headers();
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + contextPath + "/runtime/tasks")
                .queryParam("processInstanceId", processInstanceId).queryParam("size", 3)
                .build().encode().toUriString();
        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), Map.class).getBody();
        if (respuesta == null || !(respuesta.get("data") instanceof List<?> tareas)) {
            throw new IllegalStateException("Flowable no devolvió la lista de tareas.");
        }
        if (tareas.isEmpty()) return null;
        if (tareas.size() != 1 || !(tareas.get(0) instanceof Map<?, ?> tarea)) {
            throw new IllegalStateException("Hay más de una tarea activa en el proceso.");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> unica = (Map<String, Object>) tarea;
        return unica;
    }

    public void completarEtapaCliente(String processInstanceId, String clave, Map<String, Object> valores) {
        if (!"ut_datos_personales_modalidad_entrega".equals(clave)
                && !"ut_cargar_comprobante".equals(clave)
                && !"reenviar_comprobante".equals(clave)) {
            throw new IllegalArgumentException("La tarea no es una etapa automatizable del cliente.");
        }
        completarTarea(processInstanceId, clave, valores);
    }


    public void completarEntrega(String processInstanceId, String tipo, Map<String, Object> valores) {
        completarTarea(processInstanceId,
                "RETIRO".equals(tipo) ? "ut_registrar_retiro" : "registraInformacionDespacho", valores);
    }


    public void completarRevision(String processInstanceId, String decision, String observaciones) {
        completarTarea(processInstanceId, "ut_revisar_comprobante",
                Map.of("confirmarPago", decision, "estadoDelPago", observaciones));
    }

    private void completarTarea(String processInstanceId, String clave, Map<String, Object> valores) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            throw new IllegalStateException("El pedido no tiene una instancia de Flowable asociada.");
        }
        HttpHeaders headers = headers();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + contextPath + "/runtime/tasks")
                .queryParam("processInstanceId", processInstanceId)
                .queryParam("taskDefinitionKey", clave)
                .queryParam("size", 2)
                .build().encode().toUriString();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultado = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class).getBody();
            Object datos = resultado == null ? null : resultado.get("data");
            if (!(datos instanceof List<?> tareas) || tareas.size() != 1 || !(tareas.get(0) instanceof Map<?, ?> tarea)
                    || !clave.equals(tarea.get("taskDefinitionKey")) || !(tarea.get("id") instanceof String id)) {
                throw new IllegalStateException("No hay exactamente una tarea " + clave
                        + " pendiente en Flowable para este pedido. Comprueba que el proceso haya llegado a esta etapa.");
            }
            List<Map<String, Object>> variables = new ArrayList<>();
            valores.forEach((nombre, valor) -> variables.add(Map.of("name", nombre, "value", valor)));
            restTemplate.postForEntity(baseUrl + contextPath + "/runtime/tasks/" + id,
                    new HttpEntity<>(Map.of("action", "complete", "variables", variables), headers), String.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.warn("Flowable rechazó completar la tarea {} del proceso {}: HTTP {}", clave, processInstanceId, e.getStatusCode());
            throw new IllegalStateException("Flowable no completó la tarea (HTTP " + e.getStatusCode().value()
                    + "). Comprueba permisos del token, asignación de la tarea y formulario del BPMN.");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("Flowable no está disponible al completar {} del proceso {}", clave, processInstanceId, e);
            throw new IllegalStateException("No se pudo conectar con Flowable. Revisa el estado del pedido y reintenta la tarea.");
        }
    }













    public String iniciarProceso(String idPedido, Map<String, Object> variables) {
        if (processDefinitionKey == null || processDefinitionKey.isBlank()
                || "REEMPLAZAR_KEY_PROCESO".equals(processDefinitionKey)) {
            log.warn("No se inició el proceso en Flowable para el pedido {}: falta configurar "
                    + "flowable.process.rest.definition-key en application.properties.", idPedido);
            return null;
        }

        try {
            List<Map<String, Object>> variablesFlowable = new ArrayList<>();
            for (Map.Entry<String, Object> entrada : variables.entrySet()) {
                variablesFlowable.add(Map.of("name", entrada.getKey(), "value", entrada.getValue()));
            }

            Map<String, Object> body = Map.of(
                    "processDefinitionKey", processDefinitionKey,
                    "businessKey", idPedido,
                    "returnVariables", false,
                    "variables", variablesFlowable
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(username, password);
            HttpEntity<Object> request = new HttpEntity<>(body, headers);

            String url = baseUrl + contextPath + "/runtime/process-instances";
            @SuppressWarnings("unchecked")
            Map<String, Object> respuesta = restTemplate.postForObject(url, request, Map.class);

            String processInstanceId = respuesta != null ? String.valueOf(respuesta.get("id")) : null;
            log.info("Proceso iniciado en Flowable para pedido {}: processInstanceId={}", idPedido, processInstanceId);
            return processInstanceId;

        } catch (Exception e) {
            log.error("No se pudo iniciar el proceso en Flowable para el pedido {}: {}", idPedido, e.getMessage(), e);
            return null;
        }
    }
}
