package com.tiendagamer.security;

import com.tiendagamer.model.NombreRol;
import com.tiendagamer.model.Rol;
import com.tiendagamer.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRETO = "una-clave-de-pruebas-con-mas-de-32-caracteres-1234";

    private final JwtService jwtService = new JwtService(SECRETO, 30, 7);

    private Usuario usuario(NombreRol rol) {
        Usuario u = new Usuario();
        u.setCorreo("admin@tienda.cl");
        u.setRol(new Rol(rol));
        return u;
    }

    @Test
    void accessTokenContieneCorreoRolYTipo() {
        String token = jwtService.generarAccessToken(usuario(NombreRol.ADMIN));

        Claims claims = jwtService.leerClaims(token);

        assertEquals("admin@tienda.cl", claims.getSubject());
        assertEquals("ADMIN", claims.get("rol", String.class));
        assertEquals(JwtService.TIPO_ACCESS, claims.get("tipo", String.class));
    }

    @Test
    void refreshTokenTieneTipoRefresh() {
        String token = jwtService.generarRefreshToken(usuario(NombreRol.CLIENTE));

        assertEquals(JwtService.TIPO_REFRESH, jwtService.leerClaims(token).get("tipo", String.class));
    }

    @Test
    void tokenAlteradoEsRechazado() {
        String token = jwtService.generarAccessToken(usuario(NombreRol.ADMIN));
        String alterado = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> jwtService.leerClaims(alterado));
    }

    @Test
    void tokenFirmadoConOtraClaveEsRechazado() {
        JwtService otro = new JwtService("otra-clave-distinta-con-mas-de-32-caracteres-9999", 30, 7);
        String token = otro.generarAccessToken(usuario(NombreRol.ADMIN));

        assertThrows(JwtException.class, () -> jwtService.leerClaims(token));
    }

    @Test
    void tokenExpiradoEsRechazado() {
        JwtService expirado = new JwtService(SECRETO, -1, 7);
        String token = expirado.generarAccessToken(usuario(NombreRol.ADMIN));

        assertThrows(JwtException.class, () -> jwtService.leerClaims(token));
    }

    @Test
    void secretoCortoNoEsPermitido() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", 30, 7));
    }
}
