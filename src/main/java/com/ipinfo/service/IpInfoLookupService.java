package com.ipinfo.service;

import com.ipinfo.dto.IpLookupResponse;
import com.ipinfo.exception.IpNotFoundException;
import com.ipinfo.model.IpInfoLiteRecord;
import com.ipinfo.validation.IpAddressValidator;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

/**
 * Servicio de negocio principal para la resolución de direcciones IP.
 *
 * Coordina:
 * 1. La validación y parseo sintáctico de la IP mediante IpAddressValidator.
 * 2. El descarte de direcciones privadas/no públicas.
 * 3. La consulta en memoria contra la base MMDB mediante IpInfoDatabaseManager.
 * 4. La transformación del resultado al DTO IpLookupResponse.
 */
@Service
public class IpInfoLookupService {

    private final IpInfoDatabaseManager databaseManager;

    /**
     * Construye el servicio inyectando el gestor de la base de datos MMDB.
     *
     * @param databaseManager componente encargado de realizar búsquedas en el lector MMDB en memoria.
     */
    public IpInfoLookupService(IpInfoDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Resuelve los datos de geolocalización y proveedor ASN para una IP dada.
     *
     * @param ip cadena con la IP (IPv4 o IPv6).
     * @return IpLookupResponse con la información geográfica y de red resuelta.
     * @throws IllegalArgumentException si la IP no es válida o pertenece a un rango privado/local/CGNAT.
     * @throws IpNotFoundException si la IP es pública pero no tiene registro en la base de datos IPinfo Lite.
     * @throws com.ipinfo.exception.DatabaseUnavailableException si la base MMDB no está cargada en memoria.
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
        		 record.asn(),
        		 record.asName(),
        		 record.asDomain(),
        		 record.countryCode(),
        		 record.country(),
        		 record.continentCode(),
        		 record.continent()
        );
        
     
        
        
        
        
        
        
        
        
        
        
        
        
    }
}
