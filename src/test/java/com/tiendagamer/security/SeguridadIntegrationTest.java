package com.tiendagamer.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración: levantan toda la aplicación (con H2) y verifican
 * autenticación JWT, roles y reglas de acceso de punta a punta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SeguridadIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private static final String PRODUCTO_NUEVO = """
            {"codigo":"TEST001","nombre":"Producto de prueba","descripcion":"x",
             "precio":1000,"stock":5,"stockCritico":1,"categoria":"Pruebas"}""";

    private JsonNode login(String correo, String password) throws Exception {
        String cuerpo = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(cuerpo);
    }

    private String tokenDe(String correo, String password) throws Exception {
        return login(correo, password).get("accessToken").asText();
    }

    @Test
    void catalogoEsPublico() throws Exception {
        mvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").exists());
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@tienda.cl\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearProductoSinTokenDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_NUEVO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoPuedeCrearProductos() throws Exception {
        String token = tokenDe("cliente@tienda.cl", "Cliente123!");

        mvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_NUEVO))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendedorNoPuedeCrearProductosPeroSiVerPedidos() throws Exception {
        String token = tokenDe("vendedor@tienda.cl", "Vendedor123!");

        mvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_NUEVO))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/pedidos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeVerPedidosNiUsuarios() throws Exception {
        String token = tokenDe("cliente@tienda.cl", "Cliente123!");

        mvc.perform(get("/api/v1/pedidos").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeCrearProductoYListarUsuarios() throws Exception {
        String token = tokenDe("admin@tienda.cl", "Admin123!");

        mvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_NUEVO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("TEST001"));

        mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenNoSirveParaAccederARecursos() throws Exception {
        String refresh = login("admin@tienda.cl", "Admin123!").get("refreshToken").asText();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshEntregaNuevosTokens() throws Exception {
        String refresh = login("cliente@tienda.cl", "Cliente123!").get("refreshToken").asText();

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"));
    }

    @Test
    void compraRequiereSesionYRegistraElPedido() throws Exception {
        String cuerpo = """
                {"cliente":{"nombre":"Cliente Prueba","correo":"cliente@tienda.cl","telefono":"+56911111111",
                 "direccion":"Calle 1","region":"Metropolitana","comuna":"Santiago"},
                 "productos":[{"id":1,"cantidad":1}]}""";

        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());

        String token = tokenDe("cliente@tienda.cl", "Cliente123!");

        mvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedido.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.pedido.total").value(20000));
    }
}
