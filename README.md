# IPinfo Local Service

Microservicio Spring Boot de alto rendimiento para consultar localmente país, continente, ASN y organización de direcciones IPv4 o IPv6 utilizando la base de datos descargable **IPinfo Lite MMDB**.

---

## Características Principales

- **Rendimiento y Localidad**: Consulta 100% en memoria con Java 21 y Spring Boot 3.5 (sin latencia de llamadas HTTP externas por cada IP).
- **Descarga Inicial Automática**: Descarga el archivo `.mmdb` automáticamente en el arranque si no existe en disco.
- **Actualización Programada**: Descarga periódica configurable (por defecto cada sábado a las 03:00 en `America/Bogota`).
- **Recarga en Caliente (Zero-Downtime)**: Concurrencia segura mediante `ReentrantReadWriteLock`. El servicio nunca se detiene ni rechaza consultas mientras se actualiza la base de datos.
- **Sustitución Atómica**: Descarga previa a archivo temporal, validación de integridad estructural MaxMind y reemplazo atómico en disco.
- **Notificaciones por Correo**: Alerta automática por email si ocurre algún fallo durante la descarga o validación de la base de datos.
- **Filtrado y Validación de IPs**: Soporta IPv4 e IPv6 públicas; rechaza y categoriza direcciones privadas (RFC 1918), loopback, link-local, multicast y rango CGNAT (`100.64.0.0/10`).
- **Observabilidad**: Métricas y Health Indicators de Spring Boot Actuator para el estado de la base MMDB en memoria.
- **Perfiles Maven y Spring**: Configuración modular para entornos `local` y `produccion`.

