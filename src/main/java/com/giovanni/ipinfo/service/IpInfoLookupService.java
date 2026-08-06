package com.giovanni.ipinfo.service;

import com.giovanni.ipinfo.dto.IpLookupResponse;
import com.giovanni.ipinfo.exception.IpNotFoundException;
import com.giovanni.ipinfo.model.IpInfoLiteRecord;
import com.giovanni.ipinfo.validation.IpAddressValidator;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

/**
 * Servicio encargado de orquestar la validación y consulta de geolocalización de IPs.
 */
@Service
public class IpInfoLookupService {

    private final IpInfoDatabaseManager databaseManager;

    /**
     * Construye un servicio de búsqueda de IP.
     *
     * @param databaseManager administrador de base de datos a consultar.
     */
    public IpInfoLookupService(IpInfoDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Valida sintácticamente la IP, asegura que sea pública y consulta sus datos
     * geográficos y de red en la base de datos MMDB cargada localmente.
     *
     * @param ip dirección IP en formato de texto literal (IPv4 o IPv6).
     * @return {@link IpLookupResponse} que encapsula la geolocalización y los metadatos de carga.
     * @throws IllegalArgumentException si la dirección IP no es válida o no es pública (ej. privada, local, loopback).
     * @throws IpNotFoundException      si la IP no se encuentra registrada en la base de datos.
     */
    public IpLookupResponse lookup(String ip) {
        InetAddress address = IpAddressValidator.parseLiteral(ip);
        if (IpAddressValidator.isNonPublic(address)) {
            throw new IllegalArgumentException("La IP es privada, local, CGNAT o no enrutable públicamente");
        }

        IpInfoLiteRecord record = databaseManager.lookup(address)
                .orElseThrow(() -> new IpNotFoundException("La IP no fue encontrada en la base IPinfo Lite"));

        return new IpLookupResponse(
                address.getHostAddress(),
                record.country(),
                record.countryCode(),
                record.continent(),
                record.continentCode(),
                record.asn(),
                record.asName(),
                record.asDomain(),
                databaseManager.loadedAt(),
                "IPinfo Lite local MMDB"
        );
    }
}
