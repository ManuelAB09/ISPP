package es.us.meerkat.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.meerkat.backend.client.OverpassClient;
import es.us.meerkat.backend.dto.UbicacionRequest;
import es.us.meerkat.backend.dto.UbicacionResponse;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.repository.UbicacionRepository;
import lombok.RequiredArgsConstructor;

/**
 * Servicio para gestionar la lógica de negocio relacionada con las ubicaciones.
 *
 * <p>Permite crear, editar y consultar ubicaciones geográficas asociadas a eventos.
 */
@Service
@RequiredArgsConstructor
public class UbicacionService {

    /** Repositorio para acceder a las ubicaciones. */
    private final UbicacionRepository ubicacionRepository;

    /** Cliente para ejecutar consultas a Overpass API. */
    private final OverpassClient overpassClient;

    /** Mapeador JSON para procesar respuestas de Overpass. */
    private final ObjectMapper objectMapper;

    // ===============================
    // CREAR UBICACIÓN
    // ===============================

    /**
     * Crea una nueva ubicación.
     *
     * @param requestParam Datos de la ubicación.
     * @return DTO con los datos de la ubicación creada.
     */
    @Transactional
    public UbicacionResponse crearUbicacion(final UbicacionRequest requestParam) {
        // Primero, buscamos si ya existe una ubicación con la misma latitud y longitud
        Optional<Ubicacion> existente =
                ubicacionRepository.findByLatitudAndLongitud(
                        requestParam.getLatitud(), requestParam.getLongitud());

        if (existente.isPresent()) {
            // Si existe, devolvemos la que ya está en la base de datos
            return mapToResponse(existente.get());
        }

        // Si no existe, creamos una nueva
        final Ubicacion nuevaUbicacion =
                Ubicacion.builder()
                        .nombre(requestParam.getNombre())
                        .direccion(requestParam.getDireccion())
                        .latitud(requestParam.getLatitud())
                        .longitud(requestParam.getLongitud())
                        .tipo(requestParam.getTipo())
                        .coste(requestParam.getCoste())
                        .build();

        ubicacionRepository.save(nuevaUbicacion);

        return mapToResponse(nuevaUbicacion);
    }

    // ===============================
    // EDITAR UBICACIÓN
    // ===============================

