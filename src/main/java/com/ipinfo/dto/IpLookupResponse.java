package com.ipinfo.dto;

import java.time.Instant;

/**
 * DTO que contiene la información de respuesta para la geolocalización de una IP.
 *
 * @param ip                 Dirección IP consultada.
 * @param country            Nombre completo del país.
 * @param countryCode        Código de dos letras del país (ISO-3166).
 * @param continent          Nombre del continente.
 * @param continentCode      Código del continente (ej. NA, SA, EU).
 * @param asn                Número de Sistema Autónomo (ej. AS15169).
 * @param organization       Nombre de la organización asociada al ASN (ej. Google LLC).
 * @param organizationDomain Dominio de la organización asociada al ASN (ej. google.com).
 * @param databaseLoadedAt   Momento en el que se cargó la base de datos MMDB local en memoria.
 * @param dataSource         Fuente de datos utilizada para resolver la IP (ej. IPinfo Lite local MMDB).
 */
public record IpLookupResponse(
	    String ip,
	    String asn,
	    String as_name,
	    String as_domain,
	    String country_code,
	    String country,
	    String continent_code,
	    String continent
) {
}
