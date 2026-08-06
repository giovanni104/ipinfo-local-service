package com.giovanni.ipinfo.dto;

import java.time.Instant;

/**
 * DTO que reporta el resultado de un intento de actualización de la base de datos MMDB local.
 *
 * @param success   {@code true} si la descarga, validación e instalación de la base de datos fue exitosa.
 * @param message   Mensaje descriptivo con el resultado de la operación.
 * @param sizeBytes Tamaño en bytes del archivo MMDB descargado (0 en caso de fallo).
 * @param updatedAt Marca de tiempo en que finalizó la operación de actualización.
 */
public record DatabaseUpdateResponse(
        boolean success,
        String message,
        long sizeBytes,
        Instant updatedAt
) {
}
