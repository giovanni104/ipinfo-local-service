package com.ipinfo.service;

import com.ipinfo.dto.SendMailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Service
public class MailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MailNotificationService.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String mailUrl; 
    private final String to;
    private final String subject;

    public MailNotificationService(
            RestClient.Builder restClientBuilder,
            @Value("${ipinfo.mail-notification.enabled}") boolean enabled,
            @Value("${ipinfo.mail-notification.url}") String mailUrl,             
            @Value("${ipinfo.mail-notification.to}") String to,
            @Value("${ipinfo.mail-notification.subject}") String subject
    ) {
        this.restClient = restClientBuilder.build();
        this.enabled = enabled;
        this.mailUrl = mailUrl;        
        this.to = to;
        this.subject = subject;
    }

    public void notificarErrorActualizacion(Throwable error) {
        if (!enabled) {
            log.info("Notificación por correo deshabilitada por configuración");
            return;
        }

        try {
            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String mensajeError = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();

            String htmlBody = String.format("""
                <div style="font-family: Arial, sans-serif; color: #333;">
                    <h2 style="color: #d9534f;">Alerta: Fallo en Actualización de Base de Datos IPinfo</h2>
                    <p>Se presentó un error durante la ejecución del proceso <b>updateNow()</b>.</p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 600px;">
                        <tr><td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Fecha:</td><td style="padding: 8px; border: 1px solid #ddd;">%s</td></tr>
                        <tr><td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Error:</td><td style="padding: 8px; border: 1px solid #ddd; color: #d9534f;">%s</td></tr>
                        <tr><td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Causa:</td><td style="padding: 8px; border: 1px solid #ddd;">%s</td></tr>
                    </table>
                    <p style="margin-top: 15px; font-size: 12px; color: #777;">Este es un correo automático generado por ipinfo-local-service.</p>
                </div>
                """,
                fecha,
                mensajeError,
                error.getCause() != null ? error.getCause().toString() : "N/A"
            );

            SendMailDTO mailDto = new SendMailDTO(
                     
                    to,
                    subject,
                    htmlBody,
                    Collections.emptyList()
            );

            restClient.post()
                    .uri(mailUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mailDto)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notificación de error enviada exitosamente a {}", to);

        } catch (Exception ex) {
            // Se registra el fallo del correo SIN lanzar excepción para no tapar el error original de la BD
            log.error("No fue posible enviar la notificación de correo de error: {}", ex.getMessage(), ex);
        }
    }
}
