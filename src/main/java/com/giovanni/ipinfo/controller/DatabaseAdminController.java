package com.giovanni.ipinfo.controller;

import com.giovanni.ipinfo.dto.DatabaseStatusResponse;
import com.giovanni.ipinfo.dto.DatabaseUpdateResponse;
import com.giovanni.ipinfo.service.IpInfoDatabaseManager;
import com.giovanni.ipinfo.service.IpInfoDatabaseUpdater;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST administrativo para la gestión de la base de datos MMDB local de IPinfo.
 */
@RestController
@RequestMapping("/api/v1/admin/database")
public class DatabaseAdminController {

    private final IpInfoDatabaseManager databaseManager;
    private final IpInfoDatabaseUpdater databaseUpdater;

    /**
     * Construye una instancia de {@link DatabaseAdminController}.
     *
     * @param databaseManager el administrador de la base de datos local.
     * @param databaseUpdater el actualizador programado de la base de datos.
     */
    public DatabaseAdminController(
            IpInfoDatabaseManager databaseManager,
            IpInfoDatabaseUpdater databaseUpdater
    ) {
        this.databaseManager = databaseManager;
        this.databaseUpdater = databaseUpdater;
    }

    /**
     * Endpoint para obtener el estado actual de la base de datos (ruta, tamaño, fecha de carga y modificación).
     *
     * @return {@link ResponseEntity} con el {@link DatabaseStatusResponse} detallado.
     */
    @GetMapping("/status")
    public ResponseEntity<DatabaseStatusResponse> status() {
        return ResponseEntity.ok(databaseManager.status());
    }

    /**
     * Endpoint para forzar una descarga y recarga manual en caliente de la base de datos.
     *
     * @return {@link ResponseEntity} con la confirmación de la actualización en un {@link DatabaseUpdateResponse}.
     */
    @PostMapping("/update")
    public ResponseEntity<DatabaseUpdateResponse> update() {
        return ResponseEntity.ok(databaseUpdater.updateNow());
    }
}
