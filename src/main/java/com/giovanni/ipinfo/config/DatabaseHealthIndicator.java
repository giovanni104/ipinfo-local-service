package com.giovanni.ipinfo.config;

import com.giovanni.ipinfo.dto.DatabaseStatusResponse;
import com.giovanni.ipinfo.service.IpInfoDatabaseManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Indicador de salud personalizado integrado con Spring Boot Actuator.
 * Reporta el estado de salud de la base de datos MMDB local de IPinfo.
 * Si la base no está cargada en memoria, reporta un estado DOWN.
 */
@Component("ipinfoDatabase")
public class DatabaseHealthIndicator implements HealthIndicator {

    private final IpInfoDatabaseManager databaseManager;

    /**
     * Crea una instancia del indicador de salud.
     *
     * @param databaseManager el administrador de la base de datos de IPinfo de donde obtener el estado.
     */
    public DatabaseHealthIndicator(IpInfoDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Evalúa y devuelve la salud detallada del archivo MMDB local.
     *
     * @return {@link Health} que detalla si la base de datos está cargada, su ruta, tamaño y tipo.
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
