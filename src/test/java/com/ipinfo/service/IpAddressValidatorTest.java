package com.ipinfo.service;

import com.ipinfo.validation.IpAddressValidator;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para la clase utilitaria IpAddressValidator.
 *
 * Valida el análisis de cadenas literales a direcciones IP (IPv4 e IPv6)
 * y la detección de direcciones no enrutables públicamente (privadas, loopback, CGNAT, etc.).
 */
class IpAddressValidatorTest {

    /**
     * Verifica que se acepten direcciones IPv4 válidas en formato literal.
     */
    @Test
    void parseLiteral_acceptsValidIpv4() {
        InetAddress address = assertDoesNotThrow(() -> IpAddressValidator.parseLiteral("8.8.8.8"));
        assertEquals("8.8.8.8", address.getHostAddress());
    }

    /**
     * Verifica que se acepten direcciones IPv6 válidas en formato literal.
     */
    @Test
    void parseLiteral_acceptsValidIpv6() {
        InetAddress address = assertDoesNotThrow(() -> IpAddressValidator.parseLiteral("2001:4860:4860::8888"));
        assertEquals("2001:4860:4860:0:0:0:0:8888", address.getHostAddress());
    }

    /**
     * Verifica que se rechacen nombres de host/dominio para prevenir resolución DNS no deseada.
     */
    @Test
    void parseLiteral_rejectsHostnames() {
        assertThrows(IllegalArgumentException.class, () -> IpAddressValidator.parseLiteral("google.com"));
    }

    /**
     * Verifica que se rechacen cadenas con formatos inválidos, valores nulos o cadenas vacías/en blanco.
     */
    @Test
    void parseLiteral_rejectsInvalidFormats() {
        assertThrows(IllegalArgumentException.class, () -> IpAddressValidator.parseLiteral("999.999.999.999"));
        assertThrows(IllegalArgumentException.class, () -> IpAddressValidator.parseLiteral(""));
        assertThrows(IllegalArgumentException.class, () -> IpAddressValidator.parseLiteral("   "));
        assertThrows(IllegalArgumentException.class, () -> IpAddressValidator.parseLiteral(null));
    }

    /**
     * Verifica la correcta clasificación de rangos privados, loopback, link-local, CGNAT (100.64.0.0/10)
     * e IPs públicas.
     */
    @Test
    void isNonPublic_identifiesPrivateRanges() {
        assertTrue(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("127.0.0.1")));
        assertTrue(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("10.0.0.1")));
        assertTrue(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("192.168.1.1")));
        assertTrue(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("172.16.0.1")));
        assertTrue(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("100.64.0.1")));
        assertTrue(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("::1")));
        assertTrue(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("fe80::1")));

        assertFalse(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("8.8.8.8")));
        assertFalse(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("1.1.1.1")));
        assertFalse(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("2001:4860:4860::8888")));
    }
}
