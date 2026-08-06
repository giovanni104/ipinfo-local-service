package com.giovanni.ipinfo;

import com.giovanni.ipinfo.config.IpInfoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de inicio para la aplicación IPinfo Local Service.
 * Habilita la configuración automática de Spring Boot, el programador de tareas
 * para la actualización periódica y el mapeo de propiedades del servicio.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(IpInfoProperties.class)
public class IpInfoLocalApplication {

    /**
     * Constructor por defecto para la clase de arranque de la aplicación.
     */
    public IpInfoLocalApplication() {
    }

    /**
     * Punto de entrada principal de la aplicación.
     *
     * @param args argumentos de línea de comandos pasados al iniciar el servicio.
     */
    public static void main(String[] args) {
        SpringApplication.run(IpInfoLocalApplication.class, args);
    }
}
