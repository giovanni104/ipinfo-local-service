# IPinfo Local Service

Microservicio Spring Boot para consultar localmente país, continente, ASN y organización de una IPv4 o IPv6 usando la base gratuita **IPinfo Lite MMDB**.

## Características

- Java 21 y Spring Boot 3.5.
- Consulta completamente local: no envía cada IP a IPinfo.
- Descarga inicial automática cuando no existe la base.
- Actualización semanal cada sábado a las 03:00 en `America/Bogota`.
- Descarga a archivo temporal, validación MMDB y reemplazo atómico.
- Recarga en caliente del lector sin reiniciar el servicio.
- Conserva la base anterior si la descarga o validación falla.
- Soporta IPv4 e IPv6 públicas.
- Rechaza IP privadas, locales y el rango CGNAT `100.64.0.0/10`.
- Actuator y health indicator para la base.

> Datos de IP proporcionados por [IPinfo](https://ipinfo.io). IPinfo Lite se distribuye bajo CC BY-SA 4.0; conserva esta atribución en el producto o repositorio.

## Requisitos

- JDK 21.
- Maven 3.6.3 o superior.
- Cuenta gratuita de IPinfo y un Access Token.

## Configuración

En PowerShell:

```powershell
$env:IPINFO_TOKEN="TU_TOKEN"
$env:IPINFO_DATABASE_PATH="C:\developer\ipinfo\ipinfo_lite.mmdb"
mvn spring-boot:run
```

En Linux/WSL:

```bash
export IPINFO_TOKEN="TU_TOKEN"
export IPINFO_DATABASE_PATH="/opt/ipinfo/ipinfo_lite.mmdb"
mvn spring-boot:run
```

Si la base no existe, el servicio la descarga durante el arranque. La descarga oficial usada es:

```text
https://ipinfo.io/data/ipinfo_lite.mmdb?token=TOKEN
```

## Endpoints

### Consultar una IP

```bash
curl http://localhost:8080/api/v1/ips/8.8.8.8
```

Respuesta:

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
  "databaseLoadedAt": "2026-08-05T20:00:00Z",
  "dataSource": "IPinfo Lite local MMDB"
}
```

### Estado de la base

```bash
curl http://localhost:8080/api/v1/admin/database/status
```

### Forzar actualización manual

```bash
curl -X POST http://localhost:8080/api/v1/admin/database/update
```

> El endpoint administrativo no tiene autenticación en este proyecto base. En producción debe quedar detrás de Spring Security, una red interna o una política de Ingress.

### Health check

```bash
curl http://localhost:8080/actuator/health
```

## Actualización semanal

Configuración predeterminada:

```yaml
ipinfo:
  update-cron: "0 0 3 * * SAT"
  update-zone: "America/Bogota"
```

Puedes modificarla con:

```bash
IPINFO_UPDATE_CRON="0 0 2 * * SUN"
IPINFO_UPDATE_ZONE="America/Bogota"
```

La secuencia de actualización es:

1. Descargar `ipinfo_lite.mmdb.download`.
2. Verificar código HTTP y tamaño mínimo.
3. Abrir el archivo con el lector MMDB para validar su estructura.
4. Reemplazar la base activa de manera atómica cuando el sistema de archivos lo permite.
5. Cargar el nuevo archivo en memoria.
6. Cerrar el lector anterior.

## Docker

```bash
cp .env.example .env
# Edita .env y agrega el token
mvn clean package
docker compose up --build -d
```

La base se guarda en el volumen `ipinfo-data`, por lo que sobrevive a recreaciones del contenedor.

## Ejecutar pruebas

```bash
mvn test
```

## Consideraciones de producción

- Protege `/api/v1/admin/**`.
- No registres el token ni lo subas a Git.
- Usa Secret Manager o Kubernetes Secret para `IPINFO_TOKEN`.
- Si tienes varias réplicas, cada pod puede descargar su copia; para evitar descargas duplicadas, usa un CronJob/volumen compartido o una sola réplica responsable de actualizar.
- País y ASN son señales técnicas, no una ubicación personal exacta.

## Kubernetes

Los manifiestos base están en `k8s/`. El ejemplo usa una sola réplica para que exista un único programador semanal. Antes de desplegar:

1. Cambia la imagen de `k8s/deployment.yml`.
2. Crea el Secret real sin subirlo al repositorio.
3. Aplica PVC, Secret y Deployment.

```bash
kubectl apply -f k8s/pvc.yml
kubectl apply -f k8s/secret.yml
kubectl apply -f k8s/deployment.yml
```
