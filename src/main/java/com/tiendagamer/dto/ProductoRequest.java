package com.tiendagamer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 30, message = "El código es demasiado largo")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre es demasiado largo")
        String nombre,

        @Size(max = 500, message = "La descripción es demasiado larga")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        Integer precio,

        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,

        @PositiveOrZero(message = "El stock crítico no puede ser negativo")
        Integer stockCritico,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 60, message = "La categoría es demasiado larga")
        String categoria,

        String imagen) {
}
