package com.ipinfo.service;

import com.ipinfo.config.IpInfoProperties;
import com.ipinfo.dto.DatabaseStatusResponse;
import com.ipinfo.exception.DatabaseUnavailableException;
import com.ipinfo.model.IpInfoLiteRecord;
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
 * Gestor del ciclo de vida y consultas en memoria de la base de datos MMDB local de IPinfo.
 *
 * Características clave de diseño:
 * - Lectura en memoria: El archivo MMDB se carga utilizando Reader.FileMode.MEMORY
 *   para garantizar latencias de consulta ultrarrápidas (sub-milisegundo).
 * - Concurrencia y Hot-Reload (Cero Downtime): Utiliza un ReentrantReadWriteLock.
 *   Múltiples hilos pueden leer concurrentemente usando readLock().
 *   Cuando se actualiza la base, reload(Path) adquiere el writeLock(),
 *   sustituye la referencia atómica y cierra de manera segura el lector anterior.
 */
@Component
public class IpInfoDatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(IpInfoDatabaseManager.class);

    private final IpInfoProperties properties;
    private final AtomicReference<LoadedDatabase> current = new AtomicReference<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Construye el gestor inyectando las propiedades de configuración.
     *
     * @param properties propiedades de configuración del microservicio.
     */
    public IpInfoDatabaseManager(IpInfoProperties properties) {
        this.properties = properties;
    }

    /**
     * Inicializa el componente al arrancar la aplicación. Si el archivo MMDB ya existe y es legible,
     * lo carga inmediatamente en memoria.
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
     * Realiza la búsqueda de una dirección IP en el lector MMDB actualmente cargado en memoria.
     *
     * @param address dirección InetAddress a consultar.
     * @return un Optional con el registro IpInfoLiteRecord si la IP se encuentra en la base, o vacío si no.
     * @throws DatabaseUnavailableException si la base no está cargada o ocurre un error de lectura.
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
     * Carga o recarga en caliente un nuevo archivo MMDB desde el disco.
     *
     * Adquiere el bloqueo de escritura exclusivo para garantizar que ninguna petición de lectura
     * acceda a un estado intermedio durante el reemplazo del Reader.
     *
     * @param path ruta física del archivo .mmdb.
     * @throws DatabaseUnavailableException si ocurre un error al abrir o validar el nuevo archivo.
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
     * Obtiene el estado actual de la base de datos tanto en disco como en memoria.
     *
     * @return objeto DatabaseStatusResponse con los detalles de estado.
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
     * Retorna la marca de tiempo de cuándo se cargó la base en memoria por última vez.
     *
     * @return Instant de la última recarga, o null si no está cargada.
     */
    public Instant loadedAt() {
        LoadedDatabase loaded = current.get();
        return loaded != null ? loaded.loadedAt() : null;
    }

    /**
     * Cierra el lector MMDB y libera los recursos en memoria al detener la aplicación Spring Boot.
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
     * Registro interno que encapsula el lector abierto, su fecha de carga y metadatos de MaxMind.
     */
    private record LoadedDatabase(Reader reader, Instant loadedAt, Metadata metadata) {
    }
}
