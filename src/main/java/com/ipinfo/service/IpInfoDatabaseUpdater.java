package com.ipinfo.service;

import com.ipinfo.config.IpInfoProperties;
import com.ipinfo.dto.DatabaseUpdateResponse;
import com.ipinfo.exception.DatabaseUnavailableException;
import com.maxmind.db.Reader;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Servicio encargado de la descarga, validación e instalación de la base de datos MMDB de IPinfo Lite.
 *
 * Gestiona:
 * - La descarga inicial automática al arrancar si el archivo local no existe.
 * - La actualización programada periódica (mediante expresión Cron con @Scheduled).
 * - La actualización manual bajo demanda invocada desde endpoints administrativos.
 *
 * Mecanismo de seguridad durante la actualización:
 * 1. Descarga a un archivo temporal (.download).
 * 2. Valida código de respuesta HTTP 200 y tamaño mínimo en bytes.
 * 3. Abre el archivo temporal con Reader para validar la integridad estructural del MMDB.
 * 4. Reemplaza atómicamente el archivo activo en disco (ATOMIC_MOVE).
 * 5. Solicita la recarga en caliente a IpInfoDatabaseManager.
 * 6. Si cualquier paso falla, se descarta el temporal y se conserva intacta la base de datos anterior.
 */
@Service
public class IpInfoDatabaseUpdater {

    private static final Logger log = LoggerFactory.getLogger(IpInfoDatabaseUpdater.class);

    private final IpInfoProperties properties;
    private final IpInfoDatabaseManager databaseManager;
    private final HttpClient httpClient;
    private final ReentrantLock updateLock = new ReentrantLock();
    private final MailNotificationService mailNotificationService;
    /**
     * Construye el actualizador e inicializa el cliente HTTP para descargas.
     *
     * @param properties      propiedades de configuración del microservicio.
     * @param databaseManager componente encargado de gestionar y recargar la base en memoria.
     */
public IpInfoDatabaseUpdater(
        IpInfoProperties properties,
        IpInfoDatabaseManager databaseManager,
        MailNotificationService mailNotificationService
) {
    this.properties = properties;
    this.databaseManager = databaseManager;
    this.mailNotificationService = mailNotificationService;
    this.httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
}

    /**
     * Tarea ejecutada inmediatamente después de la inicialización del bean (@PostConstruct).
     * Si la propiedad downloadOnStartupWhenMissing está activa y no existe el archivo MMDB local,
     * se dispara automáticamente la primera descarga.
     */
    @PostConstruct
    public void downloadAtStartupWhenMissing() {
        Path database = properties.databasePath().toAbsolutePath().normalize();
        if (properties.downloadOnStartupWhenMissing() && !Files.isReadable(database)) {
            log.info("La base no existe. Se intentará descargar al iniciar.");
            updateNow();
        }
    }

    /**
     * Tarea programada que ejecuta la actualización automática de la base según la expresión Cron
     * y zona horaria configuradas en ipinfo.update-cron e ipinfo.update-zone.
     */
    @Scheduled(cron = "${ipinfo.update-cron}", zone = "${ipinfo.update-zone}")
    public void scheduledUpdate() {
        try {
            DatabaseUpdateResponse response = updateNow();
            log.info("Actualización programada finalizada: {}", response.message());
        } catch (RuntimeException ex) {
            log.error("Falló la actualización programada de IPinfo Lite. Se conserva la base anterior.", ex);
        }
    }

