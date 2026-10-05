package com.ipinfo.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Propiedades de configuración mapeadas desde el archivo application.yml
 * bajo el prefijo "ipinfo".
 *
 * Incluye validaciones mediante Jakarta Validation para asegurar que los parámetros
 * requeridos (como el token o la ruta de almacenamiento) estén definidos correctamente al arrancar.
 *
 * @param token                          Token de acceso a la API de IPinfo para realizar la descarga de la base MMDB.
 * @param databasePath                   Ruta local del sistema de archivos donde se almacena y lee el archivo .mmdb.
 * @param downloadUrl                    URL base oficial de descarga del archivo MMDB de IPinfo.
 * @param updateCron                     Expresión Cron que define la frecuencia de actualización automática de la base.
 * @param updateZone                     Zona horaria aplicable para la expresión Cron (ej. America/Bogota).
 * @param connectTimeout                 Tiempo máximo de espera para establecer la conexión HTTP con la API de descarga.
 * @param requestTimeout                 Tiempo máximo de espera total para completar la descarga del archivo.
 * @param minimumFileSizeBytes           Tamaño mínimo en bytes para considerar válida la base descargada (evita archivos corruptos o respuestas de error).
 * @param downloadOnStartupWhenMissing   Indica si el servicio debe descargar automáticamente la base de datos si no existe al arrancar.
 */
@Validated
@ConfigurationProperties(prefix = "ipinfo")
public record IpInfoProperties(
        @NotBlank String token,
        @NotNull Path databasePath,
        @NotBlank String downloadUrl,
        @NotBlank String updateCron,
        @NotBlank String updateZone,
        @NotNull Duration connectTimeout,
        @NotNull Duration requestTimeout,
        @Positive long minimumFileSizeBytes,
        boolean downloadOnStartupWhenMissing
) {
}
