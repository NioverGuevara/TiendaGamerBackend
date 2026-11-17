package com.tiendagamer.dto;

import com.tiendagamer.model.Pedido;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        LocalDateTime fecha,
        String estado,
        Integer total,
        Cliente cliente,
        List<Item> productos) {

    public record Cliente(String nombre, String correo, String telefono,
                          String direccion, String region, String comuna) {
    }

    public record Item(Long id, String nombre, Integer precio, Integer cantidad, Integer subtotal) {
    }

    public static PedidoResponse desde(Pedido p) {
        List<Item> items = p.getDetalles().stream()
                .map(d -> new Item(d.getProducto().getId(), d.getNombreProducto(),
                        d.getPrecioUnitario(), d.getCantidad(), d.getSubtotal()))
                .toList();

        return new PedidoResponse(
                p.getId(), p.getFecha(), p.getEstado().name(), p.getTotal(),
                new Cliente(p.getClienteNombre(), p.getClienteCorreo(), p.getClienteTelefono(),
                        p.getDireccion(), p.getRegion(), p.getComuna()),
                items);
    }
}
