package com.giovanni.ipinfo.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Propiedades de configuración de IPinfo mapeadas desde el archivo de configuración {@code application.yml}
 * o variables de entorno correspondientes con prefijo {@code ipinfo}.
 * Incluye validaciones básicas para garantizar la integridad de los parámetros requeridos.
 *
 * @param token                            Token de acceso para la API de descarga de IPinfo.
 * @param databasePath                     Ruta del sistema de archivos donde se almacenará el archivo MMDB local.
 * @param downloadUrl                      URL base utilizada para descargar la base de datos IPinfo Lite.
 * @param updateCron                       Expresión Cron que define la periodicidad de actualización de la base de datos.
 * @param updateZone                       Zona horaria asociada a la ejecución de la expresión cron.
 * @param connectTimeout                   Límite de tiempo para establecer la conexión HTTP en la descarga.
 * @param requestTimeout                   Límite de tiempo para completar la petición HTTP de la descarga.
 * @param minimumFileSizeBytes            Tamaño mínimo esperado en bytes del archivo descargado para considerarlo válido.
 * @param downloadOnStartupWhenMissing     Indica si se debe descargar la base de datos en el arranque si no existe localmente.
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
