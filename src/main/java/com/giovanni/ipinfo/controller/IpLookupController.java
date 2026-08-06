package com.giovanni.ipinfo.controller;

import com.giovanni.ipinfo.dto.IpLookupResponse;
import com.giovanni.ipinfo.service.IpInfoLookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST público que expone la API de consulta de geolocalización de IPs.
 */
@RestController
@RequestMapping("/api/v1/ips")
public class IpLookupController {

    private final IpInfoLookupService lookupService;

    /**
     * Construye una instancia de {@link IpLookupController}.
     *
     * @param lookupService servicio de geolocalización de direcciones IP.
     */
    public IpLookupController(IpInfoLookupService lookupService) {
        this.lookupService = lookupService;
    }

    /**
     * Endpoint público para consultar la geolocalización y ASN de una dirección IP (IPv4 o IPv6).
     *
     * @param ip dirección IP en formato de cadena literal.
     * @return {@link ResponseEntity} que contiene {@link IpLookupResponse} con los datos de geolocalización.
     */
    @GetMapping("/{ip}")
    public ResponseEntity<IpLookupResponse> lookup(@PathVariable String ip) {
        return ResponseEntity.ok(lookupService.lookup(ip));
    }
}
