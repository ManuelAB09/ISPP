package es.us.meerkat.backend.security;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Servicio para generación y validación de tokens JWT.
 *
 * <p>Utiliza la librería JJWT. La clave secreta y el tiempo de expiración se configuran en
 * application.yaml.
 */
@Service
public class JwtService {

    /** Clave secreta en formato hexadecimal desde application.yaml. */
    @Value("${jwt.secret}")
    private String secretKey;

    /** Tiempo de expiración en milisegundos (por defecto 24h). */
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * Extrae el email (subject) del token JWT.
     *
     * @param token Token JWT.
     * @return Email del usuario.
     */
    public String extractEmail(final String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Genera un token JWT para el email dado.
     *
     * @param email Email del usuario (subject del token).
     * @return Token JWT firmado.
     */
    public String generateToken(final String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida que el token pertenezca al email y no esté expirado.
     *
     * @param token Token JWT.
     * @param email Email esperado.
     * @return true si el token es válido.
     */
    public boolean isTokenValid(final String token, final String email) {
        return extractEmail(token).equals(email) && !isTokenExpired(token);
    }

    /**
     * Extrae un claim específico del token.
     *
     * @param token Token JWT.
     * @param claimsResolver Función extractora del claim.
     * @param <T> Tipo del claim.
     * @return Valor del claim.
     */
    public <T> T extractClaim(final String token, final Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    /**
     * Comprueba si el token ha expirado.
     *
     * @param token Token JWT.
     * @return true si está expirado.
     */
    private boolean isTokenExpired(final String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Extrae todos los claims del token.
     *
     * @param token Token JWT.
     * @return Claims del token.
     */
    private Claims extractAllClaims(final String token) {
        return Jwts.parser()
                .verifyingKey(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Obtiene la clave de firma a partir de la clave secreta.
     *
     * @return Clave de firma HMAC.
     */
    private Key getSigningKey() {
        final byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
