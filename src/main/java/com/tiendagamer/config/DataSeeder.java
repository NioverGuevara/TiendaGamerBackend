package com.tiendagamer.config;

import com.tiendagamer.model.*;
import com.tiendagamer.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Carga roles, usuarios de prueba y productos la primera vez que arranca con la BD vacía. */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RolRepository rolRepository, UsuarioRepository usuarioRepository,
                      CategoriaRepository categoriaRepository, ProductoRepository productoRepository,
                      PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (NombreRol nombre : NombreRol.values()) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                rolRepository.save(new Rol(nombre));
            }
        }

        if (usuarioRepository.count() == 0) {
            crearUsuario("19011022K", "Administrador", "Principal", "admin@tienda.cl", "Admin123!", NombreRol.ADMIN);
            crearUsuario("18222333K", "Vanesa", "Vendedora", "vendedor@tienda.cl", "Vendedor123!", NombreRol.VENDEDOR);
            crearUsuario("20123456K", "Cliente", "Prueba", "cliente@tienda.cl", "Cliente123!", NombreRol.CLIENTE);
        }

        if (productoRepository.count() == 0) {
            crearProducto("PROD001", "Mouse Gamer Pro", "Mouse gamer de alta precisión", 20000, 24, 5, "Periféricos");
            crearProducto("PROD002", "Teclado Mecánico", "Teclado mecánico para gaming", 39990, 10, 5, "Periféricos");
            crearProducto("PROD003", "Audífonos Gamer", "Audífonos gamer con micrófono", 29990, 12, 5, "Periféricos");
            crearProducto("PROD004", "Monitor Gamer 24\"", "Monitor 24 pulgadas 144Hz", 149990, 8, 3, "Monitores");
            crearProducto("PROD005", "Silla Gamer", "Silla ergonómica reclinable", 189990, 5, 2, "Sillas");
            crearProducto("PROD006", "PC Gamer Ryzen 5", "Computador de escritorio para gaming", 599990, 4, 2, "Computadores");
        }
    }

    private void crearUsuario(String run, String nombre, String apellidos, String correo,
                              String password, NombreRol rol) {
        Usuario u = new Usuario();
        u.setRun(run);
        u.setNombre(nombre);
        u.setApellidos(apellidos);
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode(password));
        u.setRegion("Metropolitana");
        u.setComuna("Santiago");
        u.setDireccion("Dirección de prueba 123");
        u.setActivo(true);
        u.setRol(rolRepository.findByNombre(rol).orElseThrow());
        usuarioRepository.save(u);
    }

    private void crearProducto(String codigo, String nombre, String descripcion, int precio,
                               int stock, int stockCritico, String categoria) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setStockCritico(stockCritico);
        p.setImagen("");
        p.setCategoria(categoriaRepository.findByNombreIgnoreCase(categoria)
                .orElseGet(() -> categoriaRepository.save(new Categoria(categoria))));
        productoRepository.save(p);
    }
}
