package com.tiendagamer.security;

import com.tiendagamer.model.Usuario;
import com.tiendagamer.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Lee el header "Authorization: Bearer <token>" y autentica la petición. */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String cabecera = request.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            String token = cabecera.substring(7);

            try {
                Claims claims = jwtService.leerClaims(token);

                // Un refresh token NO sirve para acceder a los recursos.
                if (JwtService.TIPO_ACCESS.equals(claims.get("tipo", String.class))) {
                    autenticar(claims.getSubject());
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Token inválido o expirado: la petición sigue sin autenticar (401).
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private void autenticar(String correo) {
        // Se consulta la BD para respetar usuarios eliminados, desactivados o con rol cambiado.
        usuarioRepository.findByCorreoIgnoreCase(correo)
                .filter(Usuario::isActivo)
                .ifPresent(usuario -> {
                    var autoridad = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre().name());
                    var autenticacion = new UsernamePasswordAuthenticationToken(
                            usuario.getCorreo(), null, List.of(autoridad));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                });
    }
}