    /**
     * Ejecuta el ciclo completo de descarga, verificación estructural y sustitución atómica de la base MMDB.
     *
     * Utiliza un ReentrantLock no bloqueante (tryLock) para prevenir ejecuciones
     * concurrentes simultáneas de descargas.
     *
     * @return DatabaseUpdateResponse con el resultado detallado de la operación.
     * @throws DatabaseUnavailableException si el token es inválido, falla la conexión,
     *                                      el archivo descargado está incompleto o corrupto.
     */
    public DatabaseUpdateResponse updateNow() {
        if (!updateLock.tryLock()) {
            return new DatabaseUpdateResponse(false, "Ya hay una actualización en ejecución", 0L, Instant.now());
        }

        Path destination = properties.databasePath().toAbsolutePath().normalize();
        Path temporary = destination.resolveSibling(destination.getFileName() + ".download");
        try {
            validateToken();
            Files.createDirectories(destination.getParent());
            Files.deleteIfExists(temporary);

            HttpRequest request = HttpRequest.newBuilder(buildDownloadUri())
                    .timeout(properties.requestTimeout())
                    .header("Accept", "application/octet-stream")
                    .GET()
                    .build();

            HttpResponse<Path> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofFile(temporary)
            );

            if (response.statusCode() != 200) {
                throw new DatabaseUnavailableException(
                        "IPinfo respondió HTTP " + response.statusCode() + " durante la descarga"
                );
            }

            long size = Files.size(temporary);
            if (size < properties.minimumFileSizeBytes()) {
                throw new DatabaseUnavailableException(
                        "El archivo descargado es demasiado pequeño: " + size + " bytes"
                );
            }

            validateMmdb(temporary);
            replaceAtomically(temporary, destination);
            databaseManager.reload(destination);

            return new DatabaseUpdateResponse(
                    true,
                    "Base IPinfo Lite actualizada y recargada correctamente",
                    size,
                    Instant.now()
            );
           } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            DatabaseUnavailableException dbEx = new DatabaseUnavailableException("La descarga fue interrumpida", ex);
            mailNotificationService.notificarErrorActualizacion(dbEx);
            throw dbEx;
        } catch (IOException ex) {
            DatabaseUnavailableException dbEx = new DatabaseUnavailableException("No fue posible actualizar la base IPinfo Lite", ex);
            mailNotificationService.notificarErrorActualizacion(dbEx);
            throw dbEx;
        } catch (Exception ex) {
            // Captura DatabaseUnavailableException (token inválido, HTTP status != 200, tamaño incorrecto, etc.)
            mailNotificationService.notificarErrorActualizacion(ex);
            throw ex;
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ex) {
                log.warn("No fue posible eliminar el archivo temporal {}", temporary, ex);
            }
            updateLock.unlock();
        }
    }

    /**
     * Construye la URI oficial de descarga anexando el token de autenticación codificado en UTF-8.
     *
     * @return URI completa para realizar la petición HTTP.
     */
    private URI buildDownloadUri() {
        String separator = properties.downloadUrl().contains("?") ? "&" : "?";
        String encodedToken = URLEncoder.encode(properties.token(), StandardCharsets.UTF_8);
        return URI.create(properties.downloadUrl() + separator + "token=" + encodedToken);
    }

    /**
     * Valida que el token de IPinfo no esté vacío ni mantenga el valor de marcador de posición predeterminado.
     *
     * @throws DatabaseUnavailableException si el token no es válido.
     */
    private void validateToken() {
        if (!StringUtils.hasText(properties.token()) || "change-me".equals(properties.token())) {
            throw new DatabaseUnavailableException(
                    "Configura la variable IPINFO_TOKEN con un token válido de IPinfo"
            );
        }
    }

    /**
     * Abre y valida la integridad de un archivo MMDB recién descargado antes de reemplazar la base activa.
     *
     * @param path ruta del archivo temporal a validar.
     * @throws IOException si no se puede leer el archivo.
     * @throws DatabaseUnavailableException si el archivo no contiene metadata válida de MaxMind DB.
     */
    private void validateMmdb(Path path) throws IOException {
        try (Reader reader = new Reader(path.toFile(), Reader.FileMode.MEMORY)) {
            String type = reader.getMetadata().databaseType();
            if (!StringUtils.hasText(type)) {
                throw new DatabaseUnavailableException("El archivo descargado no contiene metadata MMDB válida");
            }
            log.info("Archivo MMDB validado. tipo={}, buildEpoch={}",
                    type, reader.getMetadata().buildEpoch());
        }
    }

    /**
     * Mueve el archivo temporal a la ruta definitiva mediante un reemplazo atómico cuando el sistema de archivos lo soporte.
     *
     * @param source      ruta temporal del archivo descargado.
     * @param destination ruta final destino del archivo MMDB activo.
     * @throws IOException si ocurre un fallo al mover el archivo.
     */
    private void replaceAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
