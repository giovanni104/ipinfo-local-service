package com.giovanni.ipinfo.exception;

/**
 * Excepción lanzada cuando la base de datos MMDB local de IPinfo no se encuentra disponible,
 * no está cargada en memoria, o presenta un error al intentar acceder a sus registros.
 */
public final class DatabaseUnavailableException extends RuntimeException {

    /**
     * Construye una nueva excepción con un mensaje detallado del error.
     *
     * @param message mensaje que explica la causa de la no disponibilidad.
     */
    public DatabaseUnavailableException(String message) {
        super(message);
    }

    /**
     * Construye una nueva excepción con un mensaje detallado y el error raíz.
     *
     * @param message mensaje descriptivo.
     * @param cause la causa raíz de la excepción.
     */
    public DatabaseUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
