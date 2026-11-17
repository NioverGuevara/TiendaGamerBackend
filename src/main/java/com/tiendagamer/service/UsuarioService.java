package com.tiendagamer.service;

import com.tiendagamer.dto.UsuarioRequest;
import com.tiendagamer.dto.UsuarioResponse;
import com.tiendagamer.exception.BusinessException;
import com.tiendagamer.exception.ConflictException;
import com.tiendagamer.exception.NotFoundException;
import com.tiendagamer.model.Usuario;
import com.tiendagamer.repository.PedidoRepository;
import com.tiendagamer.repository.RolRepository;
import com.tiendagamer.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestión de usuarios (solo Administrador). */
@Service
public class UsuarioService {

    private static final int LARGO_MINIMO_PASSWORD = 8;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                          PedidoRepository pedidoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.pedidoRepository = pedidoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.desde(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        if (req.password() == null || req.password().length() < LARGO_MINIMO_PASSWORD) {
            throw new BusinessException("La contraseña debe tener al menos 8 caracteres");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(req.correo().trim())) {
            throw new ConflictException("El correo ya está registrado");
        }
        if (usuarioRepository.existsByRun(req.run().trim())) {
            throw new ConflictException("El RUN ya está registrado");
        }

        Usuario usuario = new Usuario();
        aplicar(usuario, req);
        usuario.setPassword(passwordEncoder.encode(req.password()));
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req) {
        Usuario usuario = buscar(id);

        if (usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(req.correo().trim(), id)) {
            throw new ConflictException("El correo ya está registrado");
        }
        if (usuarioRepository.existsByRunAndIdNot(req.run().trim(), id)) {
            throw new ConflictException("El RUN ya está registrado");
        }

        aplicar(usuario, req);

        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < LARGO_MINIMO_PASSWORD) {
                throw new BusinessException("La contraseña debe tener al menos 8 caracteres");
            }
            usuario.setPassword(passwordEncoder.encode(req.password()));
        }

        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Long id, String correoActual) {
        Usuario usuario = buscar(id);

        if (usuario.getCorreo().equalsIgnoreCase(correoActual)) {
            throw new BusinessException("No puedes eliminar tu propia cuenta");
        }
        if (pedidoRepository.existsByUsuarioId(id)) {
            throw new ConflictException(
                    "El usuario tiene pedidos asociados: desactívalo en lugar de eliminarlo");
        }

        usuarioRepository.delete(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El usuario " + id + " no existe"));
    }

    private void aplicar(Usuario usuario, UsuarioRequest req) {
        usuario.setRun(req.run().trim());
        usuario.setNombre(req.nombre().trim());
        usuario.setApellidos(req.apellidos().trim());
        usuario.setCorreo(req.correo().trim());
        usuario.setRegion(req.region());
        usuario.setComuna(req.comuna());
        usuario.setDireccion(req.direccion());
        usuario.setActivo(req.activo() == null || req.activo());
        usuario.setRol(rolRepository.findByNombre(req.rol())
                .orElseThrow(() -> new NotFoundException("El rol " + req.rol() + " no existe")));
    }
}
