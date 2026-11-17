package com.tiendagamer.repository;

import com.tiendagamer.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
    boolean existsByProductoId(Long productoId);
}
