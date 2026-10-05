package com.ipinfo.controller;

import com.ipinfo.exception.DatabaseUnavailableException;
import com.ipinfo.exception.IpNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Manejador global de excepciones para los controladores REST de la aplicación.
 *
 * Transforma las excepciones lanzadas en el flujo de negocio en respuestas estandarizadas
 * bajo la especificación RFC 7807 (ProblemDetail).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja errores de argumentos inválidos (ej. IP con formato incorrecto, dominios, IPs privadas).
     *
     * @param ex excepción capturada.
     * @return respuesta con código HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Solicitud no válida");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Maneja la situación donde una IP no está presente en los registros del MMDB de IPinfo Lite.
     *
     * @param ex excepción capturada.
     * @return respuesta con código HTTP 404 Not Found.
     */
    @ExceptionHandler(IpNotFoundException.class)
    public ProblemDetail handleNotFound(IpNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("IP no encontrada");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Maneja errores cuando la base de datos MMDB no está cargada o no se encuentra disponible.
     *
     * @param ex excepción capturada.
     * @return respuesta con código HTTP 503 Service Unavailable.
     */
    @ExceptionHandler(DatabaseUnavailableException.class)
    public ProblemDetail handleDatabaseUnavailable(DatabaseUnavailableException ex) {
        log.error("Error relacionado con la base IPinfo: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Base de datos no disponible");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Maneja cualquier otra excepción no controlada en el sistema.
     *
     * @param ex excepción capturada.
     * @return respuesta con código HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Error inesperado procesando la solicitud", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado al procesar la solicitud"
        );
        problem.setTitle("Error interno del servidor");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
