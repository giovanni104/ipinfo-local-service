package com.ipinfo.config;

import com.ipinfo.dto.DatabaseStatusResponse;
import com.ipinfo.service.IpInfoDatabaseManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Indicador de salud personalizado para Spring Boot Actuator (/actuator/health).
 *
 * Verifica el estado del lector de base de datos local MMDB gestionado por IpInfoDatabaseManager.
 * Si la base de datos no está cargada en memoria, el indicador reporta un estado DOWN,
 * lo que permite a las sondas de Kubernetes (Readiness/Liveness) detectar si el servicio está listo
 * para recibir tráfico.
 */
@Component("ipinfoDatabase")
public class DatabaseHealthIndicator implements HealthIndicator {

    private final IpInfoDatabaseManager databaseManager;

    /**
     * Construye el indicador inyectando el administrador de la base de datos local.
     *
     * @param databaseManager componente encargado de la base de datos MMDB.
     */
    public DatabaseHealthIndicator(IpInfoDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Evalúa y construye el objeto Health reportando detalles como la ruta del archivo,
     * tamaño, fecha de carga y tipo de base de datos.
     *
     * @return Health UP si la base está cargada, o Health DOWN si no está disponible.
     */
    @Override
    public Health health() {
        DatabaseStatusResponse status = databaseManager.status();
        if (!status.loaded()) {
            return Health.down()
                    .withDetail("path", status.path())
                    .withDetail("message", "Base MMDB no cargada")
                    .build();
        }
        return Health.up()
                .withDetail("path", status.path())
                .withDetail("sizeBytes", status.sizeBytes())
                .withDetail("loadedAt", status.loadedAt())
                .withDetail("databaseType", status.databaseType())
                .build();
    }
}
