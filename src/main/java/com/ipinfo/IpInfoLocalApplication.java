package com.ipinfo;

import com.ipinfo.config.IpInfoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de inicio del microservicio IPinfo Local Service.
 *
 * Este servicio permite realizar consultas de geolocalización y ASN de direcciones IP
 * de forma 100% local, utilizando la base de datos MMDB (MaxMind DB) provista por IPinfo Lite.
 *
 * Configuraciones habilitadas:
 * - @EnableScheduling: Habilita las tareas programadas (actualización periódica de la base MMDB).
 * - @EnableConfigurationProperties: Carga y valida las propiedades IpInfoProperties.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(IpInfoProperties.class)
public class IpInfoLocalApplication {

    /**
     * Punto de entrada principal para el inicio de la aplicación Spring Boot.
     *
     * @param args argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        SpringApplication.run(IpInfoLocalApplication.class, args);
    }
}
