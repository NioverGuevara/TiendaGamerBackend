# Tienda Online Gamer – Backend (Spring Boot)

API REST de la Tienda Online (DSY1104 – Evaluación Parcial 3).
Spring Boot 3.3 · Java 17+ · Spring Data JPA · Spring Security + JWT · MySQL · Swagger (springdoc).

## Requisitos
- JDK 17 o superior
- Maven 3.9+ (o un IDE como IntelliJ / VS Code con soporte Maven)
- MySQL 8 en `localhost:3306` (o usa el perfil `h2`, que no necesita instalar nada)

## Ejecución con MySQL
1. Inicia MySQL. La base `tienda_gamer` se crea sola si no existe.
2. Si tu usuario/clave no son `root` sin contraseña, define variables de entorno:
   `DB_USER`, `DB_PASSWORD` (y opcionalmente `DB_URL`).
3. Ejecuta:
   ```
   mvn spring-boot:run
   ```
4. La API queda en `http://localhost:8080` y Swagger en `http://localhost:8080/swagger-ui.html`.

## Ejecución sin MySQL (base H2 en memoria)
```
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

## Usuarios de prueba (se crean al primer arranque)
| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@tienda.cl | Admin123! |
| Vendedor | vendedor@tienda.cl | Vendedor123! |
| Cliente | cliente@tienda.cl | Cliente123! |

## Docker
```
docker compose up --build
```
Levanta MySQL y el backend (API en `http://localhost:8080`, Swagger en `/swagger-ui.html`).
La clave de MySQL y el secreto JWT se pueden cambiar con las variables `DB_PASSWORD` y `JWT_SECRET`.

## Despliegue automático (GitHub Actions → AWS EC2)
El archivo `.github/workflows/deploy.yml` ejecuta en cada push o pull request a `main`:
1. **test:** `mvn test`.
2. **docker:** construye la imagen multi-stage; solo en push a `main` la copia por SSH a la instancia EC2,
   levanta MySQL (con volumen persistente) y reemplaza el contenedor del backend, verificando que
   `GET /api/v1/productos` responda.

Secrets requeridos en GitHub (Settings → Secrets and variables → Actions):
`EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ORIGINS`.
En la instancia EC2 debe estar instalado Docker y el puerto 8080 abierto en el grupo de seguridad.

## Pruebas
```
mvn test
```
Incluye pruebas unitarias de servicios (Mockito), del manejo de JWT y pruebas de integración
de seguridad (login, roles 401/403, refresh) con H2.

## Variables de entorno
| Variable | Uso | Valor por defecto |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a MySQL | localhost / root / vacío |
| `JWT_SECRET` | Clave de firma de los JWT (mín. 32 caracteres) | clave de desarrollo |
| `CORS_ORIGINS` | Orígenes permitidos del frontend | `http://localhost:5173,http://localhost:4173` |

**Importante:** cambia `JWT_SECRET` en cualquier entorno que no sea de desarrollo.

## Endpoints (todos bajo `/api/v1`)
Ver Swagger o el archivo `Documentacion_APIs.xlsx` entregado con el proyecto.
