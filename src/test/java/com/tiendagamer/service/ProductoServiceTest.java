package com.tiendagamer.service;

import com.tiendagamer.dto.ProductoRequest;
import com.tiendagamer.dto.ProductoResponse;
import com.tiendagamer.exception.ConflictException;
import com.tiendagamer.exception.NotFoundException;
import com.tiendagamer.model.Categoria;
import com.tiendagamer.model.Producto;
import com.tiendagamer.repository.CategoriaRepository;
import com.tiendagamer.repository.DetallePedidoRepository;
import com.tiendagamer.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock ProductoRepository productoRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock DetallePedidoRepository detallePedidoRepository;

    @InjectMocks ProductoService service;

    private ProductoRequest solicitud() {
        return new ProductoRequest("PROD010", " Mouse Pad ", "Alfombrilla", 12000, 15, null, "Periféricos", null);
    }

    private Producto producto(Long id) {
        Producto p = new Producto();
        p.setId(id);
        p.setCodigo("PROD010");
        p.setNombre("Mouse Pad");
        p.setPrecio(12000);
        p.setStock(15);
        p.setCategoria(new Categoria("Periféricos"));
        return p;
    }

    @Test
    void creaProductoYAsignaValoresPorDefecto() {
        when(productoRepository.existsByCodigoIgnoreCase("PROD010")).thenReturn(false);
        when(categoriaRepository.findByNombreIgnoreCase("Periféricos"))
                .thenReturn(Optional.of(new Categoria("Periféricos")));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoResponse respuesta = service.crear(solicitud());

        assertEquals("Mouse Pad", respuesta.nombre());          // se recortan espacios
        assertEquals(0, respuesta.stockCritico());              // por defecto 0
        assertEquals("Periféricos", respuesta.categoria());
    }

    @Test
    void creaLaCategoriaSiNoExiste() {
        when(productoRepository.existsByCodigoIgnoreCase(any())).thenReturn(false);
        when(categoriaRepository.findByNombreIgnoreCase(any())).thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        service.crear(solicitud());

        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void rechazaCodigoDuplicado() {
        when(productoRepository.existsByCodigoIgnoreCase("PROD010")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.crear(solicitud()));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void obtenerInexistenteLanzaNotFound() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.obtener(99L));
    }

    @Test
    void noEliminaProductoConPedidosAsociados() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L)));
        when(detallePedidoRepository.existsByProductoId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.eliminar(1L));
        verify(productoRepository, never()).delete(any());
    }

    @Test
    void eliminaProductoSinPedidos() {
        Producto p = producto(1L);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(detallePedidoRepository.existsByProductoId(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(productoRepository).delete(p);
    }
}
