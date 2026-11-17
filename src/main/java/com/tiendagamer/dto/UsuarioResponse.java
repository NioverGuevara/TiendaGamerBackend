package com.tiendagamer.dto;

import com.tiendagamer.model.Usuario;

public record UsuarioResponse(
        Long id,
        String run,
        String nombre,
        String apellidos,
        String correo,
        String region,
        String comuna,
        String direccion,
        String rol,
        boolean activo) {

    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getRun(), u.getNombre(), u.getApellidos(), u.getCorreo(),
                u.getRegion(), u.getComuna(), u.getDireccion(),
                u.getRol().getNombre().name(), u.isActivo());
    }
}
