package es.us.meerkat.backend.service.chats;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import es.us.meerkat.backend.dto.LinkPreviewResponse;
import es.us.meerkat.backend.exception.ValidationException;

/** Servicio para obtener metadatos de URLs y generar tarjetas de vista previa. */
@Service
public class LinkPreviewService {

    /** TTL de la caché local para metadatos de enlaces. */
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    /** Timeout máximo de conexión/lectura para la descarga del HTML. */
    private static final int HTTP_TIMEOUT_MS = 5000;

    /** User-Agent explícito para peticiones salientes. */
    private static final String PREVIEW_USER_AGENT = "MeerkatBot/1.0 (+https://meerkat.es)";

    /** Caché simple en memoria por URL normalizada. */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Obtiene la previsualización de una URL con validación, extracción y caché.
     *
     * @param rawUrl URL recibida desde el cliente.
     * @return Metadatos extraídos para mostrar en una tarjeta.
     */
    public LinkPreviewResponse getPreview(final String rawUrl) {
        final String normalizedUrl = normalizeAndValidate(rawUrl);

        final CacheEntry cached = cache.get(normalizedUrl);
        if (cached != null && !cached.isExpired()) {
            return cached.response();
        }

        final Document document = fetchDocument(normalizedUrl);
        final String resolvedUrl = normalizeAndValidate(document.location());

        final String title =
                firstNonBlank(
                        metaProperty(document, "og:title"),
                        metaName(document, "twitter:title"),
                        safeTrim(document.title()));

        final String description =
                firstNonBlank(
                        metaProperty(document, "og:description"),
                        metaName(document, "twitter:description"),
                        metaName(document, "description"));

        final String image =
                resolveImageUrl(
                        resolvedUrl,
                        firstNonBlank(
                                metaProperty(document, "og:image"),
                                metaName(document, "twitter:image")));

        final String domain = extractDomain(resolvedUrl);
        final String siteName =
                firstNonBlank(
                        metaProperty(document, "og:site_name"),
                        metaName(document, "twitter:site"),
                        domain);

        if (isBlank(title) && isBlank(description) && isBlank(image)) {
            throw new ValidationException("No se han podido extraer metadatos de la URL");
        }

        final LinkPreviewResponse response =
                LinkPreviewResponse.builder()
                        .url(resolvedUrl)
                        .domain(domain)
                        .title(title)
                        .description(description)
                        .siteName(siteName)
                        .image(image)
                        .build();

        cache.put(normalizedUrl, new CacheEntry(response, Instant.now().plus(CACHE_TTL)));
        return response;
    }

    private Document fetchDocument(final String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(PREVIEW_USER_AGENT)
                    .timeout(HTTP_TIMEOUT_MS)
                    .followRedirects(true)
                    .get();
        } catch (IOException ex) {
            throw new ValidationException("No se pudo obtener el contenido de la URL", ex);
        }
    }

    private String normalizeAndValidate(final String rawUrl) {
        if (isBlank(rawUrl)) {
            throw new ValidationException("La URL es obligatoria");
        }

        final String candidate = rawUrl.trim();
        final URI parsed = parseUri(candidate.contains("://") ? candidate : "https://" + candidate);

        final String scheme = parsed.getScheme();
        if (isBlank(scheme)
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new ValidationException("Solo se permiten URLs http/https");
        }

        final String host = parsed.getHost();
        if (isBlank(host)) {
            throw new ValidationException("La URL no contiene un host válido");
        }

        ensurePublicHost(host);
        return parsed.normalize().toString();
    }

    private URI parseUri(final String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException ex) {
            throw new ValidationException("La URL no tiene un formato válido", ex);
        }
    }

    private void ensurePublicHost(final String host) {
        final String normalizedHost = host.trim().toLowerCase();
        if ("localhost".equals(normalizedHost) || normalizedHost.endsWith(".local")) {
            throw new ValidationException("No se permiten hosts locales");
        }

        final InetAddress[] resolvedAddresses;
        try {
            resolvedAddresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            throw new ValidationException("No se pudo resolver el host de la URL", ex);
        }

        for (InetAddress address : resolvedAddresses) {
            if (isPrivateAddress(address)) {
                throw new ValidationException("No se permiten hosts privados o internos");
            }
        }
    }

    private boolean isPrivateAddress(final InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        if (address instanceof Inet4Address ipv4Address) {
            final byte[] octets = ipv4Address.getAddress();
            final int first = octets[0] & 0xFF;
            final int second = octets[1] & 0xFF;

            if (first == 0
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 169 && second == 254)
                    || (first == 198 && (second == 18 || second == 19))) {
                return true;
            }
        }

        return false;
    }

    private String resolveImageUrl(final String baseUrl, final String candidateImageUrl) {
        if (isBlank(candidateImageUrl)) {
            return null;
        }

        try {
            return URI.create(baseUrl).resolve(candidateImageUrl.trim()).toString();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String extractDomain(final String url) {
        try {
            final String host = parseUri(url).getHost();
            return isBlank(host) ? null : host;
        } catch (ValidationException ex) {
            return null;
        }
    }

    private String metaProperty(final Document document, final String property) {
        return safeTrim(document.select("meta[property='" + property + "']").attr("content"));
    }

    private String metaName(final Document document, final String name) {
        return safeTrim(document.select("meta[name='" + name + "']").attr("content"));
    }

    private String firstNonBlank(final String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String safeTrim(final String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Entrada de caché con respuesta y fecha de expiración.
     *
     * @param response Respuesta de metadatos.
     * @param expiresAt Fecha de expiración.
     */
    private record CacheEntry(LinkPreviewResponse response, Instant expiresAt) {

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
