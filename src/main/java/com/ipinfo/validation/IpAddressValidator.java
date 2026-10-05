package com.ipinfo.validation;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Clase utilitaria para la validación y análisis sintáctico de direcciones IP.
 *
 * Se encarga de:
 * - Validar que la entrada sea una dirección IP literal válida (IPv4 o IPv6), rechazando nombres de host/dominio.
 * - Identificar y filtrar direcciones IP privadas, de loopback, enlace local, multicast o rangos CGNAT (Carrier-Grade NAT).
 */
public final class IpAddressValidator {

    private IpAddressValidator() {
        // Constructor privado para evitar instanciación
    }

    /**
     * Analiza una cadena de texto y la convierte en un objeto InetAddress válido.
     *
     * Rechaza cadenas vacías, nombres de dominio (para evitar consultas DNS innecesarias o inseguras)
     * y formatos numéricos que no representen una IPv4 o IPv6 real.
     *
     * @param value cadena con la dirección IP a evaluar.
     * @return instancia de InetAddress correspondiente.
     * @throws IllegalArgumentException si la IP es nula, vacía, representa un dominio o tiene formato inválido.
     */
    public static InetAddress parseLiteral(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La dirección IP es obligatoria");
        }

        String ip = value.trim();
        if (!looksLikeIpLiteral(ip)) {
            throw new IllegalArgumentException("No se permiten nombres de dominio; envía una IPv4 o IPv6 literal");
        }

        try {
            InetAddress address = InetAddress.getByName(ip);
            if (!(address instanceof Inet4Address) && !(address instanceof Inet6Address)) {
                throw new IllegalArgumentException("La dirección IP no es válida");
            }
            return address;
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("La dirección IP no es válida: " + value, ex);
        }
    }

    /**
     * Determina si una dirección IP pertenece a un rango no enrutable en la Internet pública.
     *
     * Incluye:
     * - Direcciones wildcard / any local (0.0.0.0, ::).
     * - Direcciones de loopback (127.0.0.0/8, ::1).
     * - Rangos privados RFC 1918 (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16).
     * - Enlace local / Link-local (169.254.0.0/16, fe80::/10).
     * - Multicast (224.0.0.0/4, ff00::/8).
     * - Carrier-Grade NAT (CGNAT RFC 6598: 100.64.0.0/10).
     *
     * @param address dirección IP ya analizada.
     * @return true si la IP no es pública; false si es una IP pública enrutable.
     */
    public static boolean isNonPublic(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()
                || isCarrierGradeNat(address);
    }

    /**
     * Comprueba mediante patrones básicos si la cadena tiene estructura de IPv4 (x.x.x.x) o IPv6 (:).
     */
    private static boolean looksLikeIpLiteral(String value) {
        return value.contains(":") || value.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
    }

    /**
     * Verifica si una dirección IPv4 pertenece al bloque Carrier-Grade NAT (100.64.0.0/10, rango 100.64.0.0 a 100.127.255.255).
     */
    private static boolean isCarrierGradeNat(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length != 4) {
            return false;
        }
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        return first == 100 && second >= 64 && second <= 127;
    }
}
