package com.tiendagamer.repository;

import com.tiendagamer.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = "detalles")
    List<Pedido> findAllByOrderByFechaDesc();

    boolean existsByUsuarioId(Long usuarioId);
}
