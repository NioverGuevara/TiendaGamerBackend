package com.tiendagamer.service;

import com.tiendagamer.dto.PedidoRequest;
import com.tiendagamer.dto.PedidoResponse;
import com.tiendagamer.exception.BusinessException;
import com.tiendagamer.exception.ConflictException;
import com.tiendagamer.exception.NotFoundException;
import com.tiendagamer.model.*;
import com.tiendagamer.repository.PedidoRepository;
import com.tiendagamer.repository.ProductoRepository;
import com.tiendagamer.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock ProductoRepository productoRepository;
    @Mock UsuarioRepository usuarioRepository;

    @InjectMocks PedidoService service;

    private Producto mouse;
    private Producto teclado;

    @BeforeEach
    void preparar() {
        mouse = producto(1L, "Mouse Gamer", 20000, 5);
        teclado = producto(2L, "Teclado", 39990, 3);
    }

    private Producto producto(Long id, String nombre, int precio, int stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(stock);
        return p;
    }

    private PedidoRequest.Cliente cliente() {
        return new PedidoRequest.Cliente("Juan Pérez", "juan@correo.cl", "+56911111111",
                "Calle 123", "Metropolitana", "Santiago");
    }

    private PedidoRequest pedido(PedidoRequest.Item... items) {
        return new PedidoRequest(cliente(), List.of(items));
    }

    private void simularGuardado() {
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void calculaTotalYDescuentaStock() {
        when(productoRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(mouse));
        when(productoRepository.buscarParaActualizar(2L)).thenReturn(Optional.of(teclado));
        simularGuardado();

        PedidoResponse respuesta = service.crear(pedido(
                new PedidoRequest.Item(1L, 2),
                new PedidoRequest.Item(2L, 1)), "juan@correo.cl");

        assertEquals(79990, respuesta.total());                 // 2*20000 + 39990
        assertEquals("CONFIRMADO", respuesta.estado());
        assertEquals(3, mouse.getStock());
        assertEquals(2, teclado.getStock());
    }

    @Test
    void sumaCantidadesDeProductosRepetidos() {
        when(productoRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(mouse));
        simularGuardado();

        PedidoResponse respuesta = service.crear(pedido(
                new PedidoRequest.Item(1L, 1),
                new PedidoRequest.Item(1L, 2)), "juan@correo.cl");

        assertEquals(1, respuesta.productos().size());
        assertEquals(3, respuesta.productos().get(0).cantidad());
        assertEquals(60000, respuesta.total());
    }

    @Test
    void rechazaCompraConStockInsuficiente() {
        when(productoRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(mouse));

        assertThrows(ConflictException.class,
                () -> service.crear(pedido(new PedidoRequest.Item(1L, 6)), "juan@correo.cl"));

        assertEquals(5, mouse.getStock());                      // el stock no cambia
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void rechazaProductoInexistente() {
        when(productoRepository.buscarParaActualizar(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.crear(pedido(new PedidoRequest.Item(99L, 1)), "juan@correo.cl"));
    }

    private Pedido pedidoExistente(EstadoPedido estado) {
        Pedido p = new Pedido();
        p.setId(10L);
        p.setFecha(LocalDateTime.now());
        p.setEstado(estado);
        p.setTotal(40000);
        p.setClienteNombre("Juan");
        p.setClienteCorreo("juan@correo.cl");
        p.setClienteTelefono("1");
        p.setDireccion("x");
        p.setRegion("x");
        p.setComuna("x");

        DetallePedido d = new DetallePedido();
        d.setProducto(mouse);
        d.setNombreProducto(mouse.getNombre());
        d.setPrecioUnitario(20000);
        d.setCantidad(2);
        d.setSubtotal(40000);
        p.agregarDetalle(d);
        return p;
    }

    @Test
    void cancelarPedidoDevuelveElStock() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedidoExistente(EstadoPedido.CONFIRMADO)));
        when(productoRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(mouse));

        PedidoResponse respuesta = service.cambiarEstado(10L, EstadoPedido.CANCELADO);

        assertEquals("CANCELADO", respuesta.estado());
        assertEquals(7, mouse.getStock());                      // 5 + 2
    }

    @Test
    void noPermiteCambiarUnPedidoCancelado() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedidoExistente(EstadoPedido.CANCELADO)));

        assertThrows(BusinessException.class, () -> service.cambiarEstado(10L, EstadoPedido.ENVIADO));
    }

    @Test
    void noPermiteRetrocederDeEstado() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedidoExistente(EstadoPedido.ENVIADO)));

        assertThrows(BusinessException.class, () -> service.cambiarEstado(10L, EstadoPedido.CONFIRMADO));
    }

    @Test
    void avanzaDeConfirmadoAEnviado() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedidoExistente(EstadoPedido.CONFIRMADO)));

        PedidoResponse respuesta = service.cambiarEstado(10L, EstadoPedido.ENVIADO);

        assertEquals("ENVIADO", respuesta.estado());
        assertEquals(5, mouse.getStock());                      // no toca el stock
    }
}
