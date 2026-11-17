package com.tiendagamer.dto;

import com.tiendagamer.model.NombreRol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Usado por el Administrador para crear o editar usuarios. */
public record UsuarioRequest(
        @NotBlank(message = "El RUN es obligatorio")
        @Size(max = 12, message = "El RUN es demasiado largo")
        String run,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        String correo,

        /** Obligatoria al crear; opcional al editar (si viene vacía no se cambia). */
        @Size(max = 60, message = "La contraseña es demasiado larga")
        String password,

        String region,
        String comuna,
        String direccion,

        @NotNull(message = "El rol es obligatorio")
        NombreRol rol,

        Boolean activo) {
}