> Datos de IP proporcionados por [IPinfo](https://ipinfo.io). IPinfo Lite se distribuye bajo licencia CC BY-SA 4.0; conserva esta atribución en el producto o repositorio.

---

## Requisitos Previos

- **Java JDK 21** (recomendado Eclipse Adoptium Temurin 21).
- **Maven 3.8+** (o Maven Wrapper).
- **Token de IPinfo**: Cuenta gratuita con token de acceso a IPinfo Lite.

---

## Configuración y Variables de Entorno

El servicio utiliza perfiles de configuración `application-local.yml` y `application-produccion.yml`. Los valores pueden sobreescribirse mediante variables de entorno:

| Variable | Descripción | Valor por Defecto (Local) |
| :--- | :--- | :--- |
| `IPINFO_TOKEN` | Token de autenticación de IPinfo | `20b6eff000f10e` |
| `IPINFO_DATABASE_PATH` | Ruta del archivo binario `.mmdb` | `./data/ipinfo_lite.mmdb` |
| `IPINFO_DOWNLOAD_URL` | URL de descarga de IPinfo Lite | `https://ipinfo.io/data/ipinfo_lite.mmdb` |
| `IPINFO_UPDATE_CRON` | Expresión Cron de actualización | `0 0 3 * * SAT` (Sábados 03:00) |
| `IPINFO_UPDATE_ZONE` | Zona horaria para el Cron | `America/Bogota` |
| `IPINFO_DOWNLOAD_ON_STARTUP_WHEN_MISSING` | Descargar si no existe al iniciar | `true` |
| `IPINFO_MAIL_ENABLED` | Activar alerta por correo en fallo | `true` |
| `IPINFO_MAIL_URL` | Endpoint del servicio de correos | `http://localhost:9092/...` |
| `IPINFO_MAIL_FROM` | Remitente de la notificación | `monitoreo@tucompra.com.co` |
| `IPINFO_MAIL_TO` | Destinatario de la alerta | `giovanny.hernandez@tucompra.com.co` |
| `IPINFO_MAIL_SUBJECT` | Asunto del correo de alerta | `[ALERTA] Fallo al actualizar base de datos IPinfo Lite` |
| `SERVER_PORT` | Puerto HTTP del microservicio | `8080` |

---

## Compilación y Ejecución

### 1. Compilar el JAR con Perfiles Maven

El proyecto cuenta con perfiles Maven que inyectan el perfil activo correspondiente en `application.yml`:

```bash
# Compilar para entorno Local (por defecto):
mvn clean package -P local -DskipTests

# O compilar para entorno Demo Clientes:
mvn clean package -P democlientes -DskipTests

# O compilar para entorno Producción:
mvn clean package -P produccion -DskipTests
```

### 2. Ejecutar el JAR generado

Puedes iniciar el servicio pasando los parámetros de configuración (como el token o el perfil) de distintas formas:

```bash
# Ejecución estándar (toma variables de entorno o valores por defecto del perfil):
java -jar target/ipinfo-local-service-1.0.0.jar

# Opción 2: Pasar el token como argumento de Spring Boot (--)
java -jar target/ipinfo-local-service-1.0.0.jar --ipinfo.token=TU_TOKEN

# Opción 3: Pasar el token como propiedad JVM (-D, siempre ANTES de -jar)
java -DIPINFO_TOKEN=TU_TOKEN -jar target/ipinfo-local-service-1.0.0.jar

# Forzar perfil explícitamente:
java -jar target/ipinfo-local-service-1.0.0.jar --spring.profiles.active=democlientes

# Guardar logs en un archivo físico mientras se ejecuta:
java -jar target/ipinfo-local-service-1.0.0.jar --logging.file.name=logs/ipinfo-service.log
```

### 3. Ejecución directa en desarrollo (Spring Boot Maven Plugin)

```powershell
# PowerShell (Windows)
$env:IPINFO_TOKEN="TU_TOKEN"
mvn spring-boot:run
```

```bash
# Bash / Linux
export IPINFO_TOKEN="TU_TOKEN"
mvn spring-boot:run
```

---

## Endpoints de la API

### 1. Consultar una IP Pública

`GET /api/v1/ips/{ip}`

```bash
curl http://localhost:8080/api/v1/ips/8.8.8.8
```

**Respuesta exitosa (`200 OK`)**:
```json
{
  "ip": "8.8.8.8",
  "country": "United States",
  "countryCode": "US",
  "continent": "North America",
  "continentCode": "NA",
  "asn": "AS15169",
  "organization": "Google LLC",
  "organizationDomain": "google.com",
  "databaseLoadedAt": "2026-10-05T09:00:00Z",
  "dataSource": "IPinfo Lite local MMDB"
}
```

**Respuesta para IP privada o reservada (`400 Bad Request`)**:
```json
{
  "type": "about:blank",
  "title": "IP Inválida",
  "status": 400,
  "detail": "La IP 192.168.1.1 es privada o local y no tiene resolución pública en IPinfo Lite",
  "instance": "/api/v1/ips/192.168.1.1"
}
```

---

### 2. Estado de la Base de Datos Local

`GET /api/v1/admin/database/status`

```bash
curl http://localhost:8080/api/v1/admin/database/status
```

**Respuesta (`200 OK`)**:
```json
{
  "loaded": true,
  "path": "C:\\developer\\personal\\ipinfo-local-service\\data\\ipinfo_lite.mmdb",
  "sizeBytes": 25165824,
  "lastModified": "2026-10-05T09:15:30Z",
  "loadedAt": "2026-10-05T09:15:32Z",
  "databaseType": "IPinfo Lite",
  "buildEpoch": "1727827200"
}
```

---

### 3. Forzar Actualización Manual

`POST /api/v1/admin/database/update`

```bash
curl -X POST http://localhost:8080/api/v1/admin/database/update
```

**Respuesta (`200 OK`)**:
```json
{
  "success": true,
  "message": "Base IPinfo Lite actualizada y recargada correctamente",
  "sizeBytes": 25165824,
  "updatedAt": "2026-10-05T09:30:00Z"
}
```

> [!WARNING]
> Los endpoints administrativos `/api/v1/admin/**` no incluyen autenticación por defecto en este proyecto base. En entornos productivos deben protegerse mediante Spring Security, API Gateway, redes privadas o reglas de Ingress.

---

### 4. Health Check y Métricas

`GET /actuator/health`

```bash
curl http://localhost:8080/actuator/health
```

**Respuesta (`200 OK`)**:
```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "loaded": true,
        "type": "IPinfo Lite",
        "loadedAt": "2026-10-05T09:15:32Z"
      }
    }
  }
}
```

---

## Actualización y Notificaciones

El servicio ejecuta la actualización de forma automática mediante la tarea programada:

```yaml
ipinfo:
  update-cron: "0 0 3 * * SAT"
  update-zone: "America/Bogota"
```

### Flujo de Actualización Segura
1. Descarga el archivo a una ruta temporal (`ipinfo_lite.mmdb.download`).
2. Verifica respuesta HTTP 200 y tamaño mínimo (> 1 MB).
3. Abre el archivo temporal con el lector binario de MaxMind para validar su cabecera y metadata.
4. Realiza un reemplazo atómico en disco (`ATOMIC_MOVE` / `REPLACE_EXISTING`).
5. Recarga en caliente el `Reader` en memoria sin interrumpir el servicio.
6. **En caso de error**: Si la descarga o validación falla, conserva la base de datos anterior intacta y dispara una notificación por correo mediante `MailNotificationService`.

---

## Despliegue con Docker

El proyecto incluye soporte para contenedores con Docker y Docker Compose:

```bash
# 1. Copiar y configurar variables
cp .env.example .env
# Edita .env y define IPINFO_TOKEN

# 2. Compilar el JAR
mvn clean package -P local -DskipTests

# 3. Construir y levantar el contenedor
docker compose up --build -d
```

> **Persistencia**: El archivo `.mmdb` se almacena en el volumen Docker `ipinfo-data` mapeado a `/opt/app/data`, asegurando que la base de datos persista entre reinicios o actualizaciones de la imagen del contenedor.

---

## Ejecutar Pruebas

Para ejecutar la suite de pruebas unitarias:

```bash
mvn test
```

---

## Consideraciones para Producción y Kubernetes

- **Protección de Endpoints**: Proteger las rutas `/api/v1/admin/**` ante accesos no autorizados.
- **Gestión de Secretos**: No versionar ni registrar en texto plano el token de IPinfo; usar Kubernetes Secrets, HashiCorp Vault o Secret Manager.
- **Escalamiento y Réplicas**:
  - Si se despliega con múltiples réplicas en Kubernetes, cada pod puede mantener su copia local o compartir un volumen `ReadWriteMany`.
  - Para evitar descargas concurrentes simultáneas por parte de varias réplicas, se recomienda centralizar la actualización en una única réplica o mediante un CronJob dedicado.
- **Carpeta `k8s/`**: Directorio reservado para alojar los manifiestos de Kubernetes (`deployment.yml`, `pvc.yml`, `secret.yml`) adaptados a la infraestructura del clúster de destino.
