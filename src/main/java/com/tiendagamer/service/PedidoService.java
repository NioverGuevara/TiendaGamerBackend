package com.tiendagamer.service;

import com.tiendagamer.dto.PedidoRequest;
import com.tiendagamer.dto.PedidoResponse;
import com.tiendagamer.exception.BusinessException;
import com.tiendagamer.exception.ConflictException;
import com.tiendagamer.exception.NotFoundException;
import com.tiendagamer.model.DetallePedido;
import com.tiendagamer.model.EstadoPedido;
import com.tiendagamer.model.Pedido;
import com.tiendagamer.model.Producto;
import com.tiendagamer.repository.PedidoRepository;
import com.tiendagamer.repository.ProductoRepository;
import com.tiendagamer.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Lógica de negocio de las compras: validación de stock, totales y cambios de estado. */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductoRepository productoRepository,
                         UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {
        return pedidoRepository.findAllByOrderByFechaDesc().stream().map(PedidoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return PedidoResponse.desde(buscar(id));
    }

    /**
     * Registra una compra. Todo ocurre en una sola transacción: si algo falla
     * (por ejemplo, falta stock de un producto) no se guarda nada ni se descuenta stock.
     */
    @Transactional
    public PedidoResponse crear(PedidoRequest req, String correoUsuario) {
        // 1) Si el mismo producto viene repetido, se suman las cantidades.
        //    TreeMap ordena por id para bloquear siempre en el mismo orden (evita bloqueos cruzados).
        Map<Long, Integer> cantidades = new TreeMap<>();
        for (PedidoRequest.Item item : req.productos()) {
            cantidades.merge(item.id(), item.cantidad(), Integer::sum);
        }

        Pedido pedido = new Pedido();
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        pedido.setClienteNombre(req.cliente().nombre().trim());
        pedido.setClienteCorreo(req.cliente().correo().trim());
        pedido.setClienteTelefono(req.cliente().telefono().trim());
        pedido.setDireccion(req.cliente().direccion().trim());
        pedido.setRegion(req.cliente().region().trim());
        pedido.setComuna(req.cliente().comuna().trim());
        usuarioRepository.findByCorreoIgnoreCase(correoUsuario).ifPresent(pedido::setUsuario);

        // 2) Validar stock, calcular subtotales y descontar inventario.
        int total = 0;

        for (Map.Entry<Long, Integer> entrada : cantidades.entrySet()) {
            Producto producto = productoRepository.buscarParaActualizar(entrada.getKey())
                    .orElseThrow(() -> new NotFoundException("El producto " + entrada.getKey() + " no existe"));

            int cantidad = entrada.getValue();

            if (cantidad > producto.getStock()) {
                throw new ConflictException("Stock insuficiente para " + producto.getNombre()
                        + ". Disponible: " + producto.getStock());
            }

            int subtotal = producto.getPrecio() * cantidad;

            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setNombreProducto(producto.getNombre());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setCantidad(cantidad);
            detalle.setSubtotal(subtotal);
            pedido.agregarDetalle(detalle);

            producto.setStock(producto.getStock() - cantidad);
            total += subtotal;
        }

        pedido.setTotal(total);
        return PedidoResponse.desde(pedidoRepository.save(pedido));
    }

    /**
     * Cambia el estado de un pedido. Reglas: no se retrocede de estado, un pedido
     * ENTREGADO o CANCELADO queda cerrado, y al cancelar se devuelve el stock.
     */
    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevo) {
        Pedido pedido = buscar(id);
        EstadoPedido actual = pedido.getEstado();

        if (actual == EstadoPedido.CANCELADO || actual == EstadoPedido.ENTREGADO) {
            throw new BusinessException("Un pedido " + actual + " ya no puede cambiar de estado");
        }
        if (nuevo != EstadoPedido.CANCELADO && nuevo.ordinal() < actual.ordinal()) {
            throw new BusinessException("No se puede volver del estado " + actual + " a " + nuevo);
        }

        if (nuevo == EstadoPedido.CANCELADO) {
            devolverStock(pedido);
        }

        pedido.setEstado(nuevo);
        return PedidoResponse.desde(pedido);
    }

    @Transactional
    public void eliminar(Long id) {
        Pedido pedido = buscar(id);

        if (pedido.getEstado() != EstadoPedido.CANCELADO) {
            devolverStock(pedido);
        }

        pedidoRepository.delete(pedido);
    }

    private void devolverStock(Pedido pedido) {
        for (DetallePedido detalle : pedido.getDetalles()) {
            productoRepository.buscarParaActualizar(detalle.getProducto().getId())
                    .ifPresent(p -> p.setStock(p.getStock() + detalle.getCantidad()));
        }
    }

    private Pedido buscar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El pedido " + id + " no existe"));
    }
}
