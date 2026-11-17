package com.tiendagamer.controller;

import com.tiendagamer.dto.LoginRequest;
import com.tiendagamer.dto.RefreshRequest;
import com.tiendagamer.dto.RegistroRequest;
import com.tiendagamer.dto.TokenResponse;
import com.tiendagamer.dto.UsuarioResponse;
import com.tiendagamer.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login, registro, renovación de token y perfil")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Iniciar sesión", description = "Devuelve accessToken y refreshToken (JWT)")
    @SecurityRequirements
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @Operation(summary = "Registrar un cliente", description = "Crea un usuario con rol CLIENTE y lo deja con sesión iniciada")
    @SecurityRequirements
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registrar(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req);
    }

    @Operation(summary = "Renovar tokens", description = "Entrega un nuevo par de tokens a partir de un refreshToken válido")
    @SecurityRequirements
    @PostMapping("/refresh")
    public TokenResponse renovar(@Valid @RequestBody RefreshRequest req) {
        return authService.renovar(req.refreshToken());
    }

    @Operation(summary = "Perfil del usuario autenticado")
    @GetMapping("/me")
    public UsuarioResponse perfil(Authentication autenticacion) {
        return authService.perfil(autenticacion.getName());
    }
}
