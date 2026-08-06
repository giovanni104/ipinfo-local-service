package com.giovanni.ipinfo.model;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

/**
 * Modelo de datos interno que representa la estructura de un registro en la base de datos de IPinfo Lite.
 * Mapea los campos correspondientes del archivo MMDB utilizando el constructor anotado.
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
     * Construye un registro de datos mapeado de la base de datos MaxMind/IPinfo.
     *
     * @param country nombre del país de la IP.
     * @param countryCode código ISO de dos letras del país.
     * @param continent nombre del continente de la IP.
     * @param continentCode código de dos letras del continente.
     * @param asn número del sistema autónomo (ej. AS15169).
     * @param asName nombre del sistema autónomo (ej. Google LLC).
     * @param asDomain dominio de red de la organización del sistema autónomo.
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

    /**
     * Obtiene el nombre del país.
     *
     * @return país en formato de cadena.
     */
    public String country() { return country; }

    /**
     * Obtiene el código ISO de dos letras del país.
     *
     * @return código de país.
     */
    public String countryCode() { return countryCode; }

    /**
     * Obtiene el nombre del continente.
     *
     * @return continente en formato de cadena.
     */
    public String continent() { return continent; }

    /**
     * Obtiene el código de dos letras del continente.
     *
     * @return código de continente.
     */
    public String continentCode() { return continentCode; }

    /**
     * Obtiene el identificador del Sistema Autónomo (ASN).
     *
     * @return ASN de la IP.
     */
    public String asn() { return asn; }

    /**
     * Obtiene el nombre de la organización operadora del ASN.
     *
     * @return nombre del operador.
     */
    public String asName() { return asName; }

    /**
     * Obtiene el dominio web de la organización del ASN.
     *
     * @return dominio del operador.
     */
    public String asDomain() { return asDomain; }
}
