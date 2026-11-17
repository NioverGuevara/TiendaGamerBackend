package com.tiendagamer.service;

import com.tiendagamer.dto.LoginRequest;
import com.tiendagamer.dto.RegistroRequest;
import com.tiendagamer.dto.TokenResponse;
import com.tiendagamer.dto.UsuarioResponse;
import com.tiendagamer.exception.ConflictException;
import com.tiendagamer.exception.CredencialesInvalidasException;
import com.tiendagamer.model.NombreRol;
import com.tiendagamer.model.Usuario;
import com.tiendagamer.repository.RolRepository;
import com.tiendagamer.repository.UsuarioRepository;
import com.tiendagamer.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        // Mensaje genérico: no revela si el correo existe o si falló la contraseña.
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(req.correo().trim())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));

        return emitirTokens(usuario);
    }

    /** El registro público siempre crea usuarios con rol CLIENTE. */
    @Transactional
    public TokenResponse registrar(RegistroRequest req) {
        String correo = req.correo().trim();

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictException("El correo ya está registrado");
        }
        if (usuarioRepository.existsByRun(req.run().trim())) {
            throw new ConflictException("El RUN ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setRun(req.run().trim());
        usuario.setNombre(req.nombre().trim());
        usuario.setApellidos(req.apellidos().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(req.password()));
        usuario.setRegion(req.region());
        usuario.setComuna(req.comuna());
        usuario.setDireccion(req.direccion());
        usuario.setActivo(true);
        usuario.setRol(rolRepository.findByNombre(NombreRol.CLIENTE).orElseThrow());

        return emitirTokens(usuarioRepository.save(usuario));
    }

    /** Renueva el par de tokens a partir de un refresh token válido. */
    @Transactional(readOnly = true)
    public TokenResponse renovar(String refreshToken) {
        Claims claims;

        try {
            claims = jwtService.leerClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new CredencialesInvalidasException("El token de renovación no es válido o expiró");
        }

        if (!JwtService.TIPO_REFRESH.equals(claims.get("tipo", String.class))) {
            throw new CredencialesInvalidasException("El token entregado no es un token de renovación");
        }

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(claims.getSubject())
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new CredencialesInvalidasException("El usuario ya no está disponible"));

        return emitirTokens(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .map(UsuarioResponse::desde)
                .orElseThrow(() -> new CredencialesInvalidasException("El usuario ya no está disponible"));
    }

    private TokenResponse emitirTokens(Usuario usuario) {
        return new TokenResponse(
                jwtService.generarAccessToken(usuario),
                jwtService.generarRefreshToken(usuario),
                "Bearer",
                jwtService.segundosAccess(),
                UsuarioResponse.desde(usuario));
    }
}
