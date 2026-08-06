package com.giovanni.ipinfo.service;

import com.giovanni.ipinfo.validation.IpAddressValidator;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpAddressValidatorTest {

    @Test
    void shouldAcceptIpv4Literal() {
        InetAddress address = IpAddressValidator.parseLiteral("8.8.8.8");
        assertThat(address.getHostAddress()).isEqualTo("8.8.8.8");
    }

    @Test
    void shouldRejectHostname() {
        assertThatThrownBy(() -> IpAddressValidator.parseLiteral("example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldIdentifyPrivateAndCgnatAddresses() {
        assertThat(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("192.168.1.10"))).isTrue();
        assertThat(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("100.64.0.1"))).isTrue();
        assertThat(IpAddressValidator.isNonPublic(IpAddressValidator.parseLiteral("8.8.8.8"))).isFalse();
    }
}
