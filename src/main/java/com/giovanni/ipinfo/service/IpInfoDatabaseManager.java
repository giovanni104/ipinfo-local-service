package com.giovanni.ipinfo.service;

import com.giovanni.ipinfo.config.IpInfoProperties;
import com.giovanni.ipinfo.dto.DatabaseStatusResponse;
import com.giovanni.ipinfo.exception.DatabaseUnavailableException;
import com.giovanni.ipinfo.model.IpInfoLiteRecord;
import com.maxmind.db.Metadata;
import com.maxmind.db.Reader;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Componente responsable de gestionar el ciclo de vida en caliente del lector de la base de datos MMDB
 * en memoria. Proporciona consultas seguras para subprocesos mediante un {@link ReentrantReadWriteLock}.
 */
@Component
public class IpInfoDatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(IpInfoDatabaseManager.class);

    private final IpInfoProperties properties;
    private final AtomicReference<LoadedDatabase> current = new AtomicReference<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Construye el administrador de base de datos de IPinfo.
     *
     * @param properties las propiedades de configuración inyectadas.
     */
    public IpInfoDatabaseManager(IpInfoProperties properties) {
        this.properties = properties;
    }

    /**
     * Inicializa el componente después de su construcción. Intenta cargar el archivo de base de datos
     * MMDB si ya se encuentra presente en la ruta del sistema de archivos configurada.
     */
    @PostConstruct
    public void initialize() {
        Path path = properties.databasePath().toAbsolutePath().normalize();
        if (Files.isReadable(path)) {
            reload(path);
        } else {
            log.warn("La base IPinfo no existe todavía en {}", path);
        }
    }

    /**
     * Realiza una consulta de IP local en la base de datos cargada.
     * Adquiere un bloqueo de lectura compartido para permitir consultas concurrentes.
     *
     * @param address la dirección IP en formato {@link InetAddress} a consultar.
     * @return un {@link Optional} conteniendo el {@link IpInfoLiteRecord} si la IP se encuentra mapeada,
     *         o un Optional vacío si no se encuentra.
     * @throws DatabaseUnavailableException si la base de datos no está cargada o hay un error de E/S.
     */
    public Optional<IpInfoLiteRecord> lookup(InetAddress address) {
        lock.readLock().lock();
        try {
            LoadedDatabase database = current.get();
            if (database == null) {
                throw new DatabaseUnavailableException("La base IPinfo Lite no está cargada");
            }
            return Optional.ofNullable(database.reader().get(address, IpInfoLiteRecord.class));
        } catch (IOException ex) {
            throw new DatabaseUnavailableException("Error consultando la base IPinfo Lite", ex);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Carga o recarga la base de datos desde un archivo físico en caliente.
     * Adquiere un bloqueo exclusivo de escritura para reemplazar la referencia del lector
     * y liberar el lector anterior de forma segura.
     *
     * @param path la ruta del archivo MMDB que se desea cargar.
     * @throws DatabaseUnavailableException si falla la apertura del nuevo lector.
     */
    public void reload(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        lock.writeLock().lock();
        try {
            Reader newReader = new Reader(normalized.toFile(), Reader.FileMode.MEMORY);
            Metadata metadata = newReader.getMetadata();
            LoadedDatabase replacement = new LoadedDatabase(newReader, Instant.now(), metadata);
            LoadedDatabase previous = current.getAndSet(replacement);
            if (previous != null) {
                previous.reader().close();
            }
            log.info("Base IPinfo cargada. tipo={}, buildEpoch={}, ruta={}",
                    metadata.databaseType(), metadata.buildEpoch(), normalized);
        } catch (IOException ex) {
            throw new DatabaseUnavailableException("No fue posible cargar la base IPinfo: " + normalized, ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Consulta el estado descriptivo actual del archivo y la base de datos cargada.
     *
     * @return {@link DatabaseStatusResponse} detallando si está cargada, su ruta, tamaño y fecha.
     */
    public DatabaseStatusResponse status() {
        Path path = properties.databasePath().toAbsolutePath().normalize();
        LoadedDatabase loaded = current.get();
        try {
            long size = Files.exists(path) ? Files.size(path) : 0L;
            FileTime modified = Files.exists(path) ? Files.getLastModifiedTime(path) : FileTime.fromMillis(0L);
            return new DatabaseStatusResponse(
                    loaded != null,
                    path.toString(),
                    size,
                    Files.exists(path) ? modified.toInstant() : null,
                    loaded != null ? loaded.loadedAt() : null,
                    loaded != null ? loaded.metadata().databaseType() : null,
                    loaded != null ? loaded.metadata().buildEpoch().toString()
                            : null
            );
        } catch (IOException ex) {
            throw new DatabaseUnavailableException("No fue posible consultar el estado de la base", ex);
        }
    }

    /**
     * Obtiene el momento de carga de la base de datos activa.
     *
     * @return {@link Instant} indicando la fecha de carga, o {@code null} si no está cargada.
     */
    public Instant loadedAt() {
        LoadedDatabase loaded = current.get();
        return loaded != null ? loaded.loadedAt() : null;
    }

    /**
     * Cierra el lector de base de datos antes de la destrucción del componente (apagado del servicio).
     * Adquiere un bloqueo de escritura exclusivo para evitar peticiones concurrentes durante el apagado.
     */
    @PreDestroy
    public void close() {
        lock.writeLock().lock();
        try {
            LoadedDatabase loaded = current.getAndSet(null);
            if (loaded != null) {
                loaded.reader().close();
            }
        } catch (IOException ex) {
            log.warn("No fue posible cerrar el lector MMDB", ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Registro interno que encapsula una base de datos cargada junto a sus metadatos.
     */
    private record LoadedDatabase(Reader reader, Instant loadedAt, Metadata metadata) {
    }
}
