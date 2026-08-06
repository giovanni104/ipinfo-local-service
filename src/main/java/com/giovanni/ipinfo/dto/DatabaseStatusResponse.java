package com.giovanni.ipinfo.dto;

import java.time.Instant;

/**
 * DTO que describe el estado de la base de datos local MMDB de IPinfo.
 *
 * @param loaded       {@code true} si la base de datos está cargada en memoria y lista para consultas.
 * @param path         Ruta absoluta donde se localiza el archivo MMDB en el disco.
 * @param sizeBytes    Tamaño físico del archivo MMDB en bytes.
 * @param lastModified Marca de tiempo de la última modificación física del archivo MMDB en el disco.
 * @param loadedAt     Momento exacto en que la base de datos se cargó en la memoria del microservicio.
 * @param databaseType Tipo de base de datos reportada por los metadatos de MaxMind.
 * @param buildEpoch   Epoch de compilación/generación original de la base de datos de IPinfo.
 */
public record DatabaseStatusResponse(
        boolean loaded,
        String path,
        long sizeBytes,
        Instant lastModified,
        Instant loadedAt,
        String databaseType,
        String buildEpoch
) {
}
