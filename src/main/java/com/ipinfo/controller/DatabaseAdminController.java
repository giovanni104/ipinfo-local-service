package com.ipinfo.controller;

import com.ipinfo.dto.DatabaseStatusResponse;
import com.ipinfo.dto.DatabaseUpdateResponse;
import com.ipinfo.service.IpInfoDatabaseManager;
import com.ipinfo.service.IpInfoDatabaseUpdater;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para tareas administrativas de la base de datos local MMDB de IPinfo.
 *
 * Proporciona endpoints para consultar el estado del archivo físico y de memoria,
 * así como forzar la descarga y recarga manual en caliente.
 *
 * Nota de Seguridad: En ambientes productivos, las rutas bajo /api/v1/admin/**
 * deben ser protegidas mediante reglas de red interna, políticas de Ingress o Spring Security.
 */
@RestController
@RequestMapping("/api/v1/admin/database")
public class DatabaseAdminController {

    private final IpInfoDatabaseManager databaseManager;
    private final IpInfoDatabaseUpdater databaseUpdater;

    /**
     * Construye el controlador administrativo inyectando el gestor y el actualizador de la base de datos.
     *
     * @param databaseManager componente de gestión y lectura en memoria del MMDB.
     * @param databaseUpdater componente encargado de la descarga y validación del archivo.
     */
    public DatabaseAdminController(
            IpInfoDatabaseManager databaseManager,
            IpInfoDatabaseUpdater databaseUpdater
    ) {
        this.databaseManager = databaseManager;
        this.databaseUpdater = databaseUpdater;
    }

    /**
     * Retorna información sobre el estado actual de la base de datos MMDB local:
     * si está cargada en memoria, ruta en disco, tamaño, fechas de modificación y metadatos de compilación.
     *
     * @return ResponseEntity con DatabaseStatusResponse.
     */
    @GetMapping("/status")
    public ResponseEntity<DatabaseStatusResponse> status() {
        return ResponseEntity.ok(databaseManager.status());
    }

    /**
     * Ejecuta inmediatamente la descarga, validación y recarga en caliente de la base de datos MMDB,
     * sin esperar al cron de actualización automática semanal.
     *
     * @return ResponseEntity con DatabaseUpdateResponse que indica el resultado de la operación.
     */
    @PostMapping("/update")
    public ResponseEntity<DatabaseUpdateResponse> update() {
        return ResponseEntity.ok(databaseUpdater.updateNow());
    }
}
