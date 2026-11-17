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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           DetallePedidoRepository detallePedidoRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream().map(ProductoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.desde(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        String codigo = req.codigo().trim();

        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe un producto con el código " + codigo);
        }

        Producto producto = new Producto();
        aplicar(producto, req);
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto producto = buscar(id);
        String codigo = req.codigo().trim();

        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new ConflictException("Ya existe otro producto con el código " + codigo);
        }

        aplicar(producto, req);
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscar(id);

        // Regla de negocio: no se borra un producto que ya fue vendido (se perdería el historial).
        if (detallePedidoRepository.existsByProductoId(id)) {
            throw new ConflictException(
                    "No se puede eliminar \"" + producto.getNombre() + "\" porque tiene pedidos asociados");
        }

        productoRepository.delete(producto);
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El producto " + id + " no existe"));
    }

    private void aplicar(Producto producto, ProductoRequest req) {
        producto.setCodigo(req.codigo().trim());
        producto.setNombre(req.nombre().trim());
        producto.setDescripcion(req.descripcion() == null ? "" : req.descripcion().trim());
        producto.setPrecio(req.precio());
        producto.setStock(req.stock());
        producto.setStockCritico(req.stockCritico() == null ? 0 : req.stockCritico());
        producto.setImagen(req.imagen() == null ? "" : req.imagen().trim());
        producto.setCategoria(obtenerOCrearCategoria(req.categoria().trim()));
    }

    private Categoria obtenerOCrearCategoria(String nombre) {
        return categoriaRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> categoriaRepository.save(new Categoria(nombre)));
    }
}
