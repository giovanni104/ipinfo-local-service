package com.ipinfo.controller;

import com.ipinfo.dto.IpLookupResponse;
import com.ipinfo.service.IpInfoLookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la consulta pública de geolocalización y ASN de direcciones IP.
 *
 * Expone endpoints bajo el prefijo /api/v1/ips.
 */
@RestController
@RequestMapping("/api/v1/ips")
public class IpLookupController {

    private final IpInfoLookupService lookupService;

    /**
     * Construye el controlador inyectando el servicio de búsqueda de IP.
     *
     * @param lookupService servicio que contiene la lógica de validación y resolución de IPs.
     */
    public IpLookupController(IpInfoLookupService lookupService) {
        this.lookupService = lookupService;
    }

    /**
     * Consulta y retorna los datos de geolocalización y red (país, continente, ASN y organización)
     * para una dirección IP específica.
     *
     * @param ip dirección IPv4 o IPv6 literal pública (no se admiten dominios ni IPs privadas/locales).
     * @return ResponseEntity con el objeto IpLookupResponse y código HTTP 200 OK.
     */
    @GetMapping("/{ip}")
    public ResponseEntity<IpLookupResponse> lookup(@PathVariable String ip) {
        return ResponseEntity.ok(lookupService.lookup(ip));
    }
}
