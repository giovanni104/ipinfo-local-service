package com.giovanni.ipinfo.exception;

/**
 * Excepción lanzada cuando una dirección IP válida es consultada pero no
 * se encuentra en los registros de la base de datos MMDB local de IPinfo Lite.
 */
public final class IpNotFoundException extends RuntimeException {

    /**
     * Construye una nueva excepción indicando que la IP no fue encontrada.
     *
     * @param message mensaje descriptivo.
     */
    public IpNotFoundException(String message) {
        super(message);
    }
}
