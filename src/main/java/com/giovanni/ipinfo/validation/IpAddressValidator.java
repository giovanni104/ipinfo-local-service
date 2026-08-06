package com.giovanni.ipinfo.validation;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Clase utilitaria para la validación de sintaxis y tipología de direcciones IP (IPv4 e IPv6).
 */
public final class IpAddressValidator {

    private IpAddressValidator() {
    }

    /**
     * Convierte una cadena de texto en un objeto {@link InetAddress} si representa una dirección IP literal válida.
     * Rechaza explícitamente nombres de host o nombres de dominio.
     *
     * @param value cadena de texto con la IP a evaluar.
     * @return un objeto {@link InetAddress} correspondiente a la dirección IP suministrada.
     * @throws IllegalArgumentException si la dirección IP está vacía, es un nombre de dominio o tiene un formato incorrecto.
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
     * Determina si una IP no es pública (ej. local, privada, loopback, multicast o dentro del rango CGNAT).
     *
     * @param address la dirección IP en formato {@link InetAddress} a evaluar.
     * @return {@code true} si la IP no es pública; {@code false} en caso contrario.
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
     * Verifica superficialmente si el formato de la cadena parece ser una IPv4 o IPv6 literal.
     *
     * @param value cadena a evaluar.
     * @return {@code true} si contiene ":" (IPv6) o cumple el patrón básico de IPv4.
     */
    private static boolean looksLikeIpLiteral(String value) {
        return value.contains(":") || value.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
    }

    /**
     * Verifica si una IP pertenece al bloque Carrier-Grade NAT (CGNAT) `100.64.0.0/10`.
     *
     * @param address la dirección IP a evaluar.
     * @return {@code true} si pertenece a CGNAT; {@code false} en caso contrario.
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
