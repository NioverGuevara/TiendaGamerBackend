package com.tiendagamer.security;

import com.tiendagamer.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * Genera y valida los tokens JWT (firmados con HMAC-SHA256).
 * Hay dos tipos: "access" (corta duración, se envía en cada petición)
 * y "refresh" (larga duración, solo sirve para pedir un nuevo par de tokens).
 */
@Service
public class JwtService {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey clave;
    private final Duration duracionAccess;
    private final Duration duracionRefresh;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.access-minutes}") long minutosAccess,
                      @Value("${app.jwt.refresh-days}") long diasRefresh) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracionAccess = Duration.ofMinutes(minutosAccess);
        this.duracionRefresh = Duration.ofDays(diasRefresh);
    }

    public String generarAccessToken(Usuario usuario) {
        return construir(usuario, TIPO_ACCESS, duracionAccess);
    }

    public String generarRefreshToken(Usuario usuario) {
        return construir(usuario, TIPO_REFRESH, duracionRefresh);
    }

    public long segundosAccess() {
        return duracionAccess.toSeconds();
    }

    /** Verifica firma y expiración. Lanza JwtException si el token no es válido. */
    public Claims leerClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String construir(Usuario usuario, String tipo, Duration duracion) {
        Date ahora = new Date();

        return Jwts.builder()
                .subject(usuario.getCorreo())
                .claim("rol", usuario.getRol().getNombre().name())
                .claim("tipo", tipo)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracion.toMillis()))
                .signWith(clave)
                .compact();
    }
}
