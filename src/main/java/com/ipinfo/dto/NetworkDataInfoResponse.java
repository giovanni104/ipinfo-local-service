package com.ipinfo.dto;
 

public record NetworkDataInfoResponse (
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
