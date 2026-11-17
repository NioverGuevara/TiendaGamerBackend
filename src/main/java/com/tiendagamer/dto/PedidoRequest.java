package com.tiendagamer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PedidoRequest(
        @NotNull(message = "Debes enviar los datos del cliente")
        @Valid
        Cliente cliente,

        @NotEmpty(message = "Debes enviar al menos un producto")
        List<@Valid Item> productos) {

    public record Cliente(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "Correo electrónico no válido") String correo,
            @NotBlank(message = "El teléfono es obligatorio") String telefono,
            @NotBlank(message = "La dirección es obligatoria") String direccion,
            @NotBlank(message = "La región es obligatoria") String region,
            @NotBlank(message = "La comuna es obligatoria") String comuna) {
    }

    public record Item(
            @NotNull(message = "El producto es obligatorio") Long id,
            @NotNull(message = "La cantidad es obligatoria")
            @Positive(message = "La cantidad debe ser mayor a cero") Integer cantidad) {
    }
}
