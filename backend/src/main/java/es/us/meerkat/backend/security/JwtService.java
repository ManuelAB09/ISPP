package es.us.meerkat.backend.security;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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

    /** Tiempo de expiración del token de restablecimiento de contraseña (15 minutos). */
    private static final long PASSWORD_RESET_EXPIRATION = 15 * 60 * 1000L;

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
                .signWith(getSigningKey())
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
        try {
            final Claims claims = extractAllClaims(token);
            final String purpose = claims.get("purpose", String.class);
            // Rechazar tokens con propósito específico (p.ej. password-reset)
            if (purpose != null) {
                return false;
            }
            final Date expiration = claims.getExpiration();
            return claims.getSubject().equals(email)
                    && expiration != null
                    && expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrae la fecha de emisión (iat) del token JWT.
     *
     * @param token Token JWT.
     * @return Date de emisión del token.
     */
    public Date extractIssuedAt(final String token) {
        return extractClaim(token, Claims::getIssuedAt);
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
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
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

    /**
     * Genera un token JWT de corta duración para restablecer contraseña.
     *
     * @param email Email del usuario.
     * @return Token JWT firmado con expiración de 15 minutos.
     */
    public String generatePasswordResetToken(final String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("purpose", "password-reset")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + PASSWORD_RESET_EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Valida un token de restablecimiento de contraseña y extrae el email.
     *
     * @param token Token JWT de restablecimiento.
     * @return Email del usuario si el token es válido.
     * @throws io.jsonwebtoken.JwtException si el token es inválido o ha expirado.
     */
    public String validatePasswordResetToken(final String token) {
        final Claims claims = extractAllClaims(token);
        final String purpose = claims.get("purpose", String.class);
        if (!"password-reset".equals(purpose)) {
            throw new io.jsonwebtoken.JwtException("Token no es de restablecimiento de contraseña");
        }
        return claims.getSubject();
    }
}
