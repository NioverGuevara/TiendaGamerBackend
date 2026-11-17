package com.tiendagamer.controller;

import com.tiendagamer.dto.UsuarioRequest;
import com.tiendagamer.dto.UsuarioResponse;
import com.tiendagamer.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios (solo ADMIN)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Listar usuarios")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @Operation(summary = "Detalle de un usuario")
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @Operation(summary = "Crear usuario con cualquier rol")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest req) {
        return usuarioService.crear(req);
    }

    @Operation(summary = "Actualizar usuario")
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
        return usuarioService.actualizar(id, req);
    }

    @Operation(summary = "Eliminar usuario")
    @DeleteMapping("/{id}")
    public Map<String, String> eliminar(@PathVariable Long id, Authentication autenticacion) {
        usuarioService.eliminar(id, autenticacion.getName());
        return Map.of("mensaje", "Usuario eliminado correctamente");
    }
}
