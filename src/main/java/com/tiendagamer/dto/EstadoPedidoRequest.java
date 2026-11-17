package com.tiendagamer.dto;

import com.tiendagamer.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record EstadoPedidoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPedido estado) {
}
