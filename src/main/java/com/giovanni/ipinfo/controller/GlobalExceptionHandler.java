package com.giovanni.ipinfo.controller;

import com.giovanni.ipinfo.exception.DatabaseUnavailableException;
import com.giovanni.ipinfo.exception.IpNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Manejador global de excepciones del microservicio.
 * Captura excepciones específicas y las transforma en una respuesta uniforme
 * formateada según el estándar RFC 7807 ({@link ProblemDetail}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Constructor por defecto para el manejador global de excepciones.
     */
    public GlobalExceptionHandler() {
    }

    /**
     * Captura excepciones de argumentos no válidos (por ejemplo, IP inválida o privada).
     *
     * @param ex la excepción capturada.
     * @return respuesta HTTP 400 (Bad Request) con el detalle del error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalidIp(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage());
    }

    /**
     * Captura excepciones cuando una IP no se encuentra en el archivo local de base de datos.
     *
     * @param ex la excepción capturada.
     * @return respuesta HTTP 404 (Not Found) con el detalle del error.
     */
    @ExceptionHandler(IpNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(IpNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "IP no encontrada", ex.getMessage());
    }

    /**
     * Captura excepciones cuando la base de datos MMDB local no se encuentra disponible o no se puede cargar.
     *
     * @param ex la excepción capturada.
     * @return respuesta HTTP 503 (Service Unavailable) con el detalle del error.
     */
    @ExceptionHandler(DatabaseUnavailableException.class)
    public ResponseEntity<ProblemDetail> unavailable(DatabaseUnavailableException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Base IPinfo no disponible", ex.getMessage());
    }

    /**
     * Construye un objeto {@link ProblemDetail} estandarizado.
     *
     * @param status el código HTTP de respuesta.
     * @param title el título corto del error.
     * @param detail el detalle descriptivo del error.
     * @return {@link ResponseEntity} envolviendo el {@link ProblemDetail}.
     */
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(problem);
    }
}
