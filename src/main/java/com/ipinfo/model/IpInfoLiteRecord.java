package com.ipinfo.model;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

/**
 * Modelo de datos inmutable que mapea la estructura binaria de un registro en la base de datos MMDB
 * de IPinfo Lite mediante las anotaciones del SDK de MaxMind DB.
 *
 * Cada instancia representa la información geográfica y de red asociada a una IP o rango CIDR.
 */
public final class IpInfoLiteRecord {

    private final String country;
    private final String countryCode;
    private final String continent;
    private final String continentCode;
    private final String asn;
    private final String asName;
    private final String asDomain;

    /**
     * Constructor utilizado por el deserializador de MaxMind DB para instanciar el registro extrayendo las claves del MMDB.
     *
     * @param country       Nombre del país (clave country).
     * @param countryCode   Código ISO del país (clave country_code).
     * @param continent     Nombre del continente (clave continent).
     * @param continentCode Código del continente (clave continent_code).
     * @param asn           Número de Sistema Autónomo (clave asn).
     * @param asName        Nombre de la organización o titular del ASN (clave as_name).
     * @param asDomain      Dominio del titular del ASN (clave as_domain).
     */
    @MaxMindDbConstructor
    public IpInfoLiteRecord(
            @MaxMindDbParameter(name = "country") String country,
            @MaxMindDbParameter(name = "country_code") String countryCode,
            @MaxMindDbParameter(name = "continent") String continent,
            @MaxMindDbParameter(name = "continent_code") String continentCode,
            @MaxMindDbParameter(name = "asn") String asn,
            @MaxMindDbParameter(name = "as_name") String asName,
            @MaxMindDbParameter(name = "as_domain") String asDomain
    ) {
        this.country = country;
        this.countryCode = countryCode;
        this.continent = continent;
        this.continentCode = continentCode;
        this.asn = asn;
        this.asName = asName;
        this.asDomain = asDomain;
    }

    /** @return Nombre completo del país. */
    public String country() { return country; }

    /** @return Código de dos letras del país (ISO-3166). */
    public String countryCode() { return countryCode; }

    /** @return Nombre del continente. */
    public String continent() { return continent; }

    /** @return Código del continente (ej. NA, SA, EU). */
    public String continentCode() { return continentCode; }

    /** @return Identificador del Sistema Autónomo (ej. AS15169). */
    public String asn() { return asn; }

    /** @return Nombre de la organización dueña del ASN (ej. Google LLC). */
    public String asName() { return asName; }

    /** @return Dominio de la organización dueña del ASN (ej. google.com). */
    public String asDomain() { return asDomain; }
}
