package com.tiendagamer.dto;

import com.tiendagamer.model.Producto;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Integer precio,
        Integer stock,
        Integer stockCritico,
        String categoria,
        String imagen) {

    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(
                p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(),
                p.getPrecio(), p.getStock(), p.getStockCritico(),
                p.getCategoria().getNombre(), p.getImagen());
    }
}
