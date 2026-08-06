package com.giovanni.ipinfo.service;

import com.giovanni.ipinfo.config.IpInfoProperties;
import com.giovanni.ipinfo.dto.DatabaseUpdateResponse;
import com.giovanni.ipinfo.exception.DatabaseUnavailableException;
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
 * Servicio encargado de gestionar las descargas y actualizaciones de la base de datos MMDB local.
 * Implementa una descarga automática al arrancar (si se requiere y el archivo falta) y una
 * tarea programada semanalmente para mantener los datos al día.
 */
@Service
public class IpInfoDatabaseUpdater {

    private static final Logger log = LoggerFactory.getLogger(IpInfoDatabaseUpdater.class);

    private final IpInfoProperties properties;
    private final IpInfoDatabaseManager databaseManager;
    private final HttpClient httpClient;
    private final ReentrantLock updateLock = new ReentrantLock();

    /**
     * Construye un actualizador de base de datos de IPinfo.
     *
     * @param properties      propiedades de configuración del servicio.
     * @param databaseManager administrador responsable de la recarga en memoria.
     */
    public IpInfoDatabaseUpdater(IpInfoProperties properties, IpInfoDatabaseManager databaseManager) {
        this.properties = properties;
        this.databaseManager = databaseManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Descarga la base de datos durante el arranque del servicio si el archivo no existe localmente
     * y la opción {@code downloadOnStartupWhenMissing} está activa.
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
     * Tarea programada semanalmente para la descarga y actualización de la base de datos.
     * Los parámetros de cron y zona horaria se inyectan dinámicamente desde la configuración.
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
     * Realiza el proceso completo de descarga y recarga en caliente de la base de datos de forma segura:
     * <ol>
     *     <li>Descarga la base de datos en un archivo temporal (.download)</li>
     *     <li>Valida el código HTTP de respuesta y el tamaño mínimo del archivo</li>
     *     <li>Verifica la estructura interna MMDB del archivo descargado</li>
     *     <li>Mueve el archivo de forma atómica a la ruta de destino definitiva</li>
     *     <li>Notifica al {@link IpInfoDatabaseManager} para recargar el lector en memoria</li>
     * </ol>
     *
     * @return un {@link DatabaseUpdateResponse} con el resultado detallado de la operación.
     * @throws DatabaseUnavailableException si el token no es válido o ocurre un error irrecuperable de descarga.
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
            throw new DatabaseUnavailableException("La descarga fue interrumpida", ex);
        } catch (IOException ex) {
            throw new DatabaseUnavailableException("No fue posible actualizar la base IPinfo Lite", ex);
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
     * Construye la URI de descarga concatenando el token de forma segura.
     *
     * @return {@link URI} para la descarga.
     */
    private URI buildDownloadUri() {
        String separator = properties.downloadUrl().contains("?") ? "&" : "?";
        String encodedToken = URLEncoder.encode(properties.token(), StandardCharsets.UTF_8);
        return URI.create(properties.downloadUrl() + separator + "token=" + encodedToken);
    }

    /**
     * Valida que el token configurado no sea nulo, vacío ni coincida con el valor por defecto.
     *
     * @throws DatabaseUnavailableException si el token es inválido.
     */
    private void validateToken() {
        if (!StringUtils.hasText(properties.token()) || "change-me".equals(properties.token())) {
            throw new DatabaseUnavailableException(
                    "Configura la variable IPINFO_TOKEN con un token válido de IPinfo"
            );
        }
    }

    /**
     * Abre y valida temporalmente un archivo MMDB recién descargado para confirmar que contiene metadatos válidos.
     *
     * @param path ruta del archivo a validar.
     * @throws IOException si falla la lectura.
     * @throws DatabaseUnavailableException si el archivo no contiene estructura MMDB válida.
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
     * Reemplaza el archivo de destino con el nuevo de forma atómica en el sistema de archivos si es posible.
     *
     * @param source      ruta del archivo descargado.
     * @param destination ruta destino de la base de datos definitiva.
     * @throws IOException si falla el movimiento.
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