    /**
     * Edita una ubicación existente.
     *
     * @param ubicacionIdParam Identificador de la ubicación.
     * @param requestParam Nuevos datos.
     * @return DTO actualizado.
     */
    @Transactional
    public UbicacionResponse editarUbicacion(
            final Long ubicacionIdParam, final UbicacionRequest requestParam) {

        final Ubicacion ubicacion =
                ubicacionRepository
                        .findById(ubicacionIdParam)
                        .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));

        ubicacion.setNombre(requestParam.getNombre());
        ubicacion.setDireccion(requestParam.getDireccion());
        ubicacion.setLatitud(requestParam.getLatitud());
        ubicacion.setLongitud(requestParam.getLongitud());
        ubicacion.setTipo(requestParam.getTipo());
        ubicacion.setCoste(requestParam.getCoste());

        ubicacionRepository.save(ubicacion);

        return mapToResponse(ubicacion);
    }

    // ===============================
    // OBTENER UBICACIÓN
    // ===============================

    /**
     * Obtiene una ubicación por su ID.
     *
     * @param ubicacionIdParam Identificador.
     * @return DTO con los datos.
     */
    @Transactional(readOnly = true)
    public UbicacionResponse obtenerUbicacion(final Long ubicacionIdParam) {

        final Ubicacion ubicacion =
                ubicacionRepository
                        .findById(ubicacionIdParam)
                        .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));

        return mapToResponse(ubicacion);
    }

    /**
     * Obtiene todas las ubicaciones registradas.
     *
     * @return Lista de ubicaciones.
     */
    @Transactional(readOnly = true)
    public List<Ubicacion> obtenerTodas() {
        return ubicacionRepository.findAll();
    }

    // ===============================
    // ELIMINAR UBICACIÓN
    // ===============================

    /**
     * Elimina una ubicación por su ID.
     *
     * @param ubicacionIdParam Identificador.
     */
    @Transactional
    public void eliminarUbicacion(final Long ubicacionIdParam) {
        if (!ubicacionRepository.existsById(ubicacionIdParam)) {
            throw new RuntimeException("Ubicación no encontrada");
        }

        ubicacionRepository.deleteById(ubicacionIdParam);
    }

    @Transactional(readOnly = true)
    public List<UbicacionResponse> buscarSitiosEstudio(Double lat, Double lon, Integer radio) {

        String query = construirQuery(lat, lon, radio);
        String json = overpassClient.ejecutar(query);

        return parsear(json);
    }

    private String construirQuery(Double lat, Double lon, Integer radio) {
        return String.format(
                Locale.US,
                """
                [out:json];
                (
                  node["amenity"="library"](around:%d,%.6f,%.6f);
                  node["amenity"="community_centre"](around:%d,%.6f,%.6f);
                  node["amenity"="training"](around:%d,%.6f,%.6f);
                  node["amenity"="university"](around:%d,%.6f,%.6f);
                  node["amenity"="hackerspace"](around:%d,%.6f,%.6f);
                  node["amenity"="coworking_space"](around:%d,%.6f,%.6f);
                  node["amenity"="studio"](around:%d,%.6f,%.6f);
                  node["leisure"="park"](around:%d,%.6f,%.6f);
                  node["leisure"="playground"](around:%d,%.6f,%.6f);
                );
                out body center;
                """,
                radio,
                lat,
                lon,
                radio,
                lat,
                lon,
                radio,
                lat,
                lon,
                radio,
                lat,
                lon,
                radio,
                lat,
                lon,
                radio,
                lat,
                lon,
                radio,
                lat,
                lon,
                radio,
                lat,
                lon,
                radio,
                lat,
                lon);
    }

    private List<UbicacionResponse> parsear(String json) {

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode elements = root.get("elements");

            List<UbicacionResponse> lista = new ArrayList<>();

            for (JsonNode e : elements) {

                JsonNode tags = e.get("tags");
                if (tags == null || !tags.has("name")) {
                    continue;
                }

                String tipo =
                        tags.has("amenity")
                                ? tags.get("amenity").asText()
                                : tags.has("leisure")
                                        ? tags.get("leisure").asText()
                                        : "desconocido";

                String coste = clasificarCoste(tags, tipo);
                String direccion = construirDireccion(tags);

                lista.add(
                        UbicacionResponse.builder()
                                .nombre(tags.get("name").asText())
                                .latitud(e.get("lat").asDouble())
                                .longitud(e.get("lon").asDouble())
                                .tipo(tipo)
                                .coste(coste)
                                .direccion(direccion)
                                .build());
            }

            return lista;

        } catch (Exception ex) {
            throw new RuntimeException("Error procesando respuesta Overpass", ex);
        }
    }

    private String clasificarCoste(JsonNode tags, String tipo) {

        String fee = tags.has("fee") ? tags.get("fee").asText() : null;
        String access = tags.has("access") ? tags.get("access").asText() : null;
        String operator = tags.has("operator") ? tags.get("operator").asText() : null;

        if ("yes".equalsIgnoreCase(fee)) {
            return "DE_PAGO";
        }

        if ("no".equalsIgnoreCase(fee)) {
            return "GRATIS";
        }

        if ("private".equalsIgnoreCase(access)) {
            return "DE_PAGO";
        }

        switch (tipo) {
            case "library":
                return "PROBABLEMENTE_GRATIS";

            case "university":
                return "public".equalsIgnoreCase(operator) ? "PARCIALMENTE_GRATIS" : "DESCONOCIDO";

            case "coworking_space":
            case "studio":
                return "PROBABLEMENTE_DE_PAGO";

            case "park":
            case "playground":
                return "GRATIS";

            default:
                return "DESCONOCIDO";
        }
    }

    private String construirDireccion(JsonNode tags) {

        String street = tags.has("addr:street") ? tags.get("addr:street").asText() : "";
        String number = tags.has("addr:housenumber") ? tags.get("addr:housenumber").asText() : "";
        String city = tags.has("addr:city") ? tags.get("addr:city").asText() : "";
        String postcode = tags.has("addr:postcode") ? tags.get("addr:postcode").asText() : "";

        StringBuilder direccion = new StringBuilder();

        if (!street.isEmpty()) {
            direccion.append(street);
            if (!number.isEmpty()) {
                direccion.append(" ").append(number);
            }
        }

        if (!city.isEmpty()) {
            if (direccion.length() > 0) {
                direccion.append(", ");
            }
            direccion.append(city);
        }

        if (!postcode.isEmpty()) {
            direccion.append(" ").append(postcode);
        }

        return direccion.length() > 0 ? direccion.toString() : "Dirección no disponible";
    }

    // ===============================
    // MAPPER
    // ===============================

    /** Mapea entidad a DTO. */
    private UbicacionResponse mapToResponse(final Ubicacion ubicacionParam) {
        return UbicacionResponse.builder()
                .id(ubicacionParam.getId())
                .nombre(ubicacionParam.getNombre())
                .direccion(ubicacionParam.getDireccion())
                .latitud(ubicacionParam.getLatitud())
                .longitud(ubicacionParam.getLongitud())
                .build();
    }
}
